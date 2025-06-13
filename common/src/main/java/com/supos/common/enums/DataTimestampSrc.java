package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 采集器数据时间戳来源
 * @date 2025/4/7 16:56
 */
@Getter
@AllArgsConstructor
public enum DataTimestampSrc {

    GATEWAY,
    SERVER;

    public static DataTimestampSrc get(String type) {
        for (DataTimestampSrc src : DataTimestampSrc.values()) {
            if (StringUtils.equals(src.toString(), type)) {
                return src;
            }
        }
        return null;
    }
}
