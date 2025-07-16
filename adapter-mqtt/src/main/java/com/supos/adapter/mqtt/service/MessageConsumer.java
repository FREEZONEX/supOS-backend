package com.supos.adapter.mqtt.service;

import com.supos.adapter.mqtt.dto.LastMessage;

import java.util.Map;

public interface MessageConsumer {

    long getQueueFrontIndex();

    long getQueueTailIndex();

    long getQueueSize();

    long getEnqueuedSize();

    long getDequeuedSize();

    long getPublishedCalcSize();

    long getPublishedMergedSize();

    long getArrivedCalcSize();

    String getQueueHead();

    void onMessage(String topic, int msgId, byte[] payload);

    double[] statisticsThroughput();

    LastMessage getLastMessage();
}
