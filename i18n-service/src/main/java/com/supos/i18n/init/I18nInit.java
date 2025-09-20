package com.supos.i18n.init;

import cn.hutool.core.date.DateUtil;
import com.supos.i18n.common.Constants;
import com.supos.i18n.common.LanguageType;
import com.supos.i18n.common.ModuleType;
import com.supos.i18n.dto.AddLanguageDto;
import com.supos.i18n.service.I18nManagerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * 将平台i18n下的语言文件导入到数据库中
 * @author sunlifang
 * @version 1.0
 * @description: 国际化初始化
 * @date 2025/9/2 14:20
 */
@Slf4j
@Component
public class I18nInit {

    private static final String I18N_PLUGIN_PATH = "/data/i18n/plugin";
    private static final String I18N_APP_PATH = "/data/i18n/third-apps";

    @Autowired
    private I18nManagerService i18nManagerService;

    @EventListener(classes = ContextRefreshedEvent.class)
    public void init() {
        saveLanguage();
        scanPlatformI18n();

        //scanPluginI18n();

        //scanAppI18n();
    }

    private void saveLanguage() {
        AddLanguageDto zhLanguage = new AddLanguageDto();
        zhLanguage.setLanguageCode("zh_CN");
        zhLanguage.setLanguageName("中文（简体）");
        zhLanguage.setLanguageType(LanguageType.BUILTIN.getType());
        zhLanguage.setHasUsed(true);
        i18nManagerService.saveLanguage(zhLanguage, true);

        AddLanguageDto enLanguage = new AddLanguageDto();
        enLanguage.setLanguageCode("en_US");
        enLanguage.setLanguageName("English");
        enLanguage.setLanguageType(LanguageType.BUILTIN.getType());
        enLanguage.setHasUsed(true);
        i18nManagerService.saveLanguage(enLanguage, true);
    }

    // 扫描平台国际化文件
    public void scanPlatformI18n() {
        // 保存模块
        String moduleCode = Constants.DEFAULT_MODULE_CODE;
        i18nManagerService.saveModule(moduleCode, "i18n.builtin.platform", ModuleType.BUILTIN);

        // 提交国际化
        try {
            File i18nDir = new File("/data/resource/i18n");
            if (i18nDir.exists()) {
                File[] i18nFiles = i18nDir.listFiles();
                if (i18nFiles != null && i18nFiles.length > 0) {
                    // 取到i18n文件版本号
                    String dbVersion = i18nManagerService.getVersion(moduleCode);
                    String currentVersion = getPlatformI18nVersion();
                    if (higherVersion(dbVersion, currentVersion)) {
                        for (File i18nFile : i18nFiles) {
                            String fileName = i18nFile.getName();

                            i18nManagerService.saveResource(moduleCode, parseLanguageCode(fileName), parseProperties(i18nFile), false, true);
                        }
                        i18nManagerService.saveVersion(moduleCode, currentVersion);
                    }
                }
            }
        } catch (Throwable throwable) {
            log.error("scan platform i18n error", throwable);
        }
    }

    /**
     * 扫描插件国际化文件
     */
    public void scanPluginI18n(String pluginName, String showName, String path) {
        try {
            String moduleCode = pluginName;
            i18nManagerService.saveModule(moduleCode, showName, ModuleType.CUSTOM);

            String version = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            File[] i18nFiles = new File(path).listFiles();
            for (File i18nFile : i18nFiles) {
                String fileName = i18nFile.getName();

                i18nManagerService.saveResource(moduleCode, parseLanguageCode(fileName), parseProperties(i18nFile), false, true);
            }
            i18nManagerService.saveVersion(moduleCode, version);
        } catch (Throwable throwable) {
            log.error("scan plugin {} i18n error", pluginName, throwable);
            throw throwable;
        }
    }

    public void deletePluginI18n(String pluginName) {
        try {
            i18nManagerService.deleteModule(pluginName);
        } catch (Throwable throwable) {
            log.error("delete plugin {} i18n error", pluginName, throwable);
        }
    }

    /**
     * 扫描app国际化文件
     */
    public void scanAppI18n(String appId, String showName, String path) {
        try {
            String moduleCode = appId;
            i18nManagerService.saveModule(moduleCode, showName, ModuleType.CUSTOM);

            String version = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            File[] i18nFiles = new File(path).listFiles();
            for (File i18nFile : i18nFiles) {
                String fileName = i18nFile.getName();

                i18nManagerService.saveResource(moduleCode, parseLanguageCode(fileName), parseProperties(i18nFile), false, true);
            }
            i18nManagerService.saveVersion(moduleCode, version);
        } catch (Throwable throwable) {
            log.error("scan app {} i18n error", appId, throwable);
            throw throwable;
        }
    }

    public void deleteAppI18n(String pluginName) {
        try {
            i18nManagerService.deleteModule(pluginName);
        } catch (Throwable throwable) {
            log.error("delete plugin {} i18n error", pluginName, throwable);
        }
    }

    /**
     * 从yaml中解析出国际化版本号
     * @return
     */
    private String getPlatformI18nVersion() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new ClassPathResource("application.yml").getInputStream()) {
            Map<String, Object> obj = yaml.load(inputStream);
            if (obj != null && obj.containsKey("i18nVersion")) {
                return String.valueOf(obj.get("i18nVersion"));
            }
            throw new RuntimeException("get platform i18n version error");
        } catch (Exception e) {
            throw new RuntimeException("get platform i18n version error", e);
        }
    }

    /**
     * 从国际化文件解析出语言码
     * @param fileName
     * @return
     */
    private String parseLanguageCode(String fileName) {
        String languageCode = fileName.replace("messages_", "");
        languageCode = languageCode.replace(".properties", "");
        return languageCode;
    }

    /**
     * 从国际化文件解析封装为Properties
     * @param resourceFile
     * @return
     */
    private Properties parseProperties(File resourceFile) {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(resourceFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("parse properties error", e);
        }
        return properties;
    }

    /**
     * 校验模块版本是否有更新
     * @param dbVersion
     * @param currentVersion
     * @return
     */
    public boolean higherVersion(String dbVersion, String currentVersion) {
        if (dbVersion == null) {
            return true;
        }
        return dbVersion.compareTo(currentVersion) < 0;
    }
}
