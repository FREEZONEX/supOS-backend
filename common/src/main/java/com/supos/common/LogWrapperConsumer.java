package com.supos.common;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
@Data
public class LogWrapperConsumer implements Consumer<RunningStatus> {
        final Consumer<RunningStatus> target;
        Boolean finished;
        String lastTask;
        Double lastProgress;

        public LogWrapperConsumer(Consumer<RunningStatus> target) {
            this.target = target;
        }

        @Override
        public void accept(RunningStatus runningStatus) {
            log.info("** status: {}", JSON.toJSONString(runningStatus));
            finished = runningStatus.getFinished();
            String task = runningStatus.getTask();
            if (task != null) {
                lastTask = task;
            }
            Double progress = runningStatus.getProgress();
            if (progress != null) {
                lastProgress = progress;
            }
            target.accept(runningStatus);
        }
    }