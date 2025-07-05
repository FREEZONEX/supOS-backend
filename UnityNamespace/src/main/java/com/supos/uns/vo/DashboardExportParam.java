package com.supos.uns.vo;

import lombok.Data;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 10:45
 */
@Data
public class DashboardExportParam {
    private List<String> ids;
    private String exportType;
}
