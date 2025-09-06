package com.supos.common.dto;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.DataTypeValidator;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTopicDto {

    @Hidden
    Long id;
    /**
     * 批次号
     */
    @Hidden
    int batch;
    /**
     * 批次内序号
     */
    @Hidden
    int index = -1;

    @Hidden
    String flagNo;

    @JsonIgnore
    private String tmField;

    public String getTopic() {
        return Constants.useAliasAsTopic ? alias : path;
    }

    public String getTimestampField() {
        if (tmField != null) {
            return tmField;
        }
        if (fields != null) {
            FieldDefine ts = FieldUtils.getTimestampField(fields);
            return tmField = ts != null ? ts.getName() : null;
        }
        return null;
    }

    public String getQualityField() {
        if (fields != null && fields.length > 2 && dataSrcId != null) {
            FieldDefine qos = FieldUtils.getQualityField(fields, dataSrcId.typeCode);
            return qos != null ? qos.getName() : null;
        }
        return null;
    }

    @NotEmpty(message = "uns.name.empty")
    @Length(max = 63)
    String name;// 文件名

    /**
     * 显示名
     */
    @Schema(description = "显示名")
    @Length(max = 128)
    String displayName;

    @Schema(description = "文件类型：文件， 目录，模板")
    @NotNull(message = "uns.pathType.empty")
    @Min(value = Constants.PATH_TYPE_DIR, message = "uns.pathType.invalid")
    @Max(value = Constants.PATH_TYPE_FILE, message = "uns.pathType.invalid")
    Integer pathType;

    @Hidden
    String path;// 文件路径
    @Hidden
    String[] primaryField;//主键字段
    @Hidden
    boolean hasBlobField;

    @AliasValidator
    @Schema(description = "引用的其他文件别名")
    @Hidden
    String referUns;//引用的其他文件别名

    @Schema(description = "聚合的其他多个实例主题IDs")
    Long[] referIds;// 聚合的其他多个实例主题IDs
    @Hidden
    Map<Long, Integer> refUns;
    @Hidden String layRec;
    String referTable;

    @Schema(description = "流引用表的字段定义")
    FieldDefine[] refFields;// 流引用表的字段定义

    @Schema(description = "流引用表的 modeId")
    String referModelId; // 流引用表的 modeId

    @Hidden
    Set<Long> cited = new ConcurrentHashSet<>();// 被引用实例所引用

    @NotEmpty(message = "uns.invalid.alias.empty")
    @AliasValidator
    @Schema(description = "别名")
    String alias;

    @Schema(description = "模板ID")
    Long modelId;//模板IDa

    @AliasValidator
    @Schema(description = "模板别名")
    @JsonProperty("modelAlias") // 序列化时输出 a
    @JsonAlias({"templateAlias"})//兼容pride字段
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
    @Hidden
    String tbFieldName;//当前uns别名对应目标表的字段名

    @DataTypeValidator
    @Schema(description = "数据类型：1--时序库 2--关系库")
    @Min(value = Constants.TIME_SEQUENCE_TYPE, message = "uns.file.dataType.invalid")
    @Max(value = Constants.CITING_TYPE, message = "uns.file.dataType.invalid")
    Integer dataType;// 数据类型：1--时序库 2--关系库

    @Hidden
    SrcJdbcType dataSrcId;

    @Schema(description = "字段定义")
    FieldDefine[] fields;// 字段定义

    @Schema(description = "扩展字段使用")
    String[] extendFieldUsed;

    @Hidden
    String dataPath;// 数据在rest 数据当中的路径; 模型字段对应的数据字段映射放在 FieldDefine.index

    @Schema(description = "描述")
    @Length(max = 255)
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
    @Hidden
    Boolean addDashBoard;
    @Schema(description = "是否创持久化")
    Boolean save2db;
    @Hidden
    Boolean retainTableWhenDeleteInstance;
    @Hidden
    Boolean createTemplate;//是否同步创建模板：只针对文件夹

    @StreamTimeValidator(field = "frequency")
    @Schema(description = "聚合计算频率：当聚合类型时(dataType=6)的计算时间间隔，单位支持：秒:s 分钟:m 小时：h；如三分钟：3m")
    String frequency;

    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    @Hidden
    Long frequencySeconds;

    @Schema(description = "扩展字段JSON对象：{\"k1\":\"v1\",\"k2\":\"v2\"}")
    @Size(max = 3)
    LinkedHashMap<String, Object> extend;//扩展字段   workflow表主键ID

    @Schema(description = "标签名称列表，创建文件时支持打标签")
    String[] labelNames;//标签名称列表
    @Hidden
    TreeMap<Long, String> labelIds;
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

    @Schema(description = "北向访问级别。READ_ONLY-只读，READ_WRITE-读写")
    String accessLevel;

    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    transient HashMap<Long, Set<String>> refTopicFields;

    @Hidden
    Boolean fieldsChanged;// Update时字段有没有修改

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
        if (tableName != null && !tableName.isEmpty()) {
            return tableName;
        }
        if (alias != null) {
            return alias;
        }
        return path;
    }

    public void setTableName(String table) {
        this.tableName = table != null && !table.isEmpty() ? table : null;
        if (tableName != null) {
            String[] dots = table.split("[.]");
            if (dots.length == 3) {
                this.tableName = dots[0].trim() + "." + dots[1].trim();
                this.tbFieldName = dots[2].trim();
            }
        }
    }

    public CreateTopicDto(String path, String alias) {
        setAlias(alias);
        setPath(path);
    }

    public CreateTopicDto(String path, String alias, @NotEmpty FieldDefine[] fields) {
        setAlias(alias);
        setPath(path);
        setFields(fields);
    }

    public CreateTopicDto(String path, String alias, int dataType, @NotEmpty FieldDefine[] fields) {
        setAlias(alias);
        setPath(path);
        setFields(fields);
        this.dataType = dataType;
    }

    public CreateTopicDto(String path, String alias, Integer dataType, FieldDefine[] fields, String description) {
        setAlias(alias);
        setPath(path);
        setFields(fields);
        this.dataType = dataType;
        this.description = description;
    }

    public CreateTopicDto(String path, String alias, String description, Map<String, Object> protocol) {
        setAlias(alias);
        setPath(path);
        this.description = description;
        this.protocol = protocol;
    }

    public CreateTopicDto(String path, String alias, String description, Map<String, Object> protocol, String protocolType) {
        setAlias(alias);
        setPath(path);
        this.description = description;
        this.protocol = protocol;
        this.protocolType = protocolType;
    }

    public CreateTopicDto setDataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    public CreateTopicDto setCalculation(InstanceField[] refers, String expression) {
        this.refers = refers;
        this.expression = expression;
        return this;
    }

    public CreateTopicDto setStreamCalculation(String referTopic, StreamOptions streamOptions) {
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

    public String gainBatchIndex() {
        if (StringUtils.isNotBlank(flagNo)) {
            return flagNo;
        }
        return String.format("%d-%d", batch, index);
    }

    /**
     * 过滤出类型为BLOB或LBLOB的字段名称
     *
     * @return
     */
    public List<FieldDefine> filterAllBlobField() {
        if (fields == null || fields.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(fields).filter(field -> field.getType() == FieldType.BLOB || field.getType() == FieldType.LBLOB).collect(Collectors.toList());
    }

    public List<FieldDefine> filterBlobField() {
        if (fields == null || fields.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(fields).filter(field -> field.getType() == FieldType.BLOB).collect(Collectors.toList());
    }

    public void setFields(FieldDefine[] fields) {
        this.fields = fields;
        fieldDefines = new FieldDefines(fields);
        this.hasBlobField = CollectionUtil.isNotEmpty(filterAllBlobField());
        if (fields != null) {
            TreeSet<String> pkSet = new TreeSet<>(); //主键
            for (FieldDefine fieldDefine : fields) {
                if (fieldDefine.isUnique()) {
                    pkSet.add(fieldDefine.getName());
                }
                if (!StringUtils.isEmpty(fieldDefine.getTbValueName())) {
                    tbFieldName = fieldDefine.getName();
                }
            }
            this.primaryField = !pkSet.isEmpty() ? pkSet.toArray(new String[0]) : null;
        } else if (primaryField != null) {
            primaryField = null;
        }
    }

    @JsonIgnore
    private transient FieldDefines fieldDefines;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
