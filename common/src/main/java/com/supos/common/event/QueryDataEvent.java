package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FileBlobDataQueryDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/4/15 14:17
 */
@Getter
@Setter
public class QueryDataEvent extends ApplicationEvent {

    private CreateTopicDto topicDto;

    private List<FileBlobDataQueryDto.EQCondition> eqConditions;


    private List<Map<String, Object>> values;

    public QueryDataEvent(Object source, CreateTopicDto topicDto, List<FileBlobDataQueryDto.EQCondition> eqConditions) {
        super(source);
        this.topicDto = topicDto;
        this.eqConditions = eqConditions;
    }
}
