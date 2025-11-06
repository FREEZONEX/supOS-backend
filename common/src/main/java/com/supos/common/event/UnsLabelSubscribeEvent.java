package com.supos.common.event;

import com.supos.common.dto.UnsLabelDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class UnsLabelSubscribeEvent extends ApplicationEvent {
    public final List<UnsLabelDto> labelList;

    public UnsLabelSubscribeEvent(Object source, List<UnsLabelDto> labelList) {
        super(source);
        this.labelList = labelList;
    }
}
