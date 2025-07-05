package com.supos.uns.service.exportimport.json;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.FuxaUtils;
import com.supos.common.utils.GrafanaUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.DashboardMapper;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.service.exportimport.core.DashboardImportContext;
import com.supos.uns.service.exportimport.json.data.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.*;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 15:54
 */
@Slf4j
public class DashboardDataImporter {
    public DashboardDataImporter(DashboardImportContext context, DashboardMapper dashboardMapper) {
        this.context = context;
        this.dashboardMapper = dashboardMapper;
    }

    private DashboardImportContext context;
    private DashboardMapper dashboardMapper;
    private DashboardJsonWrapper dashboardJsonWrapper;
    @Getter
    private StopWatch stopWatch = new StopWatch();

    public void importData(File file) {
        try {
            JsonMapper jsonMapper = new JsonMapper();
            dashboardJsonWrapper = jsonMapper.readValue(file, DashboardJsonWrapper.class);
        } catch (Exception e) {
            log.error("解析json文件失败", e);
            throw new BuzException("dashboard.import.json.error");
        }
        try {
            handleImportData(dashboardJsonWrapper);
            log.info("dashboard import running time:{}s", stopWatch.getTotalTimeSeconds());
            log.info(stopWatch.prettyPrint());
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new BuzException("dashboard.import.error");
        }
    }

    private void handleImportData(DashboardJsonWrapper dashboardJsonWrapper) {
        if (dashboardJsonWrapper == null || CollUtil.isEmpty(dashboardJsonWrapper.getData())) {
            return;
        }
        context.setTotal(dashboardJsonWrapper.getData().size());
        Date now = new Date();
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (DashboardPo dashboardPo : dashboardJsonWrapper.getData()) {
            dashboardPo.setCreateTime(now);
            dashboardPo.setUpdateTime(now);
            ids.add(dashboardPo.getId());
            names.add(dashboardPo.getName());
        }
        List<DashboardPo> dashboardPos = dashboardMapper.selectByIds(ids);
        Map<String, DashboardPo> existMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dashboardPos)) {
            for (DashboardPo dashboardPo : dashboardPos) {
                existMap.put(dashboardPo.getId(), dashboardPo);
            }
        }
        List<DashboardPo> dashboardPoList = dashboardMapper.selectByFlowNames(names);
        Map<String, DashboardPo> existNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dashboardPoList)) {
            for (DashboardPo dashboardPo : dashboardPoList) {
                existNameMap.put(dashboardPo.getName(), dashboardPo);
            }
        }
        List<DashboardPo> addList = new ArrayList<>();
        for (DashboardPo dashboardPo : dashboardJsonWrapper.getData()) {
            if (existMap.containsKey(dashboardPo.getId())) {
                context.addError(dashboardPo.getId(), I18nUtils.getMessage("dashboard.id.already.exists"));
            }else if(existNameMap.containsKey(dashboardPo.getName())){
                context.addError(dashboardPo.getId(), I18nUtils.getMessage("dashboard.name.already.exists"));
            } else {
                addList.add(dashboardPo);
            }
        }
        if (CollUtil.isNotEmpty(addList)) {
            dashboardMapper.insert(addList);
            for (DashboardPo dashboardPo : addList) {
                if (Objects.equals(dashboardPo.getType(), 1)) {
                    GrafanaUtils.create(dashboardPo.getJsonContent());
                } else if (Objects.equals(dashboardPo.getType(), 2)) {
                    FuxaUtils.create(dashboardPo.getJsonContent());
                }
            }
        }
    }

    public void writeError(File outFile) {
        try {
            JsonFactory factory = new JsonMapper().getFactory();
            JsonGenerator jsonGenerator = factory.createGenerator(outFile, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            // 导出模板
            jsonGenerator.writeFieldName("data");
            jsonGenerator.writeStartArray();
            List<DashboardPo> data = dashboardJsonWrapper.getData();
            if (CollectionUtils.isNotEmpty(data)) {
                for (DashboardPo dashboardPo : data) {
                    dashboardPo.setError(context.getCheckErrorMap().get(dashboardPo.getId()));
                    jsonGenerator.writePOJO(dashboardPo);
                }
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
        } catch (Exception e) {
            log.error("Dashboard导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }
}
