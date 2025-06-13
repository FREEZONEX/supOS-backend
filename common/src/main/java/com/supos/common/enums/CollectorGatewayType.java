package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器网关类型
 * @date 2025/4/7 16:53
 */
@Getter
@AllArgsConstructor
public enum CollectorGatewayType {

    GRPC_GATEWAY;


    public static CollectorGatewayType get(String type) {
        for (CollectorGatewayType gatewayType : CollectorGatewayType.values()) {
            if (StringUtils.equals(gatewayType.toString(), type)) {
                return gatewayType;
            }
        }
        return null;
    }
}
