package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.SaveDataDto;
import org.springframework.context.ApplicationEvent;

public class SaveDataEvent extends ApplicationEvent {
    public final SrcJdbcType jdbcType;
    public final SaveDataDto[] topicData;
    // "true" or "false" or ""
    public final String duplicateIgnore;

    public SaveDataEvent(Object source, SrcJdbcType jdbcType, SaveDataDto[] topicData, String duplicateIgnore) {
        super(source);
        this.jdbcType = jdbcType;
        this.topicData = topicData;
        this.duplicateIgnore = duplicateIgnore;
    }
}
