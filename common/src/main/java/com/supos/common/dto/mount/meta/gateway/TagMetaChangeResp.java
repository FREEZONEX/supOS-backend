package com.supos.common.dto.mount.meta.gateway;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TagMetaChangeResp
 * @date 2025/9/20 10:07
 */
@Data
public class TagMetaChangeResp implements Serializable {

    private List<TagMetaDto> saveTagMetaDtos;

    private List<TagMetaDto> deleteTagMetaDtos;

    public boolean hasChange() {
        return CollectionUtil.isNotEmpty(saveTagMetaDtos) || CollectionUtil.isNotEmpty(deleteTagMetaDtos);
    }
}
