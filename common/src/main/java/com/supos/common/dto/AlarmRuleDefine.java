package com.supos.common.dto;


import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class AlarmRuleDefine {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_IS_ALARM = "is_alarm";
    public static final String FIELD_CURRENT_VALUE = "current_value";
    public static final String FIELD_LIMIT_VALUE = "limit_value";

    public static final String FIELD_UNS_ID = "uns";
    public static final String READ_STATUS = "read_status";

    /**
     * 条件
     */
    private String condition;

    /**
     * 限值
     */
    @NotNull
    private Double limitValue;

    /**
     * 死区类型 1-值，2-百分比
     */
    private Integer deadBandType;

    /**
     * 死区值
     */
    private Double deadBand;

    /**
     * 越限时长
     */
    private Long overTime;

    public void parseExpression(String expression) {
        if (StrUtil.isBlank(expression)) {
            return;
        }
        expression = expression.replaceAll(" ", "");
        String regex = "(\\w+)([!=<>]=?)(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(expression);
        if (matcher.find()) {
            String variable = matcher.group(1); // 提取变量名
            condition = matcher.group(2); // 提取条件操作符
            limitValue = Double.valueOf(matcher.group(3)); // 提取条件值
        }
    }

    private static final Snowflake snow = new Snowflake();

    public static long nextId() {
        return snow.nextId();
    }
}
