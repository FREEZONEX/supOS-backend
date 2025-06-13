package com.supos.common.utils;

import cn.hutool.core.util.ArrayUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

public class I18nUtils {

    private static MessageSource messageSource;

    private static String lang = System.getenv("SYS_OS_LANG");

    public I18nUtils(MessageSource messageSource) {
        I18nUtils.messageSource = messageSource;
    }

    public static String getMessage(String code, Object... args) {
        if (messageSource == null) {
            return ArrayUtil.isNotEmpty(args) ? "#" + code + Arrays.toString(args) : "#" + code;//用来区分单元测试环境（不带spring的）
        }
        Locale locale = null;
        // 优先从java环境变量获取语言
        if (StringUtils.hasText(lang)) {
            locale = Locale.forLanguageTag(lang);
        } else {
            // 从http请求头accept-language获取
            locale = LocaleContextHolder.getLocale();
        }
        String message = code.trim();
        try {
            message = messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return ArrayUtil.isNotEmpty(args) ? code + Arrays.toString(args) : code;
        }
        return message;
    }
}
