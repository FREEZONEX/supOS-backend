package com.supos.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class DateTimeUtils {
    static final ZoneId utcZone = Clock.systemUTC().getZone();
    static final DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter fmtMills = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter fmtSimpleSec = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String dateTimeUTC(Long mills) {
        if (mills == null || mills < 1000) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(mills);
        return instant.atZone(utcZone).format(fmt);
    }

    public static String dateMillsStr(Long mills) {
        if (mills == null) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(mills);
        return instant.atOffset(ZoneOffset.ofHours(8)).format(fmtMills);
    }

    public static String dateSimple() {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        return instant.atOffset(ZoneOffset.ofHours(8)).format(fmtSimpleSec);
    }

    public static String getDateTimeStr(Object inTime) {
        String nowStr;
        if (inTime != null) {
            long dataInTimeMirco = (Long) inTime;
            Instant instant;
            if (dataInTimeMirco > 100000000000000000L) {// 微妙
                long dataInTimeMills = dataInTimeMirco / 1000000;
                int micro = (int) (dataInTimeMirco % 1000000);
                instant = Instant.ofEpochMilli(dataInTimeMills).plus(micro, ChronoUnit.MICROS);
            } else {// 毫秒
                instant = Instant.ofEpochMilli(dataInTimeMirco);
            }
            nowStr = instant.atZone(utcZone).format(fmt);
        } else {
            nowStr = ZonedDateTime.now(utcZone).format(fmt);
        }
        return nowStr;
    }

    public static long convertToMills(long timestamp) {
        if (timestamp > 10000000000000L) {
            timestamp = Long.parseLong(String.valueOf(timestamp).substring(0, 13));
        } else if (timestamp < 1000000000000L) {
            StringBuilder sr = new StringBuilder(16).append(timestamp);
            while (sr.length() < 13) {
                sr.append('0');
            }
            timestamp = Long.parseLong(sr.toString());
        }
        return timestamp;
    }

    static final ZoneOffset defaultZone;
    static List<DateTimeFormatter> formatters;

    static {
        int hours = TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60);
        defaultZone = ZoneOffset.ofHours(hours);
        // 定义支持的多组日期格式（按优先级排序）
        formatters = Arrays.asList(
                // 带时区的格式
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS Z"),
                // 不带时区的格式
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );
    }

    public static Instant parseDate(String datetime) {
        return parseDate(datetime, defaultZone);
    }


    public static Instant parseDate(String datetime, ZoneOffset zoneOffset) {
        if (datetime == null || datetime.trim().isEmpty()) {
            return null;
        }
        if (zoneOffset == null) {
            zoneOffset = defaultZone;
        }

        for (DateTimeFormatter formatter : formatters) {
            try {
                TemporalAccessor temporal = formatter.parse(datetime);

                // 处理带时区的情况
                if (temporal.query(TemporalQueries.offset()) != null) {
                    return Instant.from(temporal);
                }

                // 处理无时区的情况（应用默认时区）
                LocalDateTime ldt = parseLocalDateTime(temporal);
                if (ldt == null) continue;

                ZonedDateTime zdt = ldt.atZone(zoneOffset);
                return zdt.toInstant();

            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个格式
            }
        }
        return null;
    }

    // 从 TemporalAccessor 提取 LocalDateTime
    private static LocalDateTime parseLocalDateTime(TemporalAccessor temporal) {
        if (temporal.isSupported(ChronoField.HOUR_OF_DAY)) {
            return LocalDateTime.from(temporal);
        } else if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
            LocalDate date = LocalDate.from(temporal);
            return date.atStartOfDay();
        }
        return null;
    }

    public static boolean isValidTime(Object value) {
        if (value == null) {
            return false;
        }

        // 1️⃣ 检查是否为时间戳（Long 或可转为 Long 的 String）
        if (value instanceof Number) {
            return true;
        }

        if (value instanceof String) {
            String str = ((String) value).trim();

            // 尝试解析为 Long
            try {
                Long.parseLong(str);
                return true;
            } catch (NumberFormatException e) {
                // 不是时间戳，继续判断是否为UTC时间字符串
            }

            // 2️⃣ 判断是否为UTC时间字符串
            try {
                // 尝试解析为 Instant (支持Z结尾)
                Instant.parse(str);
                return true;
            } catch (DateTimeParseException e1) {
                // 不是Z结尾，尝试解析为 OffsetDateTime (支持+08:00)
                try {
                    OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    return true;
                } catch (DateTimeParseException e2) {
                    // 都不行，返回 false
                }
            }
        }

        return false;
    }
}
