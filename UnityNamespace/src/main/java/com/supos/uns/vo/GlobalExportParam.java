package com.supos.uns.vo;

import lombok.Data;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 10:44
 */
@Data
public class GlobalExportParam {
    private String name;
    private ExportParam unsExportParam;
    private SourceFlowExportParam sourceFlowExportParam;
    private EventFlowExportParam eventFlowExportParam;
    private DashboardExportParam dashboardExportParam;
}
