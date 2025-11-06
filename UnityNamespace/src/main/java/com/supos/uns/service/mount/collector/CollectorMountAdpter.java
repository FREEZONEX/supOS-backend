package com.supos.uns.service.mount.collector;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.mount.MountDeviceDto;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.MountSourceDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.common.dto.mount.meta.gateway.CollectorMetaChangeResp;
import com.supos.common.dto.mount.meta.gateway.CollectorVersionResp;
import com.supos.common.enums.mount.*;
import com.supos.common.exception.BuzException;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountFlag;
import com.supos.uns.service.mount.adpter.MountAdpter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器挂载适配器
 * @date 2025/9/18 16:01
 */
@Slf4j
public class CollectorMountAdpter implements MountAdpter {

    private MountCoreService mountCoreService;

    private CollectorAdpter collectorAdpter;

    private CollectorInitMountManager collectorInitMountManager;
    private CollectorMetaChangeManager collectorMetaChangeManager;
    private CollectorOnlineManager collectorOnlineManager;

    public CollectorMountAdpter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
        this.collectorAdpter = new CollectorAdpter();
        this.collectorMetaChangeManager = new CollectorMetaChangeManager(mountCoreService, collectorAdpter);
        this.collectorInitMountManager = new CollectorInitMountManager(mountCoreService, collectorAdpter);
        this.collectorOnlineManager = new CollectorOnlineManager(mountCoreService, collectorAdpter);
    }

    @Override
    public void createMountInfo(UnsPo targetUns, MountDto mountDto) {
        MountSourceDto mountSource = mountDto.getExtend();
        if (!mountConflict(mountSource)) {
            throw new BuzException("uns.mount.onlyone.collector.all");
        }
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
        if (CollectionUtil.isNotEmpty(mountSource.getDevices())) {
            // 创建设备挂载信息
            createDeviceMountInfo(seq, targetUns, mountSource, mountDto.getDataType(), flags);
        } else {
            // 创建采集器挂载信息
            createCollectorMountInfo(seq, targetUns, mountSource, mountDto.getDataType(), flags);
        }
    }

    @Override
    public void handleMount() {
        CollectorVersionResp version = collectorAdpter.querycollectorMetaVersion();
        if (version == null) {
            return;
        }
        List<UnsMountPo> mountPos = mountCoreService.queryMountInfo(MountModel.COLLECTOR_ALL, null,null);
        if (CollectionUtil.isNotEmpty(mountPos)) {
            for (UnsMountPo mount : mountPos) {
                try {
                    collectorOnlineManager.checkOnline(mount);
                    if (mount.getMountStatus() == null || mount.getMountStatus() == 0) {
                        collectorInitMountManager.init(version,  mount);
                    } else if (mount.getMountStatus() == 1 || mount.getMountStatus() == -1) {
                        collectorMetaChangeManager.metaChange(version,  mount);
                    }

                } catch (Throwable throwable) {
                    log.error("handle collector mount error", throwable);
                }
            }
        }
    }

    @Override
    public List<CommonMountSourceDto> queryMountSource() {
        CollectorMetaChangeResp changeResp = collectorAdpter.queryCollectorChange(null, null, null,  true);
        if (changeResp != null && CollectionUtil.isNotEmpty(changeResp.getSaveCollectorMetaDtos())) {
            List<CommonMountSourceDto> mountSourceDtos = changeResp.getSaveCollectorMetaDtos().stream().map(collectorMetaDto -> {
                CommonMountSourceDto mountSourceDto = new CommonMountSourceDto();
                mountSourceDto.setAlias(collectorMetaDto.getAliasName());
                mountSourceDto.setName(collectorMetaDto.getDisplayName());
                return mountSourceDto;
            }).collect(Collectors.toList());
            return mountSourceDtos;
        }
        return new ArrayList<>();
    }


    /**
     * 判断挂载是否存在冲突
     * @param mountSource
     * @return
     */
    private boolean mountConflict(MountSourceDto mountSource) {
        // 获取已存在的采集器挂载
        List<UnsMountPo> existMounts = queryMountInfo(mountSource.getSourceAlias());
        if (CollectionUtil.isNotEmpty(existMounts)) {
            for (UnsMountPo existMount : existMounts) {
                if (MountModel.COLLECTOR_ALL.getType().equals(existMount.getMountModel())) {
                    // 已经存在全挂载，怎么都不能再创建挂载了
                    return false;
                }
            }

            List<MountDeviceDto> devices = mountSource.getDevices();
            if (CollectionUtil.isEmpty(devices)) {
                // 没有勾选源点，说明是创建采集器全挂载。但已经存在对应采集器挂载了，无法再次创建
                return false;
            } else {
                // 勾选了源点，判断源点是否被其他采集器挂载
                List<UnsMountExtendPo> existMountDevices = queryMountExtendInfo(mountSource.getSourceAlias(), null);
                Set<String> existMountDeviceAliasSet = existMountDevices.stream().map(UnsMountExtendPo::getSecondSourceAlias).collect(Collectors.toSet());
                for (MountDeviceDto device : devices) {
                    if (existMountDeviceAliasSet.contains(device.getAlias())) {
                        // 存在源点被其他采集器挂载，无法再次创建
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 创建采集器挂载信息
     * @param targetUns
     * @param mountSource
     * @param dataType
     * @return
     */
    private void createCollectorMountInfo(String seq, UnsPo targetUns, MountSourceDto mountSource, Integer dataType, Integer flags) {
        // 主挂载信息
        UnsMountPo mountInfo = new UnsMountPo();
        mountInfo.setTargetType(MountTargetType.FOLDER.getType());
        mountInfo.setTargetAlias(targetUns.getAlias());
        mountInfo.setMountModel(MountModel.COLLECTOR_ALL.getType());
        mountInfo.setSourceAlias(mountSource.getSourceAlias());
        mountInfo.setDataType(dataType);
        mountInfo.setMountSeq(seq);
        mountInfo.setStatus(MountStatus.OFFLINE.getStatus());
        mountInfo.setMountStatus(0);
        mountInfo.setWithFlags(flags);
        mountCoreService.saveMountInfo(mountInfo);

        // 辅挂载信息
        UnsMountExtendPo collectorMountExtendInfo = new UnsMountExtendPo();
        collectorMountExtendInfo.setSourceSubType(MountSubSourceType.COLLECTOR.getType());
        collectorMountExtendInfo.setTargetAlias(targetUns.getAlias());
        collectorMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSecondSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSourceName(mountSource.getSourceName());
        collectorMountExtendInfo.setMountSeq(seq);
        mountCoreService.saveMountExtend(collectorMountExtendInfo);
    }

    /**
     * 创建设备挂载信息
     * @param seq
     * @param targetUns
     * @param mountSource
     */
    private void createDeviceMountInfo(String seq, UnsPo targetUns, MountSourceDto mountSource, Integer dataType, Integer flags) {
        // 主挂载信息
        UnsMountPo mountInfo = new UnsMountPo();
        mountInfo.setTargetType(MountTargetType.FOLDER.getType());
        mountInfo.setTargetAlias(targetUns.getAlias());
        mountInfo.setMountModel(MountModel.COLLECTOR_DEVICE.getType());
        mountInfo.setSourceAlias(mountSource.getSourceAlias());
        mountInfo.setDataType(dataType);
        mountInfo.setMountSeq(seq);
        mountInfo.setStatus(MountStatus.OFFLINE.getStatus());
        mountInfo.setMountStatus(0);
        mountInfo.setWithFlags(flags);
        mountInfo.setSourceType(MountSourceType.COLLECTOR.getTypeValue());
        mountCoreService.saveMountInfo(mountInfo);

        // 辅挂载信息
        List<UnsMountExtendPo> mountExtendInfos = new ArrayList<>();
        // 直接创建设备文件夹
        List<CommonFolderMetaDto> folders = new ArrayList<>();

        UnsMountExtendPo collectorMountExtendInfo = new UnsMountExtendPo();
        collectorMountExtendInfo.setSourceSubType(MountSubSourceType.COLLECTOR.getType());
        collectorMountExtendInfo.setTargetAlias(targetUns.getAlias());
        collectorMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSecondSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSourceName(mountSource.getSourceName());
        collectorMountExtendInfo.setMountSeq(seq);
        mountExtendInfos.add(collectorMountExtendInfo);

        for (MountDeviceDto device : mountSource.getDevices()) {
            CommonFolderMetaDto folder = new CommonFolderMetaDto();
            folder.setName(device.getName());
            folder.setCode(device.getAlias());
            folder.setDisplayName(device.getName());
            folders.add(folder);

            UnsMountExtendPo deviceMountExtendInfo = new UnsMountExtendPo();
            deviceMountExtendInfo.setSourceSubType(MountSubSourceType.COLLECTOR_DEVICE.getType());
            deviceMountExtendInfo.setTargetAlias(device.getAlias());
            deviceMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
            deviceMountExtendInfo.setSecondSourceAlias(device.getAlias());
            deviceMountExtendInfo.setSourceName(device.getName());
            deviceMountExtendInfo.setMountSeq(seq);
            mountExtendInfos.add(deviceMountExtendInfo);
        }

        mountCoreService.createFolder(targetUns.getAlias(), folders);
        mountCoreService.saveMountExtend(mountExtendInfos);
    }

    /**
     * 查询采集器主挂载信息
     * @return
     */
    public List<UnsMountPo> queryMountInfo(String sourceAlias) {
        List<UnsMountPo> mounts = new ArrayList<>();
        mounts.addAll(mountCoreService.queryMountInfo(MountModel.COLLECTOR_ALL, sourceAlias, null));
        mounts.addAll(mountCoreService.queryMountInfo(MountModel.COLLECTOR_DEVICE, sourceAlias, null));
        return mounts;
    }

    /**
     * 查询采集器辅挂载信息
     * @return
     */
    public List<UnsMountExtendPo> queryMountExtendInfo(String firstSourceAlias, String mountSeq) {
        List<UnsMountExtendPo> mounts = new ArrayList<>();
        mounts.addAll(mountCoreService.queryMountExtendInfo(MountSubSourceType.COLLECTOR, firstSourceAlias, mountSeq));
        mounts.addAll(mountCoreService.queryMountExtendInfo(MountSubSourceType.COLLECTOR_DEVICE, firstSourceAlias, mountSeq));
        return mounts;
    }
}
