package com.supos.uns.service.exportimport.core;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import com.supos.uns.dao.po.UnsLabelPo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: DataExporter
 * @date 2025/5/10 17:51
 */
@Slf4j
public abstract class DataExporter {

    public abstract String exportData(ExcelExportContext context);

    protected String currentTime() {
        String timezone = null;
        try {
            File timezoneFile = new File("/etc/timezone");
            if (timezoneFile.exists()) {
                timezone = FileUtil.readUtf8String(timezoneFile);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        if (timezone != null) {
            try {
                timezone = timezone.trim();
                ZoneId zoneId = ZoneId.of(timezone);

                // 获取当前时间，并根据设置的时区
                ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

                // 定义日期时间格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

                // 格式化时间
                return zonedDateTime.format(formatter);
            }catch (Exception e) {
                log.error(e.toString(), e);
                return DateTime.now().toString("yyyyMMddHHmmss");
            }

        } else {
            return DateTime.now().toString("yyyyMMddHHmmss");
        }
    }
}
