package com.supos.common.utils;

import com.supos.common.enums.FieldType;

import java.util.HashMap;
import java.util.Map;

public class SqlserverTypeUtils {
    public static final Map<String, String> dbType2FieldTypeMap = new HashMap<>(16);

    static {
        dbType2FieldTypeMap.put("int", FieldType.INT.name);
        dbType2FieldTypeMap.put("smallint", FieldType.INT.name);
        dbType2FieldTypeMap.put("tinyint", FieldType.INT.name);
        dbType2FieldTypeMap.put("bigint", FieldType.LONG.name);

        dbType2FieldTypeMap.put("float", FieldType.FLOAT.name);
        dbType2FieldTypeMap.put("real", FieldType.FLOAT.name);
        dbType2FieldTypeMap.put("decimal", FieldType.DOUBLE.name);
        dbType2FieldTypeMap.put("numeric", FieldType.DOUBLE.name);

        dbType2FieldTypeMap.put("char", FieldType.STRING.name);
        dbType2FieldTypeMap.put("varchar", FieldType.STRING.name);
        dbType2FieldTypeMap.put("text", FieldType.STRING.name);
        dbType2FieldTypeMap.put("nchar", FieldType.STRING.name);
        dbType2FieldTypeMap.put("nvarchar", FieldType.STRING.name);
        dbType2FieldTypeMap.put("ntext", FieldType.STRING.name);

        dbType2FieldTypeMap.put("bit", FieldType.BOOLEAN.name);

        dbType2FieldTypeMap.put("date", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("time", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("datetime", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("datetime2", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("datetimeoffset", FieldType.DATETIME.name);

        dbType2FieldTypeMap.put("binary", FieldType.BLOB.name);
        dbType2FieldTypeMap.put("varbinary", FieldType.BLOB.name);
    }
}
