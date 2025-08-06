package com.supos.common.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class I18nUtils {

    private static MessageSource messageSource;

    private final static Locale sysOsLocale;

    static {
        String SYS_OS_LANG = SystemUtil.get("SYS_OS_LANG");
        sysOsLocale = SYS_OS_LANG != null ? Locale.forLanguageTag(SYS_OS_LANG) : null;
    }

    public I18nUtils(MessageSource messageSource) {
        I18nUtils.messageSource = messageSource;
    }

    private static Map<String, MessageSource> pluginMessageSource = new HashMap<>();

    public static String getMessage(String code, Object... args) {
        if (messageSource == null) {
            return ArrayUtil.isNotEmpty(args) ? "#" + code + Arrays.toString(args) : "#" + code;//用来区分单元测试环境（不带spring的）
        }
        Locale locale = sysOsLocale != null ? sysOsLocale : LocaleContextHolder.getLocale();// 从http请求头accept-language获取
        String message = code.trim();
        try {
            message = messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return ArrayUtil.isNotEmpty(args) ? code + Arrays.toString(args) : code;
        }
        return message;
    }

    public static void addPluginMessageSource(String pluginName, MessageSource messageSource) {
        pluginMessageSource.put(pluginName, messageSource);
    }

    public static void updatePluginMessageSource(String pluginName, MessageSource messageSource) {
        removePluginMessageSource(pluginName);
        addPluginMessageSource(pluginName, messageSource);
    }

    public static void removePluginMessageSource(String pluginName) {
        pluginMessageSource.remove(pluginName);
    }

    public static String getMessage4Plugin(String pluginName, String code, Object... args) {
        MessageSource ms = pluginMessageSource.get(pluginName);
        String msg = getMessage(ms, code, args);
        log.info("pluginName:{},code:{},args:{},return msg:{}", pluginName, code, args, msg);
        return msg;
    }

    static String getMessage(MessageSource msgSrc, final String code, Object... args) {
        if (msgSrc == null) {
            return getMessage(code, args);
        }
        Locale locale = sysOsLocale != null ? sysOsLocale : LocaleContextHolder.getLocale();
        String message = null;
        try {
            message = msgSrc.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            try {
                message = messageSource.getMessage(code, args, locale);
            } catch (NoSuchMessageException e2) {
            }
        }
        if (code != null && message != null && message.startsWith(code)) {
            try {
                message = messageSource.getMessage(code, args, locale);
                return message;
            } catch (NoSuchMessageException e2) {
            }
        }
        return message;
    }
}
