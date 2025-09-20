package com.supos.i18n.common;

import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 语言类型
 * @date 2025/9/2 13:09
 */
@Getter
public enum LanguageType {

    /**内置*/
    BUILTIN(1),
    /**自定义*/
    CUSTOM(2);

    private int type;

    LanguageType(int type) {
        this.type = type;
    }

    public static LanguageType getByType(int type) {
        for (LanguageType value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }
}
