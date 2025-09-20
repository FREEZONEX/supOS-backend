package com.supos.uns.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.RunningStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月24日 13:53
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalRunningStatus extends RunningStatus {
    public GlobalRunningStatus(int code, String msg) {
        super(code, msg);
    }

    public GlobalRunningStatus(int code, String msg, String errTipFile) {
        super(code, msg, errTipFile);
    }

    public GlobalRunningStatus(int n, int i, String task, String msg) {
        super(n, i, task, msg);
    }

    private final List<RunningStatus> runningStatusList = Collections.synchronizedList(new ArrayList<>());

}
