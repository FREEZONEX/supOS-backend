package com.supos.common.dto.mount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载详情
 * @date 2025/6/17 9:23
 */
@Data
public class MountSourceDto implements Serializable {

    /**
     * 挂载元数据别名
     */
    @Schema(description = "挂载源数据别名", example = "col1")
    private String sourceAlias;

    /**
     * 挂载元数据别名
     */
    @Schema(description = "挂载源数据名称", example = "col1")
    private String sourceName;

    /**
     *
     */
    private List<MountDeviceDto> devices;
}
