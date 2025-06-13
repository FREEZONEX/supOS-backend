package com.supos.common.dto.auth;

import com.supos.common.annotation.RoleNameValidator;
import com.supos.common.group.Create;
import com.supos.common.group.Update;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: RoleSaveDto
 * @date 2025/4/21 14:59
 */
@Data
public class RoleSaveDto implements Serializable {

    @NotBlank(message = "role.id.null", groups = {Update.class})
    private String id;

    /**
     * 角色名称（唯一）
     */
    @NotBlank(message = "role.name.null", groups = {Create.class})
    @RoleNameValidator
    private String name;

    /**
     * 拒绝资源
     */
    private List<ResourceDto> denyResourceList;

    /**
     * 允许资源
     */
    private List<ResourceDto> allowResourceList;
}
