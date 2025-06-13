package com.supos.uns.service.exportimport.core.parser;

import com.supos.common.utils.I18nUtils;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.service.exportimport.json.data.LabelData;
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

    @Override
    public void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context) {
        if (isEmptyRow(dataMap)) {
            return;
        }
        String label = getString(dataMap, "name", "");
        if (StringUtils.isBlank(label)) {
            return;
        }
        if (StringUtils.length(label) > LENGTH_LIMIT) {
            String batchIndex = String.format("%d-%d", batch, index);
            context.addError(batchIndex, I18nUtils.getMessage("uns.label.length.limit.exceed", LENGTH_LIMIT));
            return;
        }
        context.addLabel(label);
    }

    @Override
    public void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context) {
        if (data == null) {
            return;
        }
        LabelData labelData = (LabelData) data;
        String label = labelData.getName();
        if (StringUtils.isBlank(label)) {
            return;
        }
        if (StringUtils.length(label) > LENGTH_LIMIT) {
            String batchIndex = String.format("%d-%d", batch, index);
            context.addError(batchIndex, I18nUtils.getMessage("uns.label.length.limit.exceed", LENGTH_LIMIT));
            return;
        }
        context.addLabel(label);
    }
}
