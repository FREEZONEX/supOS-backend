package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;

/**
 * 聚合查询，支持函数同supOS工厂版，包括：first, mean (等同于avg), sum, max, last, count
 */
public enum SelectFunction {
    First// 窗口内首个
    , Last// 窗口内最后一个
    , Avg// 平均值
    , Sum// 求和
    , Max// 最大值
    , Min// 最小值
    , Count// 计数值
    ;

    private static final HashMap<String, SelectFunction> nameMap = new HashMap<>(16);

    static {
        for (SelectFunction ft : SelectFunction.values()) {
            nameMap.put(ft.name().toUpperCase(), ft);
        }
    }

    @JsonCreator
    public static SelectFunction getByNameIgnoreCase(String name) {
        return nameMap.get(name.toUpperCase());
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
