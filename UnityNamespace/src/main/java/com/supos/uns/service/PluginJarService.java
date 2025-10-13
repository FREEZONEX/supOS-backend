package com.supos.uns.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisXMLMapperBuilder;
import com.google.common.cache.Cache;
import com.supos.common.dto.PlugInfoYml;
import com.supos.uns.bo.PackageClassLoaderInfo;
import com.supos.uns.bo.PlugInfo;
import com.supos.uns.util.BootJarToLibJar;
import com.supos.uns.util.LongestCommonPrefix;
import com.supos.uns.util.YamlToProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.util.LifecycleBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.jetbrains.annotations.NotNull;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.*;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

@Slf4j
@Service
public class PluginJarService {
    private ApplicationContext beanFactory;
    private Map<String, RootBeanDefinition> mergedBeanDefinitions;
    private ContextRefreshedEvent contextRefreshedEvent;
    private ContextClosedEvent contextClosedEvent;
    private ConfigurableEnvironment environment;
    private BeanDefinition springBootStartBean;
    //    private String bootBeanName;
    private String CONFIGURATION_CLASS_ATTRIBUTE;

    //    @Autowired
//    MultipleOpenApiResource openApi;
//    @Autowired
//    SpringWebProvider springWebProvider;
    @Autowired
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired
    RequestMappingHandlerMapping handlerMapping;
    @Autowired
    ApplicationEventMulticaster eventMulticaster;
    @Autowired
    MybatisPlusAutoConfiguration mybatisPlusAutoConfiguration;
    Method getMergedLocalBeanDefinition;
    @Autowired
    private ServletContext servletContext;
    private org.apache.catalina.core.ApplicationContext origAppContext;
    private StandardContext origServletContext;
    Set<String> loadedResources;

    @Order(1001)
    @EventListener(classes = ContextRefreshedEvent.class)
    void init(ContextRefreshedEvent event) throws Exception {
        contextRefreshedEvent = event;
        contextClosedEvent = new ContextClosedEvent(event.getApplicationContext());
        beanFactory = event.getApplicationContext();
        environment = (ConfigurableEnvironment) beanFactory.getEnvironment();
        final Field mergedBeanDefinitionsField;
        try {
            mergedBeanDefinitionsField = AbstractBeanFactory.class.getDeclaredField("mergedBeanDefinitions");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        mergedBeanDefinitionsField.setAccessible(true);
        mergedBeanDefinitions = (Map<String, RootBeanDefinition>) mergedBeanDefinitionsField.get(beanFactory.getAutowireCapableBeanFactory());
        String[] bootBeanNames = beanFactory.getBeanNamesForAnnotation(SpringBootApplication.class);
        if (bootBeanNames.length > 0) {
            String bootBeanName = bootBeanNames[0];
            BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
            springBootStartBean = beanDefinitionRegistry.getBeanDefinition(bootBeanName);
        }
        Field attr = ConfigurationClassUtils.class.getDeclaredField("CONFIGURATION_CLASS_ATTRIBUTE");
        attr.setAccessible(true);
        CONFIGURATION_CLASS_ATTRIBUTE = (String) attr.get(null);
        getMergedLocalBeanDefinition = AbstractBeanFactory.class.getDeclaredMethod("getMergedLocalBeanDefinition", String.class);//
        getMergedLocalBeanDefinition.setAccessible(true);
        if (servletContext instanceof ApplicationContextFacade) {
            Field ctxTarget = ApplicationContextFacade.class.getDeclaredField("context");
            ctxTarget.setAccessible(true);
            Object tar = ctxTarget.get(servletContext);
            if (tar instanceof org.apache.catalina.core.ApplicationContext appCtx) {
                origAppContext = appCtx;
                ctxTarget = org.apache.catalina.core.ApplicationContext.class.getDeclaredField("context");
                ctxTarget.setAccessible(true);
                Object directCtx = ctxTarget.get(appCtx);
                if (directCtx instanceof StandardContext tomcatCtx) {
                    origServletContext = tomcatCtx;
                }
            }
        }
        Configuration configuration = getConfiguration(sqlSessionTemplate);
        Field loadedResourcesF = Configuration.class.getDeclaredField("loadedResources");
        loadedResourcesF.setAccessible(true);
        loadedResources = (Set<String>) loadedResourcesF.get(configuration);
    }

    public void setPlugName(File pluginJar, PlugInfo plugInfo) throws IOException {
        try (JarFile jarFile = new JarFile(pluginJar)) {
            Manifest manifest = jarFile.getManifest();
            String name, version;
            {
                String[] names = new String[1], versions = new String[1];
                manifest.getMainAttributes().forEach((k, v) -> {
                    String key = k.toString().toLowerCase();
                    if (key.contains("title")) {
                        names[0] = v.toString();
                    } else if (key.contains("version")) {
                        versions[0] = v.toString();
                    }
                });
                name = names[0];
                version = versions[0];
                PlugInfoYml yml = plugInfo.getPlugInfoYml();
                if (yml == null) {
                    plugInfo.setPlugInfoYml(yml = new PlugInfoYml());
                }
                yml.setName(name);
                yml.setVersion(version);
            }
        }
    }

    private PluginClassLoader loadBaseInfo(PlugInfo plugInfo, File pluginJar, Function<String, PlugInfo> dependencySupplier) throws Exception {
        PlugInfoYml plugInfoYml = plugInfo.getPlugInfoYml();
        List<PackageClassLoaderInfo> depends = null;
        if (plugInfoYml != null && plugInfoYml.getDependencies() != null) {
            depends = new ArrayList<>(plugInfoYml.getDependencies().size());
            for (PlugInfoYml.PlugDependency plugDependency : plugInfoYml.getDependencies()) {
                PlugInfo plugDependencyInfo = dependencySupplier.apply(plugDependency.getName());
                ClassLoader loader;
                if (plugDependencyInfo != null && (loader = plugDependencyInfo.getClassLoader()) != null) {
                    depends.add(new PackageClassLoaderInfo(plugDependencyInfo.getBasePackage(), loader));
                }
            }
        }
        boolean isSpringBootJar = false;
        try (JarFile jarFile = new JarFile(pluginJar)) {
            Manifest manifest = jarFile.getManifest();
            String BootClasses = manifest.getMainAttributes().getValue("Spring-Boot-Classes");
            if (BootClasses != null) {
                isSpringBootJar = true;
            }
        }
        final List<URL> libJars;
        if (isSpringBootJar) {
            // 修改为普通jar
            libJars = new ArrayList<>(128);
            File libsDir = new File(pluginJar.getParentFile(), plugInfo.getName() + "_libs");
            if (libsDir.exists()) {
                FileUtil.del(libsDir);
            }
            BootJarToLibJar.searchAndExec(pluginJar, libsDir, (jarName, jarIn) -> {
                Boolean acceptJar = null;
                try (JarInputStream jarInputStream = new JarInputStream(jarIn)) {
                    ZipEntry entry = jarInputStream.getNextEntry();
                    boolean firstClass = true;
                    while (entry != null) {
                        String name = entry.getName();
                        if (name.endsWith(".class")) {
                            URL res = PluginJarService.class.getClassLoader().getResource(name);
                            if (res == null) {
                                log.debug("classNotFound: {}, from jar: {} ", name, jarName);
                                if (firstClass) {
                                    entry = jarInputStream.getNextEntry();
                                    firstClass = false;
                                    continue;
                                }
                            }
                            acceptJar = res == null;
                            break;
                        }
                        entry = jarInputStream.getNextEntry();
                    }
                } catch (Exception ex) {
                    acceptJar = false;
                }
                if (acceptJar == null) {
                    acceptJar = true;
                }
                if (Boolean.TRUE.equals(acceptJar)) {
                    try {
                        URL uri = new File(libsDir, jarName.substring(jarName.lastIndexOf('/') + 1)).toURL();
                        libJars.add(uri);
                    } catch (MalformedURLException e) {
                    }
                }
                return acceptJar;
            });
        } else {
            libJars = Collections.emptyList();
        }
        HashSet<String> paths = new HashSet<>();
        Properties properties = new Properties();
        try (JarFile jarFile = new JarFile(pluginJar)) {
            Enumeration<JarEntry> itr = jarFile.entries();
            while (itr.hasMoreElements()) {
                JarEntry jarEntry = itr.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    if (!entryName.contains("/proto/")) {
                        int sp = entryName.lastIndexOf('/');
                        if (sp > 0) {
                            paths.add(entryName.substring(0, sp));
                        }
                    }
                } else if (depends == null && entryName.endsWith("pom.xml")) {
                    // 解析 pom.xml的 当前插件名 和 依赖
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc;
                    try (InputStream in = jarFile.getInputStream(jarEntry)) {
                        doc = builder.parse(in);
                    }
                    NodeList nodeList = doc.getDocumentElement().getChildNodes();
                    List<PlugInfoYml.PlugDependency> dependencies = new ArrayList<>(nodeList.getLength());
                    if (plugInfoYml != null) {
                        plugInfoYml.setDependencies(dependencies);
                    }
                    for (int i = 0, SZ = nodeList.getLength(); i < SZ; i++) {
                        Node node = nodeList.item(i);
                        String name = node.getNodeName();
                        if ("dependencies".equals(name)) {
                            NodeList deps = node.getChildNodes();
                            final int DEPENDS = deps.getLength();
                            if (DEPENDS > 0) {
                                depends = new ArrayList<>(DEPENDS);
                                for (int k = 0; k < DEPENDS; k++) {
                                    Node dep = deps.item(k);
                                    if (dep instanceof Element dependency) {
                                        String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent().trim();
                                        PlugInfo plugDependencyInfo = dependencySupplier.apply(artifactId);
                                        ClassLoader loader;
                                        if (plugDependencyInfo != null && (loader = plugDependencyInfo.getClassLoader()) != null) {
                                            dependencies.add(new PlugInfoYml.PlugDependency(artifactId));
                                            depends.add(new PackageClassLoaderInfo(plugDependencyInfo.getBasePackage(), loader));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (entryName.endsWith(".yml") || entryName.endsWith(".yaml")) {
                    try (InputStream stream = jarFile.getInputStream(jarEntry)) {
                        properties.putAll(YamlToProperties.convert(stream));
                    }
                }
            }
        }
        final String basePackage;
        {
            String basePkg = LongestCommonPrefix.longestCommonPrefix(paths.toArray(new String[0]));
            if (basePkg.endsWith("/")) {
                basePkg = basePkg.substring(0, basePkg.length() - 1);
            }
            basePackage = basePkg.replace('/', '.');
        }
        plugInfo.setBasePackage(basePackage);
        URL[] jarUrls;
        URL jarUrl = pluginJar.toURL();
        if (!libJars.isEmpty()) {
            libJars.add(0, jarUrl);
            jarUrls = libJars.toArray(new URL[0]);
        } else {
            jarUrls = new URL[]{jarUrl};
        }
        PluginClassLoader classLoader = new PluginClassLoader(plugInfo.getName(), jarUrls, basePackage, depends != null ? depends.toArray(new PackageClassLoaderInfo[0]) : null);
        plugInfo.setClassLoader(classLoader);

        if (!properties.isEmpty()) {
            injectInnerEnvYaml(plugInfo, properties);
        }
        return classLoader;
    }

    private void injectInnerEnvYaml(PlugInfo plugInfo, Properties properties) {
        // 创建 PropertySource 并添加到 Environment 中
        String propertySourceName = envPropertyInnerName(plugInfo);
        PropertiesPropertySource propertySource = new PropertiesPropertySource(propertySourceName, properties);
        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(propertySourceName)) {
            propertySources.replace(propertySourceName, propertySource);
        } else {
            propertySources.addLast(propertySource);
        }
    }

    private void removeInnerEnvYaml(PlugInfo plugInfo) {
        String propertySourceName = envPropertyInnerName(plugInfo);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(propertySourceName);
    }

    @NotNull
    private static String envPropertyInnerName(PlugInfo plugInfo) {
        return String.format("plug_in_%s_properties", plugInfo.getName());
    }

    public synchronized boolean tryInstallPlugin(PlugInfo plugInfo, File pluginJar, Function<String, PlugInfo> dependencySupplier) throws Exception {
        log.info("tryInstallPlugin： {} {}", plugInfo.getName(), plugInfo.getId());
        PluginClassLoader classLoader = loadBaseInfo(plugInfo, pluginJar, dependencySupplier);

        String basePackage = plugInfo.getBasePackage();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
        // 创建 @Mapper 扫描器
        String[] mapperNames, pkgBeanNames, xmlResources;
        try {
            AtomicReference<String[]> xmlResourcesHd = new AtomicReference<>();
            mapperNames = this.scanPluginMappers(beanDefinitionRegistry, resourceLoader, classLoader, basePackage, xmlResourcesHd);
            xmlResources = xmlResourcesHd.get();
            log.info("{}：{} 扫描Mapper：{}, xmlResources: {}", plugInfo.getName(), plugInfo.getBasePackage(), Arrays.toString(mapperNames), Arrays.toString(xmlResources));
            // 扫描Bean
            pkgBeanNames = this.scanPluginBeans(classLoader, resourceLoader, basePackage);
        } catch (Exception ex) {
            log.error("PluginScanERR: {}: {}", plugInfo.getName(), ex.getMessage());
            uninstallPlugin(plugInfo);
            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALL_FAIL);
            throw ex;
        }
        Method processCandidateBean = AbstractHandlerMethodMapping.class.getDeclaredMethod("processCandidateBean", String.class);
        processCandidateBean.setAccessible(true);

        plugInfo.setBaseInfo(pluginJar.getAbsolutePath(), classLoader, basePackage, pkgBeanNames, mapperNames, xmlResources);
        log.info("setBaseInfo: {}", plugInfo);

        AbstractBeanFactory abstractBeanFactory = (AbstractBeanFactory) beanFactory.getAutowireCapableBeanFactory();
        // 替换 classloader
        for (BeanPostProcessor processor : abstractBeanFactory.getBeanPostProcessors()) {
            if (processor instanceof ProxyProcessorSupport ps) {
                ps.setProxyClassLoader(classLoader);
            }
        }
        ArrayList<String> normalBeans = new ArrayList<>(pkgBeanNames.length);
        ArrayList<String> restBeans = new ArrayList<>(pkgBeanNames.length);
        int eventListenerCount = 0;
        ArrayList<Runnable> closeCallbacks = new ArrayList<>(pkgBeanNames.length);

        ArrayList<Lifecycle> lifecycles = new ArrayList<>(4);
        ArrayList<ApplicationListener> onCloses = new ArrayList<>(pkgBeanNames.length);
        ArrayList<ApplicationListener> onStarts = new ArrayList<>(pkgBeanNames.length);
        Exception installError = null;
        final int PLUG_ID = plugInfo.getId();
        for (final String beanName : pkgBeanNames) {
            AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinitionRegistry.getBeanDefinition(beanName);
            log.debug("插件Bean: {}, plug={}", beanName, plugInfo.getName());
            try {
                // 注册事件处理器
                Object bean = beanFactory.getBean(beanName);
                if (bean instanceof Lifecycle lifecycle) {
                    lifecycles.add(lifecycle);
                }
                final Class klass = definition.hasBeanClass() ? definition.getBeanClass() : AopProxyUtils.ultimateTargetClass(bean);
                // 注册 Controller 接口
                if (AnnotatedElementUtils.hasAnnotation(klass, Controller.class)) {
                    processCandidateBean.invoke(handlerMapping, beanName);
                    restBeans.add(beanName);
                } else {
                    normalBeans.add(beanName);
                }

                if (processBean(PLUG_ID, beanName, klass, bean, onStarts::add, onCloses::add)) {
                    eventListenerCount++;
                }
                Field[] allFields = ReflectUtil.getFieldsDirectly(klass, true);
                Object tarBean = AopProxyUtils.getSingletonTarget(bean);
                if (tarBean == null) {
                    tarBean = bean;
                }
                if (tarBean instanceof Filter filter) {
                    if (origServletContext != null) {
                        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>(filter);
                        registrationBean.setName(beanName);
                        final LifecycleState state = origServletContext.getState();
                        Field stateF = LifecycleBase.class.getDeclaredField("state");
                        stateF.setAccessible(true);
                        stateF.set(origServletContext, LifecycleState.STARTING_PREP);
                        registrationBean.onStartup(servletContext);
                        stateF.set(origServletContext, state);
                        {
                            FilterDef filterDef = origServletContext.findFilterDef(beanName);
                            if (filterDef == null) {
                                filterDef = new FilterDef();
                                filterDef.setFilterName(beanName);
                                filterDef.setFilterClass(filter.getClass().getName());
                                filterDef.setFilter(filter);
                                origServletContext.addFilterDef(filterDef);
                                log.info("注册ServletFilter: {}", beanName);
                            } else {
                                log.info("已有 ServletFilter: {}", filterDef);
                            }
                            origServletContext.filterStart();// 新增后, 刷新 Filter 缓存
                        }
                        FilterDef filterDef = origServletContext.findFilterDef(beanName);
                        if (filterDef != null) {
                            closeCallbacks.add(new NamedRunnable("Filter-" + beanName, () -> {
                                log.info("ServletFilter 卸载回调: {}", beanName);
                                origServletContext.removeFilterDef(filterDef);
                                filter.destroy();
                                origServletContext.filterStart();// 卸载后, 刷新 Filter 缓存
                            }));
                        }
                    }
                }
                for (Field field : allFields) {
                    addUninstallCallbacksByField(field, tarBean, closeCallbacks);
                }
            } catch (Exception ex) {
                normalBeans.clear();
                normalBeans = new ArrayList<>(Arrays.asList(pkgBeanNames));//添加所有beanName以备卸载
                log.error("插件安装失败 " + beanName + ", jar = " + pluginJar, ex);
                installError = ex;
                break;
            }
        }
        if (!closeCallbacks.isEmpty() || !onCloses.isEmpty() || !lifecycles.isEmpty()) {
            if (!onCloses.isEmpty()) {
                Collections.sort(onCloses, PluginJarService::compareBean);
                closeCallbacks.addAll(0, onCloses.stream().map(cl -> new NamedRunnable("contextClosed", () -> cl.onApplicationEvent(contextClosedEvent))).toList());
            }
            if (!lifecycles.isEmpty()) {
                Collections.sort(lifecycles, PluginJarService::compareBean);
                closeCallbacks.addAll(0, lifecycles.stream().map(lifecycle -> new NamedRunnable(lifecycle.toString(), lifecycle::stop)).toList());
            }
            plugInfo.setUninstallCallbacks(closeCallbacks.toArray(new Runnable[0]));
        }
        plugInfo.setBeanNames(normalBeans.toArray(new String[0]));
        plugInfo.setControllerNames(restBeans.toArray(new String[0]));
        plugInfo.setEventListener(eventListenerCount);
        // 还原 classloader
        ClassLoader origCl = getClass().getClassLoader();
        for (BeanPostProcessor processor : abstractBeanFactory.getBeanPostProcessors()) {
            if (processor instanceof ProxyProcessorSupport ps) {
                ps.setProxyClassLoader(origCl);
            }
        }
        // 安装后，清理 swagger 接口缓存

        if (installError != null) {
            uninstallPlugin(plugInfo);
            plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALL_FAIL);
            throw installError;
        }
//        clearSwaggerApiCache(null);

        plugInfo.setInstallStatus(PlugInfo.STATUS_INSTALLED);
        if (!onStarts.isEmpty()) {
            Collections.sort(onStarts, PluginJarService::compareBean);
            for (ApplicationListener listener : onStarts) {
                log.info("插件 {} 启动回调: {}", plugInfo.getName(), listener);
                try {
                    listener.onApplicationEvent(contextRefreshedEvent);
                } catch (Throwable ex) {
                    log.error(plugInfo.getName() + " 插件启动回调失败:" + listener, ex);
                }
            }
        } else {
            log.info("插件 {} 没有启动回调", plugInfo.getName());
        }
        if (!lifecycles.isEmpty()) {
            for (Lifecycle lifecycle : lifecycles) {
                log.info("{} 插件启动生命周期: {}", plugInfo.getName(), lifecycle);
                try {
                    if (!lifecycle.isRunning()) {
                        lifecycle.start();
                    }
                } catch (Throwable ex) {
                    log.error(plugInfo.getName() + " 插件启动回调Lifecycle失败:" + lifecycle, ex);
                }
            }
        }
        log.info("InstalledPlugin： {} {} {}", plugInfo.getName(), plugInfo.getId(), plugInfo.getInstallStatus());
        return true;
    }

    private static int compareBean(Object a, Object b) {
        Integer n1 = getOrder(a), n2;
        if (n1 != null && (n2 = getOrder(b)) != null) {
            return n1 - n2;
        } else if (a instanceof Comparable ac && b instanceof Comparable bc) {
            return ac.compareTo(bc);
        }
        return 0;
    }

    private static Integer getOrder(Object obj) {
        if (obj instanceof Ordered ordered) {
            return ordered.getOrder();
        } else if (obj instanceof Phased phased) {
            return phased.getPhase();
        }
        return null;
    }

    private static void addUninstallCallbacksByField(Field field, Object bean, ArrayList<Runnable> closeCallbacks) throws IllegalAccessException {
        Class type = field.getType();
        if (ExecutorService.class.isAssignableFrom(type)) {
            field.setAccessible(true);
            ExecutorService exec = (ExecutorService) field.get(bean);
            if (exec != null) {
                closeCallbacks.add(new NamedRunnable(field, () -> {
                    exec.shutdown();
                    try {
                        exec.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                    }
                }));
            }
        } else if (Closeable.class.isAssignableFrom(type)) {
            field.setAccessible(true);
            Closeable closeable = (Closeable) field.get(bean);
            if (closeable != null) {
                closeCallbacks.add(new NamedRunnable(field, () -> {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                    }
                }));
            }
        } else if (Cache.class.isAssignableFrom(type)) {
            field.setAccessible(true);
            Cache cache = (Cache) field.get(bean);
            if (cache != null) {
                closeCallbacks.add(new NamedRunnable(field, cache::cleanUp));
            }
        }
    }

    private static class NamedRunnable implements Runnable {
        final String name;
        final Runnable target;

        private NamedRunnable(Field field, Runnable target) {
            this(field.toString(), target);
        }

        private NamedRunnable(String name, Runnable target) {
            this.name = name;
            this.target = target;
        }

        @Override
        public void run() {
            target.run();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @NotNull
    private String[] scanPluginBeans(PluginClassLoader classLoader, DefaultResourceLoader resourceLoader, String basePackage) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory.getAutowireCapableBeanFactory();

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(listableBeanFactory, true, beanFactory.getEnvironment(), resourceLoader);
        Field resc = ClassPathScanningCandidateComponentProvider.class.getDeclaredField("resourcePatternResolver");
        resc.setAccessible(true);
        resc.set(scanner, new PathMatchingResourcePatternResolver(resourceLoader));
        int count = scanner.scan(basePackage);
        int size = beanFactory.getBeanDefinitionCount();
        log.info("扫描数量：{}", count);
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        LinkedHashSet<String> pkgBeanNames = new LinkedHashSet<>(count);
        LinkedHashSet<String> ctlPkgBeanNames = new LinkedHashSet<>(16);

        final ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory.getAutowireCapableBeanFactory();
        for (int i = size - count, j = 0; i < size; i++) {
            final String beanName = beanNames[i];
            BeanDefinition definition = listableBeanFactory.getBeanDefinition(beanName);
            log.debug("扫描：{}, def={}", beanName, definition);
            Class klass = classLoader.loadClass(definition.getBeanClassName());
            if (definition instanceof AbstractBeanDefinition rv) {
                rv.setBeanClass(klass);
            }
            RootBeanDefinition rootBeanDefinition = null;
            try {
                rootBeanDefinition = (RootBeanDefinition) getMergedLocalBeanDefinition.invoke(configurableBeanFactory, beanName);
            } catch (Exception e) {
            }
            if (rootBeanDefinition != null) {
                rootBeanDefinition.setTargetType(klass);
            }
            // Controller 接口
            if (AnnotatedElementUtils.hasAnnotation(klass, Controller.class)) {
                ctlPkgBeanNames.add(beanName);
            } else {
                pkgBeanNames.add(beanName);
            }
        }
        pkgBeanNames.addAll(ctlPkgBeanNames);
        log.info("{} 扫描组件：{}", basePackage, pkgBeanNames);


        listableBeanFactory.setBeanClassLoader(classLoader);
        boolean allowBeanDefinitionOverriding = listableBeanFactory.isAllowBeanDefinitionOverriding();
        listableBeanFactory.setAllowBeanDefinitionOverriding(true);

        springBootStartBean.removeAttribute(CONFIGURATION_CLASS_ATTRIBUTE);
        String[] beanNamesBeforeCfg = beanFactory.getBeanDefinitionNames();
        MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(resourceLoader);

        PlugBeanPostProcessor.post(metadataReaderFactory, listableBeanFactory, new PassThroughSourceExtractor(), resourceLoader, this.environment, AnnotationBeanNameGenerator.INSTANCE);
        String[] beanNamesAfterCfg = beanFactory.getBeanDefinitionNames();

        final int scanCfgCount = beanNamesAfterCfg.length - beanNamesBeforeCfg.length;
        Set<String> cfgBeans = Collections.emptySet();
        if (scanCfgCount > 0) {
            cfgBeans = new LinkedHashSet<>(scanCfgCount);
            log.info("{} 扫描配置类数量：{}", basePackage, scanCfgCount);
            try {
                for (int i = scanCfgCount; i > 0; i--) {
                    final String beanName = beanNamesAfterCfg[beanNamesAfterCfg.length - i];
                    cfgBeans.add(beanName);
                    BeanDefinition definition = listableBeanFactory.getBeanDefinition(beanName);
                    log.debug("扫描配置：{}, def={}:{}", beanName, definition.getClass().getSimpleName(), definition);
                    if (definition.getBeanClassName() != null && definition instanceof AbstractBeanDefinition rv && !rv.hasBeanClass()) {
                        Class klass = classLoader.loadClass(definition.getBeanClassName());
                        rv.setBeanClass(klass);
                    }
                }
            } finally {
                listableBeanFactory.setBeanClassLoader(getClass().getClassLoader());
                listableBeanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
            }
        }
        if (!cfgBeans.isEmpty()) {
            cfgBeans.addAll(pkgBeanNames);
            return cfgBeans.toArray(new String[0]);
        } else {
            return pkgBeanNames.toArray(new String[0]);
        }
    }

    private @NotNull String[] scanPluginMappers(BeanDefinitionRegistry beanDefinitionRegistry, ResourceLoader resourceLoader, ClassLoader classLoader, String basePackage, AtomicReference<String[]> xmlResources) {
        String[] mapperNames = new String[0];
        if (sqlSessionTemplate != null) {
            ClassPathMapperScanner mapperScanner = new ClassPathMapperScanner(beanDefinitionRegistry);
            mapperScanner.setAddToConfig(true);
            mapperScanner.setAnnotationClass(Mapper.class);
            mapperScanner.setSqlSessionTemplate(sqlSessionTemplate);
            mapperScanner.setSqlSessionTemplateBeanName("sqlSessionTemplate");
            mapperScanner.setResourceLoader(resourceLoader);
            mapperScanner.setDefaultScope("");
            mapperScanner.registerFilters();

            final ClassLoader origClassLoader = Resources.getDefaultClassLoader();
            final ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory.getAutowireCapableBeanFactory();
            final TypeConverter origCvt = configurableBeanFactory.getTypeConverter();
            try {
                Resources.setDefaultClassLoader(classLoader);
                int mapperCount = mapperScanner.scan(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
                mapperNames = new String[mapperCount];
                String[] beanNames = beanFactory.getBeanDefinitionNames();
                int size = beanFactory.getBeanDefinitionCount();
                configurableBeanFactory.setTypeConverter(new SimpleTypeConverter() {
                    @Override
                    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {
                        if (value instanceof String str && requiredType == Class.class) {
                            try {
                                T cls = (T) classLoader.loadClass(str);
                                return cls;
                            } catch (ClassNotFoundException e) {
                                log.error("ClassNotFoundException: {}, loader={}", str, classLoader, e);
                            }
                        }
                        return super.convertIfNecessary(value, requiredType, typeDescriptor);
                    }
                });
                for (int i = size - mapperCount, j = 0; i < size; i++) {
                    final String mapperName = beanNames[i];
                    mapperNames[j++] = mapperName;

                    RootBeanDefinition rootBeanDefinition = null;
                    try {
                        rootBeanDefinition = (RootBeanDefinition) getMergedLocalBeanDefinition.invoke(configurableBeanFactory, mapperName);
                    } catch (Exception e) {
                    }
                    if (rootBeanDefinition != null && !rootBeanDefinition.hasBeanClass()) {
                        rootBeanDefinition.setBeanClass(MapperFactoryBean.class);
                    }
                    try {
                        Class mapper = beanFactory.getType(mapperName, true);
                        log.debug("扫描Mapper：{}, mapper={}", mapperName, mapper);
                    } catch (RuntimeException lex) {
                        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(mapperName);
                        if (beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE) instanceof String cls) {
                            try {
                                log.info("{}.className = {}, class={}", mapperName, cls, classLoader.loadClass(cls));
                            } catch (ClassNotFoundException e) {
                                log.warn("ClassNotFoundException: {}", cls);
                            }
                        }
                        log.error("扫描Mapper Err:mapperName={},err= {}", mapperName, lex.getMessage());
                        throw lex;
                    }
                }
                List<String> resList = tryMapperXml(resourceLoader, sqlSessionTemplate);
                if (resList != null && !resList.isEmpty()) {
                    xmlResources.set(resList.toArray(new String[0]));
                }
            } finally {
                configurableBeanFactory.setTypeConverter(origCvt);// 还原 TypeConverter
                Resources.setDefaultClassLoader(origClassLoader);// 还原 classLoader
            }
        }
        return mapperNames;
    }

    private List<String> tryMapperXml(ResourceLoader resourceLoader, SqlSessionTemplate sqlSessionTemplate) {
        List<String> xmlResources = new ArrayList<>();
        try {
            Field fieldProperties = MybatisPlusAutoConfiguration.class.getDeclaredField("properties");
            fieldProperties.setAccessible(true);
            MybatisPlusProperties properties = (MybatisPlusProperties) fieldProperties.get(mybatisPlusAutoConfiguration);

            Field fieldMapperLocations = MybatisPlusProperties.class.getDeclaredField("mapperLocations");
            fieldMapperLocations.setAccessible(true);
            String[] mapperLocations = (String[]) fieldMapperLocations.get(properties);

            PathMatchingResourcePatternResolver resolver = new JarLocalPathMatchingResourcePatternResolver(resourceLoader);
            Resource[] mapperXmls = Stream.of(mapperLocations).flatMap(location -> Stream.of(getResources(resolver, location))).toArray(Resource[]::new);
            log.info("tryMapperXml: mapperXmls={}, mapperLocations={}", Arrays.toString(mapperXmls), Arrays.toString(mapperLocations));
            Configuration configuration = getConfiguration(sqlSessionTemplate);

            for (Resource mapperLocation : mapperXmls) {
                if (mapperLocation == null) {
                    continue;
                }
                try {
                    String res = mapperLocation.toString();
                    xmlResources.add(res);
                    MybatisXMLMapperBuilder xmlMapperBuilder = new MybatisXMLMapperBuilder(mapperLocation.getInputStream(), configuration, res, configuration.getSqlFragments());
                    xmlMapperBuilder.parse();
                } catch (Exception e) {
                    throw new IOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
                } finally {
                    ErrorContext.instance().reset();
                }
                log.debug("Parsed mapper file: '" + mapperLocation + "'");
            }
        } catch (Exception ex) {
            log.error("MappXml 读取失败", ex);
        }
        return xmlResources;
    }

    private static Configuration getConfiguration(SqlSessionTemplate sqlSessionTemplate) throws NoSuchFieldException, IllegalAccessException {
        SqlSessionFactory sessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        DefaultSqlSessionFactory defaultSqlSessionFactory = (DefaultSqlSessionFactory) sessionFactory;
        Field fConfiguration = DefaultSqlSessionFactory.class.getDeclaredField("configuration");
        fConfiguration.setAccessible(true);
        Configuration configuration = (Configuration) fConfiguration.get(defaultSqlSessionFactory);
        return configuration;
    }

    private Resource[] getResources(ResourcePatternResolver resolver, String location) {
        try {
            return resolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }

    static class JarLocalPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {
        public JarLocalPathMatchingResourcePatternResolver(ResourceLoader loader) {
            super(loader);
        }

        protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
            Set<Resource> result = new LinkedHashSet<>(16);
            URLClassLoader cl = (URLClassLoader) getClassLoader();
            Enumeration<URL> resourceUrls = cl.findResources(path);//只扫描当前jar包
            while (resourceUrls.hasMoreElements()) {
                URL url = resourceUrls.nextElement();
                result.add(convertClassLoaderURL(url));
            }
            if (!StringUtils.hasLength(path)) {
                addAllClassLoaderJarRoots(cl, result);
            }
            return result;
        }
    }

    private boolean processBean(final int pluginId, final String beanName, final Class<?> targetType, Object bean, Consumer<ApplicationListener> onStarts, Consumer<ApplicationListener> onCloses) {
        if (AnnotationUtils.isCandidateClass(targetType, EventListener.class)) {

            Map<Method, EventListener> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(targetType, (MethodIntrospector.MetadataLookup<EventListener>) method -> AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class));
            } catch (Throwable ex) {
                // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
                if (log.isDebugEnabled()) {
                    log.debug("Could not resolve methods for bean with name '" + beanName + "'", ex);
                }
            }

            if (CollectionUtils.isEmpty(annotatedMethods)) {
                log.trace("No @EventListener annotations found on bean class: {}", targetType.getName());
            } else {
                for (Method method : annotatedMethods.keySet()) {
                    Method methodToUse = AopUtils.selectInvocableMethod(method, beanFactory.getType(beanName));
                    PluginApplicationListenerMethodAdapter applicationListener = new PluginApplicationListenerMethodAdapter(beanName, targetType, methodToUse, pluginId, bean);
                    if (applicationListener.supportsEventType(ContextRefreshedEvent.class)) {
                        onStarts.accept(applicationListener);
                    } else if (applicationListener.supportsEventType(ContextClosedEvent.class)) {
                        onCloses.accept(applicationListener);
                    } else {
                        eventMulticaster.addApplicationListener(applicationListener);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" + beanName + "': " + annotatedMethods);
                }
                return true;
            }
        }
        return false;
    }

    static Field fieldTargetSourcedBeans, fieldEarlyProxyReferences, fieldAdvisedBeans, fieldProxyTypes;
    static Method getCacheKey;

    static {
        Class autoProxyCreatorClass = AbstractAutoProxyCreator.class;
        try {
            fieldTargetSourcedBeans = autoProxyCreatorClass.getDeclaredField("targetSourcedBeans");
            fieldTargetSourcedBeans.setAccessible(true);

            fieldEarlyProxyReferences = autoProxyCreatorClass.getDeclaredField("earlyProxyReferences");
            fieldEarlyProxyReferences.setAccessible(true);

            fieldAdvisedBeans = autoProxyCreatorClass.getDeclaredField("advisedBeans");
            fieldAdvisedBeans.setAccessible(true);

            fieldProxyTypes = autoProxyCreatorClass.getDeclaredField("proxyTypes");
            fieldProxyTypes.setAccessible(true);

            //Object getCacheKey(Class<?> beanClass, @Nullable String beanName)
            getCacheKey = autoProxyCreatorClass.getDeclaredMethod("getCacheKey", Class.class, String.class);
            getCacheKey.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void uninstallPlugin(PlugInfo plugInfo) throws Exception {
        if (plugInfo == null || plugInfo.getClassLoader() == null) {
            return;
        }

        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
        // 删除接口路由
        if (ArrayUtil.isNotEmpty(plugInfo.getControllerNames())) {
            Method getMappingForMethod = AbstractHandlerMethodMapping.class.getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
            getMappingForMethod.setAccessible(true);

            for (String beanName : plugInfo.getControllerNames()) {
                AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinitionRegistry.getBeanDefinition(beanName);
                Class clazz = definition.getBeanClass();
                if (AnnotatedElementUtils.hasAnnotation(clazz, Controller.class)) {
                    Class<?> userType = ClassUtils.getUserClass(clazz);
                    Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType, (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> {
                        try {
                            return (RequestMappingInfo) getMappingForMethod.invoke(handlerMapping, method, userType);
                        } catch (Throwable ex) {
                            throw new IllegalStateException("Invalid mapping on handler class [" + userType.getName() + "]: " + method, ex);
                        }
                    });
                    if (log.isDebugEnabled()) {
                        log.debug("{} 删除URL路由：{}", plugInfo.getName(), methods.values());
                    }
                    methods.values().forEach(handlerMapping::unregisterMapping);
                }
            }
        }
        Runnable[] uninstallCallbacks = plugInfo.getUninstallCallbacks();
        if (uninstallCallbacks != null) {
            log.info("卸载 {} 回调 {}", plugInfo.getName(), uninstallCallbacks.length);
            for (Runnable runnable : uninstallCallbacks) {
                try {
                    runnable.run();
                } catch (Throwable ex) {
                    log.error(plugInfo.getName() + " 插件卸载回调失败:" + runnable, ex);
                }
            }
            Thread.sleep(1000);
            log.info("{} 卸载回调完毕", plugInfo.getName());
        }

        // 删除 事件监听器
        if (plugInfo.getEventListener() > 0) {
            final int pluginId = plugInfo.getId();
            eventMulticaster.removeApplicationListeners(applicationListener -> (applicationListener instanceof PluginApplicationListenerMethodAdapter pl) && pluginId == pl.pluginId);
        }

        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanFactory.getAutowireCapableBeanFactory();

        // 清理AOP class 缓存
        LinkedList<BiConsumer<Class<?>, String>> proxyTypesClears = new LinkedList<>();
        BiConsumer<Class<?>, String> proxyTypesClear = (beanClass, beanName) -> {
            for (BiConsumer<Class<?>, String> consumer : proxyTypesClears) {
                consumer.accept(beanClass, beanName);
            }
        };
        List<BeanPostProcessor> beanPostProcessors = factory.getBeanPostProcessors();

        for (BeanPostProcessor processor : beanPostProcessors) {
            if (processor instanceof AbstractAutoProxyCreator) {
                Set<String> targetSourcedBeans = (Set<String>) fieldTargetSourcedBeans.get(processor);
                Map<Object, Class<?>> proxyTypesMap = (Map<Object, Class<?>>) fieldProxyTypes.get(processor);
                Map<Object, Object> earlyProxyReferences = (Map<Object, Object>) fieldEarlyProxyReferences.get(processor);
                Map<Object, Boolean> advisedBeans = (Map<Object, Boolean>) fieldAdvisedBeans.get(processor);

                proxyTypesClears.add((beanClass, beanName) -> {
                    targetSourcedBeans.remove(beanName);
                    if (beanClass != null) {
                        Object cacheKey;
                        try {
                            cacheKey = getCacheKey.invoke(processor, beanClass, beanName);
                        } catch (Exception e) {
                            log.warn("getCacheKey Err! beanName=" + beanName, e);
                            cacheKey = beanName;
                        }
                        Class<?> proxyRemoved = proxyTypesMap.remove(cacheKey);
                        log.debug("proxyRemoved({}): {}, beanClass={}", cacheKey, proxyRemoved, beanClass);
                        earlyProxyReferences.remove(cacheKey);
                        advisedBeans.remove(cacheKey);
                    } else {
                        log.debug("proxy not exists: {}", beanName);
                    }
                });
            }
        }
        // 删除 controller BeanDefinition
        String[] ctlNames = plugInfo.getControllerNames();
        if (ArrayUtil.isNotEmpty(ctlNames)) {
            for (String beanName : ctlNames) {
                Class<?> beanClass = removeBean(plugInfo, beanName, factory);
                proxyTypesClear.accept(beanClass, beanName);
            }
        }
        // 删除 普通 BeanDefinition
        String[] beanNames = plugInfo.getBeanNames();
        if (ArrayUtil.isNotEmpty(beanNames)) {
            for (String beanName : beanNames) {
                Class<?> beanClass = removeBean(plugInfo, beanName, factory);
                proxyTypesClear.accept(beanClass, beanName);
            }
        }
        Consumer<Class> mapperClear = clazz -> {
        };
        try {
            Configuration configuration = sqlSessionTemplate.getSqlSessionFactory().getConfiguration();
            if (configuration instanceof MybatisConfiguration mybatisConfiguration) {
                mapperClear = mapperClass -> {
                    log.debug("卸载Mapper: {}", mapperClass);
                    if (mapperClass != null) {
                        mybatisConfiguration.removeMapper(mapperClass);
                    }
                };
            }
        } catch (BeansException ex) {
            log.error("Map获取失败", ex);
        }
        String[] xmlResources = plugInfo.getXmlResources();
        if (ArrayUtil.isNotEmpty(xmlResources)) {
            for (String xmlRes : xmlResources) {
                loadedResources.remove(xmlRes);
            }
        }
        // 删除 @Mapper BeanDefinition
        String[] mapperNames = plugInfo.getMapperNames();
        if (ArrayUtil.isNotEmpty(mapperNames)) {
            for (String mapperName : mapperNames) {
                RootBeanDefinition definition = mergedBeanDefinitions.get(mapperName);
                Class<?> factoryBeanClass = definition != null && definition.hasBeanClass() ? definition.getBeanClass() : null;
                Class<?> mapperClass = removeBean(plugInfo, mapperName, factory);
                mapperClear.accept(mapperClass);
                proxyTypesClear.accept(factoryBeanClass != null ? factoryBeanClass : mapperClass, mapperName);
            }
        }
        // 清理元数据缓存
        factory.clearMetadataCache();
        // 清理 BeanPostProcessorCache 缓存
        Method resetBeanPostProcessorCache = AbstractBeanFactory.class.getDeclaredMethod("resetBeanPostProcessorCache");
        resetBeanPostProcessorCache.setAccessible(true);
        resetBeanPostProcessorCache.invoke(factory);
        // 卸载后，清理 swagger 接口缓存
//        clearSwaggerApiCache(ctlNames);

        plugInfo.getClassLoader().close();// 关闭 URLClassLoader 以释放文件句柄
        ResolvableType.clearCache();
        removeInnerEnvYaml(plugInfo);
        plugInfo.setInstallStatus(PlugInfo.STATUS_NOT_INSTALL);
        plugInfo.setUninstallCallbacks(null);
        plugInfo.setClassLoader(null);
    }

//    private void clearSwaggerApiCache(@Nullable String[] ctlNames) throws NoSuchFieldException, IllegalAccessException {
//        Field fieldGroupedOpenApiResources = MultipleOpenApiResource.class.getDeclaredField("groupedOpenApiResources");
//        fieldGroupedOpenApiResources.setAccessible(true);
//        Map<String, OpenApiResource> openApiResources = (Map<String, OpenApiResource>) fieldGroupedOpenApiResources.get(openApi);
//        //org.springdoc.api.AbstractOpenApiResource.openAPIService
//        Field fieldOpenApiResource = AbstractOpenApiResource.class.getDeclaredField("openAPIService");
//        fieldOpenApiResource.setAccessible(true);
//
//        Field fieldCachedOpenAPI = OpenAPIService.class.getDeclaredField("cachedOpenAPI");
//        fieldCachedOpenAPI.setAccessible(true);
//
//        for (OpenApiResource apiResource : openApiResources.values()) {
//            OpenAPIService openAPIService = (OpenAPIService) fieldOpenApiResource.get(apiResource);
//            Map<String, OpenAPI> cachedOpenAPI = (Map<String, OpenAPI>) fieldCachedOpenAPI.get(openAPIService);
//            cachedOpenAPI.clear();
//            if (ctlNames != null) {
//                Map<String, Object> mappingsMap = openAPIService.getMappingsMap();
//                for (String rest : ctlNames) {
//                    mappingsMap.remove(rest);
//                }
//            }
//        }
//        Field handlerMethods = SpringWebProvider.class.getDeclaredField("handlerMethods");
//        handlerMethods.setAccessible(true);
//        handlerMethods.set(springWebProvider, null);
//    }

    private Class<?> removeBean(PlugInfo plugInfo, String beanName, DefaultListableBeanFactory factory) {
        try {
            Object bean = beanFactory.getBean(beanName);
            AbstractBeanFactory bfc = (AbstractBeanFactory) beanFactory.getAutowireCapableBeanFactory();
            List<BeanPostProcessor> processors = bfc.getBeanPostProcessors();
            for (BeanPostProcessor processor : processors) {
                if (processor instanceof DestructionAwareBeanPostProcessor dap) {
                    dap.postProcessBeforeDestruction(bean, beanName);
                }
            }
        } catch (Exception ex) {
            log.warn("removeBean:" + beanName, ex);
        }
        Class<?> beanClass = null;
        try {
            if (factory.getBeanDefinition(beanName) instanceof ScannedGenericBeanDefinition scd) {
                try {
                    beanClass = plugInfo.getClassLoader().loadClass(scd.getMetadata().getClassName());
                } catch (Exception ex) {
                }
            }
        } catch (NoSuchBeanDefinitionException nsex) {
            log.warn("找不到bean定义:" + beanName);
        }
        RootBeanDefinition definition = mergedBeanDefinitions.remove(beanName);//必须要这一步，否则卸载后重装会报错
        if (definition != null) {
            if (beanClass == null && definition.hasBeanClass()) {
                beanClass = definition.getBeanClass();
            }
            definition.setTargetType((ResolvableType) null);
        }
        try {
            factory.removeBeanDefinition(beanName);
            log.debug("删除定义: {}? {}", beanName, factory.containsBeanDefinition(beanName));
        } catch (Exception ex) {
            log.warn("删除定义失败:{}, err={}", beanName, ex.getMessage());
        }
        return beanClass;
    }

    static class PluginClassLoader extends URLClassLoader {
        final String basePackage;
        final PackageClassLoaderInfo[] depends;

        public PluginClassLoader(String name, URL[] urls, String basePackage, PackageClassLoaderInfo[] depends) {
            super(name, urls, PluginJarService.class.getClassLoader());
            this.basePackage = basePackage;
            this.depends = depends != null ? depends : new PackageClassLoaderInfo[0];
        }

        @Override
        public URL findResource(String name) {
            return super.findResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            return super.findResources(name);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(basePackage) && depends.length > 0) {
                PackageClassLoaderInfo bestMatch = null;
                int maxMatchLength = 0;
                for (PackageClassLoaderInfo info : depends) {
                    String pkg = info.getBasePackage();
                    if (name.startsWith(pkg)) {
                        // 优先选择更长的匹配包路径（如a.b.c优于a.b）
                        if (pkg.length() > maxMatchLength) {
                            maxMatchLength = pkg.length();
                            bestMatch = info;
                        }
                    }
                }
                if (bestMatch != null) {// 先按包名加载最匹配的
                    try {
                        return bestMatch.getClassLoader().loadClass(name);
                    } catch (ClassNotFoundException dpe) {
                    }
                }
            }
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException ex) {
                log.trace("{} 找不到类: {}", getName(), name);
                for (PackageClassLoaderInfo info : depends) {
                    try {
                        return info.getClassLoader().loadClass(name);
                    } catch (ClassNotFoundException dpe) {
                    }
                }
                throw ex;
            }
        }

        @Override
        public String toString() {
            return getName() + ":@" + Integer.toHexString(hashCode()) + " " + Arrays.toString(depends);
        }
    }

    static class PluginApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter {
        final int pluginId;
        final Object bean;

        PluginApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method, int pluginId, Object bean) {
            super(beanName, targetClass, method);
            this.pluginId = pluginId;
            this.bean = bean;
        }

        @Override
        protected Object getTargetBean() {
            return bean;
        }
    }
}
