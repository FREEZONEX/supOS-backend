package com.supos.common.enums.mount;

import lombok.Getter;

/**
 * 挂载方式
 * @author sunlifang
 * @version 1.0
 * @description:
 * @date 2025/6/17 13:32
 */
@Getter
public enum MountModel {

    /**采集器全挂载*/
    COLLECTOR_ALL("collector_all"),
    /**采集器部分设备挂载*/
    COLLECTOR_DEVICE("collector_device"),
    /**MQTT全挂载*/
    MQTT_ALL("mqtt_all"),
    /**kafka全挂载*/
    KAFKA_ALL("kafka_all"),
    /**rabbitmq全挂载*/
    RABBITMQ_ALL("rabbitmq_all");

    private String type;

    MountModel(String type) {
        this.type = type;
    }

    public static MountModel getByType(String type) {
        for (MountModel sourceType : MountModel.values()) {
            if (sourceType.getType().equals(type)) {
                return sourceType;
            }
        }
        return null;
    }
}
