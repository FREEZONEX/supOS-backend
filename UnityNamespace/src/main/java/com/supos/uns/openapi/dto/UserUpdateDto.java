package com.supos.uns.openapi.dto;

import com.supos.common.annotation.UserNameConstraint;
import com.supos.common.annotation.UserNameValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: UserUpdateDto
 * @date 2025/4/18 15:18
 */
@Data
public class UserUpdateDto implements Serializable {

    /**
     * 用户名
     */
    @Schema(description = "用户名，唯一", example = "username", minLength = 3, maxLength = 200, pattern = UserNameConstraint.REGEX)
    @NotEmpty(message = "user.username.null")
    private String username;

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
    private List<UserRoleSaveDto> roleList;


    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true", pattern = "^(true|false)$")
    private Boolean enabled;
}
