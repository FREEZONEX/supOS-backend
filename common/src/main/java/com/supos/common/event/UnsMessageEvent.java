package com.supos.common.event;

import com.supos.common.dto.UpdateFileDTO;
import org.springframework.context.ApplicationEvent;

import java.util.List;


public class UnsMessageEvent extends ApplicationEvent {
    public List<UpdateFileDTO> dataList;

    public UnsMessageEvent(Object source, List<UpdateFileDTO> dataList) {
        super(source);
        this.dataList = dataList;
    }
}
