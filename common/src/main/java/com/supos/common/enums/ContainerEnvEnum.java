package com.supos.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务容器内置环境变量
 */
@Getter
@AllArgsConstructor
public enum ContainerEnvEnum {

    SERVICE_IS_SHOW("service_is_show","服务是否显示"),
    SERVICE_LOGO("service_logo","LOGO"),
    SERVICE_DESCRIPTION("service_description","服务描述"),
    SERVICE_REDIRECT_URL("service_redirect_url","高阶使用跳转路由"),//控制是否高阶使用显示
    SERVICE_ACCOUNT("service_account","帐号"),
    SERVICE_PASSWORD("service_password","密码"),
    ;


    public final String name;

    public final String description;
}
