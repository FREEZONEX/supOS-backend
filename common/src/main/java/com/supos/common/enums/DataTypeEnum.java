package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum DataTypeEnum {

    TEMPLATE_TYPE(0, "模板"),
    TIME_SEQUENCE_TYPE(1, "时序"),
    RELATION_TYPE(2, "关系"),
    CALCULATION_REAL_TYPE(3,"实时计算"),
    CALCULATION_HIST_TYPE(4, "历史值计算"),
    ALARM_RULE_TYPE(5,"报警规则类型"),
    MERGE_TYPE(6, "聚合类型"),
    CITING_TYPE(7,"引用类型，不持久化，只读, 不能引用引用类型的文件"),
    JSONB_TYPE(8, "JSONB 整个json当做一个字段存储"),
    ;

    private Integer type;

    private String comment;

    public static DataTypeEnum parse(Integer type) {
        if (type == null) {
            return null;
        }
        for (DataTypeEnum each : values()) {
            if (type.equals(each.type)) {
                return each;
            }
        }
        return null;
    }

    public static DataTypeEnum parseByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (DataTypeEnum each : values()) {
            if (each.name().equals(name.toUpperCase())) {
                return each;
            }
        }
        return null;
    }
}

