package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器网关连接状态
 * @date 2025/4/8 11:06
 */
@Getter
@AllArgsConstructor
public enum CollectorGatewayStatus {

    /**已连接*/
    ONLINE("online"),

    /**未连接*/
    OFFLINE("offline");

    public final String name;
}
