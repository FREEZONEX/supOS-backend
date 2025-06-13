package com.supos.common.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: KeycloakPolicyInfoDto
 * @date 2025/4/21 14:42
 */
@Data
public class KeycloakPolicyInfoDto implements Serializable {

    private String id;

    private String name;
    private String description;
}
