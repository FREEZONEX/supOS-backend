package com.supos.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Getter
public enum FieldType {

    INTEGER("INTEGER", true, 0),

    LONG("LONG", true, 0),

    FLOAT("FLOAT", true, 0.0f),

    DOUBLE("DOUBLE", true, 0.0d),

    BOOLEAN("BOOLEAN", false, false),

    DATETIME("DATETIME", false, null),

    STRING("STRING", false, null),

    BLOB("BLOB", false, null),
    LBLOB("LBLOB", false, null);

    public final String name;
    public final boolean isNumber;
    public final Object defaultValue;

    FieldType(String name, boolean isNumber, Object defaultValue) {
        this.name = name;
        this.isNumber = isNumber;
        this.defaultValue = defaultValue;
    }

    private static final HashMap<String, FieldType> nameMap = new HashMap<>(16);

    static {
        for (FieldType ft : FieldType.values()) {
            nameMap.put(ft.name.toUpperCase(), ft);
        }
    }

    public static FieldType getByName(String name) {
        FieldType type = nameMap.get(name);
        if (type == null && "int".equalsIgnoreCase(name)) {
            type = FieldType.INTEGER;
        }
        return type;
    }

    @JsonCreator
    public static FieldType getByNameIgnoreCase(String name) {
        return getByName(name.toUpperCase());
    }

    public String toString() {
        return name;
    }

    public static Set<FieldType> enablePrimaryKeyFields = Set.of(FieldType.STRING, FieldType.INTEGER, FieldType.LONG);

}
