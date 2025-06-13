package com.supos.common.service;

import com.supos.common.dto.CreateTopicDto;

import java.util.List;
import java.util.Map;

public interface IUnsManagerService {

    /**
     * 批量创建UNS
     */
    Map<String, String> createModelAndInstance(List<CreateTopicDto> topicDtos, boolean fromImport);

}
