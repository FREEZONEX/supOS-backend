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

    Explanation("Explanation", null, 0),
    Template("Template", null, 1),
    Label("Label", null, 2),
    Folder("Folder", null, 3),

    FILE_TIMESERIES("File-timeseries", Constants.TIME_SEQUENCE_TYPE, 4),
    FILE_RELATION("File-relation", Constants.RELATION_TYPE, 5),
    FILE_CALCULATE("File-calculate", Constants.CALCULATION_REAL_TYPE, 6),
    FILE_AGGREGATION("File-aggregation", Constants.MERGE_TYPE, 7),
    FILE_REFERENCE("File-reference", Constants.CITING_TYPE, 8),

    ERROR("error", 0, -1);

    private final String code;

    private final Integer dataType;// 1--时序库 2--关系库

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

    public static void main(String[] args) {
        System.out.println(ExcelTypeEnum.sort());
    }
}
