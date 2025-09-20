package com.supos.i18n.common;

import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 模块类型
 * @date 2025/9/2 13:09
 */
@Getter
public enum ModuleType {

    /**内置*/
    BUILTIN(1),
    /**自定义*/
    CUSTOM(2);

    private int type;

    ModuleType(int type) {
        this.type = type;
    }

    public static ModuleType getByType(int type) {
        for (ModuleType value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }
}
