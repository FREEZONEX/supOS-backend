package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

/**
 * 网关变更请求查询
 * @author sunlifang
 * @version 1.0
 * @description: CollectorMetaChangeReq
 * @date 2025/9/20 10:06
 */
@Data
public class CollectorMetaChangeReq extends MetaChangeReq {

    /**
     * 采集器别名
     */
    private String gatewayAlias;

    /**
     * 只返回有效的数据
     */
    private Boolean onlyValid;
}
