package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 测点位号元数据
 * @date 2025/9/19 11:09
 */
@Data
public class TagMetaDto implements Serializable {

    private Long id;

    /**
     * code
     */
    private String code;

    /**
     * 位号名称
     */
    private String name;

    /**
     * 位号显示名称
     */
    private String displayName;

    /**
     * 位号描述
     */
    private String description;

    /**
     * 位号值类型
     */
    private Integer valueType;

    /**
     * 位号单位
     */
    private String unit;

    /**
     * 位号量程(下限-上限)，值域取闭区间，如0-100
     */
    private String range;

    /**
     * 是否持久化
     */
    private Boolean storage;

    /**
     * 所属采集器别名
     */
    private String collectorAlias;

    /**
     * 所属设备别名
     */
    private String deviceAlias;
}
