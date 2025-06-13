package com.supos.adapter.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttConsumerTest {
    public static void main(String[] args) throws InterruptedException {
        String broker = "tcp://100.100.100.20:31017"; // EMQX 服务器地址
        broker = "tcp://localhost:1883";
        final MqttClient client = testMqttConsume(broker, "JavaClient:" + System.currentTimeMillis());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                Scanner sc = new Scanner(System.in);
                String expr = sc.next();
                if (expr.startsWith("!")) {
                    expr = expr.substring(1);
                    System.out.println("unSub: " + expr);
                    client.unsubscribe(expr);
                } else {
                    System.out.println("sub: " + expr);
                    client.subscribe(expr);
                }
            }
        });
        Thread.sleep(1800 * 1000);
    }

    static MqttClient testMqttConsume(String broker, String clientId) {

        String[] topics = new String[]{
//                "#"
                "dhbimport1/aaaaaaaaaa/bbbbbbbbbb/cccccccccc/dddddddddd/#",
                "auto_1"
        };
        MqttClient client = null;
        try {
            // 创建 MQTT 客户端
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            // 连接到 EMQX 服务器
            client.connect(options);
            System.out.println("Connected to broker: " + broker);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
            // 设置消息回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost! " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.printf("%s| Message[%s]: %s\n", Instant.now().atZone(ZoneOffset.ofHours(8)).format(fmt),
                            topic, new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 不需要处理
                }
            });

            // 订阅主题
            client.subscribe(topics);
            System.out.println("client_Id: " + clientId);
            System.out.println("Subscribed to topics: " + Arrays.toString(topics));

        } catch (MqttException e) {
            e.printStackTrace();
        }
        return client;
    }
}
