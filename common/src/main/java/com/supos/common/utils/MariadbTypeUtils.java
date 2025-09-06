package com.supos.common.utils;

import com.supos.common.enums.FieldType;

import java.util.HashMap;
import java.util.Map;

public class MariadbTypeUtils {
    public static final Map<String, String> dbType2FieldTypeMap = new HashMap<>(16);

    static {
        dbType2FieldTypeMap.put("int", FieldType.INTEGER.name);
        dbType2FieldTypeMap.put("smallint", FieldType.INTEGER.name);
        dbType2FieldTypeMap.put("tinyint", FieldType.INTEGER.name);
        dbType2FieldTypeMap.put("MEDIUMINT", FieldType.LONG.name);
        dbType2FieldTypeMap.put("bigint", FieldType.LONG.name);

        dbType2FieldTypeMap.put("float", FieldType.FLOAT.name);
        dbType2FieldTypeMap.put("double", FieldType.DOUBLE.name);
        dbType2FieldTypeMap.put("decimal", FieldType.DOUBLE.name);

        dbType2FieldTypeMap.put("char", FieldType.STRING.name);
        dbType2FieldTypeMap.put("varchar", FieldType.STRING.name);
        dbType2FieldTypeMap.put("text", FieldType.STRING.name);
        dbType2FieldTypeMap.put("tinytext", FieldType.STRING.name);
        dbType2FieldTypeMap.put("mediumtext", FieldType.STRING.name);

        dbType2FieldTypeMap.put("json", FieldType.STRING.name);

        dbType2FieldTypeMap.put("bit", FieldType.LONG.name);//存储位值

        dbType2FieldTypeMap.put("date", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("time", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("datetime", FieldType.DATETIME.name);
        dbType2FieldTypeMap.put("TIMESTAMP", FieldType.DATETIME.name);

        dbType2FieldTypeMap.put("tinyblob", FieldType.BLOB.name);
        dbType2FieldTypeMap.put("blob", FieldType.BLOB.name);
        dbType2FieldTypeMap.put("mediumblob", FieldType.BLOB.name);
        dbType2FieldTypeMap.put("longblob", FieldType.BLOB.name);
    }
}
