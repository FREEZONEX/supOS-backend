package com.supos.common.event;

import com.supos.common.dto.SimpleUnsInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class RemoveTimeScaleTopicsEvent extends ApplicationEvent {

    /**
     * VQT数据
     */
    private List<? extends SimpleUnsInfo> standard;

    private List<? extends SimpleUnsInfo> nonStandard;

    public RemoveTimeScaleTopicsEvent(Object source, List<? extends SimpleUnsInfo> standard, List<? extends SimpleUnsInfo> nonStandard) {
        super(source);
        this.standard = standard;
        this.nonStandard = nonStandard;
    }
}
