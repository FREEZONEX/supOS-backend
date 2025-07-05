package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 14:36
 */
@AllArgsConstructor
@Getter
public enum GlobalExportModuleEnum {
    UNS("uns", ""),
    SOURCE_FLOW("sourceFlow", ""),
    EVENT_FLOW("eventFlow", ""),
    DASHBOARD("dashboard", "");
    private final String code;
    private final String name;

    public boolean is(String code) {
        return this.getCode().equals(code);
    }
}
