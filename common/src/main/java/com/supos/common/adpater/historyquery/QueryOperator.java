package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;

public enum QueryOperator {
    Eq("=",0), Ne("<>", 0),
    Gt(">", 1), Ge(">=",1 ),
    Lt("<", -1), Le("<=", -1)
    ;
    public final String op;
    public final int direction;

    QueryOperator(String op, int direction) {
        this.op = op;
        this.direction = direction;
    }

    private static final HashMap<String, QueryOperator> nameMap = new HashMap<>(16);

    static {
        for (QueryOperator ft : QueryOperator.values()) {
            nameMap.put(ft.name().toUpperCase(), ft);
        }
    }

    @JsonCreator
    public static QueryOperator getByNameIgnoreCase(String name) {
        return nameMap.get(name.toUpperCase());
    }
}
