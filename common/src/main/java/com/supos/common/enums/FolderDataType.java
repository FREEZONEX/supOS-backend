package com.supos.common.enums;

import com.supos.common.Constants;
import lombok.Getter;

@Getter
public enum FolderDataType {

    NORMAL(0,"uns.folder.type.normal"),

    STATE(1, "uns.folder.type.state"),//关系 jsonb 聚合 引用

    ACTION(2, "uns.folder.type.action"),//JSONB

    METRICS(3, "uns.folder.type.metrics");//时序 计算 聚合 引用

    private int typeIndex;

    private String i18nName;

    FolderDataType(int typeIndex, String showName) {
        this.typeIndex = typeIndex;
        this.i18nName = showName;
    }

    public static FolderDataType getFolderDataType(int index) {
        for (FolderDataType fdt : FolderDataType.values()) {
            if (fdt.getTypeIndex() == index) {
                return fdt;
            }
        }
        return NORMAL;
    }

    public static boolean isTypeMatched(Integer parentDataType, Integer fileDataType) {
        if (parentDataType == null || fileDataType == null) {
            return false;
        }
        return switch (parentDataType) {
            case 1 -> fileDataType == Constants.RELATION_TYPE
                    || fileDataType == Constants.JSONB_TYPE
                    || fileDataType == Constants.MERGE_TYPE
                    || fileDataType == Constants.CITING_TYPE;
            case 2 -> fileDataType == Constants.JSONB_TYPE;
            case 3 -> fileDataType == Constants.TIME_SEQUENCE_TYPE
                    || fileDataType == Constants.CALCULATION_REAL_TYPE
                    || fileDataType == Constants.CALCULATION_HIST_TYPE
                    || fileDataType == Constants.MERGE_TYPE
                    || fileDataType == Constants.CITING_TYPE;
            default -> false;
        };
    }

    public static int getDefaultParentType(Integer fileDataType){
        if (Constants.RELATION_TYPE == fileDataType) {
            return STATE.typeIndex;
        }

        if (Constants.JSONB_TYPE == fileDataType) {
            return ACTION.typeIndex;
        }

        if (Constants.TIME_SEQUENCE_TYPE == fileDataType || Constants.CALCULATION_REAL_TYPE == fileDataType || Constants.CALCULATION_HIST_TYPE == fileDataType) {
            return METRICS.typeIndex;
        }
        return STATE.typeIndex;
    }
}
