package com.supos.common.config;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

//@Configuration
//@EnableConfigurationProperties
public class MessageSourceConfig {


    @Bean
    @ConfigurationProperties(prefix = "spring.messages")
    public MessageSourceProperties messageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    public MessageSource messageSource(MessageSourceProperties properties) {
        AbstractResourceBasedMessageSource messageSource = null;
        // 判断是不是file开头的，file开头就是外置的情况
        if (properties.getBasename().startsWith("file:")) {
            messageSource = new ReloadableResourceBundleMessageSource();
        } else {
            messageSource = new ResourceBundleMessageSource();
        }
        messageSource.setBasenames(properties.getBasename().split(","));

        // 设置编码
        messageSource.setDefaultEncoding(properties.getEncoding().toString());
        return messageSource;
    }

    // 基于 HTTP 请求头中的 Accept-Language 来解析用户的区域设置
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver slr = new AcceptHeaderLocaleResolver();
        //设置默认语言，英文
        slr.setDefaultLocale(Locale.US);
        return slr;
    }


}
