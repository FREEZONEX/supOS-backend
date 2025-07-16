package com.supos.common.service;

import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mqtt.TopicDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IUnsDefinitionService {

    Map<Long, TopicDefinition> getTopicDefinitionMap();

    CreateTopicDto getDefinitionByAlias(String alias);

    CreateTopicDto getDefinitionByPath(String path);

    CreateTopicDto getDefinitionById(Long id);

    ConcurrentHashMap<String, Long> getAliasMap();

    ConcurrentHashMap<String, Long> getTopicMap();

    ConcurrentHashMap<String, Long> getPathMap();

}
