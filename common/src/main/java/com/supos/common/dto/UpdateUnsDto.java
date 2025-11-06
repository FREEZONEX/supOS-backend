package com.supos.common.dto;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.DataTypeValidator;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;


@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUnsDto {


    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    @Hidden
    Long id;

    String name;// 文件名

    /**
     * 显示名
     */
    @Schema(description = "显示名")
    String displayName;

    @Schema(description = "文件类型：文件， 目录，模板")
    @Min(value = Constants.PATH_TYPE_DIR, message = "uns.pathType.invalid")
    @Max(value = Constants.PATH_TYPE_FILE, message = "uns.pathType.invalid")
    Integer pathType;

    @Hidden
    String path;// 文件路径

    @AliasValidator
    @Schema(description = "引用的其他实例主题")
    String referUns;//引用的其他实例主题

    @Schema(description = "聚合的其他多个实例主题IDs")
    Long[] referIds;// 聚合的其他多个实例主题IDs

    String referTable;

    @Schema(description = "流引用表的字段定义")
    FieldDefine[] refFields;// 流引用表的字段定义

    @Schema(description = "流引用表的 modeId")
    String referModelId; // 流引用表的 modeId

    @Schema(description = "被引用实例所引用")
    Set<Long> cited = new ConcurrentHashSet<>();// 被引用实例所引用

    @NotEmpty(message = "uns.invalid.alias.empty")
    @AliasValidator
    @Schema(description = "别名")
    String alias;

    @Schema(description = "模板ID")
    Long modelId;//模板ID

    @AliasValidator
    @Schema(description = "模板别名")
    String modelAlias;// 模板别名

    @Hidden
    String template;// 模板名称

    @AliasValidator
    @Schema(description = "目录别名")
    String parentAlias;// 目录别名

    @Schema(description = "父级ID")
    Long parentId;//父级ID

    @Hidden
    String tableName;

    @DataTypeValidator
    @Schema(description = "数据类型：1--时序库 2--关系库")
    Integer dataType;// 数据类型：1--时序库 2--关系库

    @Hidden
    SrcJdbcType dataSrcId;

    @Schema(description = "字段定义")
    FieldDefine[] fields;// 字段定义

    @Hidden
    String dataPath;// 数据在rest 数据当中的路径; 模型字段对应的数据字段映射放在 FieldDefine.index

    @Schema(description = "描述")
    String description; //描述

    @Hidden
    Map<String, Object> protocol;
    @Hidden
    String protocolType;
    @Hidden
    Object protocolBean;

    @Valid
    InstanceField[] refers;// 计算实例引用的其他实例字段

    @Size(max = 255)
    @Schema(description = "计算表达式")
    String expression;// 计算表达式

    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    @Hidden
    transient Object compileExpression;

    @Valid
    StreamOptions streamOptions;// 流（历史）计算定义

    @Hidden
    Integer flags;

    @Hidden
    AlarmRuleDefine alarmRuleDefine;// 报警规则

    @Schema(description = "是否创建Flow")
    Boolean addFlow;
    @Schema(description = "是否创建数据看板")
    Boolean addDashBoard;
    @Schema(description = "是否创持久化")
    Boolean save2db;
    @Hidden
    Boolean retainTableWhenDeleteInstance;

    @StreamTimeValidator(field = "frequency")
    @Schema(description = "当集合类型时的计算时间间隔，单位：秒:s 分钟:m 小时：h；如三分钟：3m")
    String frequency;// 当集合类型时的计算时间间隔，单位：秒:s 分钟:m 小时：h；如三分钟：3m

    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    @Hidden
    Long frequencySeconds;

    @Schema(description = "扩展字段JSON对象：{\"k1\":\"v1\",\"k2\":\"v2\"}")
    Map<String,Object> extend;//扩展字段   workflow表主键ID

    @Schema(description = "标签名称列表，创建文件时支持打标签")
    String[] labelNames;//标签名称列表

    @Hidden
    Integer order;

    @Schema(description = "引用源别名")
    String refSource;//引用源别名 pride

    @Schema(description = "value的类型")
    String valueType; //value的类型 pride

    @Schema(description = "文件的初始值")
    Object initValue; //文件的初始值

    @Schema(description = "当valueType=STRING时，可以设置该参数。默认512字符")
    Integer strMaxLen;//当valueType=STRING时，可以设置该参数。默认512字符

    /**
     * 读写模式：北向访问级别。READ_ONLY-只读，READ_WRITE-读写
     * @see com.supos.common.enums.FileReadWriteMode
     */
    String accessLevel;


    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    transient HashMap<Long, Set<String>> refTopicFields;

    public void setFrequency(String frequency) {
        this.frequency = frequency;
        if (frequency != null && !(frequency = frequency.trim()).isEmpty()) {
            Long nano = TimeUnits.toNanoSecond(frequency);
            if (nano != null) {
                frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            }
        }
    }

    public String getTable() {
        if (tableName != null) {
            return tableName;
        }
        if (alias != null) {
            return alias;
        }
        return path;
    }

    public UpdateUnsDto(String path, String alias) {
        setAlias(alias);
        setPath(path);
    }

    public UpdateUnsDto(String path, String alias, @NotEmpty FieldDefine[] fields) {
        setAlias(alias);
        setPath(path);
        this.fields = fields;
    }

    public UpdateUnsDto(String path, String alias, int dataType, @NotEmpty FieldDefine[] fields) {
        setAlias(alias);
        setPath(path);
        this.fields = fields;
        this.dataType = dataType;
    }

    public UpdateUnsDto(String path, String alias, Integer dataType, FieldDefine[] fields, String description) {
        setAlias(alias);
        setPath(path);
        this.dataType = dataType;
        this.fields = fields;
        this.description = description;
    }

    public UpdateUnsDto(String path, String alias, String description, Map<String, Object> protocol) {
        setAlias(alias);
        setPath(path);
        this.description = description;
        this.protocol = protocol;
    }

    public UpdateUnsDto(String path, String alias, String description, Map<String, Object> protocol, String protocolType) {
        setAlias(alias);
        setPath(path);
        this.description = description;
        this.protocol = protocol;
        this.protocolType = protocolType;
    }

    public UpdateUnsDto setDataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    public UpdateUnsDto setCalculation(InstanceField[] refers, String expression) {
        this.refers = refers;
        this.expression = expression;
        return this;
    }

    public UpdateUnsDto setStreamCalculation(String referTopic, StreamOptions streamOptions) {
        this.referUns = referTopic;
        this.streamOptions = streamOptions;
        return this;
    }

    public void setPath(String path) {
        if (path != null && !path.isEmpty()) {
            path = path.trim();
        }
        this.path = path;
    }

    public void setAlias(String alias) {
        if (alias != null && !alias.isEmpty()) {
            alias = alias.trim();
        } else {
            alias = null;
        }
        this.alias = alias;
    }

    public int countNumberFields() {
        int rs = 0;
        if (fields != null) {
            rs = countNumberFields(fields);
        }
        return rs;
    }

    public static int countNumberFields(FieldDefine[] fieldDefines) {
        int rs = 0;
        for (FieldDefine define : fieldDefines) {
            if (define.getType().isNumber && !define.isSystemField()) {
                rs++;
            }
        }
        return rs;
    }


    /**
     * 过滤出类型为BLOB或LBLOB的字段名称
     * @return
     */
    public Set<String> filterBlobField() {
        if (fields == null || fields.length == 0) {
            return new HashSet<>();
        }
        return Arrays.stream(fields).filter(field -> field.getType() == FieldType.BLOB || field.getType() == FieldType.LBLOB).map(FieldDefine::getName).collect(Collectors.toSet());
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
