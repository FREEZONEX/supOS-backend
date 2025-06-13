package com.supos.common.dto.auth;

import com.supos.common.vo.UserAttributeVo;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/29 15:35
 * @description
 */
@Data
public class KeycloakCreateUserDto {

    private String id;

    /**
     * 用户名
     */
    private String username;

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
     * 用户属性
     */
    private UserAttributeVo attributes;
}
