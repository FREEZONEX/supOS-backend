package com.supos.uns.service;


import cn.hutool.core.collection.ListUtil;
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
import com.supos.common.dto.SysModuleDto;
import com.supos.common.enums.ContainerEnvEnum;
import com.supos.common.enums.SysModuleEnum;
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
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        systemConfig.setContainerMap(SystemConfigService.getSystemContainerMap());
        log.info(">>>>>>>>>>>>>>系统配置  - 容器配置 - 结束，配置信息：{}", JsonUtil.toJson(systemConfig));
        this.systemConfig = systemConfig;
        return systemConfig;
    }

    private static final String SYSTEM_SERVICE_PREFIX = "service_";

    private static final String ACTIVE_SERVICES_FILE = "active-services.txt";

    public ResultVO<SystemConfig> getSystemConfig() {
        return ResultVO.successWithData(systemConfig);
    }

    private static Map<String, ContainerInfo> getSystemContainerMap() {
        Map<String, ContainerInfo> containerMap = Maps.newHashMapWithExpectedSize(10);
        File fileDir = null;
        if (RuntimeUtil.isLocalProfile()){
            ClassPathResource resource = new ClassPathResource("templates");
            try {
                fileDir = resource.getFile();
            } catch (IOException e) {
            }
        } else {
            String dir = String.format("%s%s", FileUtils.getFileRootPath(), Constants.SYSTEM_ROOT);
            fileDir = new File(dir);
        }

        if (ObjectUtil.isNull(fileDir.listFiles())) {
            log.warn("docker-compose文件未找到，获取容器信息失败");
            return containerMap;
        }

        File activeServicesFile = new File(fileDir, ACTIVE_SERVICES_FILE);
        if (!activeServicesFile.exists()) {
            log.warn("active-services.txt文件未找到，获取容器信息失败");
            return containerMap;
        }

        List<String> lines = FileUtil.readUtf8Lines(activeServicesFile);
        if (CollectionUtils.isEmpty(lines)) {
            log.warn("active-services.txt文件数据为空，获取容器信息失败");
            return containerMap;
        }

        String activeServices = lines.get(0);

        File composeFile = Arrays.stream(fileDir.listFiles()).filter(file -> file.getName().startsWith("docker-compose-")).findFirst().orElse(null);
        if (null == composeFile || !composeFile.exists()) {
            log.warn("docker-compose文件未找到，获取容器信息失败");
            return containerMap;
        }
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(composeFile);
        } catch (FileNotFoundException e) {
            log.warn("docker-compose文件未找到，获取容器信息失败");
            return containerMap;
        }
        Yaml yaml = new Yaml();
        JSONObject map = yaml.loadAs(fs, JSONObject.class);
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

    @EventListener(classes = ContextRefreshedEvent.class)
    void onStartup(ContextRefreshedEvent event) {
        if (!RuntimeUtil.isLocalProfile()){
            ThreadUtil.execAsync(() -> {
                ThreadUtil.sleep(15000);
                keycloakUtil.setLocale(systemConfig.getLang());
            });
        }
    }
}
