package com.supos.uns.service.exportimport.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 16:38
 */
@Data
public class SourceFlowImportContext {
    public SourceFlowImportContext(String file) {
        this.file = file;
    }

    private String file;
    private int total;
    private String nodeRedHost;
    private String nodeRedPort;
    private Map<String, String> checkErrorMap = new HashMap<>(4);

    public void addError(String key, String error) {
        checkErrorMap.put(key, error);
    }

    public boolean dataEmpty() {
        return false;
    }

    public void addAllError(Map<String, String> errorMap) {
        checkErrorMap.putAll(errorMap);
    }
}
