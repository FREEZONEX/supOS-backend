package com.supos.common.utils;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
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

    private static Map<String, MessageSource> pluginMessageSources = new HashMap<>();

    private static Map<String, MessageSource> appMessageSources = new HashMap<>();

    static {
        log.info("init i18n utils");
        // 加载app语言包
        loadAppMessage();
    }

    public static void loadAppMessage() {
        try {
            String rootPath = "/data/i18n/third-apps/";
            File rootPathFile = new File(rootPath);
            if (rootPathFile.exists() && rootPathFile.isDirectory()) {
                File[] appDirs = rootPathFile.listFiles(File::isDirectory);
                if (appDirs != null && appDirs.length > 0) {
                    for (File appDir : appDirs) {
                        String appId = appDir.getName();
                        File appMessageDir = new File(rootPath + appId + "/i18n/");
                        if (appMessageDir.exists() && appMessageDir.isDirectory()) {
                            String i18nPath = String.format("file:%s", rootPath + appId + "/i18n/messages");
                            log.info("load i18n: {}", i18nPath);

                            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
                            messageSource.setBasename(i18nPath);
                            messageSource.setDefaultEncoding("UTF-8");
                            messageSource.setUseCodeAsDefaultMessage(true);
                            I18nUtils.addAppMessageSource(appId, messageSource);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("load app message error", e);
        }
    }

    public static String getMessage(String code, Object... args) {
//        if (messageSource == null) {
//            return ArrayUtil.isNotEmpty(args) ? "#" + code + Arrays.toString(args) : "#" + code;//用来区分单元测试环境（不带spring的）
//        }
//
//        Locale locale = null;
//        // 优先从java环境变量获取语言
//        if (StringUtils.hasText(SYS_OS_LANG)) {
//            locale = Locale.forLanguageTag(SYS_OS_LANG);
//        } else {
//            // 从http请求头accept-language获取
//            locale = LocaleContextHolder.getLocale();
//        }
//        String message = null;
//        try {
//            message = messageSource.getMessage(code, args, locale);
//        } catch (NoSuchMessageException e) {
//            return ArrayUtil.isNotEmpty(args) ? code + Arrays.toString(args) : code;
//        }
//        return message;
        String msg = doGetMessage(messageSource, code, false, args);
        if (msg == null || StringUtils.equals(msg, code)) {
            // 从插件中获取
            for (MessageSource pluginMessageSource : pluginMessageSources.values()) {
                msg = doGetMessage(pluginMessageSource, code, false, args);
                if (msg != null) {
                    break;
                }
            }
        }

        if (msg == null || StringUtils.equals(msg, code)) {
            // 从app中获取
            for (MessageSource appMessageSource : appMessageSources.values()) {
                log.info("get app message:{}, {}", code, appMessageSource);
                msg = doGetMessage(appMessageSource, code, false, args);
                if (msg != null) {

                    break;
                }
            }
        }

        msg = msg == null ? defaultMsg(code, args) : msg;
        return msg;
    }

    public static void addPluginMessageSource(String pluginName, MessageSource messageSource) {
        pluginMessageSources.put(pluginName, messageSource);
    }

    public static void updatePluginMessageSource(String pluginName, MessageSource messageSource) {
        removePluginMessageSource(pluginName);
        addPluginMessageSource(pluginName, messageSource);
    }

    public static void removePluginMessageSource(String pluginName) {
        pluginMessageSources.remove(pluginName);
    }

    public static String getMessage4Plugin(String pluginName, String code, Object... args) {
        MessageSource ms = pluginMessageSources.get(pluginName);
        String msg = doGetMessage(ms, code, true, args);
        log.info("pluginName:{},code:{},args:{},return msg:{}", pluginName, code, args, msg);
        return msg;
    }

    public static void addAppMessageSource(String appId, MessageSource messageSource) {
        appMessageSources.put(appId, messageSource);
    }

    public static void removeAppMessageSource(String appId) {
        appMessageSources.remove(appId);
    }

    private static String doGetMessage(MessageSource msgSrc, String code, boolean defaultMsg, Object... args) {
        if (code == null) {
            return null;
        }
        if (msgSrc == null) {
            return ArrayUtil.isNotEmpty(args) ? "#" + code + Arrays.toString(args) : "#" + code;//用来区分单元测试环境（不带spring的）
        }
        Locale locale = LocaleContextHolder.getLocale();
        String message = null;
        try {
            message = msgSrc.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.error("can not find message for code:{}", code);
            if (defaultMsg) {
                return defaultMsg(code, args);
            }
        }
        return message;
    }

    private static String defaultMsg(String code, Object... args) {
        return ArrayUtil.isNotEmpty(args) ? code + Arrays.toString(args) : code;
    }
}
