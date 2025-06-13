package com.supos.common.utils;

import com.supos.common.enums.FieldType;

import java.util.HashMap;
import java.util.Map;

public class PostgresqlTypeUtils {
    public static final Map<String, String> dbType2FieldTypeMap = new HashMap<>(16);

    static {
        dbType2FieldTypeMap.put("integer", FieldType.INT.name);
        dbType2FieldTypeMap.put("serial", FieldType.INT.name);
        dbType2FieldTypeMap.put("serial2", FieldType.INT.name);
        dbType2FieldTypeMap.put("serial4", FieldType.INT.name);
        dbType2FieldTypeMap.put("intserial", FieldType.INT.name);
        dbType2FieldTypeMap.put("int2", FieldType.INT.name);
        dbType2FieldTypeMap.put("int4", FieldType.INT.name);
        dbType2FieldTypeMap.put("bigint", FieldType.LONG.name);
        dbType2FieldTypeMap.put("bigserial", FieldType.LONG.name);
        dbType2FieldTypeMap.put("serial8", FieldType.LONG.name);
        dbType2FieldTypeMap.put("int8", FieldType.LONG.name);
        dbType2FieldTypeMap.put("timestamptz", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("timestamp", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("float", FieldType.FLOAT.name);
        dbType2FieldTypeMap.put("float4", FieldType.FLOAT.name);
        dbType2FieldTypeMap.put("double", FieldType.DOUBLE.name);
        dbType2FieldTypeMap.put("float8", FieldType.DOUBLE.name);
        dbType2FieldTypeMap.put("text", FieldType.STRING.name);
        dbType2FieldTypeMap.put("char", FieldType.STRING.name);
        dbType2FieldTypeMap.put("json", FieldType.STRING.name);
        dbType2FieldTypeMap.put("jsonb", FieldType.STRING.name);
        dbType2FieldTypeMap.put("varchar", FieldType.STRING.name);
        dbType2FieldTypeMap.put("bool", FieldType.BOOLEAN.name);
        dbType2FieldTypeMap.put("boolean", FieldType.BOOLEAN.name);
        dbType2FieldTypeMap.put("blob", FieldType.BLOB.name);
    }
}
