package com.supos.common.enums.mount;

import lombok.Getter;

/**
 * @author sunlifang@supos.supcon.com
 * @title MountSubSourceType
 * @description
 * @create 2025/7/7 下午1:04
 */
public enum MountSubSourceType {

    /**挂载采集器源*/
    COLLECTOR("collector"),
    /**挂载采集器设备源*/
    COLLECTOR_DEVICE("collector_device"),
    /**MQTT全挂载*/
    MQTT_ALL("mqtt_all"),
    MQTT_FOLDER("mqtt_folder"),
    /**kafka全挂载*/
    KAFKA_ALL("kafka_all"),
    /**rabbitmq全挂载*/
    RABBITMQ_ALL("rabbitmq_all");

    @Getter
    private String type;

    MountSubSourceType(String type) {
        this.type = type;
    }

    public static MountSubSourceType getByType(String type) {
        for (MountSubSourceType subSourceType : MountSubSourceType.values()) {
            if (subSourceType.getType().equals(type)) {
                return subSourceType;
            }
        }
        return null;
    }

    public static boolean isALL(String type) {
        return MountSubSourceType.MQTT_ALL.getType().equals(type)
                || MountSubSourceType.KAFKA_ALL.getType().equals(type)
                || MountSubSourceType.RABBITMQ_ALL.getType().equals(type)
                || MountSubSourceType.COLLECTOR.getType().equals(type);
    }
}
