package com.supos.uns.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: UserRoleSaveDto
 * @date 2025/4/18 14:32
 */
@Data
public class UserRoleSaveDto implements Serializable {

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String roleName;
}
