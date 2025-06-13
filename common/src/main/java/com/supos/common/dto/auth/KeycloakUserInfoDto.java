package com.supos.common.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: KeycloakUserInfoDto
 * @date 2025/4/18 15:40
 */
@Data
public class KeycloakUserInfoDto implements Serializable {
    private String id;
    private String username;
}
