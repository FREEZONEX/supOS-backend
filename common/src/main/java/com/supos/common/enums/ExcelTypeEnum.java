package com.supos.common.enums;

import com.google.common.collect.Lists;
import com.supos.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/13 11:27
 * @description
 */
@Getter
@AllArgsConstructor
public enum ExcelTypeEnum {

    Explanation("Explanation", 0),
    Template("Template", 1),
    Label("Label", 2),
    Folder("Path", 3),

    FILE_TIMESERIES("Topic-timeseries", 4),
    FILE_RELATION("Topic-relation", 5),
    FILE_CALCULATE("Topic-calculate", 6),
    FILE_AGGREGATION("Topic-aggregation", 7),
    FILE_REFERENCE("Topic-reference", 8),
    FILE_JSONB("Topic-jsonb", 9),

    UNS("UNS", 3),
    File("File", 4),

    ERROR("error", -1);

    private final String code;

    /**
     * 批次序号
     */
    private final Integer index;

    public static ExcelTypeEnum valueOfCode(String code) {
        for (ExcelTypeEnum obj : ExcelTypeEnum.values()) {
            if (obj.code.equals(code)) {
                return obj;
            }
        }
        return ERROR;
    }

    public static List<ExcelTypeEnum> sort() {
        return Arrays.stream(ExcelTypeEnum.values()).sorted(Comparator.comparing(ExcelTypeEnum::getIndex)).collect(Collectors.toList());
    }

    public static ExcelTypeEnum valueOfIndex(int index) {
        for (ExcelTypeEnum obj : ExcelTypeEnum.values()) {
            if (obj.index == index) {
                return obj;
            }
        }
        return ERROR;
    }

    public static int size() {
        return ExcelTypeEnum.values().length -1;
    }

    public static List<ExcelTypeEnum> listFile() {
        return Lists.newArrayList(ExcelTypeEnum.FILE_TIMESERIES, ExcelTypeEnum.FILE_RELATION, ExcelTypeEnum.FILE_CALCULATE, ExcelTypeEnum.FILE_AGGREGATION, ExcelTypeEnum.FILE_REFERENCE);
    }

    public static ExcelTypeEnum getByDataType(int dataType) {
        if (dataType == Constants.TIME_SEQUENCE_TYPE) {
            return FILE_TIMESERIES;
        } else if (dataType == Constants.RELATION_TYPE) {
            return FILE_RELATION;
        } else if (dataType == Constants.CALCULATION_REAL_TYPE) {
            return FILE_CALCULATE;
        } else if (dataType == Constants.MERGE_TYPE) {
            return FILE_AGGREGATION;
        } else if (dataType == Constants.CITING_TYPE) {
            return FILE_REFERENCE;
        } else if (dataType == Constants.JSONB_TYPE) {
            return FILE_JSONB;
        }
        return ERROR;
    }

    public static void main(String[] args) {
        System.out.println(ExcelTypeEnum.sort());
    }
}
