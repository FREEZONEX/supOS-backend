package com.supos.uns.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 14:24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalExportConfig {
    private String name;

    private String supOsVersion;

    private String description;
}
