package com.supos.uns.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.common.Constants;
import com.supos.i18n.dao.po.I18nResourcePO;
import com.supos.i18n.service.I18nResourceService;
import com.supos.uns.bo.PlugInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/7 11:33
 */
@Slf4j
@Service
public class I18nService {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private I18nResourceService i18nResourceService;


    // 获取国际化信息
    public String readMessages(String lang) {
        StringBuilder messages = new StringBuilder();
        try {
            if (lang == null) {
                lang = getSysOsLang();
            }

            // 转化为可识别语言
            lang = transformLanguage(lang);

            List<I18nResourcePO> i18nResourcePOs = i18nResourceService.getAllResourceByModule(lang, Constants.DEFAULT_MODULE_CODE);
            if (CollectionUtil.isNotEmpty(i18nResourcePOs)) {
                for (I18nResourcePO i18nResourcePO : i18nResourcePOs) {
                    messages.append(i18nResourcePO.getI18nKey()).append("=").append(i18nResourcePO.getI18nValue()).append("\n");
                }
            }
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
        // err or not exist
        return messages.toString();
    }

    public String readMessages4Plugin(String lang, List<String> pluginId) {
        if (CollectionUtils.isEmpty(pluginId)) {
            return "";
        }
        Collection<PlugInfo> plugInfos = pluginManager.getPlugins();
        if (CollectionUtils.isEmpty(plugInfos)) {
            return "";
        }
        if (lang == null) {
            lang = getSysOsLang();
        }
        // 转化为可识别语言
        lang = transformLanguage(lang);

        StringBuilder messages = new StringBuilder();
        for (PlugInfo plugInfo : plugInfos) {
            List<I18nResourcePO> i18nResourcePOs = i18nResourceService.getAllResourceByModule(lang, plugInfo.getName());
            if (CollectionUtil.isNotEmpty(i18nResourcePOs)) {
                for (I18nResourcePO i18nResourcePO : i18nResourcePOs) {
                    messages.append(i18nResourcePO.getI18nKey()).append("=").append(i18nResourcePO.getI18nValue()).append("\n");
                }

            }
        }
        return messages.toString();
    }

    private String readMessages4Plugin(String lang, PlugInfo plugInfo) {
        String msgFileName = getMessageFileName(lang);
        String plugPath = plugInfo.getPlugPath();
        // /data/plugins/installed/{plugxxx}
        // plug/i18n/messages_zh_CN.properties
        String filePath = plugPath + File.separator + "i18n" + File.separator + msgFileName;
        try {
            if (FileUtil.exist(filePath)) {
                return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public String readMessages4App(String lang, List<String> appId) {
        if (CollectionUtils.isEmpty(appId)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        String finalLang = lang;
        appId.stream().forEach(aid -> {
            stringBuilder.append("# app begin---" + aid + "---\n");
            stringBuilder.append(readMessages4App(finalLang, aid));
            stringBuilder.append("\n");
            stringBuilder.append("# app end---" + aid + "---\n");
        });


        return stringBuilder.toString();
    }

    public String readMessages4App(String lang, String appId) {
        String msgFileName = getMessageFileName(lang);
        String appPath = "/data/i18n/third-apps/" + appId;

        String filePath = appPath + File.separator + "i18n" + File.separator + msgFileName;
        try {
            if (FileUtil.exist(filePath)) {
                return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    private String getSysMessageFilePath(String lang) {
        return "i18n/" + getMessageFileName(lang);
    }

    private String getMessageFileName(String lang) {
        String sysLang = lang.replace("-", "_");
        return "messages_" + sysLang + ".properties";
    }

    private String transformLanguage(String lang) {
        return lang.replace("-", "_");
    }

    public String getSysOsLang() {
        //
        return I18nUtils.SYS_OS_LANG;
    }
}
