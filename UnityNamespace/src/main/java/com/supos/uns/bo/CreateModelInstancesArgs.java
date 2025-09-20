package com.supos.uns.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.RunningStatus;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Data
public class CreateModelInstancesArgs {
    public List<CreateTopicDto> topics;
    public boolean fromImport;
    public boolean retainTableWhenDeleteInstance;
    public boolean throwModelExistsErr;
    public String flowName;

    public Map<String, String[]> labelsMap;

    @JsonIgnore
    public transient Consumer<RunningStatus> statusConsumer;

    public String toString() {
        return JsonUtil.toJson(this);
    }
}
