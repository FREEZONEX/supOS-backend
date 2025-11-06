package com.supos.common.enums.mount;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/9/19 11:01
 */
public enum MountMetaQueryType {

    /**采集器版本*/
    COLLECTOR_VERSION,
    /**采集器*/
    COLLECTOR,
    /**采集器设备*/
    COLLECTOR_DEVICE,
    /**测点位号*/
    COLLECTOR_TAG,
    /**MQTT连接信息*/
    MQTT_BROKER,
    /**MQTT topic*/
    MQTT_TOPIC,
    /**kafka连接信息*/
    KAFKA_BROKER,
    /**kafka topic*/
    KAFKA_TOPIC,
    /**rabbitmq连接信息*/
    RABBITMQ_BROKER,
    /**rabbitmq topic*/
    RABBITMQ_TOPIC;
}
