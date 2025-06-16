package com.supos.uns.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.util.DynamicClassLoader;
import com.supos.adpter.kong.dto.MenuDto;
import com.supos.adpter.kong.service.MenuService;
import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PlugInfoYml;
import com.supos.common.event.EventBus;
import com.supos.common.event.PluginPreUnInstallEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PlugUtils;
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
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
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
        ThreadUtil.execute(() -> {
            try {
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

        if (pluginScanPath.exists() && pluginScanPath.isDirectory()) {
            File[] pluginPackageFiles = pluginScanPath.listFiles(File::isFile);
            if (pluginPackageFiles != null && pluginPackageFiles.length > 0) {
                Map<String, String> pluginDirs = scanPluginDirs(pluginTempPath, pluginInstalledPath);
                for (File pluginPackageFile : pluginPackageFiles) {
                    String pluginPackageName = pluginPackageFile.getName();
                    log.info("Scanning plugin file:{}", pluginPackageName);
                    if (!StringUtils.endsWithIgnoreCase(pluginPackageName, PLUGIN_EXT_NAME)) {
                        log.warn("Unrecognizable file:{}", pluginPackageName);
                        continue;
                    }
                    String pluginDirName = StringUtils.replaceIgnoreCase(pluginPackageName, PLUGIN_EXT_NAME, "");
                    String pluginPath = pluginDirs.get(pluginDirName);
                    if (pluginPath == null) {
                        isChanged = true;
                        String unzipTargetPath = String.format("%s/%s", pluginTempPath.getAbsolutePath(), pluginDirName);
                        unzipAndCheckPlugin(pluginPackageFile, unzipTargetPath);
                    } else {
                        // 尝试覆盖
                        log.warn("Plugin Dir already exists:{}", pluginDirName);
                        if (initScan) {
                            isChanged = true;
                            String unzipTargetPath = pluginPath;
                            unzipAndCheckPlugin(pluginPackageFile, unzipTargetPath);
                        } else {
                            PlugInfo existPlugInfo = getByPlugDir(pluginDirName);
                            if (existPlugInfo != null) {
                                if (existPlugInfo.getInstallStatus().equals(PlugInfo.STATUS_NOT_INSTALL) || existPlugInfo.getInstallStatus().equals(PlugInfo.STATUS_INSTALL_FAIL)) {
                                    isChanged = true;
                                    String unzipTargetPath = existPlugInfo.getPlugPath();
                                    unzipAndCheckPlugin(pluginPackageFile, unzipTargetPath);
                                } else {
                                    log.warn("Plugin already exists:{}", existPlugInfo.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
        return isChanged;
    }

    private Map<String, PlugInfo> scanPluginTempDir() {
        Map<String, PlugInfo> scanPlugins = new HashMap<>();

        // 扫描临时目录，设置为未安装
        File pluginTempPath = new File(Constants.PLUGIN_TEMP_PATH);
        File[] pluginTempDirs = pluginTempPath.listFiles();
        for (File pluginTempDir : pluginTempDirs) {
            PlugInfoYml plugInfoYml = PlugUtils.getPlugInfoYml(pluginTempDir.getAbsolutePath());
            plugInfoYml.setShowName(plugInfoYml.getShowName() != null ? I18nUtils.getMessage(plugInfoYml.getShowName()) : plugInfoYml.getShowName());
            plugInfoYml.setDescription(plugInfoYml.getDescription() != null ? I18nUtils.getMessage(plugInfoYml.getDescription()) : plugInfoYml.getDescription());

            PlugInfo plugInfo = new PlugInfo();
            plugInfo.setPlugInfoYml(plugInfoYml);
            plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
            plugInfo.setPlugPath(pluginTempDir.getAbsolutePath());
            scanPlugins.put(plugInfoYml.getName(), plugInfo);
        }
        return scanPlugins;
    }

    private Map<String, PlugInfo> scanPluginInstalledDir() {
        Map<String, PlugInfo> scanPlugins = new HashMap<>();

        // 扫描已安装目录，设置为已安装
        File pluginInstalledPath = new File(Constants.PLUGIN_INSTALLED_PATH);
        File[] pluginInstalledDirs = pluginInstalledPath.listFiles();
        for (File pluginInstalledDir : pluginInstalledDirs) {
            PlugInfoYml plugInfoYml = PlugUtils.getPlugInfoYml(pluginInstalledDir.getAbsolutePath());
            plugInfoYml.setShowName(plugInfoYml.getShowName() != null ? I18nUtils.getMessage(plugInfoYml.getShowName()) : plugInfoYml.getShowName());
            plugInfoYml.setDescription(plugInfoYml.getDescription() != null ? I18nUtils.getMessage(plugInfoYml.getDescription()) : plugInfoYml.getDescription());

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

            plugins.put(e.getKey(), plugInfo);
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

    public JsonResult<Collection<PlugInfo>> listPlugins() {
        if (scanPluginPackage(false)) {
            Map<String, PlugInfo> scanPlugins = scanPluginDirs(Constants.PLUGIN_TEMP_PATH);
            for (Map.Entry<String, PlugInfo> e : scanPlugins.entrySet()) {
                PlugInfo plugInfo = plugins.get(e.getKey());
                if (plugInfo == null) {
                    plugins.put(e.getKey(), e.getValue());
                } else {
                    plugInfo.setPlugInfoYml(e.getValue().getPlugInfoYml());
                }
            }
        }

        return new JsonResult<>(0, "ok", plugins.values());
    }

    public PlugInfo getPluginDetail(String name) {
        return plugins.get(name);
    }

    public void putPlugin(PlugInfo plugInfo) {
        plugins.put(plugInfo.getName(), plugInfo);
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
        PlugInfo plugInfo = plugins.get(pluginName);
        if (plugInfo == null) {
            throw new BuzException("plugin.manager.null");
        }
        if (StringUtils.equals(plugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED)) {
            return;
        }

        // 校验依赖插件是否已安装
        Set<String> unInstallDependencies = checkNotInstallDependency(plugInfo);
        if (CollectionUtils.isNotEmpty(unInstallDependencies)) {
            for (String dependency : unInstallDependencies) {
                PlugInfo dependencyPlugInfo = plugins.get(dependency);
                if (dependencyPlugInfo == null) {
                    throw new BuzException("plugin.manager.null");
                }
                try {
                    doInstallPlugin(dependencyPlugInfo);
                } catch (Exception e) {
                    log.error("install dependency plugin {} error", dependency, e);
                    throw e;
                }
            }
        }
        doInstallPlugin(plugInfo);
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
        } catch (Exception e) {
            log.error("installPlugin error", e);
            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALL_FAIL);
            log.info("start rollback plugin");
            doUninstallPlugin(plugInfo);
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
            PlugInfo plugInfo = plugins.get(pluginName);
            if (plugInfo == null) {
                throw new BuzException("plugin.manager.null");
            }
            if (StringUtils.equals(plugInfo.getInstallStatus(), PlugInfo.STATUS_INSTALLED)) {
                return new JsonResult<>(0, pluginName);
            }

            // 校验依赖插件是否已安装
            Set<String> unInstallDependencies = checkNotInstallDependency(plugInfo);
            if (CollectionUtils.isNotEmpty(unInstallDependencies)) {
                throw new BuzException("plugin.manager.install.dependency", StringUtils.join(unInstallDependencies, ","));
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
        PlugInfo oldPlug = plugins.get(plugName);
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
                plugins.put(plugInfo.getName(), plugInfo);
                plugInfo.getInstallStepFlags().add("backend");
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
     * 获取出未安装的依赖插件
     *
     * @param plugInfo
     * @return
     */
    private Set<String> checkNotInstallDependency(PlugInfo plugInfo) {
        PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
        Set<String> dependencyPkgNames = new HashSet<>();
        if (plugInfoYml != null && CollectionUtils.isNotEmpty(plugInfoYml.getDependencies())) {


            for (PlugInfoYml.PlugDependency plugDependency : plugInfoYml.getDependencies()) {
                PlugInfo plugDependencyInfo = plugins.get(plugDependency.getName());
                if (plugDependencyInfo == null || !StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugDependencyInfo.getInstallStatus())) {
                    dependencyPkgNames.add(plugDependency.getName());
                }
            }

        }
        return dependencyPkgNames;
    }

    /**
     * 检查依赖该插件的插件
     *
     * @param currentPlugInfo
     * @return
     */
    private Set<String> checkInstalledPluginDependOn(PlugInfo currentPlugInfo) {
        String plugName = currentPlugInfo.getName();

        Set<String> dependOnNames = new HashSet<>();
        for (PlugInfo plugInfo : plugins.values()) {
            if (!StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())) {
                continue;
            }
            PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
            if (plugInfoYml != null && CollectionUtils.isNotEmpty(plugInfoYml.getDependencies())) {
                for (PlugInfoYml.PlugDependency plugDependency : plugInfoYml.getDependencies()) {
                    if (StringUtils.equals(plugName, plugDependency.getName())) {
                        dependOnNames.add(plugInfo.getName());
                    }
                }
            }
        }

        return dependOnNames;
    }


    private Pair<Boolean, String> doUninstallPlugin(PlugInfo plugInfo) {
        AtomicBoolean uninstallSuccess = new AtomicBoolean(true);
        String msg = null;
        if (!StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())
                && !StringUtils.equals(PlugInfo.STATUS_INSTALL_FAIL, plugInfo.getInstallStatus())) {
            return Pair.of(uninstallSuccess.get(), msg);
        }
        try {
            AtomicReference<String> checkMsg = new AtomicReference<>();
            EventBus.publishEvent(new PluginPreUnInstallEvent(this, plugInfo.getName(), (canUnInstallMsg) -> {
                if (canUnInstallMsg != null) {
                    uninstallSuccess.set(false);
                    checkMsg.set(canUnInstallMsg);
                }
            }));
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
                    runUnstallSql(plugInfo);
                }

                plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
                cleanInstallStepFlag(plugInfo);
                moveInstalledToTemp(plugInfo);
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
        log.info("uninstall plugin:{}", pluginName);
        PlugInfo plugInfo = plugins.get(pluginName);
        if (plugInfo == null) {
            throw new BuzException("plugin.manager.null");
        }
        if (!StringUtils.equals(PlugInfo.STATUS_INSTALLED, plugInfo.getInstallStatus())) {
            return new JsonResult<>();
        }

        // 校验是否有其它已安装的插件依赖当前插件
        Set<String> dependOnNames = checkInstalledPluginDependOn(plugInfo);
        if (CollectionUtils.isNotEmpty(dependOnNames)) {
            throw new BuzException("plugin.manager.uninstall.dependency", StringUtils.join(dependOnNames, ","));
        }

        Pair<Boolean, String> result = doUninstallPlugin(plugInfo);
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
}
