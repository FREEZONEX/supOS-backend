package com.supos.adapter.mqtt.util;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DateUtil {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    public static String dateStr(Long mills) {
        if (mills == null) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(mills);
        return instant.atOffset(ZoneOffset.ofHours(8)).format(fmt);
    }

    public static long dateToLong(Object dateStr, String format) {
        if (dateStr == null) {
            return new Date().getTime();
        }
        if (dateStr instanceof Long) {
            return (Long)dateStr;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date parse = sdf.parse(dateStr.toString());
            return parse.getTime();
        } catch (ParseException e) {
            log.error("时间格式转换错误：{}", dateStr);
        }
        return new Date().getTime();
    }

}
