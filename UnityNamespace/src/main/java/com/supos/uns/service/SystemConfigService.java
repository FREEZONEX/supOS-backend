package com.supos.uns.service;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.supos.common.Constants;
import com.supos.common.config.ContainerInfo;
import com.supos.common.config.SystemConfig;
import com.supos.common.enums.ContainerEnvEnum;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.KeycloakUtil;
import com.supos.common.utils.RuntimeUtil;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemConfigService {

    private SystemConfig systemConfig;

    @Autowired
    private KeycloakUtil keycloakUtil;

    @Bean("i18nUtils")
    public I18nUtils i18nUtils(MessageSource messageSource) {
        log.info("初始化 I18nUtils");
        return new I18nUtils(messageSource);
    }

    @Bean("systemConfig")
    @DependsOn("i18nUtils")
    public SystemConfig buildSystemConfig() {
        SystemConfig systemConfig = new SystemConfig();
        log.info(">>>>>>>>>>>>>>系统配置  - 容器配置 - 开始");
        try {
            systemConfig.setContainerMap(SystemConfigService.getSystemContainerMap());
        } catch (IOException ignored) {

        }
        log.info(">>>>>>>>>>>>>>系统配置  - 容器配置 - 结束，配置信息：{}", JsonUtil.toJson(systemConfig));
        this.systemConfig = systemConfig;
        return systemConfig;
    }

    private static final String SYSTEM_SERVICE_PREFIX = "service_";

    private static final String ACTIVE_SERVICES_FILE = "active-services.txt";

    public ResultVO<SystemConfig> getSystemConfig() {
        return ResultVO.successWithData(systemConfig);
    }

    private static Map<String, ContainerInfo> getSystemContainerMap() throws IOException {
        Map<String, ContainerInfo> containerMap = Maps.newHashMapWithExpectedSize(10);

        String activeServices = getActiveServices();
        String composeFile = getComposeFile();
        Yaml yaml = new Yaml();
        JSONObject map = yaml.loadAs(composeFile, JSONObject.class);
        JSONObject services = map.getJSONObject("services");
        for (String serviceName : services.keySet()) {
            JSONObject service = services.getJSONObject(serviceName);
            Map<String, Object> envMap = Maps.newHashMapWithExpectedSize(10);
            Object environment = service.get("environment");
            if (ObjectUtil.isNull(environment)) {
                continue;
            }
            if (environment instanceof List) {
                List<String> envList = service.getObject("environment", new TypeReference<List<String>>() {
                });
                envMap = envList.stream().distinct().filter(envItem -> envItem.contains("=")).map(envItem -> envItem.split("=", 2))
                        .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
            } else if (environment instanceof Map) {
                envMap = service.getJSONObject("environment");
            }
            //只保留service_前缀的配置
            envMap = envMap.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(SYSTEM_SERVICE_PREFIX))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            //只显示activeServices 中包含的容器
            String containerName = service.getString("container_name");//emqx
            //gmqtt
            if (activeServices.contains(containerName)) {
                envMap.put(ContainerEnvEnum.SERVICE_IS_SHOW.getName(), true);
                ContainerInfo containerInfo = new ContainerInfo();
                containerInfo.setName(containerName);
                containerInfo.setVersion(StrUtil.subAfter(service.getString("image"), ":", true));
                containerInfo.setEnvMap(envMap);
                containerInfo.setDescription(I18nUtils.getMessage(StrUtil.toString((envMap.get(ContainerEnvEnum.SERVICE_DESCRIPTION.getName())))));
                containerMap.put(containerName, containerInfo);
            }

            //gmqtt 特殊处理   emqx 和gmqtt只会存在一个
            if ("emqx".equals(serviceName) && activeServices.contains("gmqtt")) {
                envMap.put(ContainerEnvEnum.SERVICE_IS_SHOW.getName(), true);
                ContainerInfo containerInfo = new ContainerInfo();
                containerInfo.setName("gmqtt");
                containerInfo.setVersion(StrUtil.subAfter(service.getString("image"), ":", true));
                containerInfo.setEnvMap(envMap);
                containerInfo.setDescription(I18nUtils.getMessage(StrUtil.toString((envMap.get(ContainerEnvEnum.SERVICE_DESCRIPTION.getName())))));
                containerMap.put("gmqtt", containerInfo);
            }

        }
        return containerMap;
    }


    private static String getActiveServices() throws IOException {
        if (RuntimeUtil.isLocalProfile()) {
            try (InputStream in = SystemConfigService.class.getClassLoader().getResourceAsStream("templates/" + ACTIVE_SERVICES_FILE)) {
                if (in == null) {
                    throw new FileNotFoundException("未在 classpath 中找到文件: " + ACTIVE_SERVICES_FILE);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        return line;
                    }
                }
            }
        } else {
            String dir = String.format("%s%s", FileUtils.getFileRootPath(), Constants.SYSTEM_ROOT);
            List<String> content = FileUtil.readLines(new File(dir, ACTIVE_SERVICES_FILE), StandardCharsets.UTF_8);
            if (CollectionUtils.isNotEmpty(content)) {
                return content.get(0);
            }
        }
        return null;
    }

    private static String getComposeFile() throws IOException {
        if (RuntimeUtil.isLocalProfile()) {
            try (InputStream in = SystemConfigService.class.getClassLoader().getResourceAsStream("templates/" + "docker-compose-8c16g.yml")) {
                if (in == null) {
                    throw new FileNotFoundException("未在 classpath 中找到文件: docker-compose-8c16g.yml");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        } else {
            File fileDir = new File(FileUtils.getFileRootPath(), Constants.SYSTEM_ROOT);
            File composeFile = Arrays.stream(fileDir.listFiles()).filter(file -> file.getName().startsWith("docker-compose-")).findFirst().orElse(null);

            if (!composeFile.exists()) {
                throw new FileNotFoundException("未在文件系统找到文件: docker-compose文件");
            }
            try (InputStream in = new FileInputStream(composeFile)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    void onStartup(ContextRefreshedEvent event) {
        if (!RuntimeUtil.isLocalProfile()) {
            ThreadUtil.execAsync(() -> {
                ThreadUtil.sleep(15000);
                keycloakUtil.setLocale(systemConfig.getLang());
            });
        }
    }
}
