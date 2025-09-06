package com.supos.uns.vo;

import lombok.Data;

import java.util.List;

@Data
public class UnsExportRecordConfirmReq {
    private List<Long> ids;
}