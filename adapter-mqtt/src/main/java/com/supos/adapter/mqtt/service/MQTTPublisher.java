package com.supos.adapter.mqtt.service;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Collection;

public interface MQTTPublisher {
    void publishMessage(String topic, byte[] msg, int qos) throws MqttException;

    void subscribe(Collection<String> topics, boolean throwException);

    void unSubscribe(Collection<String> topics);
}
