package com.supos.uns.service.exportimport.json;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.common.Constants;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.service.exportimport.core.DashboardExportContext;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 13:19
 */
@Slf4j
public class DashboardDataExporter {
    public String exportData(DashboardExportContext context) {
        try {
            String path = String.format("%s%s/%s/%s", Constants.GLOBAL_EXPORT, GlobalExportModuleEnum.DASHBOARD.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), Constants.GLOBAL_EXPORT_DASHBOARD);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            File exportJsonFile = FileUtil.touch(targetPath);
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(exportJsonFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName("data");
            jsonGenerator.writeStartArray();
            if (CollUtil.isNotEmpty(context.getDashboardPos())) {
                for (DashboardPo dashboardPo : context.getDashboardPos()) {
                    jsonGenerator.writePOJO(dashboardPo);
                }
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            log.info("dashboard export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
