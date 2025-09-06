package com.supos.uns.service;

import cn.hutool.core.io.FileUtil;
import com.supos.common.utils.I18nUtils;
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


    // 获取国际化信息
    public String readMessages(String lang) {
        try {
            if (lang == null) {
                lang = getSysOsLang();
            }

            String fileName = getSysMessageFilePath(lang);

            ClassPathResource resource = new ClassPathResource(fileName);
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.info(e.getMessage(), e);
        }
        // err or not exist
        return "";
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
        Set<PlugInfo> needReturnPlugs = new HashSet<>();
        pluginId.stream().forEach(pid -> {
            plugInfos.stream().forEach(plugInfo -> {
                if (Objects.equals(pid, plugInfo.getName())
                        || (plugInfo.getPlugInfoYml() != null
                        && plugInfo.getPlugInfoYml().getRoute() != null
                        && Objects.equals(pid, plugInfo.getPlugInfoYml().getRoute().getName()))) {
                    needReturnPlugs.add(plugInfo);
                }
            });
        });

        if (CollectionUtils.isEmpty(needReturnPlugs)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String finalLang = lang;
        needReturnPlugs.stream().forEach(plugInfo -> {
            sb.append("# plugin begin---" + plugInfo.getName() + "---\n");
            sb.append(readMessages4Plugin(finalLang, plugInfo));
            sb.append("\n");
            sb.append("# plugin end---" + plugInfo.getName() + "---\n");
        });

        return sb.toString();
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

    public String getSysOsLang() {
        //
        return I18nUtils.SYS_OS_LANG;
    }
}
