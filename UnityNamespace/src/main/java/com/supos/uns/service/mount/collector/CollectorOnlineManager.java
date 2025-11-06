package com.supos.uns.service.mount.collector;

import com.supos.common.enums.mount.MountStatus;
import com.supos.uns.dao.po.UnsMountPo;
import com.supos.uns.service.mount.MountCoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author sunlifang
 * @version 1.0
 * @description: OnlineManager
 * @date 2025/9/20 14:32
 */
@Slf4j
public class CollectorOnlineManager {

    private MountCoreService mountCoreService;

    private CollectorAdpter collectorAdpter;

    public CollectorOnlineManager(MountCoreService mountCoreService, CollectorAdpter collectorAdpter) {
        this.mountCoreService = mountCoreService;
        this.collectorAdpter = collectorAdpter;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void checkOnline(UnsMountPo mount) {
        log.info("check online");

        try {
            Boolean online = collectorAdpter.queryOnline(mount);
            MountStatus newStatus = MountStatus.getByStatusValue(online);
            MountStatus oldStatus = mount.getStatus() != null ? MountStatus.getByStatus(mount.getStatus()) : null;
            if (newStatus != oldStatus) {
                log.info("change mount[seq:{}] online status[{}].", mount.getMountSeq(), newStatus);
                mountCoreService.updateMountStatus(mount.getId(), newStatus);
            }
        } catch (Throwable throwable) {
            log.error("update collector online error", throwable);
            mountCoreService.updateMountStatus(mount.getId(), MountStatus.OFFLINE);
        }
    }
}
