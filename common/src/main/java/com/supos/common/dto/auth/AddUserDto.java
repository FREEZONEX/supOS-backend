package com.supos.common.dto.auth;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import org.checkerframework.checker.regex.qual.Regex;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/28 9:04
 * @description
 */
@Data
public class AddUserDto {

    private String id;

    /**
     * 用户名
     */
    @NotEmpty(message = "username can't be empty")
    @Length(min = 3, message = "用户名长度不可小于3")
    private String username;

    /**
     * 密码
     */
    @NotEmpty(message = "password can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9!@#$%^&*()_+\\-={};':\"\\\\|,.<>/?]{3,10}$",message = "user.password.invalid")
    private String password;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 名字
     */
    private String firstName;

    /**
     * 手机号
     */
    private String phone;


    /**
     * 角色列表
     */
    private List<RoleDto> roleList;
}
