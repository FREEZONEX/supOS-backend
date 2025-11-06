package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

import java.util.Set;

/**
 * 位号元数据变更请求
 * @author sunlifang
 * @version 1.0
 * @description: TagMetaChangeReq
 * @date 2025/9/20 10:06
 */
@Data
public class TagMetaChangeReq extends MetaChangeReq {

    /**
     * 采集器别名
     */
    private String gatewayAlias;

    /**
     * 设备别名
     */
    private Set<String> deviceAliases;
}
