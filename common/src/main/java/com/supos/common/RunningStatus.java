package com.supos.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.utils.I18nUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunningStatus {
    @Schema(description = "模块 uns sourceFlow eventFlow dashboard")
    String module;
    @Schema(description = "状态码 200表示成功")
    int code;
    String msg;
    String errTipFile;
    Integer n;
    Integer i;
    @Schema(description = "任务名称")
    String task;
    Long spendMills;
    @Schema(description = "是否完成")
    Boolean finished;
    @Schema(description = "进度：0-100")
    Double progress; // 进度 [0,100]

    private Long startTime;
    private Long endTime;
    private int totalCount;
    private int errorCount;
    private int successCount;

    public RunningStatus() {

    }

    public RunningStatus(int code, String msg) {
        this.code = code;
        this.msg = I18nUtils.getMessage(msg);
        this.finished = true;
    }

    public RunningStatus(int code, String msg, String errTipFile) {
        this.code = code;
        this.msg = I18nUtils.getMessage(msg);
        this.errTipFile = errTipFile;
        this.finished = true;
    }

    public RunningStatus(int n, int i, String task, String msg) {
        this.n = n;
        this.i = i;
        this.task = task;
        this.msg = msg;
    }

    public RunningStatus setSpendMills(Long spend) {
        this.spendMills = spend;
        Integer n = this.n;
        if (n != null && n > 0 && i != null) {
            progress = ((int) (1000 * i.doubleValue() / n.doubleValue())) / 10.0;
        }
        return this;
    }

    public RunningStatus setProgress(Double progress) {
        this.progress = progress;
        return this;
    }

    public RunningStatus setCode(int code) {
        this.code = code;
        return this;
    }

    public RunningStatus setTask(String task) {
        this.task = task;
        return this;
    }

    public RunningStatus setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    public RunningStatus setEndTime(Long endTime) {
        this.endTime = endTime;
        return this;
    }

    public RunningStatus setFinished(Boolean finished) {
        this.finished = finished;
        return this;
    }
}
