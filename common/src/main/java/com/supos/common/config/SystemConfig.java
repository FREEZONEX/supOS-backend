package com.supos.common.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "sys.os")
public class SystemConfig {

    private String appTitle;
    /**
     * 系统版本
     */
    @Schema(description = "系统版本")
    private String version;

    /**
     * 语言
     * en-US
     * zh-CN
     */
    private String lang;

    /**
     * 是否开启keycloak校验
     */
    private Boolean authEnable = false;

    /**
     * 大语言模型
     */
    private String llmType = "ollama";

    /**
     * mqtt tcp端口
     */
    private Integer mqttTcpPort = 1883;

    /**
     * mqtt WebSocket加密端口
     */
    private Integer mqttWebsocketTslPort = 8084;

    /**
     * 登录页url
     *
     * @法约
     */
    private String loginPath = "/supos-login";

    /**
     * 基础平台类型
     */
    private String platformType = "linux";

    /**
     * 系统入口地址：PROTOCOL+DOMAIN+PORT
     */
    private String entranceUrl;

    /**
     * 单双topic
     */
    private Boolean multipleTopic;

    /**
     * 是否使用别名alias作为 mqtt topic,false 则使用文件路径作为 mqtt topic
     */
    private Boolean useAliasPathAsTopic;

    /**
     * 系统容器
     * key:容器名称
     * value:容器信息
     */
    private Map<String, ContainerInfo> containerMap = new HashMap<>();

    /**
     * 质量码字段名称
     */
    private String qualityName = "status";

    /**
     * 系统时间字段名称
     */
    private String timestampName = "timeStamp";

    /**
     * 是否启用UNS树懒加载模式
     */
    private Boolean lazyTree = false;

    /**
     * 是否启用LDAP用户体系
     */
    private Boolean ldapEnable = false;

    /**
     * 是否开启文件自动归类
     */
    private Boolean enableAutoCategorization = false;
}
