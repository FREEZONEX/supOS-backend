package com.supos.common.enums.mount;

import lombok.Getter;

/**
 * @author sunlifang@supos.supcon.com
 * @title MountStatus
 * @description
 * @create 2025/6/19 下午4:50
 */
@Getter
public enum MountStatus {
    ONLINE("online", true),
    OFFLINE("offline", false);

    private String status;
    private boolean statusValue;

    MountStatus(String status, boolean statusValue) {
        this.status = status;
        this.statusValue = statusValue;
    }

    public static MountStatus getByStatusValue(boolean statusValue) {
        return statusValue ? ONLINE : OFFLINE;
    }

    public static MountStatus getByStatus(String status) {
        if (ONLINE.getStatus().equals(status)) {
            return ONLINE;
        }
        return OFFLINE;
    }
}
