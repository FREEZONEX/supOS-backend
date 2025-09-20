package com.supos.i18n.service.excel.parser;

import com.supos.common.utils.I18nUtils;
import com.supos.i18n.service.excel.ExcelImportContext;
import com.supos.i18n.service.excel.entity.LanguageData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 语言解析器
 * @date 2025/9/4 18:48
 */
public class LanguageParser {

    public boolean parse(String flagNo, Map<String, Integer> currentHeadMap, Map<Integer, Object> data, ExcelImportContext context) {
        if (context.getLanguage() != null) {
            // 说明语言页有多行，直接中断
            context.addError(flagNo, I18nUtils.getMessage("i18n.import.language_has_more_error"));
            context.setShoutDown(true);
            return false;
        }

        LanguageData language = parseLanguage(currentHeadMap, data);
        if (language == null) {
            return true;
        }

        if (StringUtils.isBlank(language.getCode())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "code"));
            return false;
        }

        if (StringUtils.isBlank(language.getName())) {
            context.addError(flagNo, I18nUtils.getMessage("param.null", "name"));
            return false;
        }

        language.setCheckSuccess(true);
        language.setFlagNo(flagNo);
        context.setLanguage(language);
        return true;
    }

    private LanguageData parseLanguage(Map<String, Integer> currentHeadMap, Map<Integer, Object> data) {
        Object codeObj = data.get(currentHeadMap.get("code"));
        Object nameObj = data.get(currentHeadMap.get("name"));

        if (codeObj == null && nameObj == null) {
            return null;
        }

        LanguageData language = new LanguageData();
        language.setCode(codeObj == null ? null : codeObj.toString());
        language.setName(nameObj == null ? null : nameObj.toString());
        language.setCheckSuccess(false);
        return language;
    }
}
