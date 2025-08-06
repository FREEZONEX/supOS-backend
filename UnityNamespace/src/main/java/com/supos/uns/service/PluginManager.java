package com.supos.uns.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.util.DynamicClassLoader;
import com.supos.adpter.kong.dto.MenuDto;
import com.supos.adpter.kong.service.KongAdapterService;
import com.supos.adpter.kong.service.MenuService;
import com.supos.adpter.kong.vo.RoutResponseVO;
import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PlugInfoYml;
import com.supos.common.event.EventBus;
import com.supos.common.event.PluginPreUnInstallEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PlugUtils;
import com.supos.common.utils.RuntimeUtil;
import com.supos.common.utils.SqlScriptExecutor;
import com.supos.uns.bo.PlugInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PluginManager implements EnvironmentAware {

    private final static String PLUGIN_EXT_NAME = ".tar.gz";

    private final Map<String, PlugInfo> plugins = new ConcurrentHashMap<>();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MenuService menuService;

    @Autowired
    private KongAdapterService kongAdapterService;

    @Autowired
    PluginJarService pluginJarService;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    {
        try {
            DynamicClassLoader dynamicClassLoader = DynamicClassLoader.getInstance();
            Field classes = DynamicClassLoader.class.getDeclaredField("classes");
            classes.setAccessible(true);
            Map clssMap = (Map) classes.get(dynamicClassLoader);
            classes.set(dynamicClassLoader, new AbstractMapDecorator(clssMap) {
                @Override
                public Object get(Object key) {
                    if (key instanceof String name) {
                        for (PlugInfo plugInfo : plugins.values()) {
                            if (PlugInfo.STATUS_INSTALLED.equals(plugInfo.getInstallStatus()) && plugInfo.getBasePackage() != null && name.startsWith(plugInfo.getBasePackage())) {
                                try {
                                    Class vClass = plugInfo.getClassLoader().loadClass(name);
                                    log.info("fastJson 从插件 {} 加载类: {}", plugInfo.getName(), name);
                                    return vClass;
                                } catch (ClassNotFoundException ex) {
                                }
                            }
                        }
                    }
                    return super.get(key);
                }
            });
            // new PluginsClassLoader(getClass().getClassLoader(), plugins)
        } catch (Exception e) {
            log.error("DynamicClassLoader Err", e);
        }
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order// 优先级排到最后
    void init(ContextRefreshedEvent event) {
        if (RuntimeUtil.isLocalProfile()) {
            return;
        }
        ThreadUtil.execute(() -> {
            try {
                int retry = 0;
                while (true) {
                    try {
                        RoutResponseVO routResponseVO = kongAdapterService.fetchRoute("backend");
                        if (routResponseVO != null || retry > 60) {
                            break;
                        }

                    } catch (Exception e) {
                        log.error("kong adapter init error", e);
                    }
                    retry++;
                    Thread.sleep(2000);
                }


                init();
            } catch (Exception e) {
                log.error("scanPlugins error", e);
            }
        });
    }

    private PlugInfo getByPlugDir(String pluginDir) {
        for (PlugInfo plugInfo : plugins.values()) {
            if (plugInfo.getPlugPath().contains(pluginDir)) {
                return plugInfo;
            }
        }
        return null;
    }

    private Map<String, String> scanPluginDirs(File pluginTempPath, File pluginInstalledPath) {
        Map<String, String> pluginDirs = new HashMap<>();
        pluginDirs.putAll(Arrays.stream(pluginTempPath.listFiles()).filter(File::isDirectory).collect(Collectors.toMap(File::getName, File::getAbsolutePath, (k1, k2) -> k2)));
        pluginDirs.putAll(Arrays.stream(pluginInstalledPath.listFiles()).filter(File::isDirectory).collect(Collectors.toMap(File::getName, File::getAbsolutePath, (k1, k2) -> k2)));
        return pluginDirs;
    }

    private boolean isPlugInTemp(PlugInfo plugInfo) {
        return plugInfo.getPlugPath().startsWith(Constants.PLUGIN_TEMP_PATH);
    }

    private boolean isPlugInInstalled(PlugInfo plugInfo) {
        return plugInfo.getPlugPath().startsWith(Constants.PLUGIN_INSTALLED_PATH);
    }

    private void unzipAndCheckPlugin(File pluginPackageFile, String unzipTargetPath) {
        FileUtil.del(unzipTargetPath);

        PlugUtils.unzipPlugin(pluginPackageFile, unzipTargetPath);
        log.info("Extract plugin to directory:{}", unzipTargetPath);

        FileUtil.del(pluginPackageFile);
        log.info("remove plugin package:{}", pluginPackageFile.getName());
    }


    /**
     * 扫描插件包
     */
    private boolean scanPluginPackage(boolean initScan) {
        log.info("Scanning plugin root directory...");
        boolean isChanged = false;

        File pluginScanPath = new File(Constants.PLUGIN_PATH);
        // 创建插件临时目录
        File pluginTempPath = new File(Constants.PLUGIN_TEMP_PATH);
        if (!pluginTempPath.exists()) {
            pluginTempPath.mkdirs();
        }
        // 创建插件已安装目录
        File pluginInstalledPath = new File(Constants.PLUGIN_INSTALLED_PATH);
        if (!pluginInstalledPath.exists()) {
            pluginInstalledPath.mkdirs();
        }

        File pluginUpgradePath = new File(Constants.PLUGIN_UPGRADE_PATH);
        if (!pluginUpgradePath.exists()) {
            pluginUpgradePath.mkdirs();
        } else {
            FileUtil.clean(pluginUpgradePath);
        }

        File pluginUpgradeTempPath = new File(Constants.PLUGIN_UPGRADE_TEMP_PATH);
        if (!pluginUpgradeTempPath.exists()) {
            pluginUpgradeTempPath.mkdirs();
        } else {
            FileUtil.clean(pluginUpgradeTempPath);
        }

        if (pluginScanPath.exists() && pluginScanPath.isDirectory()) {
            File[] pluginPackageFiles = pluginScanPath.listFiles(File::isFile);
            if (pluginPackageFiles != null && pluginPackageFiles.length > 0) {
                Map<String, PlugInfo> existPlugins = new HashMap<>();
                existPlugins.putAll(scanPluginTempDir());
                existPlugins.putAll(scanPluginInstalledDir());

                for (File pluginPackageFile : pluginPackageFiles) {
                    String pluginPackageName = pluginPackageFile.getName();
                    log.info("Scanning plugin file:{}", pluginPackageName);
                    if (!StringUtils.endsWithIgnoreCase(pluginPackageName, PLUGIN_EXT_NAME)) {
                        log.warn("Unrecognizable file:{}", pluginPackageName);
                        continue;
                    }
                    // 将插件包解压到升级临时目录
                    String newTempDirName = String.format("%s-%d", StringUtils.replaceIgnoreCase(pluginPackageName, PLUGIN_EXT_NAME, ""), System.currentTimeMillis());
                    String newTempPath = String.format("%s/%s", pluginUpgradeTempPath.getAbsolutePath(), newTempDirName);
                    unzipAndCheckPlugin(pluginPackageFile, newTempPath);
                    PlugInfoYml plugInfoYml = parsePlugInfoYml(newTempPath);

                    PlugInfo existPluginInfo = existPlugins.get(plugInfoYml.getName());
                    existPluginInfo = formatI18n(existPluginInfo);
                    File newUninstallPath = new File(pluginTempPath, newTempDirName);
                    if (existPluginInfo == null) {
                        isChanged = true;
                        FileUtil.move(new File(newTempPath), newUninstallPath, true);
                        log.info("plugin {} not exist, move from {} to {}", plugInfoYml.getName(), newTempPath, newUninstallPath.getAbsolutePath());
                    } else {
                        // 尝试覆盖
                        if (initScan) {
                            isChanged = true;
                            FileUtil.del(existPluginInfo.getPlugPath());
                            FileUtil.move(new File(newTempPath), new File(pluginTempPath, newTempDirName), true);
                            log.info("init scan, plugin {} , move from {} to {}", plugInfoYml.getName(), newTempPath, newUninstallPath.getAbsolutePath());
                        } else {
                            if (existPluginInfo.getInstallStatus().equals(PlugInfo.STATUS_NOT_INSTALL) || existPluginInfo.getInstallStatus().equals(PlugInfo.STATUS_INSTALL_FAIL)) {
                                isChanged = true;
                                FileUtil.del(existPluginInfo.getPlugPath());
                                FileUtil.move(new File(newTempPath), new File(pluginTempPath, newTempDirName), true);
                                log.info("plugin {} not installed, move from {} to {}", plugInfoYml.getName(), newTempPath, newUninstallPath.getAbsolutePath());
                            } else {
                                log.warn("Plugin already installed:{}", plugInfoYml.getName());
                            }
                        }
                    }
                    FileUtil.del(newTempPath);
                }
            }
        }
        return isChanged;
    }

    private PlugInfoYml parsePlugInfoYml(String plugPath) {
        PlugInfoYml plugInfoYml = PlugUtils.getPlugInfoYml(plugPath);
//        plugInfoYml.setShowName(plugInfoYml.getShowName() != null ? I18nUtils.getMessage(plugInfoYml.getShowName()) : plugInfoYml.getShowName());
//        plugInfoYml.setDescription(plugInfoYml.getDescription() != null ? I18nUtils.getMessage(plugInfoYml.getDescription()) : plugInfoYml.getDescription());

        return plugInfoYml;
    }

    private Map<String, PlugInfo> scanPluginTempDir() {
        Map<String, PlugInfo> scanPlugins = new HashMap<>();

        // 扫描临时目录，设置为未安装
        File pluginTempPath = new File(Constants.PLUGIN_TEMP_PATH);
        File[] pluginTempDirs = pluginTempPath.listFiles();
        for (File pluginTempDir : pluginTempDirs) {
            if (pluginTempDir.isDirectory()) {
                PlugInfoYml plugInfoYml = parsePlugInfoYml(pluginTempDir.getAbsolutePath());
                PlugInfo plugInfo = new PlugInfo();
                plugInfo.setPlugInfoYml(plugInfoYml);
                plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
                plugInfo.setPlugPath(pluginTempDir.getAbsolutePath());
                scanPlugins.put(plugInfoYml.getName(), plugInfo);
            }
        }
        return scanPlugins;
    }

    private Map<String, PlugInfo> scanPluginInstalledDir() {
        Map<String, PlugInfo> scanPlugins = new HashMap<>();

        // 扫描已安装目录，设置为已安装
        File pluginInstalledPath = new File(Constants.PLUGIN_INSTALLED_PATH);
        File[] pluginInstalledDirs = pluginInstalledPath.listFiles();
        for (File pluginInstalledDir : pluginInstalledDirs) {
            PlugInfoYml plugInfoYml = parsePlugInfoYml(pluginInstalledDir.getAbsolutePath());
            PlugInfo plugInfo = new PlugInfo();
            plugInfo.setPlugInfoYml(plugInfoYml);
            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALLED);
            plugInfo.setPlugPath(pluginInstalledDir.getAbsolutePath());
            scanPlugins.put(plugInfoYml.getName(), plugInfo);

        }
        return scanPlugins;
    }

    /**
     * 扫描插件目录
     *
     * @return
     */
    private Map<String, PlugInfo> scanPluginDirs(String scanPath) {
        Map<String, PlugInfo> scanPlugins = new HashMap<>();
        if (scanPath == null) {
            scanPlugins.putAll(scanPluginTempDir());
            scanPlugins.putAll(scanPluginInstalledDir());
        } else if (Constants.PLUGIN_TEMP_PATH.equals(scanPath)) {
            scanPlugins.putAll(scanPluginTempDir());
        } else if (Constants.PLUGIN_INSTALLED_PATH.equals(scanPath)) {
            scanPlugins.putAll(scanPluginInstalledDir());
        } else {
            log.warn("Unrecognizable scan path:{}", scanPath);
        }
        return scanPlugins;
    }

    private void init() {
        scanPluginPackage(true);
        Map<String, PlugInfo> scanPlugins = scanPluginDirs(null);

        // 预处理初始安装插件（将那些需要初始安装的插件状态置为已安装，目录移至installed）
        Set<String> needInitInstallPlugins = new HashSet<>();
        for (Map.Entry<String, PlugInfo> e : scanPlugins.entrySet()) {
            PlugInfo plugInfo = e.getValue();

            // installed目录下的插件默认安装
            if (plugInfo.getInstallStatus().equals(PlugInfo.STATUS_INSTALLED)) {
                needInitInstallPlugins.add(plugInfo.getName());
                plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
            }

            // yaml中配置了自动安装，默认安装
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
            if (plugInfoYml != null && Boolean.TRUE.equals(plugInfoYml.getAutoInstall())) {
                moveTempToInstalled(plugInfo);
                needInitInstallPlugins.add(plugInfo.getName());
                plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
            }

//            plugins.put(e.getKey(), plugInfo);
            putPlugin(plugInfo);
        }

        // 初始安装插件
        for (Map.Entry<String, PlugInfo> e : scanPlugins.entrySet()) {
            PlugInfo plugInfo = e.getValue();
            if (needInitInstallPlugins.contains(e.getKey())) {
                try {

                    doInstallPluginWithDependency(plugInfo.getName());
                } catch (Exception ex) {
                    log.error("init install plugin {} error", plugInfo.getName(), ex);
                }

            }
        }
    }

    /**
     * 插件安装完成后，移动临时目录到已安装目录
     *
     * @param plugInfo
     */
    private void moveTempToInstalled(PlugInfo plugInfo) {
        File srcFile = new File(plugInfo.getPlugPath());
        String plugPath = srcFile.getAbsolutePath();
        String tempPath = new File(Constants.PLUGIN_TEMP_PATH).getAbsolutePath();
        if (srcFile.exists() && StringUtils.contains(plugPath, tempPath)) {
            String targetPath = plugPath.replace(tempPath, new File(Constants.PLUGIN_INSTALLED_PATH).getAbsolutePath());
            FileUtil.move(srcFile, new File(targetPath), true);
            plugInfo.setPlugPath(targetPath);
        }
    }

    /**
     * 插件安装完成后，移动临时目录到已安装目录
     *
     * @param plugInfo
     */
    private void moveInstalledToTemp(PlugInfo plugInfo) {
        if (plugInfo.getPlugPath() == null) {
            return;
        }
        String plugPath = new File(plugInfo.getPlugPath()).getAbsolutePath();
        String tempPath = new File(Constants.PLUGIN_INSTALLED_PATH).getAbsolutePath();
        if (StringUtils.contains(plugPath, tempPath)) {
            String targetPath = plugPath.replace(tempPath, new File(Constants.PLUGIN_TEMP_PATH).getAbsolutePath());
            FileUtil.move(new File(plugInfo.getPlugPath()), new File(targetPath), true);
            plugInfo.setPlugPath(targetPath);
        }
    }

    /**
     * 插件升级中，移动临时目录到升级临时目录
     *
     * @param plugInfo
     */
    private void moveTempToUpgradeTemp(PlugInfo plugInfo) {
        if (plugInfo.getPlugPath() == null) {
            return;
        }
        String plugPath = new File(plugInfo.getPlugPath()).getAbsolutePath();
        String tempPath = new File(Constants.PLUGIN_TEMP_PATH).getAbsolutePath();
        if (StringUtils.contains(plugPath, tempPath)) {
            String targetPath = plugPath.replace(tempPath, new File(Constants.PLUGIN_UPGRADE_TEMP_PATH).getAbsolutePath());
            FileUtil.move(new File(plugInfo.getPlugPath()), new File(targetPath), true);
        }
    }

    /**
     * 插件升级回滚中，移动升级临时目录到临时目录
     *
     * @param plugInfo
     */
    private void moveUpgradeTempToTemp(PlugInfo plugInfo) {
        if (plugInfo.getPlugPath() == null) {
            return;
        }
        String plugPath = new File(plugInfo.getPlugPath()).getAbsolutePath();
        String tempPath = new File(Constants.PLUGIN_TEMP_PATH).getAbsolutePath();
        if (StringUtils.contains(plugPath, tempPath)) {
            String targetPath = plugPath.replace(tempPath, new File(Constants.PLUGIN_UPGRADE_TEMP_PATH).getAbsolutePath());
            FileUtil.move(new File(plugInfo.getPlugPath()), new File(targetPath), true);
        }
    }

    public JsonResult<Collection<PlugInfo>> listPlugins() {
/*        if (scanPluginPackage(false)) {
            Map<String, PlugInfo> scanPlugins = scanPluginDirs(Constants.PLUGIN_TEMP_PATH);
            for (Map.Entry<String, PlugInfo> e : scanPlugins.entrySet()) {
                PlugInfo plugInfo = plugins.get(e.getKey());
                if (plugInfo == null) {
                    plugins.put(e.getKey(), e.getValue());
                } else {
                    plugInfo.setPlugInfoYml(e.getValue().getPlugInfoYml());
                }
            }
        }*/
        Collection<PlugInfo> plugInfos = plugins.values();
        List<PlugInfo> newPlugins = new ArrayList<>();

        if (CollUtil.isNotEmpty(plugInfos)) {
            plugInfos.forEach(plugInfo -> {
                PlugInfo newPlugInfo = formatI18n(plugInfo);
                newPlugins.add(newPlugInfo);
            });
        }

        return new JsonResult<>(0, "ok", newPlugins);
    }

    private PlugInfo formatI18n(PlugInfo plugInfo) {
        if (plugInfo == null) {
            return null;
        }
        PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
        if (plugInfoYml != null) {
            if (plugInfoYml.getShowName() != null && plugInfoYml.getShowNameI18nCode() == null) {
                plugInfoYml.setShowNameI18nCode(plugInfoYml.getShowName());
                plugInfoYml.setShowName(I18nUtils.getMessage4Plugin(plugInfoYml.getName(), plugInfoYml.getShowNameI18nCode()));
                log.info("plugin showName:{},i18n:{}", plugInfoYml.getShowName(), plugInfoYml.getShowNameI18nCode());
            }
            if (plugInfoYml.getDescription() != null && plugInfoYml.getDescriptionI18nCode() == null) {
                plugInfoYml.setDescriptionI18nCode(plugInfoYml.getDescription());
                plugInfoYml.setDescription(I18nUtils.getMessage4Plugin(plugInfoYml.getName(), plugInfoYml.getDescriptionI18nCode()));
                log.info("plugin description:{},i18n:{}", plugInfoYml.getDescription(), plugInfoYml.getDescriptionI18nCode());
            }
        }
        return plugInfo;
    }

    public Collection<PlugInfo> getPlugins() {
        Collection<PlugInfo> plugInfos = plugins.values();
        List<PlugInfo> newPlugins = new ArrayList<>();

        if (CollUtil.isNotEmpty(plugInfos)) {
            plugInfos.forEach(plugInfo -> {
                PlugInfo newPlugInfo = formatI18n(plugInfo);
                newPlugins.add(newPlugInfo);
            });
        }

        return newPlugins;
    }

    public PlugInfo getPluginDetail(String name) {
        PlugInfo plugInfo = plugins.get(name);
        if (plugInfo == null) {
            return plugInfo;
        }

        return formatI18n(plugInfo);
    }

    public void putPlugin(PlugInfo plugInfo) {
        plugins.put(plugInfo.getName(), plugInfo);

        // add i18n
        String plugPath = plugInfo.getPlugPath();
        if (plugPath != null) {
            String protocol = "file:///";
            if (plugPath.startsWith(File.separator)) {
                plugPath = plugPath.substring(1);
            }
            String i18nPath = protocol + plugPath + File.separator + "i18n" + File.separator + "messages";
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename(i18nPath);
            messageSource.setDefaultEncoding("UTF-8");
            messageSource.setUseCodeAsDefaultMessage(true);
            I18nUtils.addPluginMessageSource(plugInfo.getName(), messageSource);

            log.info("load plugin:{} and put i18n basename:{}", plugInfo.getName(), i18nPath);
        }
    }

    public void deletePlugin(String name) {
        plugins.remove(name);
    }

    /**
     * 安装插件，会自动先安装依赖插件
     *
     * @param pluginName
     * @return
     */
    private void doInstallPluginWithDependency(String pluginName) {
        log.info("install plugin:{}", pluginName);
        PlugInfo plugInfo = getPluginDetail(pluginName);
        if (plugInfo == null) {
            throw new BuzException("plugin.manager.null");
        }
        if (StringUtils.equals(plugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED)) {
            return;
        }

        // 获取依赖插件，并首先安装依赖插件
        List<String> dependencies = listDependenciesDown(plugInfo);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            for (String dependency : dependencies) {
                doInstallPluginWithDependency(dependency);
            }
        }
        try {
            doInstallPlugin(plugInfo);
        } catch (BuzException e) {
            throw e;
        } catch (Exception e) {
            log.error("install plugin error", e);
            throw new RuntimeException("install plugin error", e);
        }
    }

    private void doInstallPlugin(PlugInfo plugInfo) {
        log.info("install plugin:{}", plugInfo.getName());

        if (StringUtils.equals(plugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED)) {
            log.info("plugin:{} has installed, skip.");
            return;
        }

        try {
            // 安装前将插件文件夹移到已安装目录
            moveTempToInstalled(plugInfo);
            // sql脚本
            runInitSql(plugInfo);
            // evironment
            injectEnvironment(plugInfo);

            saveStatic(plugInfo);

            // 安装后端jar
            List<String> fileNames = FileUtil.listFileNames(plugInfo.getPlugPath());
            if (CollectionUtils.isNotEmpty(fileNames)) {
                for (String fileName : fileNames) {
                    if (fileName.endsWith(".jar")) {
                        File jarFile = new File(plugInfo.getPlugPath() + "/" + fileName);
                        log.info("install jar:{}, md5:{}", jarFile.getAbsolutePath(), MD5.create().digestHex16(jarFile));
                        JsonResult<String> result = installPlugin(jarFile, plugInfo, true);
                        if (result.getCode() != 0) {
                            throw new BuzException("plugin.manager.install.error");
                        }
                    }
                }
            }
            addInstallStepFlag(plugInfo, "backend");
            // 安装前端
            installFront(plugInfo);

            // 注册路由
            saveRoute(plugInfo);

            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALLED);
            plugInfo.setInstallTime(System.currentTimeMillis());
        } catch (Exception e) {
            log.error("installPlugin error", e);
            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALL_FAIL);
            log.info("start rollback plugin {}", plugInfo.getName());
            doUninstallPlugin(plugInfo, false);
            log.info("rollback plugin {} finished", plugInfo.getName());
            throw new BuzException("plugin.manager.install.error");
        }
    }

    /**
     * 安装插件
     *
     * @param pluginName
     * @return
     */
    public JsonResult<String> installPlugin(String pluginName) {
        try {
            log.info("install plugin:{}", pluginName);
            PlugInfo plugInfo = getPluginDetail(pluginName);
            if (plugInfo == null) {
                throw new BuzException("plugin.manager.null");
            }
            if (StringUtils.equals(plugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED)) {
                return new JsonResult<>(0, pluginName);
            }

            // 校验依赖插件是否已安装
            List<String> dependencies = listDependenciesDown(plugInfo);
            if (CollectionUtils.isNotEmpty(dependencies)) {
                Set<String> unInstallDependencies = dependencies.stream().filter(dependency -> {
                    PlugInfo dependencyPlugInfo = getPluginDetail(dependency);
                    return dependencyPlugInfo == null || !StringUtils.equals(dependencyPlugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED);
                }).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(unInstallDependencies)) {
                    throw new BuzException("plugin.manager.install.dependency", StringUtils.join(unInstallDependencies, ","));
                }
            }
            doInstallPlugin(plugInfo);

            return new JsonResult<>(0, pluginName);
        } catch (BuzException e) {
            return new JsonResult<>(500, I18nUtils.getMessage(e.getMsg(), e.getParams()));
        } catch (Exception e) {
            log.error("installPlugin error", e);
            return new JsonResult<>(500, I18nUtils.getMessage("plugin.manager.install.error"));
        }
    }

    public JsonResult<String> installPlugin(final File pluginJar, final @Nullable PlugInfo baseInfo, final boolean force) throws Exception {
        PlugInfo plugInfo = baseInfo != null ? baseInfo : new PlugInfo();
        if (baseInfo == null) {
            pluginJarService.setPlugName(pluginJar, plugInfo);
        }
        final String plugName = plugInfo.getName();
        PlugInfo oldPlug = getPluginDetail(plugName);
        if (oldPlug != null && PlugInfo.STATUS_INSTALLED.equals(oldPlug.getInstallStatus())) {
            if (force) {
                log.info("同包名的插件已安装: {}, 卸载重装..", oldPlug);
                pluginJarService.uninstallPlugin(oldPlug);
            } else {
                boolean del = pluginJar.delete();
                log.info("同包名的插件已安装: {}, 已忽略本次安装. {}", oldPlug, del);
                return new JsonResult<>(0, plugName + " exists!");
            }
        }

        log.info("安装插件: {}, 路径: {}", plugName, pluginJar);

        if (pluginJarService.tryInstallPlugin(plugInfo, pluginJar, plugins::get)) {
            if (baseInfo == null) {
//                plugins.put(plugInfo.getName(), plugInfo);
                putPlugin(plugInfo);
                plugInfo.getInstallStepFlags().add("backend");
            } else {
                // add i18n
                String plugPath = plugInfo.getPlugPath();
                String protocol = "file:///";
                if (plugPath.startsWith(File.separator)) {
                    plugPath = plugPath.substring(1);
                }
                String i18nPath = protocol + plugPath + File.separator + "i18n" + File.separator + "messages";

                ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
                messageSource.setBasename(i18nPath);
                messageSource.setDefaultEncoding("UTF-8");
                messageSource.setUseCodeAsDefaultMessage(true);
                I18nUtils.addPluginMessageSource(plugInfo.getName(), messageSource);

                log.info("load plugin 4 install:{} and put i18n basename:{}", plugInfo.getName(), i18nPath);
            }


            return new JsonResult<>(0, plugInfo.getName() + " , " + plugInfo.getBasePackage());
        } else {
            return new JsonResult<>(0, "没有Bean!");
        }
    }

    /**
     * 执行插件的sql脚本
     *
     * @param plugInfo 插件信息
     */
    public void runInitSql(PlugInfo plugInfo) {
        try {
            log.info("安装执行插件{}的sql脚本", plugInfo.getName());
            File sqlFile = new File(plugInfo.getPlugPath() + "/bin/" + "init.sql");
            if (sqlFile.exists()) {
                log.info("执行插件{}的sql脚本{}", plugInfo.getName(), sqlFile.getAbsolutePath());
                new SqlScriptExecutor().executeSqlScript(dataSource, sqlFile);
            }
        } catch (Exception e) {
            log.error("安装执行插件的sql脚本失败", e);
            throw new RuntimeException(e);
        }
        addInstallStepFlag(plugInfo, "sql");
    }

    /**
     * 执行插件的sql脚本
     *
     * @param plugInfo 插件信息
     */
    public void runUnstallSql(PlugInfo plugInfo) {
        try {
            log.info("卸载执行插件{}的sql脚本", plugInfo.getName());
            File sqlFile = new File(plugInfo.getPlugPath() + "/bin/" + "uninstall.sql");
            if (sqlFile.exists()) {
                log.info("执行插件{}的sql脚本{}", plugInfo.getName(), sqlFile.getAbsolutePath());
                new SqlScriptExecutor().executeSqlScript(dataSource, sqlFile);
            }
        } catch (Exception e) {
            log.error("卸载执行插件的sql脚本失败", e);
            throw new RuntimeException(e);
        }
    }

    public void injectEnvironment(PlugInfo plugInfo) {
        log.info("安装加载插件{}的properties", plugInfo.getName());
        File file = new File(plugInfo.getPlugPath() + "/env/" + "plug.properties");
        if (file.exists()) {
            try {
                log.info("加载插件{}的properties文件{}", plugInfo.getName(), file.getAbsolutePath());
                PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
                Properties properties = new Properties();
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file), StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }

                // 创建 PropertySource 并添加到 Environment 中
                String propertySourceName = String.format("plug_%s_properties", plugInfoYml.getName());
                PropertiesPropertySource propertySource =
                        new PropertiesPropertySource(propertySourceName, properties);
                ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;

                MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
                if (propertySources.contains(propertySourceName)) {
                    propertySources.replace(propertySourceName, propertySource);
                } else {
                    propertySources.addLast(propertySource);
                }
            } catch (Exception e) {
                log.error("注入插件配置文件失败", e);
                throw new RuntimeException(e);
            }
        }
        addInstallStepFlag(plugInfo, "properties");



/*        DotenvBuilder builder = new DotenvBuilder();
        builder.directory(path);
        builder.filename(fileName);
        Dotenv dotenv = builder.load();

        Map<String, Object> envMap = new HashMap<>();
        for (DotenvEntry entry : dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
            if (!environment.containsProperty(entry.getKey())) {
                envMap.put(entry.getKey(), entry.getValue());
            }
        }

        String propertySourceName = String.format("plug_%s_env", plugName);
        ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        if (propertySources.contains(propertySourceName)) {
            propertySources.replace(propertySourceName, new MapPropertySource(propertySourceName, envMap));
        } else {
            propertySources.addLast(new MapPropertySource(propertySourceName, envMap));
        }*/
    }

    public void pullUpEnvironment(PlugInfo plugInfo) {
        File file = new File(plugInfo.getPlugPath() + "/env/" + "plug.properties");
        if (file.exists()) {
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
            String propertySourceName = String.format("plug_%s_properties", plugInfoYml.getName());
            ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;

            MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
            propertySources.remove(propertySourceName);
        }
    }

    private void saveStatic(PlugInfo plugInfo) {
        //  抽取静态文件
        try {
            log.info("复制插件{}的静态资源", plugInfo.getName());
            File staticPath = new File(plugInfo.getPlugPath() + "/static");
            if (staticPath.exists()) {
                for (File file : staticPath.listFiles()) {
                    FileUtil.copy(file, new File(Constants.ROOT_PATH + "/system/resource/supos/"), true);
                }
            }
        } catch (Exception e) {
            log.error("installStatic error", e);
            throw new RuntimeException(e);
        }
        addInstallStepFlag(plugInfo, "static");
    }

    private void unInstallStatic(PlugInfo plugInfo) {
        //  抽取静态文件
        try {
            log.info("删除插件{}的静态资源", plugInfo.getName());
            File staticPath = new File(plugInfo.getPlugPath() + "/static");
            if (staticPath.exists()) {
                for (File file : staticPath.listFiles()) {
                    FileUtil.del(new File(Constants.ROOT_PATH + "/system/resource/supos/" + file.getName()));
                }
            }
        } catch (Exception e) {
            log.error("unInstallStatic error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 注册路由
     *
     * @param plugInfo
     */
    private void saveRoute(PlugInfo plugInfo) {
        try {
            log.info("注册插件{}的路由", plugInfo.getName());
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();

            if (plugInfoYml != null && plugInfoYml.getRoute() != null) {
                PlugInfoYml.PlugRoute plugRoute = plugInfoYml.getRoute();

                MenuDto menuDto = new MenuDto();
                menuDto.setName(plugRoute.getName());
                menuDto.setServiceName("frontend");

                List<String> tags = new ArrayList<>();
                tags.add("menu");
                tags.add("remote");
                if (StringUtils.isNotBlank(plugRoute.getDescription())) {
                    tags.add(String.format("description:%s", plugRoute.getDescription()));
                }

                if (plugRoute.getSort() != null) {
                    tags.add(String.format("sort:%d", plugRoute.getSort()));
                }
                if (StringUtils.isNotBlank(plugRoute.getParentName())) {
                    tags.add(String.format("parentName:%s", plugRoute.getParentName()));
                }
                if (StringUtils.isNotBlank(plugRoute.getIcon())) {
                    tags.add(String.format("iconUrl:%s", plugRoute.getIcon()));
                }
                if (StringUtils.isNotBlank(plugRoute.getModuleName())) {
                    tags.add(String.format("moduleName:%s", plugRoute.getModuleName()));
                }
                if (StringUtils.isNotBlank(plugRoute.getHomeParentName())) {
                    tags.add(String.format("homeParentName:%s", plugRoute.getHomeParentName()));
                }
                if (StringUtils.isNotBlank(plugRoute.getHomeIconUrl())) {
                    tags.add(String.format("homeIconUrl:%s", plugRoute.getHomeIconUrl()));
                }
                menuDto.setTags(tags);
                menuDto.setBaseUrl(plugRoute.getPath());

                menuService.createRoutewithNoService(menuDto, false, false);
            }
        } catch (Exception e) {
            log.error("saveRoute error", e);
            throw new RuntimeException(e);
        }
        addInstallStepFlag(plugInfo, "route");
    }

    /**
     * 卸载路由
     *
     * @param plugInfo
     */
    private void deleteRoute(PlugInfo plugInfo) {
        try {
            log.info("卸载插件{}的路由", plugInfo.getName());
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();

            if (plugInfoYml != null && plugInfoYml.getRoute() != null) {

                PlugInfoYml.PlugRoute plugRoute = plugInfoYml.getRoute();
                menuService.deleteMenu(plugRoute.getName());
            }
        } catch (Exception e) {
            log.error("deleteRoute error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 安装前端
     *
     * @param plugInfo
     */
    private void installFront(PlugInfo plugInfo) {
        //  抽取前端文件
        try {
            log.info("安装插件{}的前端", plugInfo.getName());
            File frontPath = new File(plugInfo.getPlugPath() + "/front");
            if (frontPath.exists()) {
                for (File file : frontPath.listFiles()) {
                    FileUtil.copy(file, new File(Constants.PLUGIN_FRONTEND_PATH), true);
                }
            }
        } catch (Exception e) {
            log.error("installFront error", e);
            throw new RuntimeException(e);
        }
        addInstallStepFlag(plugInfo, "front");
    }

    /**
     * 卸载前端
     *
     * @param plugInfo
     */
    private void unInstallFront(PlugInfo plugInfo) {
        // 卸载前端文件
        try {
            log.info("卸载插件{}的前端", plugInfo.getName());
            File frontPath = new File(plugInfo.getPlugPath() + "/front");
            if (frontPath.exists()) {
                for (File file : frontPath.listFiles()) {
                    FileUtil.del(new File(Constants.PLUGIN_FRONTEND_PATH + "/" + file.getName()));
                }
            }
        } catch (Exception e) {
            log.error("unInstallFront error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 列出插件所依赖的插件名
     *
     * @param currentPlugInfo
     * @return
     */
    private List<String> listDependenciesDown(PlugInfo currentPlugInfo) {
        PlugInfoYml plugInfoYml = currentPlugInfo.getPlugInfoYml();
        if (plugInfoYml != null && CollectionUtils.isNotEmpty(plugInfoYml.getDependencies())) {
            return plugInfoYml.getDependencies().stream().map(plugDependency -> plugDependency.getName()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * 列出依赖该插件的插件名
     *
     * @param currentPlugInfo
     * @return
     */
    private List<String> listDependenciesUp(PlugInfo currentPlugInfo, boolean deep) {
        String plugName = currentPlugInfo.getName();

        List<String> dependOnNames = new ArrayList<>();
        for (PlugInfo plugInfo : plugins.values()) {
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
            if (plugInfoYml != null && CollectionUtils.isNotEmpty(plugInfoYml.getDependencies())) {
                for (PlugInfoYml.PlugDependency plugDependency : plugInfoYml.getDependencies()) {
                    if (StringUtils.equals(plugName, plugDependency.getName())) {
                        if (deep) {
                            dependOnNames.addAll(listDependenciesUp(plugInfo, true));
                        }
                        dependOnNames.add(plugInfo.getName());
                    }
                }
            }
        }

        return dependOnNames;
    }


    private Pair<Boolean, String> doUninstallPlugin(PlugInfo plugInfo, Boolean deleteSqlData) {
        log.info("uninstall plugin:{}, deleteSqlData:{}", plugInfo, deleteSqlData);
        AtomicBoolean uninstallSuccess = new AtomicBoolean(true);
        AtomicReference<String> checkMsg = new AtomicReference<>();
        String msg = null;
        if (!StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())
                && !StringUtils.equals(PlugInfo.STATUS_INSTALL_FAIL, plugInfo.getInstallStatus())) {
            return Pair.of(uninstallSuccess.get(), msg);
        }
        try {
            if (deleteSqlData) {

                EventBus.publishEvent(new PluginPreUnInstallEvent(this, plugInfo.getName(), (canUnInstallMsg) -> {
                    if (canUnInstallMsg != null) {
                        uninstallSuccess.set(false);
                        checkMsg.set(canUnInstallMsg);
                    }
                }));
            }

            if (Boolean.TRUE.equals(uninstallSuccess.get())) {
                if (isInstallStepFlag(plugInfo, "route")) {
                    deleteRoute(plugInfo);
                }

                if (isInstallStepFlag(plugInfo, "front")) {
                    unInstallFront(plugInfo);
                }

                if (isInstallStepFlag(plugInfo, "backend")) {
                    pluginJarService.uninstallPlugin(plugInfo);
                }

                if (isInstallStepFlag(plugInfo, "static")) {
                    unInstallStatic(plugInfo);
                }

                if (isInstallStepFlag(plugInfo, "properties")) {
                    pullUpEnvironment(plugInfo);
                }

                if (isInstallStepFlag(plugInfo, "sql")) {
                    if (deleteSqlData) {
                        runUnstallSql(plugInfo);
                    }
                }

                plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
                cleanInstallStepFlag(plugInfo);
                moveInstalledToTemp(plugInfo);

                I18nUtils.removePluginMessageSource(plugInfo.getName());
            } else {
                log.error("plugin check uninstall error, result:{}", checkMsg.get() != null ? checkMsg.get() : "");
                uninstallSuccess.set(false);
                msg = checkMsg.get() != null ? checkMsg.get() : I18nUtils.getMessage("plugin.manager.uninstall.error");
            }
        } catch (Exception e) {
            log.error("doUninstallPlugin error", e);
            uninstallSuccess.set(false);
            msg = I18nUtils.getMessage("plugin.manager.uninstall.error");
        }
        return Pair.of(uninstallSuccess.get(), msg);
    }

    public JsonResult<String> uninstallPlugin(String pluginName) {
        PlugInfo plugInfo = getPluginDetail(pluginName);
        if (plugInfo == null) {
            throw new BuzException("plugin.manager.null");
        }

        if (!StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())) {
            return new JsonResult<>();
        }
        if (plugInfo.getPlugInfoYml().getRemovable() != null && !plugInfo.getPlugInfoYml().getRemovable()) {
            throw new BuzException("plugin.uninstall.forbidden");
        }

        // 校验是否有其它已安装的插件依赖当前插件
        List<String> dependOnNames = listDependenciesUp(plugInfo, false);
        if (CollectionUtils.isNotEmpty(dependOnNames)) {
            Set<String> installedPluginsDependOn = dependOnNames.stream().filter(dependency -> {
                PlugInfo dependencyPlugInfo = getPluginDetail(dependency);
                return dependencyPlugInfo != null && StringUtils.equals(dependencyPlugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED);
            }).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(installedPluginsDependOn)) {
                throw new BuzException("plugin.manager.uninstall.dependency", StringUtils.join(installedPluginsDependOn, ","));
            }
        }

        Pair<Boolean, String> result = doUninstallPlugin(plugInfo, true);
        if (!result.getLeft()) {
            return new JsonResult<>(500, result.getRight());
        }
        return new JsonResult<>();
    }

    private void addInstallStepFlag(PlugInfo plugInfo, String flag) {
        plugInfo.getInstallStepFlags().add(flag);
    }

    private void cleanInstallStepFlag(PlugInfo plugInfo) {
        if (plugInfo.getInstallStepFlags() != null) {
            plugInfo.getInstallStepFlags().clear();
        }

    }

    private boolean isInstallStepFlag(PlugInfo plugInfo, String flag) {
        return plugInfo.getInstallStepFlags().contains(flag);
    }

    public JsonResult<String> upgradePlugin(String pluginName, MultipartFile file) {
        log.info("upgrade plugin:{}", pluginName);
        PlugInfo plugInfo = getPluginDetail(pluginName);
        if (plugInfo == null) {
            throw new BuzException("plugin.manager.null");
        }
        if (!StringUtils.endsWithIgnoreCase(file.getOriginalFilename(), PLUGIN_EXT_NAME)) {
            throw new BuzException("plugin.manager.package.invalid");
        }

        Upgrader upgrader = new Upgrader(plugInfo);
        try {
            upgrader.savePackage(file);
            upgrader.unzipAndCheckPluginUpgradePackage();
            upgrader.stopPlugin();
            upgrader.upgrade();
            return new JsonResult<>(0, pluginName);
        } catch (BuzException e) {
            upgrader.rollback();
            throw e;
        } catch (Exception e) {
            log.error("upgrade plugin error", e);
            upgrader.rollback();
            return new JsonResult<>(500, I18nUtils.getMessage("plugin.manager.upgrade.error"));
        } finally {
            upgrader.clean();
        }
    }

    class Upgrader {
        /**
         * 待升级的插件
         */
        private PlugInfo plugInfo;

        private PlugInfoYml oldPlugInfoYml;
        private String oldPlugDirName;

        /**
         * 记录停止的插件，升级成功或回滚需要重新安装
         */
        private List<String> installedPlugins = new ArrayList<>();

        /**
         * 升级包文件
         */
        private File pluginUpgradePackageFile;
        /**
         * 升级包解压目录
         */
        private File pluginUpgradeUnzipPath;
        /**
         * 升级包Yaml信息
         */
        private PlugInfoYml newPlugInfoYml;
        private String newPlugDirName;

        private String flag = "";

        public Upgrader(PlugInfo plugInfo) {
            this.plugInfo = plugInfo;
            this.oldPlugInfoYml = plugInfo.getPlugInfoYml();
        }

        /**
         * 保存插件包到upgrade-temp目录
         *
         * @param file
         */
        public File savePackage(MultipartFile file) {
            flag = "savePackage-start";
            String pluginUpgradePackageName = file.getOriginalFilename();
            pluginUpgradePackageFile = new File(Constants.PLUGIN_UPGRADE_TEMP_PATH, pluginUpgradePackageName);
            try {
                FileUtil.copyFile(file.getInputStream(), pluginUpgradePackageFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("transferTo error", e);
                throw new BuzException("plugin.manager.transfer.error");
            }
            flag = "savePackage-end";
            return pluginUpgradePackageFile;
        }

        /**
         * 将插件升级包解压到upgrade-temp目录，并校验plugin.yaml文件
         */
        public void unzipAndCheckPluginUpgradePackage() {
            flag = "unzipAndCheckPluginUpgradePackage-start";
            String pluginUpgradePackageName = pluginUpgradePackageFile.getName();
            String pluginUpgradeDirName = String.format("%s-%d", StringUtils.replaceIgnoreCase(pluginUpgradePackageName, PLUGIN_EXT_NAME, ""), System.currentTimeMillis());
            pluginUpgradeUnzipPath = new File(Constants.PLUGIN_UPGRADE_TEMP_PATH, pluginUpgradeDirName);

            // 解压插件包
            unzipAndCheckPlugin(pluginUpgradePackageFile, pluginUpgradeUnzipPath.getAbsolutePath());

            newPlugInfoYml = parsePlugInfoYml(pluginUpgradeUnzipPath.getAbsolutePath());

            if (newPlugInfoYml == null) {
                throw new BuzException("plugin.manager.package.yml.empty");
            }
            if (!StringUtils.equals(newPlugInfoYml.getName(), plugInfo.getName())) {
                throw new BuzException("plugin.manager.package.name.invalid", plugInfo.getName());
            }
            flag = "unzipAndCheckPluginUpgradePackage-end";
        }

        /**
         * 停止插件和依赖它的插件
         */
        public void stopPlugin() {
            flag = "stopPlugin-start";
            // 如果插件是安装状态，需要将插件以及所有依赖它的插件进行不删数据的卸载
            if (StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())) {
                // 列出所有依赖它的插件
                List<String> dependOnNames = listDependenciesUp(plugInfo, true);
                if (CollectionUtils.isNotEmpty(dependOnNames)) {
                    for (String dependOnName : dependOnNames) {
                        PlugInfo dependOnPlugInfo = getPluginDetail(dependOnName);
                        // 卸载依赖它的插件
                        if (dependOnPlugInfo != null && StringUtils.equals(PlugInfo.STATUS_INSTALLED, dependOnPlugInfo.getInstallStatus())) {
                            doUninstallPlugin(dependOnPlugInfo, false);
                            installedPlugins.add(dependOnName);
                        }
                    }
                }

                // 卸载当前插件
                doUninstallPlugin(plugInfo, false);
            }
            flag = "stopPlugin-end";
        }


        /**
         * 升级插件
         */
        public void upgrade() {
            flag = "upgrade-start";
            // 1.将插件目录从temp移到upgrade目录
            oldPlugDirName = new File(plugInfo.getPlugPath()).getName();
            FileUtil.move(new File(plugInfo.getPlugPath()), new File(Constants.PLUGIN_UPGRADE_PATH, oldPlugDirName), true);
            log.info("move old plugin from {} to {}", plugInfo.getPlugPath(), new File(Constants.PLUGIN_UPGRADE_PATH, oldPlugDirName).getAbsolutePath());

            // 2.将升级包目录从upgrade-temp移到temp目录
            File newPlugDir = new File(Constants.PLUGIN_TEMP_PATH, pluginUpgradeUnzipPath.getName());
            FileUtil.move(pluginUpgradeUnzipPath, newPlugDir, true);
            log.info("move new plugin from {} to {}", pluginUpgradeUnzipPath.getAbsolutePath(), newPlugDir.getAbsolutePath());
            newPlugDirName = newPlugDir.getName();

            plugInfo.setPlugInfoYml(newPlugInfoYml);
            plugInfo.setPlugPath(newPlugDir.getAbsolutePath());

            if (Boolean.TRUE.equals(newPlugInfoYml.getAutoInstall())) {
                installedPlugins.add(plugInfo.getName());
            }

            log.info("start stoped plugin");
            for (String installedPlugin : installedPlugins) {

                doInstallPluginWithDependency(installedPlugin);

            }
            FileUtil.del(new File(Constants.PLUGIN_UPGRADE_PATH, oldPlugDirName));
            flag = "upgrade-end";
        }

        public void rollback() {
            log.info("upgrade rollback");
            if (StringUtils.equals("upgrade-end", flag)) {
                return;
            } else if (StringUtils.equals("upgrade-start", flag) || StringUtils.equals("stopPlugin-end", flag)) {
                // 插件包替换回老插件包
                if (newPlugDirName != null) {
                    FileUtil.del(new File(Constants.PLUGIN_TEMP_PATH, newPlugDirName));
                    log.info("del new plugin {}", new File(Constants.PLUGIN_TEMP_PATH, newPlugDirName).getAbsolutePath());
                }

                FileUtil.move(new File(Constants.PLUGIN_UPGRADE_PATH, oldPlugDirName), new File(Constants.PLUGIN_TEMP_PATH, oldPlugDirName), true);
                log.info("move old plugin from {} to {}", new File(Constants.PLUGIN_UPGRADE_PATH, oldPlugDirName).getAbsolutePath(), new File(Constants.PLUGIN_TEMP_PATH, oldPlugDirName).getAbsolutePath());

                plugInfo.setPlugInfoYml(oldPlugInfoYml);
                plugInfo.setPlugPath(new File(Constants.PLUGIN_TEMP_PATH, oldPlugDirName).getAbsolutePath());
                log.info("start stoped plugin");
                for (String installedPlugin : installedPlugins) {

                    doInstallPluginWithDependency(installedPlugin);

                }

                if (Boolean.TRUE.equals(oldPlugInfoYml.getAutoInstall())) {
                    doInstallPluginWithDependency(plugInfo.getName());
                }
            } else if (StringUtils.equals("stopPlugin-start", flag) || StringUtils.equals("unzipAndCheckPluginUpgradePackage-end", flag)) {
                for (String installedPlugin : installedPlugins) {

                    doInstallPluginWithDependency(installedPlugin);

                }

                if (Boolean.TRUE.equals(oldPlugInfoYml.getAutoInstall())) {
                    doInstallPluginWithDependency(plugInfo.getName());
                }
            }
        }

        public void clean() {
            FileUtil.del(pluginUpgradeUnzipPath);
            FileUtil.del(pluginUpgradePackageFile);
        }
    }
}
