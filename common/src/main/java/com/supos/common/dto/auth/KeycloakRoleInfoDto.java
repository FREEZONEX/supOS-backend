package com.supos.common.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: keycloak角色信息
 * @date 2025/4/21 11:22
 */
@Data
public class KeycloakRoleInfoDto implements Serializable {

    private String id;

    private String name;

    private String description;
}
