package com.supos.uns.service.mount.rabbitmq;

import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.mount.MountDto;
import com.supos.common.dto.mount.MountSourceDto;
import com.supos.common.dto.mount.meta.common.CommonMountSourceDto;
import com.supos.common.enums.mount.*;
import com.supos.uns.dao.po.UnsMountExtendPo;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.mount.MountCoreService;
import com.supos.uns.service.mount.MountFlag;
import com.supos.uns.service.mount.adpter.MountAdpter;

import java.util.List;
import java.util.UUID;

/**
 * @author sunlifang
 * @version 1.0
 * @description: rabbitmq挂载适配器
 * @date 2025/9/26 13:21
 */
public class RabbitmqMountAdpter implements MountAdpter {

    private MountCoreService mountCoreService;

    public RabbitmqMountAdpter(MountCoreService mountCoreService) {
        this.mountCoreService = mountCoreService;
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
        mountInfo.setMountModel(MountModel.RABBITMQ_ALL.getType());
        mountInfo.setSourceAlias(mountSource.getSourceAlias());
        mountInfo.setDataType(mountDto.getDataType());
        mountInfo.setMountSeq(seq);
        mountInfo.setStatus(MountStatus.OFFLINE.getStatus());
        mountInfo.setMountStatus(0);
        mountInfo.setWithFlags(flags);
        mountInfo.setSourceType(MountSourceType.RABBITMQ.getTypeValue());
        mountCoreService.saveMountInfo(mountInfo);

        // 辅挂载信息
        UnsMountExtendPo collectorMountExtendInfo = new UnsMountExtendPo();
        collectorMountExtendInfo.setSourceSubType(MountSubSourceType.RABBITMQ_ALL.getType());
        collectorMountExtendInfo.setTargetAlias(targetUns.getAlias());
        collectorMountExtendInfo.setFirstSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSecondSourceAlias(mountSource.getSourceAlias());
        collectorMountExtendInfo.setSourceName(mountSource.getSourceName());
        collectorMountExtendInfo.setMountSeq(seq);
        mountCoreService.saveMountExtend(collectorMountExtendInfo);
    }

    @Override
    public void handleMount() {

    }

    @Override
    public List<CommonMountSourceDto> queryMountSource() {
        return null;
    }


}
