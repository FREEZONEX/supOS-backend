package com.supos.uns.service.exportimport.core;

import com.supos.uns.dao.po.UnsLabelPo;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: DataExporter
 * @date 2025/5/10 17:51
 */
public abstract class DataExporter {

    public abstract String exportData(ExcelExportContext context);
}
