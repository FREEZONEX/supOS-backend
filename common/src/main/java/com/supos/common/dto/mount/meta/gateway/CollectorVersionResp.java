package com.supos.common.dto.mount.meta.gateway;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: CollectorVersionResp
 * @date 2025/9/20 10:07
 */
@Data
public class CollectorVersionResp implements Serializable {

    private Long version;
}
