package com.supos.i18n.vo;

import lombok.Data;

@Data
public class I18nExportRecordVO {
    private String id;
    private long exportTime;
    private String filePath;
    private String fileName;
    private Boolean confirm;
}