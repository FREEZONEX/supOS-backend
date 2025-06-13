package com.supos.common.annotation;

public interface WindowIntervalOffset {

    String getInterval();

    String getOffset();

    default void setIntervalMills(long mills){}
}
