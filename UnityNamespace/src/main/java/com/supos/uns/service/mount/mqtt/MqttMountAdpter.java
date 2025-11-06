package com.supos.uns.service.mount.mqtt;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.MountSourceDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.common.dto.mount.meta.connection.ConnectionDto;
import com.supos.common.dto.mount.meta.connection.ConnectionResp;
import com.supos.common.enums.mount.*;
import com.supos.common.exception.BuzException;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountFlag;
import com.supos.uns.service.mount.adpter.MountAdpter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MQTT挂载适配器
 * @date 2025/9/26 13:21
 */
@Slf4j
public class MqttMountAdpter implements MountAdpter {

    private MountCoreService mountCoreService;

    @Getter
    private MqttAdpter mqttAdpter;

    private MqttOnlineManager mqttOnlineManager;

    @Getter
    private MqttMetaChangeManager mqttMetaChangeManager;

    public MqttMountAdpter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
        this.mqttAdpter = new MqttAdpter(mountCoreService);
        this.mqttOnlineManager = new MqttOnlineManager(mountCoreService, mqttAdpter);
        this.mqttMetaChangeManager = new MqttMetaChangeManager(mountCoreService, mqttAdpter);
    }

    @Override
    public void createMountInfo(UnsPo targetUns, MountDto mountDto) {
        MountSourceDto mountSource = mountDto.getExtend();

        int flags = 0;
        if (Boolean.TRUE.equals(mountDto.getPersistence())) {
            flags |= MountFlag.SAVE2DB;
        }
        if (Boolean.TRUE.equals(mountDto.getDashboard())) {
            flags |= MountFlag.DASHBOARD;
        }
        if (Boolean.TRUE.equals(mountDto.getSyncMeta())) {
            flags |= MountFlag.SYNCMETA;
        }

        String seq = UUID.randomUUID().toString();
        // 主挂载信息
        UnsMountPo mountInfo = new UnsMountPo();
        mountInfo.setTargetType(MountTargetType.FOLDER.getType());
        mountInfo.setTargetAlias(targetUns.getAlias());
        mountInfo.setMountModel(MountModel.MQTT_ALL.getType());
        mountInfo.setSourceAlias(mountSource.getSourceAlias());
        mountInfo.setDataType(mountDto.getDataType());
        mountInfo.setMountSeq(seq);
        mountInfo.setStatus(MountStatus.OFFLINE.getStatus());
        mountInfo.setMountStatus(0);
        mountInfo.setWithFlags(flags);
        mountInfo.setSourceType(MountSourceType.MQTT.getTypeValue());
        mountCoreService.saveMountInfo(mountInfo);

        // 辅挂载信息
        UnsMountExtendPo collectorMountExtendInfo = new UnsMountExtendPo();
        collectorMountExtendInfo.setSourceSubType(MountSubSourceType.MQTT_ALL.getType());
        collectorMountExtendInfo.setTargetAlias(targetUns.getAlias());
        collectorMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSecondSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSourceName(mountSource.getSourceName());
        collectorMountExtendInfo.setMountSeq(seq);

        ConnectionResp connectionResp = mqttAdpter.queryMqtt(mountSource.getSourceAlias());
        if (connectionResp == null || CollectionUtil.isEmpty(connectionResp.getConnections())) {
            throw new BuzException("MQTT连接不存在");
        }
        ConnectionDto connection = connectionResp.getConnections().get(0);
        Map<String, Object> config =connection.getConfig();
        JSONObject configJson = new JSONObject();
        configJson.put("connectName", connection.getName());
        configJson.put("host", config.get("host") != null ? config.get("host").toString() : null);
        configJson.put("port", config.get("port") != null ? config.get("port").toString() : null);
        configJson.put("username", config.get("username") != null ? config.get("user").toString() : null);
        configJson.put("password", config.get("password") != null ? config.get("password").toString() : null);
        collectorMountExtendInfo.setExtend(configJson.toJSONString());
        mountCoreService.saveMountExtend(collectorMountExtendInfo);
    }

    @Override
    public void handleMount() {
        List<UnsMountPo> mountPos = mountCoreService.queryMountInfo(MountModel.MQTT_ALL, null,null);
        if (CollectionUtil.isNotEmpty(mountPos)) {
            for (UnsMountPo mount : mountPos) {
                try {
                    // 检查在离线情况
                    mqttOnlineManager.checkOnline(mount);

                    // 监控元数据变更
                    mqttMetaChangeManager.metaChange(mount);
                } catch (Throwable throwable) {
                    log.error("handle mqtt mount error", throwable);
                }
            }
        }
    }

    @Override
    public List<CommonMountSourceDto> queryMountSource() {
        List<CommonMountSourceDto> sources = new ArrayList<>();

        ConnectionResp connectionResp = mqttAdpter.queryMqtt(null);
        if (connectionResp != null && CollectionUtil.isNotEmpty(connectionResp.getConnections())) {
            for (ConnectionDto connection : connectionResp.getConnections()) {
                CommonMountSourceDto source = new CommonMountSourceDto();
                source.setAlias(connection.getName());
                source.setName(connection.getName());
                source.setSourceType(MountSourceType.MQTT.getType());
                sources.add(source);
            }
        }
        return sources;
    }
}
