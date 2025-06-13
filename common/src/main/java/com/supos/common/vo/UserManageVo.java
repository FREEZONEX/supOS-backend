package com.supos.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserManageVo extends UserAttributeVo{

    private String id;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;
    /**
     * 邮箱验证状态
     */
    @Schema(description = "邮箱验证状态")
    private Boolean emailVerified;

    /**
     * 名字
     */
    @Schema(description = "名字")
    private String firstName;

    /**
     * 用户的首选用户名，通常是登录名或与用户相关的标识符
     */
    @Schema(description = "用户的首选用户名，通常是登录名或与用户相关的标识符")
    private String preferredUsername;

    /**
     * 用户的唯一标识符：用户ID
     */
    @Schema(description = "用户的唯一标识符：用户ID")
    private String sub;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean enabled;

    /**
     * 角色列表
     */
    @Schema(description = "角色列表")
    private List<RoleDto> roleList;

    public UserManageVo(String id, String preferredUsername) {
        this.id = id;
        this.preferredUsername = preferredUsername;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
