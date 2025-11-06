package com.supos.uns.service.mount.collector;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.supos.common.dto.mount.meta.common.CommonFileMetaDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.dto.mount.meta.gateway.*;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.service.mount.MountCoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 采集器挂载初始化管理
 * @author sunlifang
 * @version 1.0
 * @description: InitMountManager
 * @date 2025/9/20 14:32
 */
@Slf4j
public class CollectorInitMountManager {

    private MountCoreService mountCoreService;
    private CollectorAdpter collectorAdpter;

    public CollectorInitMountManager(MountCoreService mountCoreService, CollectorAdpter collectorAdpter) {
        this.mountCoreService = mountCoreService;
        this.collectorAdpter = collectorAdpter;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void init(CollectorVersionResp version, UnsMountPo mount) {
        try {
            preChange(mount, version.getVersion());
            mountCollectorAll(mount);
            postChange(mount);
        } catch (Throwable throwable) {
            log.error("init collector mount target:{}, source:{} error", mount.getTargetAlias(), mount.getSourceAlias(), throwable);
        }
    }

    private void preChange(UnsMountPo unsMountPo, Long version) {
        unsMountPo.setNextVersion(version.toString());
        mountCoreService.updateMountInfo(unsMountPo);
        log.info("change version:{}", JSON.toJSONString(unsMountPo));
    }

    private void postChange(UnsMountPo unsMountPo) {
        unsMountPo.setMountStatus(1);
        mountCoreService.updateMountInfo(unsMountPo);
    }

    /**
     * 采集器全挂载
     * @param mount
     */
    private void mountCollectorAll(UnsMountPo mount) {
        // 全挂载初始化时，先创建采集器下所有设备文件夹
        DeviceMetaChangeResp deviceMetaChangeResp = collectorAdpter.queryDeviceChange(mount.getSourceAlias(), null, mount.getNextVersion());
        if (CollectionUtil.isNotEmpty(deviceMetaChangeResp.getSaveDeviceMetaDtos())) {
            log.info("found {} devices from collector {}", deviceMetaChangeResp.getSaveDeviceMetaDtos().size(), mount.getSourceAlias());
            // 初始挂载仅关注需要保存的
            List<UnsMountExtendPo> mountExtendInfos = new ArrayList<>();
            List<CommonFolderMetaDto> folders = new ArrayList<>();
            for (DeviceMetaDto device : deviceMetaChangeResp.getSaveDeviceMetaDtos()) {
                CommonFolderMetaDto folder = new CommonFolderMetaDto();
                folder.setName(device.getName());
                folder.setCode(device.getCode());
                folder.setDisplayName(device.getName());
                folders.add(folder);

                UnsMountExtendPo deviceMountExtendInfo = new UnsMountExtendPo();
                deviceMountExtendInfo.setSourceSubType(MountSubSourceType.COLLECTOR_DEVICE.getType());
                deviceMountExtendInfo.setTargetAlias(device.getCode());
                deviceMountExtendInfo.setFirstSourceAlias(mount.getSourceAlias());
                deviceMountExtendInfo.setSecondSourceAlias(device.getCode());
                deviceMountExtendInfo.setSourceName(device.getName());
                deviceMountExtendInfo.setMountSeq(mount.getMountSeq());
                mountExtendInfos.add(deviceMountExtendInfo);
            }
            // 为设备创建对应文件夹
            mountCoreService.createFolder(mount.getTargetAlias(), folders);
            // 保存设备挂载信息
            mountCoreService.saveMountExtend(mountExtendInfos);
            log.info("mount {} devices for collector {}", folders.size(), mount.getSourceAlias());
        }

        // 挂载所有位号
        TagMetaChangeResp tagMetaChangeResp = collectorAdpter.queryTagChange(mount.getSourceAlias(), null, null, mount.getNextVersion());
        saveTagToFile(mount, tagMetaChangeResp);
    }

    /**
     * 采集器部分设备挂载
     * @param mount
     */
    private void mountCollectorDevice(UnsMountPo mount) {

        // 获取采集器选择的设备
        Set<String> deviceAliases = new HashSet<>();
        List<UnsMountExtendPo> mountExtends = mountCoreService.queryMountExtendInfo(MountSubSourceType.COLLECTOR_DEVICE, null, mount.getMountSeq());
        if (CollectionUtil.isEmpty(mountExtends)) {
            return;
        }
        deviceAliases.addAll(mountExtends.stream().map(UnsMountExtendPo::getSecondSourceAlias).collect(Collectors.toSet()));

        TagMetaChangeResp tagMetaChangeResp = collectorAdpter.queryTagChange(mount.getSourceAlias(), deviceAliases, null, mount.getNextVersion());
        saveTagToFile(mount, tagMetaChangeResp);
    }

    /**
     * 将位号信息保存为uns文件
     * @param mount
     * @param tagMetaChangeResp
     */
    private void saveTagToFile(UnsMountPo mount, TagMetaChangeResp tagMetaChangeResp) {
        if (CollectionUtil.isNotEmpty(tagMetaChangeResp.getSaveTagMetaDtos())) {
            log.info("found {} tags from collector {}", tagMetaChangeResp.getSaveTagMetaDtos().size(), mount.getSourceAlias());
            // 初始挂载仅关注需要保存的
            Map<String, List<CommonFileMetaDto>> tagMap = new HashMap<>();
            for (TagMetaDto tag : tagMetaChangeResp.getSaveTagMetaDtos()) {
                // 将位号信息转换成文件信息
                CommonFileMetaDto file = collectorAdpter.tag2FileMetaDto(tag);
                List<CommonFileMetaDto> files = tagMap.computeIfAbsent(tag.getDeviceAlias(), k -> new LinkedList<>());
                files.add(file);
            }
            for (Map.Entry<String, List<CommonFileMetaDto>> entry : tagMap.entrySet()) {
                String parentAlias = entry.getKey();
                if (parentAlias == null) {
                    parentAlias = mount.getTargetAlias();
                }
                mountCoreService.saveFile(MountSourceType.COLLECTOR, parentAlias, mount, entry.getValue());
            }
        }
    }
}
