package com.supos.uns.vo;

import lombok.Data;

@Data
public class UnsExportRecordVo {
    private String id;
    private long exportTime;
    private String filePath;
    private String fileName;
    private Boolean confirm;
}