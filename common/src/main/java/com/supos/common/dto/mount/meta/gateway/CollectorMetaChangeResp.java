package com.supos.common.dto.mount.meta.gateway;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/9/20 10:07
 */
@Data
public class CollectorMetaChangeResp implements Serializable {

    private List<CollectorMetaDto> saveCollectorMetaDtos;
    private List<CollectorMetaDto> deleteCollectorMetaDtos;

    public boolean hasChange() {
        return CollectionUtil.isNotEmpty(saveCollectorMetaDtos) || CollectionUtil.isNotEmpty(deleteCollectorMetaDtos);
    }
}
