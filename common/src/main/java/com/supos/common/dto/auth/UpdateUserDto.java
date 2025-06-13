package com.supos.common.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/28 9:04
 * @description
 */
@Data
public class UpdateUserDto {

    /**
     * 用户ID
     */
    @NotEmpty(message = "userId can't be empty")
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
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

    //---------------------------UserAttributeVo----------------------------------------------
    /**
     * 手机号
     */
    private String phone;

    /**
     * 是否首次登录
     * 1：是
     * 0：否
     */
    private Integer firstTimeLogin;

    /**
     * 是否开启tips
     * 1：是
     * 0：否
     */
    private Integer tipsEnable;

    /**
     * 用户自定义首页
     */
    private String homePage;


    /**
     * 角色列表
     */
    private List<RoleDto> roleList;

    /**
     * 是否操作角色
     */
    private Boolean operateRole;
}
