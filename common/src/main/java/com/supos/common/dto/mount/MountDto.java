package com.supos.common.dto.mount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载Dto
 * @date 2025/6/16 20:30
 */
@Data
public class MountDto implements Serializable {



    /**
     * 挂载的目标类型
     * folder    -- 文件夹
     * file    -- 文件
     */
    //@Schema(description = "挂载的目标类型：folder--文件夹，file--文件", example = "folder", pattern = "[folder|file]")
    //private String targetType;

    /**
     * 挂载的目标别名
     */
    @Schema(description = "挂载的目标别名", example = "folder1")
    private String targetAlias;

    /**
     * 挂载元数据类型
     * collector    -- 采集器
     */
    @Schema(description = "挂载元数据类型：collector--采集器，videoCollector--视频采集器", example = "collector", pattern = "[collector|videoCollector]")
    private String sourceType;

    /**
     * 挂载详情
     */
    @Schema(description = "挂载详情")
    private MountSourceDto extend;

    /**
     * 挂载文件类型
     */
    @Schema(description = "挂载文件类型")
    private Integer dataType;

    @Schema(description = "是否持久化")
    private Boolean persistence;

    @Schema(description = "是否创建图表")
    private Boolean dashboard;

    @Schema(description = "是否同步元数据")
    private Boolean syncMeta;
}
