package com.supos.common.dto.grafana;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/12 10:53
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrafanaDashboardParam {

    private String uid;
    private String title;
    private String dataSourceType;
    private String dataSourceUid;
    private String schema;
    private String tableName;
    private String tagNameCondition;//vqt模式  位号名称sql条件tag='{tbValue}'
    private String columns;
    private long version;
}
