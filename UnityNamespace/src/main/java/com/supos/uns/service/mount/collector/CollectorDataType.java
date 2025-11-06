package com.supos.uns.service.mount.collector;

import com.supos.common.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/9/23 15:27
 */
@Getter
@AllArgsConstructor
public enum CollectorDataType {
    UNKNOWN(0, FieldType.STRING),
    INTEGER(1, FieldType.INTEGER),
    DOUBLE(2, FieldType.DOUBLE),
    Boolean(3, FieldType.BOOLEAN),
    String(4, FieldType.STRING),
    Bytes(5, FieldType.BLOB);

    private int type;
    private FieldType fieldType;

    public static CollectorDataType getByType(Integer type) {
        if (type == null) {
            return UNKNOWN;
        }

        for (CollectorDataType valueType : CollectorDataType.values()) {
            if (valueType.getType() == type) {
                return valueType;
            }
        }
        return UNKNOWN;
    }
}
