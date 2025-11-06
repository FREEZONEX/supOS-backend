package com.supos.common.dto.mount.meta.gateway;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 设备（源点）变更响应
 * @author sunlifang
 * @version 1.0
 * @description: DeviceMetaChangeResp
 * @date 2025/9/20 10:07
 */
@Data
public class DeviceMetaChangeResp implements Serializable {

    private List<DeviceMetaDto> saveDeviceMetaDtos;
    private List<DeviceMetaDto> deleteDeviceMetaDtos;

    public boolean hasChange() {
        return CollectionUtil.isNotEmpty(saveDeviceMetaDtos) || CollectionUtil.isNotEmpty(deleteDeviceMetaDtos);
    }
}
