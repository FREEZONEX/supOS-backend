package com.supos.i18n.service.excel.parser;

import com.supos.common.utils.I18nUtils;
import com.supos.i18n.dto.ResourceDto;
import com.supos.i18n.service.excel.ExcelImportContext;
import com.supos.i18n.service.excel.entity.ModuleData;
import com.supos.i18n.service.excel.entity.ResourceData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化资源解析器
 * @date 2025/9/4 18:49
 */
public class ResourceParser {

    public void parse(String flagNo, Map<String, Integer> currentHeadMap, Map<Integer, Object> data, ExcelImportContext context) {
        ResourceData resource = parseResource(currentHeadMap, data);

        // 跳过空行
        if (resource == null) {
            return;
        }
        // 校验模块code
        if (StringUtils.isBlank(resource.getModuleCode())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "moduleCode"));
            return;
        }
        // 校验key
        if (StringUtils.isBlank(resource.getKey())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "key"));
            return;
        }

        // 跳过重复key
        if (context.containsKey(resource.getModuleCode(), resource.getKey())) {
            context.addError(flagNo, I18nUtils.getMessage("i18n.exception.i18n_key_duplicate"));
            return;
        }

        // 校验模块是否保存成功
        Map<String, ModuleData> moduleMap = context.getModuleMap();
        ModuleData moduleData = moduleMap.get(resource.getModuleCode());
        if (moduleData != null && !moduleData.isCheckSuccess()) {
            context.addError(flagNo, I18nUtils.getMessage("i18n.import.module_save_error"));
            return;
        }

        resource.setCheckSuccess(true);
        resource.setFlagNo(flagNo);
        context.addResource(resource);
    }

    private ResourceData parseResource(Map<String, Integer> currentHeadMap, Map<Integer, Object> data) {
        Object moduleCodeObj = data.get(currentHeadMap.get("moduleCode"));
        Object keyObj = data.get(currentHeadMap.get("key"));
        Object valueObj = data.get(currentHeadMap.get("value"));

        if (moduleCodeObj == null && keyObj == null && valueObj == null) {
            return null;
        }

        ResourceData resource = new ResourceData();
        resource.setModuleCode(moduleCodeObj != null ? moduleCodeObj.toString() : null);
        resource.setKey(keyObj != null ? keyObj.toString() : null);
        resource.setValue(valueObj != null ? valueObj.toString() : null);
        resource.setCheckSuccess(false);
        return resource;
    }
}
