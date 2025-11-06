package com.supos.common.dto.grafana;

import lombok.Data;

import java.util.Date;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/29 10:08
 * @description
 */
@Data
public class DashboardDto {

    private String id;

    private String name;

    /**
     * 1-grafana 2-fuxa
     */
    private Integer type;

    private String description;

    /**
     * 1表示置顶
     */
    private Integer mark;

    private String creator;

    private Date updateTime;

    private Date createTime;


}
