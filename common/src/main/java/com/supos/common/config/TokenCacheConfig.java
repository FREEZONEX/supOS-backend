package com.supos.common.config;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.vo.UserInfoVo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenCacheConfig {

    /**
     * key:supos_community_token
     * value:access_token json
     * 默认1小时
     */
    @Bean
    public TimedCache<String, JSONObject> tokenCache() {
        return CacheUtil.newTimedCache(60 * 60 * 1000);
    }

    /**
     * key:sub
     * value:user info vo
     */
    @Bean
    public TimedCache<String, UserInfoVo> userInfoCache() {
        return CacheUtil.newTimedCache(Long.MAX_VALUE);
    }
}
