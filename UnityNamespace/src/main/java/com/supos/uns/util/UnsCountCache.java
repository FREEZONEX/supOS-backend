package com.supos.uns.util;

import cn.hutool.core.thread.ThreadUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.UnsCountDTO;
import com.supos.common.event.NamespaceChangeEvent;
import com.supos.common.event.RemoveTopicsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsCountCache {

    private final Cache<Long, UnsCountDTO> cache = CacheBuilder.newBuilder()
            .maximumSize(10_000)                      // 最多缓存1万条
            .expireAfterWrite(2, TimeUnit.HOURS)    // 过期时间
            .build();

    public UnsCountDTO get(Long nodeId) {
        return cache.getIfPresent(nodeId);
    }

    public void put(Long nodeId, UnsCountDTO countDTO) {
        cache.put(nodeId, countDTO);
    }

    public void evict(Long nodeId) {
        cache.invalidate(nodeId);
    }

    public void clearAll() {
        cache.invalidateAll();
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    public void onRemoveTopics(RemoveTopicsEvent event) {
        ThreadUtil.execAsync(() -> {
            Set<Long> parentIdSet = event.topics.stream().map(CreateTopicDto::getParentId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Long parentId : parentIdSet) {
                this.evict(parentId);
            }
        });
    }

    @EventListener(classes = NamespaceChangeEvent.class)
    public void onBatchCreateTableEvent(NamespaceChangeEvent event) {
        this.clearAll();
    }
}
