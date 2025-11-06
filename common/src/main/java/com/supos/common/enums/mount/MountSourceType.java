package com.supos.common.enums.mount;

import lombok.Getter;

/**
 * 挂载源
 * @author sunlifang@supos.supcon.com
 * @title MountSource
 * @description
 * @create 2025/7/7 下午1:04
 */
public enum MountSourceType {

    /**采集器*/
    COLLECTOR("collector", 16),
    MQTT("mqtt", 50),
    KAFKA("kafka", 51),
    RABBITMQ("rabbitmq", 52),

    CONNECT("connect", 100);

    @Getter
    private String type;

    @Getter
    private int typeValue;

    MountSourceType(String type, int typeValue) {
        this.type = type;
        this.typeValue = typeValue;
    }

    public static MountSourceType getByType(String type) {
        for (MountSourceType subSourceType : MountSourceType.values()) {
            if (subSourceType.getType().equals(type)) {
                return subSourceType;
            }
        }
        return null;
    }

}
