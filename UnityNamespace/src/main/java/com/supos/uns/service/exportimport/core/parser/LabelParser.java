package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.supos.common.Constants;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LabelParser
 * @date 2025/4/22 19:17
 */
public class LabelParser extends AbstractParser {

    private static final int LENGTH_LIMIT = 63;

    private String check(String flagNo, String label, ExcelImportContext context) {
        if (StringUtils.isBlank(label)) {
            return null;
        }
        if (StringUtils.length(label) > LENGTH_LIMIT) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.length.limit", "name", LENGTH_LIMIT));
            return null;
        }
        if (!Constants.NAME_PATTERN.matcher(label).matches()) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.formate.invalid", "name", I18nUtils.getMessage("uns.import.formate1")));
            return null;
        }
        return label;
    }

    @Override
    public void parseExcel(String flagNo, Map<String, Object> dataMap, ExcelImportContext context) {
        if (isEmptyRow(dataMap)) {
            return;
        }
        String label = getValueFromDataMap(dataMap, "name");
        label = check(flagNo, label, context);

        if (label != null) {
            context.addLabel(label);
        }
    }

    @Override
    public void parseComplexJson(String flagNo, JsonNode data, ExcelImportContext context, Object parent) {
        if (data == null) {
            return;
        }
        String label = getValueFromJsonNode(data, "name");
        label = check(flagNo, label, context);

        if (label != null) {
            context.addLabel(label);
        }
    }
}
