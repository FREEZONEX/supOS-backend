package com.supos.uns.service.exportimport.core.data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelData
 * @date 2025/5/8 16:23
 */
public interface ExportImportData extends Serializable {

    default void handleRefers(String refers) {}

}
