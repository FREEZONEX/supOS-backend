package com.supos.uns.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/open-api/**")   // 只对 /open-api/** 开放
                .allowedOriginPatterns("*")   // 允许所有前端地址
                .allowedMethods("*")          // 允许所有请求方法 GET/POST/PUT/DELETE...
                .allowedHeaders("*")          // 允许所有请求头
                .allowCredentials(true)       // 允许携带cookie
                .maxAge(3600);                // 预检请求缓存 1 小时
    }
}
