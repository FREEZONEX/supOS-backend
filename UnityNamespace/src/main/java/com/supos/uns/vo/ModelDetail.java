package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelDetail {

    private String id;

    @Schema(description = "mqtt topic")
    String topic;

    @Schema(description = "模型别名")
    String alias;//模型别名

    @Schema(description = "父级别名")
    String parentAlias;

    @Schema(description = "全路径")
    String path;

    @Schema(description = "1--时序库 2--关系库")
    Integer dataType;// 1--时序库 2--关系库
    @Schema(description = "字段定义")
    FieldDefine[] fields;// 字段定义
    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒
    @Schema(description = "修改时间--单位：毫秒")
    Long updateTime;
    @Schema(description = "模型描述")
    String description;// 模型描述

    @Schema(description = "文件夹名称")
    String name;// 文件夹名称

    @Schema(description = "显示名")
    String displayName;

    @Schema(description = "文件路径名")
    String pathName;//文件路径名
    String modelId;//模板ID
    String modelName;//模板名称
    @Schema(description = "扩展字段JSON")
    Map<String,Object> extend;//扩展字段

    @Schema(description = "模板别名")
    String templateAlias;
}
