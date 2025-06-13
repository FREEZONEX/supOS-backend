package com.supos.uns.openapi.dto;

import com.supos.common.annotation.UserNameConstraint;
import com.supos.common.annotation.UserNameValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: UserSaveDto
 * @date 2025/4/18 13:44
 */
@Data
public class UserCreateDto implements Serializable {

    /**
     * 用户名
     */
    @Schema(description = "用户名，唯一", example = "username", minLength = 3, maxLength = 200, pattern = UserNameConstraint.REGEX)
    @NotBlank(message = "user.username.null")
    @UserNameValidator(message = "user.username.invalid")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "password")
    @NotBlank(message = "user.password.null")
    private String password;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱，唯一", example = "xxx@supos.com")
    private String email;

    /**
     * 名字
     */
    @Schema(description = "名字", example = "firstName", minLength = 3, maxLength = 200, pattern = UserNameConstraint.REGEX)
    @UserNameValidator(message = "user.firstName.invalid")
    private String firstName;

    /**
     * 角色列表
     */
    @Schema(description = "角色")
    private List<UserRoleSaveDto> roleList;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true", pattern = "^(true|false)$")
    private Boolean enabled;
}
