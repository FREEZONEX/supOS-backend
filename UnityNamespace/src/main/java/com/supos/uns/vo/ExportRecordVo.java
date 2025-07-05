package com.supos.uns.vo;

import lombok.Data;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 10:21
 */
@Data
public class ExportRecordVo {
    private String id;
    private long exportTime;
    private String filePath;
    private String fileName;
    private Boolean confirm;
}
