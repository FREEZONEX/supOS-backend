package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MetaChangeReq
 * @date 2025/9/20 10:40
 */
@Data
public class MetaChangeReq implements Serializable {

    private Integer size;

    private Long startId;

    // (lowVersion, highVersion]
    private Long lowVersion;
    private Long highVersion;
}
