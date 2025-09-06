package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.ExportImportHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author sunlifang@supos.supcon.com
 * @title UnsParser
 * @description
 * @create 2025/7/21 上午11:17
 */
@Slf4j
public class UnsParser extends AbstractParser {

    private FolderParser folderParser = new FolderParser();
    private FileParser fileParser = new FileParser();

    @Override
    public void parseComplexJson(String flagNo, JsonNode data, ExcelImportContext context, Object parent) {
        JsonNode typeNode = data.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.type.error"));
            return;
        }

        String type = typeNode.textValue();
        if (!ExportImportHelper.TYPE_FOLDER.equals(type) && !ExportImportHelper.TYPE_FILE.equals(type)) {
            context.addError(flagNo, I18nUtils.getMessage("uns.import.type.error"));
            return;
        }

        switch (type) {
            case ExportImportHelper.TYPE_FOLDER:
                folderParser.parseComplexJson(flagNo, data, context, null);
                break;
            case ExportImportHelper.TYPE_FILE:
                fileParser.parseComplexJson(flagNo, data, context, null);
                break;
        }
    }
}
