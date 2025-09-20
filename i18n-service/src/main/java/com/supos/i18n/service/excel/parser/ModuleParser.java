package com.supos.i18n.service.excel.parser;

import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dto.AddModuleDto;
import com.supos.i18n.dto.ResourceDto;
import com.supos.i18n.service.excel.ExcelImportContext;
import com.supos.i18n.service.excel.entity.ModuleData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 模块解析器
 * @date 2025/9/4 18:49
 */
public class ModuleParser {

    public void parse(String flagNo, Map<String, Integer> currentHeadMap, Map<Integer, Object> data, ExcelImportContext context) {
        ModuleData module = parseModule(currentHeadMap, data);

        if (module == null) {
            return;
        }

        if (StringUtils.isBlank(module.getModuleCode())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "moduleCode"));
            return;
        }

        if (StringUtils.isBlank(module.getModuleName())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "moduleName"));
            return;
        }

        if (context.containsModuleCode(module.getModuleCode())) {
            context.addError(flagNo, I18nUtils.getMessage("i18n.exception.module_code_duplicate"));
            return;
        }

        module.setCheckSuccess(true);
        module.setFlagNo(flagNo);

        context.addModule(module);
    }

    private ModuleData parseModule(Map<String, Integer> currentHeadMap, Map<Integer, Object> data) {
        Object moduleCodeObj = data.get(currentHeadMap.get("moduleCode"));
        Object moduleNameObj = data.get(currentHeadMap.get("moduleName"));

        if (moduleCodeObj == null && moduleNameObj == null) {
            return null;
        }

        ModuleData module = new ModuleData();
        module.setModuleCode(moduleCodeObj != null ? moduleCodeObj.toString() : null);
        module.setModuleName(moduleNameObj != null ? moduleNameObj.toString() : null);
        module.setCheckSuccess(false);
        return module;
    }
}
