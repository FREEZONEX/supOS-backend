package com.supos.i18n.common;

import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 修改类型
 * @date 2025/9/4 11:06
 */
@Getter
public enum ModifyFlag {

    CUSTOM("CUSTOM"),
    SYSTEM("SYSTEM");

    private String flag;

    ModifyFlag(String flag) {
        this.flag = flag;
    }

    public static ModifyFlag getByFlag(String flag) {
        for (ModifyFlag modifyFlag : values()) {
            if (modifyFlag.flag.equals(flag)) {
                return modifyFlag;
            }
        }
        return null;
    }
}
