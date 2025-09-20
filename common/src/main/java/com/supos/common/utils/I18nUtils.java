package com.supos.common.utils;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class I18nUtils {

    private static MessageSource messageSource;

    public final static String SYS_OS_LANG = System.getenv("SYS_OS_LANG");

    public I18nUtils(MessageSource messageSource) {
        I18nUtils.messageSource = messageSource;
    }

    public static String getMessageWithSysLang(String code, Object... args) {
        LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(new Locale(transformLanguage(SYS_OS_LANG))),  true);
        return getMessage(code, args);
    }

    public static String getMessage(String code, Object... args) {
        String msg = doGetMessage(messageSource, code, false, args);

        msg = msg == null ? defaultMsg(code, args) : msg;
        return msg;
    }

    public static String getMessageWithModuleCode(String moduleCode, String code, Object... args) {
        String key = String.format("%s--%s", moduleCode, code);
        String msg = doGetMessage(messageSource, key, false, args);

        msg = msg == null ? defaultMsg(code, args) : msg;
        return msg;
    }

/*    public static void addAppMessageSource(String appId, MessageSource messageSource) {
        appMessageSources.put(appId, messageSource);
    }

    public static void removeAppMessageSource(String appId) {
        appMessageSources.remove(appId);
    }*/

    private static String doGetMessage(MessageSource msgSrc, String code, boolean defaultMsg, Object... args) {
        if (code == null) {
            return null;
        }
        if (msgSrc == null) {
            return ArrayUtil.isNotEmpty(args) ? "#" + code + Arrays.toString(args) : "#" + code;//用来区分单元测试环境（不带spring的）
        }
        Locale locale = LocaleContextHolder.getLocale();
        if (StringUtils.isBlank(locale.getLanguage())) {
            locale = new Locale(transformLanguage(SYS_OS_LANG));
        }
        String message = null;
        try {
            message = msgSrc.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.error("can not find message for code:{}, in {}", code, locale != null ? locale.getLanguage():"null");
            if (defaultMsg) {
                return defaultMsg(code, args);
            }
        }
        return message;
    }

    private static String defaultMsg(String code, Object... args) {
        return ArrayUtil.isNotEmpty(args) ? code + Arrays.toString(args) : code;
    }

    public static String transformLanguage(String lang) {
        return lang.replace("-", "_");
    }
}
