package com.supos.uns.service.mount.kafka;

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
 * @description: kafka挂载适配器
 * @date 2025/9/26 13:21
 */
@Slf4j
public class KafkaMountAdpter implements MountAdpter {

    private MountCoreService mountCoreService;

    @Getter
    private KafkaAdapter kafkaAdapter;

    private KafkaOnlineManager kafkaOnlineManager;

    @Getter
    private KafkaMetaChangeManager kafkaMetaChangeManager;

    public KafkaMountAdpter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
        this.kafkaAdapter = new KafkaAdapter(mountCoreService);
        this.kafkaOnlineManager = new KafkaOnlineManager(mountCoreService, kafkaAdapter);
        this.kafkaMetaChangeManager = new KafkaMetaChangeManager(mountCoreService, kafkaAdapter);
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
        mountInfo.setMountModel(MountModel.KAFKA_ALL.getType());
        mountInfo.setSourceAlias(mountSource.getSourceAlias());
        mountInfo.setDataType(mountDto.getDataType());
        mountInfo.setMountSeq(seq);
        mountInfo.setStatus(MountStatus.OFFLINE.getStatus());
        mountInfo.setMountStatus(0);
        mountInfo.setWithFlags(flags);
        mountInfo.setSourceType(MountSourceType.KAFKA.getTypeValue());
        mountCoreService.saveMountInfo(mountInfo);

        // 辅挂载信息
        UnsMountExtendPo collectorMountExtendInfo = new UnsMountExtendPo();
        collectorMountExtendInfo.setSourceSubType(MountSubSourceType.KAFKA_ALL.getType());
        collectorMountExtendInfo.setTargetAlias(targetUns.getAlias());
        collectorMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSecondSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSourceName(mountSource.getSourceName());
        collectorMountExtendInfo.setMountSeq(seq);

        ConnectionResp connectionResp = kafkaAdapter.queryKafka(mountSource.getSourceAlias());
        if (connectionResp == null || CollectionUtil.isEmpty(connectionResp.getConnections())) {
            throw new BuzException("kafka连接不存在");
        }
        ConnectionDto connection = connectionResp.getConnections().get(0);
        Map<String, Object> config =connection.getConfig();
        JSONObject configJson = new JSONObject();
        configJson.put("connectName", connection.getName());
        configJson.put("nodeHosts", config.get("nodeHosts") != null ? config.get("nodeHosts").toString() : null);
        //configJson.put("username", config.get("username") != null ? config.get("user").toString() : null);
        //configJson.put("password", config.get("password") != null ? config.get("password").toString() : null);
        collectorMountExtendInfo.setExtend(configJson.toJSONString());
        mountCoreService.saveMountExtend(collectorMountExtendInfo);
    }

    @Override
    public void handleMount() {
        List<UnsMountPo> mountPos = mountCoreService.queryMountInfo(MountModel.KAFKA_ALL, null,null);
        if (CollectionUtil.isNotEmpty(mountPos)) {
            for (UnsMountPo mount : mountPos) {
                try {
                    kafkaOnlineManager.checkOnline(mount);

                    kafkaMetaChangeManager.metaChange(mount);
                } catch (Throwable throwable) {
                    log.error("handle kafka mount error", throwable);
                }
            }
        }
    }

    @Override
    public List<CommonMountSourceDto> queryMountSource() {
        List<CommonMountSourceDto> sources = new ArrayList<>();

        ConnectionResp connectionResp = kafkaAdapter.queryKafka(null);
        if (connectionResp != null && CollectionUtil.isNotEmpty(connectionResp.getConnections())) {
            for (ConnectionDto connection : connectionResp.getConnections()) {
                CommonMountSourceDto source = new CommonMountSourceDto();
                source.setAlias(connection.getName());
                source.setName(connection.getName());
                source.setSourceType(MountSourceType.KAFKA.getType());
                sources.add(source);
            }
        }
        return sources;
    }
}
