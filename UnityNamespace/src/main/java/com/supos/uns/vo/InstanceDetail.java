package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import com.supos.common.vo.LabelVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceDetail {
    @Schema(description = "文件ID")
    String id;

    @Schema(description = "mqtt topic")
    String topic;

    @Schema(description = "别名")
    String alias;// 别名

    @Schema(description = "父级别名")
    String parentAlias;

    @Schema(description = "全路径")
    String path;

    @Schema(description = "1--时序库 2--关系库")
    Integer dataType;// 1--时序库 2--关系库

    String dataPath;

    /**
     * 0--文件夹，1--模板，2--文件
     */
    @Schema(description = "文件类型 0--文件夹，2--文件")
    Integer pathType;

    @Schema(description = "字段定义")
    FieldDefine[] fields;// 字段定义

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒
    @Schema(description = "修改时间--单位：毫秒")
    Long updateTime;

    @Schema(description = "协议")
    Map<String, Object> protocol;

    @Schema(description = "模板描述")
    String modelDescription;// 模板描述

    @Schema(description = "描述")
    String description;

    @Schema(description = "是否创建FLOW")
    boolean withFlow;

    @Schema(description = "是否创建看板")
    boolean withDashboard;

    @Schema(description = "是否持久化")
    boolean withSave2db;

    /**
     * 是否持久化:pride冗余
     */
    @Schema(description = "是否持久化")
    boolean save2db;

    @Schema(description = "表达式，引用用 id")
    String expression;

    @Schema(description = "用于展示的表达式，引用用 path")
    String showExpression;

    @Schema(description = "引用对象")
    InstanceFieldVo[] refers;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    List<LabelVo> labelList;

    @Schema(description = "文件名")
    String name;//文件名

    @Schema(description = "显示名")
    String displayName;

    @Schema(description = "文件路径名")
    String pathName;//文件路径名

    String modelId;// 模板ID

    String modelName;// 模板名称

    @Schema(description = "扩展字段JSON")
    Map<String, Object> extend;//扩展字段

    /**
     * 当前值对象
     */
    @Schema(description = "当前值对象")
    String payload;

    /**
     * 模板名称 冗余pride
     */
    @Schema(description = "模板名称")
    String templateName;

    @Schema(description = "模板别名")
    String templateAlias;
}
