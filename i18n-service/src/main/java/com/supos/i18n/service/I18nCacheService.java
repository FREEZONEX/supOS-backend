package com.supos.i18n.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.supos.i18n.common.Constants;
import com.supos.i18n.event.LanguageDeleteEvent;
import com.supos.i18n.event.ResourceRefreshEvent;
import com.supos.i18n.event.ResourceSignleRefreshEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化缓存管理
 * @date 2025/9/6 10:12
 */
@Service
public class I18nCacheService {

    @Autowired
    private I18nResourceService i18nResourceService;

    //[语言->[资源]]
    private Map<String, LoadingCache<String, Optional<String>>> cacheMap = new ConcurrentHashMap<>();

    private Lock lock = new ReentrantLock();

    /**
     * 预热数据
     */
    public void init() {

    }

    /**
     * 获取资源
     * @param languageCode
     * @param key
     * @return
     */
    public String getResource(String languageCode, String moduleCode, String key) {
        lock.lock();
        try {
            String language = Constants.LANGUAGE_MAP.get(languageCode);
            if (language == null) {
                language = languageCode;
            }
            LoadingCache<String, Optional<String>> resourceCache = cacheMap.computeIfAbsent(language.toLowerCase(), k -> createResourceCache());
            return resourceCache.getUnchecked(String.format("%s--%s--%s", languageCode, moduleCode, key)).orElse(null);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除语言
     * @param languageCode
     */
    public void deleteLanguage(String languageCode) {
        languageCode = languageCode.toLowerCase();
        LoadingCache<String, Optional<String>> resourceCache = cacheMap.get(languageCode);
        if (resourceCache == null) {
            return;
        }
        resourceCache.invalidateAll();
        cacheMap.remove(languageCode);
    }

    /**
     * 刷新资源
     * @param languageCode
     */
    public void refreshLanguageResource(String languageCode) {
        LoadingCache<String, Optional<String>> resourceCache = cacheMap.get(languageCode.toLowerCase());
        if (resourceCache == null) {
            return;
        }
        resourceCache.invalidateAll();
    }

    /**
     * 刷新资源
     * @param key
     */
    public void refreshResource(String key) {
        for (LoadingCache<String, Optional<String>> resourceCache : cacheMap.values()) {
            resourceCache.invalidate(key);
        }
    }

    private LoadingCache<String, Optional<String>> createResourceCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Optional<String>>() {
                    @Override
                    public Optional<String> load(String key) throws Exception {
                        String[] keys = key.split("--");
                        String resource = i18nResourceService.getResourceByKey(keys[0], keys[1], keys[2]);
                        return Optional.ofNullable(resource);
                    }
                });
    }


    @EventListener(LanguageDeleteEvent.class)
    public void onLanguageDeleteEvent(LanguageDeleteEvent event) {
        lock.lock();
        try {
            deleteLanguage(event.getLanguageCode());
        } finally {
            lock.unlock();
        }
    }

    @EventListener(ResourceRefreshEvent.class)
    public void onResourceRefreshEvent(ResourceRefreshEvent event) {
        lock.lock();
        try {
            refreshLanguageResource(event.getLanguageCode());
        } finally {
            lock.unlock();
        }
    }

    @EventListener(ResourceSignleRefreshEvent.class)
    public void onResourceRefreshEvent(ResourceSignleRefreshEvent event) {
        lock.lock();
        try {
            refreshResource(event.getKey());
        } finally {
            lock.unlock();
        }
    }
}
