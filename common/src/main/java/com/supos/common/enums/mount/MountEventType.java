package com.supos.common.enums.mount;

/**
 * @author sunlifang@supos.supcon.com
 * @title MountEventType
 * @description
 * @create 2025/6/19 下午7:36
 */
public enum MountEventType {

    COLLECTOR_MODIFY,
    COLLECTOR_DELETE,

    SOURCE_ADD,
    SOURCE_MODIFY,
    SOURCE_DELETE,
    TAG_ADD,
    TAG_MODIFY,
    TAG_DELETE,

    VIDEO_TAG_ADD,
    VIDEO_TAG_MODIFY,
    VIDEO_TAG_DELETE;


    public static boolean canBatchGroup(MountEventType type1, MountEventType type2) {
        boolean result = false;
        if (type1 == TAG_ADD && type2 == TAG_ADD) {
            result = true;
        } else if (type1 == TAG_ADD && type2 == TAG_MODIFY) {
            result = true;
        } else if (type1 == TAG_MODIFY && type2 == TAG_ADD) {
            result = true;
        } else if (type1 == TAG_MODIFY && type2 == TAG_MODIFY) {
            result = true;
        } else if (type1 == TAG_DELETE && type2 == TAG_DELETE) {
            result = true;
        }
        return result;
    }
}
