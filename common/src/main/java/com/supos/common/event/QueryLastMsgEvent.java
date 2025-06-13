package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class QueryLastMsgEvent extends ApplicationEvent {
    public final CreateTopicDto uns;

    public QueryLastMsgEvent(Object source, CreateTopicDto uns) {
        super(source);
        this.uns = uns;
    }

    @Getter
    @Setter
    private Long msgCreateTime;
    @Getter
    @Setter
    private Map<String, Object> lastMessage;
}
