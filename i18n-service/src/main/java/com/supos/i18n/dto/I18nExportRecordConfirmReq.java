package com.supos.i18n.dto;

import lombok.Data;

import java.util.List;

@Data
public class I18nExportRecordConfirmReq {
    private List<Long> ids;
}