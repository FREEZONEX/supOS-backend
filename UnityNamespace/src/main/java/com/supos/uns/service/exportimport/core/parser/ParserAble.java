package com.supos.uns.service.exportimport.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.supos.uns.service.exportimport.core.ExcelImportContext;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/5/10 10:15
 */
public interface ParserAble {

    default void parseExcel(String flagNo, Map<String, Object> dataMap, ExcelImportContext context){}

    default void parseComplexJson(String flagNo, JsonNode data, ExcelImportContext context, Object parent){}
}
