package com.supos.uns.service.mount.collector;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mount.meta.common.CommonFileMetaDto;
import com.supos.common.dto.mount.meta.common.CommonFolderMetaDto;
import com.supos.common.dto.mount.meta.gateway.*;
import com.supos.common.enums.mount.MountModel;
import com.supos.common.enums.mount.MountSourceType;
import com.supos.common.enums.mount.MountSubSourceType;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: MetaChangeManage
 * @date 2025/9/20 14:00
 */
@Slf4j
public class CollectorMetaChangeManager {

    private MountCoreService mountCoreService;
    private CollectorAdpter collectorAdpter;

    public CollectorMetaChangeManager(MountCoreService mountCoreService, CollectorAdpter collectorAdpter) {
        this.mountCoreService = mountCoreService;
        this.collectorAdpter = collectorAdpter;
    }

    private void preChange(UnsMountPo unsMountPo, CollectorVersionResp currentVersion) {
        if (unsMountPo.getMountStatus() == 1L) {
            unsMountPo.setVersion(unsMountPo.getNextVersion());
            unsMountPo.setNextVersion(currentVersion.getVersion().toString());

            unsMountPo.setMountStatus(-1);
            mountCoreService.updateMountInfo(unsMountPo);
            log.info("change version:{}", JSON.toJSONString(unsMountPo));
        }
    }

    private void postChange(UnsMountPo unsMountPo) {
        unsMountPo.setMountStatus(1);
        mountCoreService.updateMountInfo(unsMountPo);
    }

    /**
     * 元数据变更处理
     */
    public void metaChange(CollectorVersionResp currentVersion, UnsMountPo mount) {
        try {
            if (mount.getNextVersion().equals(currentVersion.getVersion().toString())) {
                return;
            }

            preChange(mount, currentVersion);
            // 采集器是否变更
            collectorMetaChange(mount);

            // 设备是否变更
            deviceMetaChange(mount);

            // 位号是否变更
            tagMetaChange(mount);
            postChange(mount);
        } catch (Throwable throwable) {
            log.error("meta change error:{}", throwable.getMessage(), throwable);
        }
    }

    /**
     * 采集器元数据变更
     * @param unsMountPo
     * @return
     */
    public void collectorMetaChange(UnsMountPo unsMountPo) {

        CollectorMetaChangeResp collectorMetaChangeResp = collectorAdpter.queryCollectorChange(unsMountPo.getSourceAlias(), unsMountPo.getVersion(), unsMountPo.getNextVersion(), null);
        if (collectorMetaChangeResp.hasChange() && CollectionUtil.isNotEmpty(collectorMetaChangeResp.getDeleteCollectorMetaDtos())) {
            // 仅关注采集器删除情况
            for (CollectorMetaDto deleteCollectorMetaDto : collectorMetaChangeResp.getDeleteCollectorMetaDtos()) {
                if (unsMountPo.getSourceAlias().equals(deleteCollectorMetaDto.getAliasName())) {
                    // 采集器网关已被删除
                    deleteCollector(unsMountPo);
                }
            }
        }
    }

    public void deleteCollector(UnsMountPo collectorMount) {
        log.info("delete mount collector folder {}", collectorMount.getSourceAlias());
        MountSourceType mountSourceType = MountSourceType.COLLECTOR;
        UnsPo collectorFolder = mountCoreService.queryUnsByAlias(collectorMount.getTargetAlias());
        if (collectorFolder != null) {
            if (collectorFolder.getMountType() != null && mountSourceType.getTypeValue() == MountSourceType.COLLECTOR.getTypeValue()) {
                // 仅删除下级节点
                mountCoreService.clearFolder(collectorFolder);

                // 更新目标文件夹的挂载属性
                mountCoreService.clearFolderMount(collectorFolder);


                // 删除设备挂载
                mountCoreService.deleteMountBySeq(collectorMount.getMountSeq());

            } else {
                log.warn("the folder {} is not mount by collector, skip.", collectorMount.getSourceAlias());
            }
        }

    }

    /**
     * 设备元数据变更
     * @param unsMountPo
     */
    public void deviceMetaChange(UnsMountPo unsMountPo) {
        DeviceMetaChangeResp deviceMetaChangeResp = collectorAdpter.queryDeviceChange(unsMountPo.getSourceAlias(), unsMountPo.getVersion(), unsMountPo.getNextVersion());
        if (deviceMetaChangeResp.hasChange()) {
            // 处理设备删除情况
            List<DeviceMetaDto> deleteDeviceMetaDtos = deviceMetaChangeResp.getDeleteDeviceMetaDtos();
            if (CollectionUtil.isNotEmpty(deleteDeviceMetaDtos)) {
                log.info("found {} device to delete for collector {}", deleteDeviceMetaDtos.size(), unsMountPo.getSourceAlias());
                deleteDevice(unsMountPo, deleteDeviceMetaDtos);
            }

            // 处理设备新增/修改情况
            List<DeviceMetaDto> saveDeviceMetaDtos = deviceMetaChangeResp.getSaveDeviceMetaDtos();
            if (CollectionUtil.isNotEmpty(saveDeviceMetaDtos)) {
                if (MountModel.COLLECTOR_ALL.getType().equals(unsMountPo.getMountModel())) {
                    // 仅采集器全挂载需要关注新增/修改的情况
                    log.info("found {} device to save for collector {}", saveDeviceMetaDtos.size(), unsMountPo.getSourceAlias());
                    saveDevice(unsMountPo, saveDeviceMetaDtos);
                }
            }
        }
    }

    /**
     * 处理设备删除
     * @param collectorMount
     * @param deviceMetas
     */
    private void deleteDevice(UnsMountPo collectorMount, List<DeviceMetaDto> deviceMetas) {
        MountSourceType mountSourceType = MountSourceType.COLLECTOR;
        for (DeviceMetaDto deviceMeta : deviceMetas) {
            log.info("delete mount collector device folder {}", deviceMeta.getCode());
            UnsPo deviceFolder = mountCoreService.queryUnsByAlias(deviceMeta.getCode());
            if (deviceFolder != null) {
                if (deviceFolder.getMountType() != null && mountSourceType.getTypeValue() == MountSourceType.COLLECTOR.getTypeValue()) {
                    // 删除文件夹
                    mountCoreService.removeFolder(deviceFolder);

                    // 删除设备挂载
                    mountCoreService.deleteMountExtend(collectorMount.getMountSeq(), collectorMount.getSourceAlias(), deviceMeta.getCode());
                } else {
                    log.warn("the folder {} is not mount by collector, skip.", deviceMeta.getCode());
                }
            }
        }
    }

    /**
     * 处理设备新增/修改
     * @param collectorMount
     * @param deviceMetas
     */
    private void saveDevice(UnsMountPo collectorMount, List<DeviceMetaDto> deviceMetas) {
        Set<String> aliases = deviceMetas.stream().map(DeviceMetaDto::getCode).collect(Collectors.toSet());

        List<UnsPo> existDeviceFolders = mountCoreService.listByAlias(aliases);
        Set<String> existDeviceAliases = existDeviceFolders.stream().map(UnsPo::getAlias).collect(Collectors.toSet());

        List<UnsMountExtendPo> mountExtendInfos = new ArrayList<>();
        List<CommonFolderMetaDto> folders = new ArrayList<>();
        for (DeviceMetaDto deviceMeta : deviceMetas) {
            if (!existDeviceAliases.contains(deviceMeta.getCode())) {
                CommonFolderMetaDto folder = new CommonFolderMetaDto();
                folder.setName(deviceMeta.getName());
                folder.setCode(deviceMeta.getCode());
                folder.setDisplayName(deviceMeta.getName());
                folder.setMountType(MountSourceType.COLLECTOR.getTypeValue());
                folder.setMountSource(deviceMeta.getCode());
                folders.add(folder);

                UnsMountExtendPo deviceMountExtendInfo = new UnsMountExtendPo();
                deviceMountExtendInfo.setSourceSubType(MountSubSourceType.COLLECTOR_DEVICE.getType());
                deviceMountExtendInfo.setTargetAlias(deviceMeta.getCode());
                deviceMountExtendInfo.setFirstSourceAlias(collectorMount.getSourceAlias());
                deviceMountExtendInfo.setSecondSourceAlias(deviceMeta.getCode());
                deviceMountExtendInfo.setSourceName(deviceMeta.getName());
                deviceMountExtendInfo.setMountSeq(collectorMount.getMountSeq());
                mountExtendInfos.add(deviceMountExtendInfo);

            } else {
                log.info("existed mount device folder {}, skip!", deviceMeta.getCode());
            }
        }
        if (CollectionUtil.isNotEmpty(folders)) {
            log.info("mount {} devices for collector {}", folders.size(), collectorMount.getSourceAlias());
            // 为设备创建对应文件夹
            mountCoreService.createFolder(collectorMount.getTargetAlias(), folders);
            // 保存设备挂载信息
            mountCoreService.saveMountExtend(mountExtendInfos);
        }
    }

    /**
     * 位号元数据变更
     * @param unsMountPo
     */
    @Transactional(rollbackFor = Throwable.class)
    public void tagMetaChange(UnsMountPo unsMountPo) {
        Set<String> deviceAliases = null;
        if (MountModel.COLLECTOR_DEVICE.getType().equals(unsMountPo.getMountModel())) {
            deviceAliases = new HashSet<>();
            // 采集器设备挂载，获取采集器设备的位号
            List<UnsMountExtendPo> unsMountExtendPos = mountCoreService.queryMountExtendInfo(MountSubSourceType.COLLECTOR_DEVICE, null, unsMountPo.getMountSeq());
            deviceAliases.addAll(unsMountExtendPos.stream().map(UnsMountExtendPo::getSecondSourceAlias).collect(Collectors.toSet()));
        }

        TagMetaChangeResp tagMetaChangeResp = collectorAdpter.queryTagChange(unsMountPo.getSourceAlias(), deviceAliases, unsMountPo.getVersion(), unsMountPo.getNextVersion());
        if (tagMetaChangeResp.hasChange()) {
            // 处理位号删除情况
            List<TagMetaDto> deleteTagMetaDtos = tagMetaChangeResp.getDeleteTagMetaDtos();
            if (CollectionUtil.isNotEmpty(deleteTagMetaDtos)) {
                log.info("found {} tag to delete for collector {}", deleteTagMetaDtos.size(), unsMountPo.getSourceAlias());
                deleteTag(deleteTagMetaDtos);
            }

            // 处理位号新增/修改情况
            List<TagMetaDto> saveTagMetaDtos = tagMetaChangeResp.getSaveTagMetaDtos();
            if (CollectionUtil.isNotEmpty(saveTagMetaDtos)) {
                if (MountModel.COLLECTOR_ALL.getType().equals(unsMountPo.getMountModel())) {
                    log.info("found {} tag to save for collector {}", saveTagMetaDtos.size(), unsMountPo.getSourceAlias());
                    saveTag(unsMountPo, saveTagMetaDtos);
                }
            }
        }
    }

    /**
     * 处理位号删除
     * @param deleteTagMetas
     */
    private void deleteTag(List<TagMetaDto> deleteTagMetas) {
        MountSourceType mountSourceType = MountSourceType.COLLECTOR;
        List<String> removeAlias = new LinkedList<>();
        for (TagMetaDto tag : deleteTagMetas) {
            String unsAlias = String.format("C%s", tag.getCode());
            CreateTopicDto existFile = mountCoreService.getDefinitionByAlias(unsAlias);
            if (existFile != null) {
                if (existFile.getMountType() != null && mountSourceType.getTypeValue() == existFile.getMountType()) {
                    removeAlias.add(unsAlias);
                } else {
                    log.warn("found exist tag {} is not mount by collector, skip.", tag.getCode());
                }
            }
        }
        if (CollectionUtil.isNotEmpty(removeAlias)) {
            mountCoreService.deleteFile(removeAlias);
        }
    }

    private void saveTag(UnsMountPo unsMountPo, List<TagMetaDto> saveTagMetas) {
        MountSourceType mountSourceType = MountSourceType.COLLECTOR;
        Map<String, List<CommonFileMetaDto>> saveFileMap = new HashMap<>();
        for (TagMetaDto tag : saveTagMetas) {
            String deviceAlias = tag.getDeviceAlias();

            String parentUnsAlias = null;
            if (StringUtils.isNotBlank(deviceAlias)) {
                // 位号挂在源点下面
                parentUnsAlias = deviceAlias;
            } else {
                // 位号直接挂在采集器下面
                parentUnsAlias = unsMountPo.getTargetAlias();
            }

            UnsPo parentFolder = mountCoreService.queryUnsByAlias(parentUnsAlias);
            if (parentFolder == null) {
                log.warn("tag {} mount to under folder {} not found", tag.getCode(), parentUnsAlias);
                continue;
            }

            CommonFileMetaDto file = collectorAdpter.tag2FileMetaDto(tag);
            saveFileMap.computeIfAbsent(parentUnsAlias, k -> new LinkedList<>()).add( file);
        }
        if (MapUtils.isNotEmpty(saveFileMap)) {
            for (Map.Entry<String, List<CommonFileMetaDto>> e : saveFileMap.entrySet()) {
                mountCoreService.saveFile(mountSourceType, e.getKey(), unsMountPo, e.getValue());
            }
        }
    }
}
