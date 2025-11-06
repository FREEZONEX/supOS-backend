package com.supos.common.dto.mount;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载源
 * @date 2025/6/17 14:49
 */
@Data
public class MountDeviceDto implements Serializable {

    /**
     * 挂载元数据别名
     */
    private String alias;

    /**
     * 挂载元数据别名
     */
    private String name;

}
