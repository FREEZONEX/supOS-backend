package com.supos.common.dto.mount.meta.gateway;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 设备元数据
 * @date 2025/9/19 11:09
 */
@Data
public class DeviceMetaDto implements Serializable {

    private Long id;

    private String code;

    private String name;
}
