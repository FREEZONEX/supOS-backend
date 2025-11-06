package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器元数据
 * @date 2025/9/19 11:09
 */
@Data
public class CollectorMetaDto implements Serializable {

    private Long id;

    /**
     * 显式名称
     */
    private String displayName;

    /**
     * 别名（唯一）
     */
    private String aliasName;
}
