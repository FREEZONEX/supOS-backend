package com.supos.common.utils;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.lang.id.IdConstants;
import cn.hutool.core.util.IdUtil;

import java.util.Date;

/**
 * 基于hutool的雪花算法实现，将默认允许的时钟回拨时间由2秒改为5分钟
 */
public class SuposIdUtil {

    /**
     * 默认回拨时间，5min
     */
    public static long DEFAULT_TIME_OFFSET = 300_000L;

    private static volatile Snowflake suposSnowflake;

    public static long nextId() {
        return getSnowflake(null, IdConstants.DEFAULT_WORKER_ID, IdConstants.DEFAULT_DATACENTER_ID, false, DEFAULT_TIME_OFFSET).nextId();
    }

    public static Snowflake getSnowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemClock, long timeOffset) {
        if (suposSnowflake == null) {
            synchronized (SuposIdUtil.class) {
                if (suposSnowflake == null) {
                    suposSnowflake = new Snowflake(
                            epochDate,            // 纪元时间（默认 2010-11-04）
                            workerId,        // 工作节点 ID (0~31)
                            dataCenterId,    // 数据中心 ID (0~31)
                            isUseSystemClock, // 是否使用系统时钟优化
                            timeOffset       // 最大容忍回拨时间
                    );
                }
            }
        }
        return suposSnowflake;
    }

}
