package com.supos.i18n.init;

import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.Constants;
import com.supos.i18n.service.I18nCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * 自定义数据库消息源
 */
@Slf4j
public class DatabaseMessageSource implements HierarchicalMessageSource {

    public final static String SYS_OS_LANG = System.getenv("SYS_OS_LANG");

    private MessageSource parentMessageSource;

    private boolean useCodeAsDefaultMessage = false;

    @Autowired
    private I18nCacheService i18nCacheService;

    @Override
    public void setParentMessageSource(MessageSource parent) {
        this.parentMessageSource = parent;
    }

    @Override
    public MessageSource getParentMessageSource() {
        return parentMessageSource;
    }

    public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
        this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
    }

    protected boolean isUseCodeAsDefaultMessage() {
        return this.useCodeAsDefaultMessage;
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        locale = parseLocale(locale);
        String message = getMessageFromSource(code, locale);
        if (message == null) {
            if (defaultMessage != null) {
                return formatMessage(defaultMessage, args, locale);
            } else {
                return getDefaultMessage(code);
            }
        }

        return formatMessage(message, args, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        locale = parseLocale(locale);
        String message = getMessageFromSource(code, locale);
        if (message == null) {
            throw new NoSuchMessageException("Message not found for code: " + code + " and locale: " + locale);
        }

        return formatMessage(message, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        locale = parseLocale(locale);
        // 首先尝试从当前消息源获取
        for (String code : resolvable.getCodes()) {
            String message = getMessageFromSource(code, locale);
            if (message != null) {
                return formatMessage(message, resolvable.getArguments(), locale);
            }
        }

        // 最后使用默认消息
        if (resolvable.getDefaultMessage() != null) {
            return resolvable.getDefaultMessage();
        }

        throw new NoSuchMessageException("No message found for codes: " + Arrays.toString(resolvable.getCodes()));
    }

    /**
     * 从数据源获取消息
     */
    private String getMessageFromSource(String code, Locale locale) {
        if (code == null || locale == null) {
            return null;
        }

        String localeKey = locale.toString();
        localeKey = I18nUtils.transformLanguage(localeKey);

        String moduleCode = null;
        String key = null;
        if (StringUtils.contains(code, "--")) {
            String[] codes = StringUtils.split(code, "--");
            moduleCode = codes[0];
            key = codes[1];
        } else {
            //moduleCode = Constants.DEFAULT_MODULE_CODE;
            key = code;
        }
        String msg = i18nCacheService.getResource(localeKey, moduleCode, key);
        // 1. 尝试从缓存获取
/*        String cachedMessage = getFromCache(code, localeKey);
        if (cachedMessage != null) {
            return cachedMessage;
        }*/

        // 2. 尝试从数据库获取
/*        String dbMessage = getFromDatabase(code, localeKey);
        if (dbMessage != null) {
            // 添加到缓存
            cacheMessage(code, localeKey, dbMessage);
            return dbMessage;
        }*/

        // 3. 尝试使用语言部分（例如：zh_CN -> zh）
/*        if (localeKey.contains("_")) {
            String languageOnly = localeKey.split("_")[0];
            String fallbackMessage = getFromCache(code, languageOnly);
            if (fallbackMessage == null) {
                fallbackMessage = getFromDatabase(code, languageOnly);
                if (fallbackMessage != null) {
                    cacheMessage(code, languageOnly, fallbackMessage);
                }
            }
            if (fallbackMessage != null) {
                return fallbackMessage;
            }
        }*/

        // 4. 尝试默认语言（英语）
/*        if (!"en".equals(localeKey) && !"en_US".equals(localeKey)) {
            String enMessage = getFromCache(code, "en");
            if (enMessage == null) {
                enMessage = getFromDatabase(code, "en");
                if (enMessage != null) {
                    cacheMessage(code, "en", enMessage);
                }
            }
            if (enMessage != null) {
                return enMessage;
            }
        }*/

        return msg;
    }

    /**
     * 格式化带参数的消息
     */
    private String formatMessage(String message, Object[] args, Locale locale) {
        if (args == null || args.length == 0) {
            return message;
        }

        try {
            MessageFormat messageFormat = new MessageFormat(message, locale);
            return messageFormat.format(args);
        } catch (Exception e) {
            log.warn("Error formatting message: {} with args: {}", message, Arrays.toString(args), e);
            return message; // 返回未格式化的原始消息
        }
    }

    /**
     * 格式化默认消息
     */
    protected String getDefaultMessage(String code) {
        if (isUseCodeAsDefaultMessage()) {
            return code;
        }
        return null;
    }

    private Locale parseLocale(Locale locale) {
        if (locale == null) {
            return new Locale(I18nUtils.transformLanguage(SYS_OS_LANG));
        }

        String languageCode = Constants.LANGUAGE_MAP.get(locale.getLanguage());
        if (languageCode != null) {
            return new Locale(I18nUtils.transformLanguage(languageCode));
        }

        return locale;
    }
}