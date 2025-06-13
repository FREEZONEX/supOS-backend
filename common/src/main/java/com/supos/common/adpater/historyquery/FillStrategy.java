package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;

/**
 * 补点策略
 */
public enum FillStrategy {
    None//  不补点（原始值）
    , Previous// 如果当前窗口没有值，以前一个窗口的值填充
    , Next// 如果当前窗口没有值，以后一个窗口的值填充
    , Linear // 线性插值
    ;
    private static final HashMap<String, FillStrategy> nameMap = new HashMap<>(16);

    static {
        for (FillStrategy ft : FillStrategy.values()) {
            nameMap.put(ft.name().toUpperCase(), ft);
        }
    }

    @JsonCreator
    public static FillStrategy getByNameIgnoreCase(String name) {
        return nameMap.get(name.toUpperCase());
    }
}
