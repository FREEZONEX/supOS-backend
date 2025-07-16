package com.supos.uns.service;

import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mqtt.TopicDefinition;
import com.supos.common.service.IUnsDefinitionService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UnsDefinitionService implements IUnsDefinitionService {

    private final ConcurrentHashMap<Long, TopicDefinition> topicDefinitionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> aliasMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> pathMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> topicMap = Constants.useAliasAsTopic ? aliasMap : pathMap;

    @Override
    public CreateTopicDto getDefinitionByAlias(String alias) {
        if (alias != null) {
            Long id = aliasMap.get(alias);
            return getUnsById(id);
        } else {
            return null;
        }
    }

    @Override
    public CreateTopicDto getDefinitionByPath(String path) {
        if (path != null) {
            Long id = pathMap.get(path);
            return getUnsById(id);
        } else {
            return null;
        }
    }

    @Override
    public CreateTopicDto getDefinitionById(Long id) {
        return getUnsById(id);
    }


    private CreateTopicDto getUnsById(Long id) {
        if (id != null) {
            TopicDefinition definition = topicDefinitionMap.get(id);
            if (definition != null) {
                return definition.getCreateTopicDto();
            }
        }
        return null;
    }

    @Override
    public Map<Long, TopicDefinition> getTopicDefinitionMap() {
        return topicDefinitionMap;
    }

    @Override
    public ConcurrentHashMap<String, Long> getAliasMap() {
        return aliasMap;
    }

    @Override
    public ConcurrentHashMap<String, Long> getTopicMap() {
        return topicMap;
    }

    @Override
    public ConcurrentHashMap<String, Long> getPathMap() {
        return pathMap;
    }
}
