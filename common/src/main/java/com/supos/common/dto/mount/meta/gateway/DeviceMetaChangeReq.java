package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

/**
 * 设备（源点）变更请求
 * @author sunlifang
 * @version 1.0
 * @description: DeviceMetaChangeReq
 * @date 2025/9/20 10:06
 */
@Data
public class DeviceMetaChangeReq extends MetaChangeReq {

    /**
     * 采集器别名
     */
    private String gatewayAlias;
}
