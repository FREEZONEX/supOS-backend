package com.supos.uns.service.mount.mqtt;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.dto.mount.meta.connection.ConnectionDto;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
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

/**
 * @author sunlifang
 * @version 1.0
 * @description: MetaChangeManager
 * @date 2025/9/29 10:57
 */
@Slf4j
public class MqttMetaChangeManager extends MetaChangeManager {

    private MountCoreService mountCoreService;
    private MqttAdpter mqttAdpter;

    public MqttMetaChangeManager(MountCoreService mountCoreService, MqttAdpter mqttAdpter) {
        super(mountCoreService);
        this.mountCoreService = mountCoreService;
        this.mqttAdpter = mqttAdpter;
    }

    /**
     * 判断连接是否改变
     */
    @Override
    protected Pair<Boolean, JSONObject> isConnectChange(JSONObject config, ConnectionResp connectionResp) {
        ConnectionDto connection = connectionResp.getConnections().get(0);
        Map<String, Object> configMap = connection.getConfig();

        Boolean isChange = false;
        String newHost = configMap.get("host") != null ? configMap.get("host").toString() : null;
        if (!StringUtils.equals(newHost, config.getString("host"))) {
            config.put("host", newHost);
            isChange = true;
        }

        String newPort = configMap.get("port") != null ? configMap.get("port").toString() : null;
        if (!StringUtils.equals(newPort, config.getString("port"))) {
            config.put("port", newPort);
            isChange = true;
        }

        String newUsername = configMap.get("username") != null ? configMap.get("username").toString() : null;
        if (!StringUtils.equals(newUsername, config.getString("username"))) {
            config.put("username", newUsername);
            isChange = true;
        }

        String newPassword = configMap.get("password") != null ? configMap.get("password").toString() : null;
        if (!StringUtils.equals(newPassword, config.getString("password"))) {
            config.put("password", newPassword);
            isChange = true;
        }
        return Pair.of(isChange,  config);
    }

    /**
     * 处理Topic消息，并转化为uns文件
     */
    @Override
    public void handleTopic(UnsMountPo unsMountPo, String connectName) {
        Map<String, String> topicMaps = mqttAdpter.getTopicMaps().get(connectName);
        if (MapUtils.isNotEmpty(topicMaps)) {
            for (Map.Entry<String, String> entry : topicMaps.entrySet()) {
                String topic = entry.getKey();
                try {
                    String parentAlias = null;
                    // topic包含/符号，先创建文件夹
                    if (StringUtils.contains(topic, '/')) {
                        parentAlias = createFolderForTopic(unsMountPo, connectName, topic);
                    } else {
                        parentAlias = unsMountPo.getTargetAlias();
                    }

                    String payload = entry.getValue();
                    createFileForTopic(unsMountPo, connectName, topic, payload, parentAlias);
                } catch (Throwable throwable) {
                    log.error("handleTopic error for {}", topic, throwable);
                }
            }
        }
    }

    @Override
    protected String createFolderForTopic(UnsMountPo unsMountPo, String connectName, String topic) {
        MountSourceType mountSourceType = getMountSourceType();
        String parentAlias = unsMountPo.getTargetAlias();
        String path = "";
        String[] topicPaths = topic.split("/");
        for (int i = 0; i < topicPaths.length - 1; i++) {
            if (StringUtils.isEmpty(topicPaths[i])) {
                continue;
            }
            path += "/" + topicPaths[i];
            String alias = MountUtils.alias(mountSourceType, connectName, path);

            CreateTopicDto createTopicDto = mountCoreService.getDefinitionByAlias(alias);
            if (createTopicDto != null) {
                parentAlias = alias;
                continue;
            }
            List<UnsMountExtendPo> mountExtendInfos = new ArrayList<>();
            List<CommonFolderMetaDto> folders = new ArrayList<>();
            CommonFolderMetaDto folder = new CommonFolderMetaDto();
            folder.setCode(alias);
            folder.setName(topicPaths[i]);
            folder.setDisplayName(topicPaths[i]);
            folder.setMountType(mountSourceType.getTypeValue());
            folder.setMountSource(topicPaths[i]);
            folders.add(folder);

            UnsMountExtendPo deviceMountExtendInfo = new UnsMountExtendPo();
            deviceMountExtendInfo.setSourceSubType(MountSubSourceType.MQTT_FOLDER.getType());
            deviceMountExtendInfo.setTargetAlias(alias);
            deviceMountExtendInfo.setFirstSourceAlias(unsMountPo.getSourceAlias());
            deviceMountExtendInfo.setSecondSourceAlias(topicPaths[i]);
            deviceMountExtendInfo.setSourceName(topicPaths[i]);
            deviceMountExtendInfo.setMountSeq(unsMountPo.getMountSeq());
            mountExtendInfos.add(deviceMountExtendInfo);

            mountCoreService.createFolder(parentAlias,  folders);
            mountCoreService.saveMountExtend(mountExtendInfos);
            parentAlias = alias;
        }
        return parentAlias;
    }

    @Override
    protected MountSourceType getMountSourceType() {
        return MountSourceType.MQTT;
    }

    @Override
    protected MountSubSourceType getMountSubSourceType() {
        return MountSubSourceType.MQTT_ALL;
    }

    @Override
    protected ConnectionResp queryConnect(String connectName) {
        return mqttAdpter.queryMqtt(connectName);
    }

    @Override
    protected void disconnect(JSONObject oldConfig) {
        mqttAdpter.closeMqttClient(oldConfig);
    }

    @Override
    protected void connect(JSONObject newConfig) {
        mqttAdpter.subscribeTopic(newConfig);
    }
}