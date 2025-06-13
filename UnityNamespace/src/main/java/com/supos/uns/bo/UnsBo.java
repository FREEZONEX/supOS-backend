package com.supos.uns.bo;

import cn.hutool.core.util.ArrayUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.StreamOptions;
import com.supos.common.utils.JsonUtil;
import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class UnsBo {
    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index;
    Long id;
    String path;
    int pathType;
    String layRec;

    Integer dataType;
    SrcJdbcType dataSrcId;

    String dataPath;
    String fields;

    Map<String, Object> protocol;
    Object protocolBean;
    String description;

    @JsonIgnore
    transient UnsBo parent;

    @JsonIgnore
    FieldDefine[] fieldDefines;
    private String alias;
    String modelAlias;
    String folderAlias;
    String tableName;

    InstanceField[] refers;// 计算实例引用的其他实例字段
    HashMap<Long, Set<String>> refTopicFields;

    String expression;// 计算表达式
    Object compileExpression;

    String protocolType;
    String referUns;// 引用的其他实例主题
    String[] referAlais;// 引用的其他多个实例主题
    String referTable; // 引用的其他实例的表名
    @Valid
    StreamOptions streamOptions;// 流（历史）计算定义
    Integer withFlags;
    AlarmRuleDefine alarmRuleDefine;
    Long frequencySeconds;// 计算时间间隔

    Map<String,Object> extend;//扩展字段   workflow表主键ID

    public UnsBo(String path) {
        this.path = path;
    }

    public UnsBo(String alias, String path, int pathType, Integer dataType, String fields) {
        this.alias = alias;
        this.path = path;
        this.pathType = pathType;
        this.dataType = dataType;
        this.fields = fields;
    }

    public UnsBo(Long id, String path, int pathType, Integer dataType, String fields) {
        this.id = id;
        this.path = path;
        this.pathType = pathType;
        this.dataType = dataType;
        this.fields = fields;
    }

    public String getFields() {
        if (!StringUtils.hasText(fields) && ArrayUtil.isNotEmpty(fieldDefines)) {
            fields = JsonUtil.toJson(fieldDefines);
        }
        return fields;
    }


    public int countNumberFields() {
        int rs = 0;
        if (fieldDefines == null && parent != null) {
            fieldDefines = parent.getFieldDefines();
        }
        if (fieldDefines != null) {
            rs = countNumberFields(fieldDefines);
        }
        return rs;
    }

    public static int countNumberFields(FieldDefine[] fieldDefines) {
        int rs = 0;
        for (FieldDefine define : fieldDefines) {
            if (define.getType().isNumber && !define.getName().startsWith(Constants.SYSTEM_FIELD_PREV)) {
                rs++;
            }
        }
        return rs;
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }
}
