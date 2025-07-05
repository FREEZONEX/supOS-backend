package com.supos.adapter.mqtt.config;

import com.supos.adapter.mqtt.adapter.MQTTConsumerAdapter;
import com.supos.adapter.mqtt.service.MessageConsumer;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Getter
@Configuration
public class MQTTConfig {

    @Bean
    public MQTTConsumerAdapter mqttConsumerAdapter(@Value("${mqtt.broker:}") String broker,
                                                   @Value("${mqtt.clientId:supos_server}") String clientIdPrefix,
                                                   @Autowired MessageConsumer consumer
    ) {
        if (!StringUtils.hasText(broker)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                broker = "tcp://100.100.100.20:31017";
            } else {
                broker = "tcp://emqx:1883";
            }
        }
        String clientId = clientIdPrefix + ":" + UUID.randomUUID();
        MqttClient client = MQTTConsumerAdapter.client(clientId, broker);
        if (client == null) {
            return null;
        }
        return new MQTTConsumerAdapter(clientId, client, consumer);
    }
}
