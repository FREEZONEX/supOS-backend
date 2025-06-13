package com.supos.uns.util;

import com.supos.common.enums.FieldType;

public class NumberRangeValidator {


    /**
     * 检查 value 是否超出指定类型的长度范围
     * @param value 输入值
     * @param targetType 目标类型
     * @return true = 超出长度限制，false = 未超出 或 无法解析
     */
    public static boolean isOutOfRange(Object value, FieldType targetType) {
        if (value == null) {
            return false; // 忽略无法解析的情况
        }

        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            return false;
        }

        try {
            switch (targetType) {
                case INT:
                    long longValue = Long.parseLong(strValue);
                    return longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE;

                case LONG:
                    // 尝试解析为 BigInteger 判断是否超出 long
                    java.math.BigInteger bigInt = new java.math.BigInteger(strValue);
                    return bigInt.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE)) < 0
                            || bigInt.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0;

                case DOUBLE:
                    // 尝试解析为 Double
                    Double.parseDouble(strValue); // 只要能解析成功，不报错就算没超出
                    return false;

                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false; // 无法解析的字符串（如 "abc"），忽略
        }
    }
}
