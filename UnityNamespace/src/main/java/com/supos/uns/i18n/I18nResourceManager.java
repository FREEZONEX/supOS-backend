package com.supos.uns.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/6/30 09:20
 */
@Slf4j
@Component
public class I18nResourceManager {

    /**
     * system message source
     */
    @Autowired
    private MessageSource messageSource;

    private Map<String, ReloadableResourceBundleMessageSource> messageSourceMap = new HashMap<>();


    public MessageSource addMessageSource(String key, String path) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(path);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);

        messageSourceMap.put(key, messageSource);
        return messageSource;

    }

    public void deleteMessageSource(String key) {
        if (messageSourceMap.containsKey(key)) {
            //
            ReloadableResourceBundleMessageSource messageSource = messageSourceMap.remove(key);
            messageSource.clearCache();
        }
    }

    public String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }

    public String getMessage(String key, Object[] args, String defaultMessage, Locale locale) {
        return messageSource.getMessage(key, args, defaultMessage, locale);
    }

    public String getMessageByExtension(String extensionName, String key, Object[] args,
                                        Locale locale) {

        // ex ms
        MessageSource extensionMessageSource = messageSourceMap.get(extensionName);
        if (extensionMessageSource == null) {
            log.warn("can not find message source by extensionName : {}", extensionName);
            // 找不到msg， 默认直接返回key

            return key;
        }

        return messageSource.getMessage(key, args, locale);
    }

    /**
     * 扩展
     *
     * @param extensionName  -> appName
     * @param key
     * @param args
     * @param defaultMessage
     * @param locale
     * @return
     */
    public String getMessageByExtension(String extensionName, String key, Object[] args,
                                        String defaultMessage,
                                        Locale locale) {

        // ex ms
        MessageSource extensionMessageSource = messageSourceMap.get(extensionName);
        if (extensionMessageSource == null) {
            log.warn("can not find message source by extensionName : {}", extensionName);
            // 找不到msg， 默认直接返回key

            return key;
        }

        return messageSource.getMessage(key, args, defaultMessage, locale);
    }

}
