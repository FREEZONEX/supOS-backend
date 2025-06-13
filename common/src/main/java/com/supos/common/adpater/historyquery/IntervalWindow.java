package com.supos.common.adpater.historyquery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.annotation.WindowIntervalOffset;
import com.supos.common.annotation.WindowTimeValidator;
import lombok.Getter;
import lombok.Setter;

/**
 * 时间间隔窗口
 */
@Getter
@Setter
@WindowTimeValidator
public class IntervalWindow implements WindowIntervalOffset {

    String interval;//时间间隔

    String offset;//偏移量


    @JsonIgnore
    transient long intervalMills;

}
