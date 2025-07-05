package com.supos.adpter.nodered.enums;

import com.supos.common.enums.FieldType;
import lombok.Getter;

@Getter
public enum DataType {

    INT8("Int8"),

    INT16("Int16"),

    INT32("Int32"),

    INT64("Int64"),

    FLOAT("Float"),

    DOUBLE("Double"),

    BOOLEAN("Boolean"),

    STRING("String"),

    DATETIME("DateTime");

    private String name;

    DataType(String name) {
        this.name = name;
    }

    /**
     * 将uns字段类型转换为node-red对应的数据类型
     * @param ft
     * @return
     */
    public static String transfer(FieldType ft) {
        switch (ft) {
            case INTEGER: return INT32.name;
            case LONG: return INT64.name;
            case FLOAT: return FLOAT.name;
            case DOUBLE: return DOUBLE.name;
            case BOOLEAN: return BOOLEAN.name;
            case DATETIME: return DATETIME.name;
            default: return STRING.name;
        }
    }
}
