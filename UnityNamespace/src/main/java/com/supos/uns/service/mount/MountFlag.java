package com.supos.uns.service.mount;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/9/25 9:44
 */
public class MountFlag {

    public static final int SAVE2DB = 1 << 0;// 是否持久化到数据库
    public static final int DASHBOARD = 1 << 1;// 是否添加数据看板
    public static final int SYNCMETA = 1 << 2;// 是否元数据同步

    public static boolean withSave2db(int flag) {
        return (flag & SAVE2DB) == SAVE2DB;
    }

    public static boolean withDashBoard(int flag) {
        return (flag & DASHBOARD) == DASHBOARD;
    }

    public static boolean withSyncMeta(int flag) {
        return (flag & SYNCMETA) == SYNCMETA;
    }
}
