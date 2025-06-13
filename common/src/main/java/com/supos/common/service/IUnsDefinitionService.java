package com.supos.common.service;

import com.supos.common.dto.CreateTopicDto;

public interface IUnsDefinitionService {

    CreateTopicDto getDefinitionByAlias(String alias);

    CreateTopicDto getDefinitionByPath(String path);

    CreateTopicDto getDefinitionById(Long id);
}
