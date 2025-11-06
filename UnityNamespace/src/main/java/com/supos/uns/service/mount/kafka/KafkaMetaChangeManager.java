package com.supos.uns.service.mount.kafka;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.mount.meta.connection.ConnectionDto;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountUtils;
import com.supos.uns.service.mount.adpter.MetaChangeManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MetaChangeManager
 * @date 2025/9/29 10:57
 */
@Slf4j
public class KafkaMetaChangeManager extends MetaChangeManager {

    private MountCoreService mountCoreService;
    private KafkaAdapter kafkaAdapter;

    public KafkaMetaChangeManager(MountCoreService mountCoreService, KafkaAdapter kafkaAdapter) {
        super(mountCoreService);
        this.mountCoreService = mountCoreService;
        this.kafkaAdapter = kafkaAdapter;
    }

    @Override
    protected Pair<Boolean, JSONObject> isConnectChange(JSONObject config, ConnectionResp connectionResp) {
        ConnectionDto  connection = connectionResp.getConnections().get(0);
        Map<String, Object> configMap = connection.getConfig();

        Boolean isChange = false;
        String newHosts = configMap.get("nodeHosts") != null ? configMap.get("nodeHosts").toString() : null;
        if (!StringUtils.equals(newHosts, config.getString("nodeHosts"))) {
            config.put("nodeHosts", newHosts);
            isChange = true;
        }
        return Pair.of(isChange,  config);
    }

    /**
     * 处理Topic消息，并转化为uns文件
     */
    @Override
    public void handleTopic(UnsMountPo unsMountPo, String connectName) {
        // 删除无效文件
//        removeInvalidFile(unsMountPo, connectName);

        // 处理Topic消息
        Map<String, String> topicMessageMaps =kafkaAdapter.getMessageMap().get(connectName);
        if (MapUtils.isNotEmpty(topicMessageMaps)) {
            for (Map.Entry<String, String> entry : topicMessageMaps.entrySet()) {
                String topic = entry.getKey();
                try {


                    String payload = entry.getValue();
                    createFileForTopic(unsMountPo, connectName, topic, payload, unsMountPo.getTargetAlias());
                } catch (Throwable throwable) {
                    log.error("handleTopic error for {}", topic, throwable);
                }
            }
        }
    }

    /**
     * 删除无效的文件
     * @param unsMountPo
     * @param connectName
     */
    private void removeInvalidFile(UnsMountPo unsMountPo, String connectName) {
        List<UnsPo> files = mountCoreService.listByParentAlias(unsMountPo.getTargetAlias());
        if (CollectionUtil.isNotEmpty(files)) {
            List<String> topics = kafkaAdapter.getTopicMap().get(connectName);
            List<String> validTopics = topics.stream().map(topic -> MountUtils.alias(MountSourceType.KAFKA, connectName, topic)).collect(Collectors.toList());

            List<String> removeAlias = new ArrayList<>();
            for (UnsPo file : files) {
                if (!validTopics.contains(file.getAlias())) {
                    // 删除文件
                    removeAlias.add(file.getAlias());
                }
            }
            if (CollectionUtil.isNotEmpty(removeAlias)) {
                // 删除文件
                mountCoreService.deleteFile(removeAlias);
            }
        }
    }

    @Override
    protected MountSourceType getMountSourceType() {
        return MountSourceType.KAFKA;
    }

    @Override
    protected MountSubSourceType getMountSubSourceType() {
        return MountSubSourceType.KAFKA_ALL;
    }

    @Override
    protected ConnectionResp queryConnect(String connectName) {
        return kafkaAdapter.queryKafka(connectName);
    }

    @Override
    protected void disconnect(JSONObject oldConfig) {
        kafkaAdapter.disconnect(oldConfig);
    }

    @Override
    protected void connect(JSONObject newConfig) {
        kafkaAdapter.connect(newConfig);
        kafkaAdapter.topicChange(newConfig);
        kafkaAdapter.startConsuming(newConfig);
    }
}