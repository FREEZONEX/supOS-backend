package com.supos.uns.service.exportimport.json;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.common.Constants;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.common.utils.FileUtils;
import com.supos.uns.service.exportimport.core.EventFlowExportContext;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 13:19
 */
@Slf4j
public class EventFlowExporter {
    public String exportData(EventFlowExportContext context) {
        try {
            String path = String.format("%s%s/%s/%s", Constants.GLOBAL_EXPORT, GlobalExportModuleEnum.EVENT_FLOW.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), Constants.GLOBAL_EXPORT_EVENT_FLOW);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            File exportJsonFile = FileUtil.touch(targetPath);
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(exportJsonFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writePOJO(context.getReqDto());
/*            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName("flows");
            jsonGenerator.writeStartArray();
            if (CollUtil.isNotEmpty(context.getFlows())) {
                for (NodeFlowPO flow : context.getFlows()) {
                    jsonGenerator.writePOJO(flow);
                }
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeFieldName("flowModels");
            jsonGenerator.writeStartArray();
            if (CollUtil.isNotEmpty(context.getFlowModels())) {
                for (NodeFlowModelPO flowModel : context.getFlowModels()) {
                    jsonGenerator.writePOJO(flowModel);
                }
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();*/
            jsonGenerator.close();
            log.info("eventFlow export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
