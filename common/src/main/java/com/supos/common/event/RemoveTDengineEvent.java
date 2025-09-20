package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;

import java.util.Collection;
import java.util.Date;

public class RemoveTDengineEvent extends RemoveTopicsEvent {

    public RemoveTDengineEvent(Object source, Collection<CreateTopicDto> topics, boolean withFlow, boolean withDashboard) {
        super(source, new Date(), withFlow, withDashboard, topics, null, null);
    }

}
