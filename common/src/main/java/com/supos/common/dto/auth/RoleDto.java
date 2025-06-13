package com.supos.common.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/26 10:17
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDto{


    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private String roleId;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String roleDescription;

    /**
     * 是否为Client角色
     */
    @Schema(description = "是否为Client角色")
    private Boolean clientRole;
}
