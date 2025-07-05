package com.supos.common.event;

import com.supos.common.dto.SimpleUnsInstance;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class RemoveTimeScaleTopicsEvent extends ApplicationEvent {

    /**
     * VQT数据
     */
    private List<SimpleUnsInstance> standard;

    private List<SimpleUnsInstance> nonStandard;

    public RemoveTimeScaleTopicsEvent(Object source, List<SimpleUnsInstance> standard, List<SimpleUnsInstance> nonStandard) {
        super(source);
        this.standard = standard;
        this.nonStandard = nonStandard;
    }
}
