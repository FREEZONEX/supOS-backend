package com.supos.uns.openapi.dto;

import com.supos.common.annotation.UserNameConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: UserUpdatePasswordDto
 * @date 2025/4/18 13:44
 */
@Data
public class UserUpdatePasswordDto implements Serializable {

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "username", minLength = 3, maxLength = 200, pattern = UserNameConstraint.REGEX)
    @NotEmpty(message = "user.username.null")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "password")
    @NotEmpty(message = "user.password.null")
    private String password;

}
