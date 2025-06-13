package com.supos.common.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: KeycloakResourceInfoDto
 * @date 2025/4/21 14:42
 */
@Data
public class KeycloakResourceInfoDto implements Serializable {

    @JsonProperty("_id")
    private String id;

    private String name;

    private String typeName;

    private String displayName;
}
