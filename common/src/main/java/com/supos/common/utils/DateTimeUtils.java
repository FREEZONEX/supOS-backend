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

            // 尝试解析为 Long 时间戳
            try {
                Long.parseLong(str);
                return true;
            } catch (NumberFormatException ignored) {}

            // 2️⃣ 尝试 ISO/UTC 格式
            try {
                Instant.parse(str); // 支持 Z 结尾
                return true;
            } catch (DateTimeParseException ignored) {}

            try {
                OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME); // 支持 +08:00
                return true;
            } catch (DateTimeParseException ignored) {}

            // 3️⃣ 尝试自定义格式
            DateTimeFormatter[] dateTimeFormatters = new DateTimeFormatter[]{
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };

            for (DateTimeFormatter formatter : dateTimeFormatters) {
                try {
                    LocalDateTime.parse(str, formatter); // 带时间的格式
                    return true;
                } catch (DateTimeParseException ignored) {}
                try {
                    LocalDate.parse(str, formatter); // 只有日期的格式
                    return true;
                } catch (DateTimeParseException ignored) {}
            }

            // 4️⃣ 尝试只包含时间的格式
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            try {
                LocalTime.parse(str, timeFormatter);
                return true;
            } catch (DateTimeParseException ignored) {}
        }

        return false;
    }

    /**
     * 将入参 dateTime 转换为 UTC ISO 格式：yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     *
     * @param dateTime 入参，可以是 Long 时间戳 或 String 日期时间
     * @return UTC ISO 格式字符串
     */
    public static String toUtcIso(Object dateTime) {
        if (dateTime == null) {
            return null;
        }

        Instant instant = null;

        try {
            // 1️⃣ 如果是 Long 或可转 Long，按时间戳处理
            if (dateTime instanceof Number) {
                instant = Instant.ofEpochMilli(((Number) dateTime).longValue());
            } else if (dateTime instanceof String) {
                String str = ((String) dateTime).trim();

                // 尝试解析为 Long 时间戳
                try {
                    long timestamp = Long.parseLong(str);
                    instant = Instant.ofEpochMilli(timestamp);
                } catch (NumberFormatException ignored) {}

                // 如果不是时间戳，再尝试解析特定格式
                if (instant == null) {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    // 仅日期 YYYY-MM-DD，补 00:00:00
                    try {
                        LocalDate date = LocalDate.parse(str, dateFormatter);
                        instant = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                    } catch (DateTimeParseException ignored) {}

                    // 仅时间 HH:mm:ss，补当天日期
                    if (instant == null) {
                        try {
                            LocalTime time = LocalTime.parse(str, timeFormatter);
                            LocalDate today = LocalDate.now(ZoneOffset.UTC);
                            instant = LocalDateTime.of(today, time).toInstant(ZoneOffset.UTC);
                        } catch (DateTimeParseException ignored) {}
                    }

                    // 完整日期时间 YYYY-MM-DD HH:mm:ss，按 UTC
                    if (instant == null) {
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(str, dateTimeFormatter);
                            instant = ldt.toInstant(ZoneOffset.UTC);
                        } catch (DateTimeParseException ignored) {}
                    }
                }
            }

            // 2️⃣ 如果解析成功，格式化输出
            if (instant != null) {
                return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .withZone(ZoneOffset.UTC)
                        .format(instant);
            }

        } catch (Exception e) {
            // 其他格式原样返回
            return dateTime.toString();
        }

        // 解析失败，原样返回
        return dateTime.toString();
    }
}
