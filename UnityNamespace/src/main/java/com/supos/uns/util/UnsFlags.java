package com.supos.uns.util;

import com.supos.common.Constants;

public class UnsFlags {

    public static int generateFlag(Boolean addFlow, Boolean saveToDB, Boolean addDashBoard, Boolean retainTableWhenDeleteInstance, String accessLevel) {
        int flags = 0;
        if (Boolean.TRUE.equals(addFlow)) {
            flags |= Constants.UNS_FLAG_WITH_FLOW;
        }
        if (Boolean.TRUE.equals(saveToDB)) {
            flags |= Constants.UNS_FLAG_WITH_SAVE2DB;
        }
        if (Boolean.TRUE.equals(addDashBoard)) {
            flags |= Constants.UNS_FLAG_WITH_DASHBOARD;
        }
        if (Boolean.TRUE.equals(retainTableWhenDeleteInstance)) {
            flags |= Constants.UNS_FLAG_ACCESS_LEVEL_READ_WRITE;
        }
        if (Constants.ACCESS_LEVEL_READ_ONLY.equals(accessLevel)) {
            flags |= Constants.UNS_FLAG_ACCESS_LEVEL_READ_ONLY;
        }
        if (Constants.ACCESS_LEVEL_READ_WRITE.equals(accessLevel)) {
            flags |= Constants.UNS_FLAG_ACCESS_LEVEL_READ_WRITE;
        }
        return flags;
    }
}
