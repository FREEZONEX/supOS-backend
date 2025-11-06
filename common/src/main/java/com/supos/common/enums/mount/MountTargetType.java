package com.supos.common.enums.mount;

import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 挂载的目标类型
 * @date 2025/6/17 13:30
 */
@Getter
public enum MountTargetType {

    FOLDER("folder"),
    FILE("file");

    private String type;

    MountTargetType(String type) {
        this.type = type;
    }

    public static MountTargetType getByType(String type) {
        for (MountTargetType targetType : MountTargetType.values()) {
            if (targetType.getType().equals(type)) {
                return targetType;
            }
        }
        return null;
    }
}
