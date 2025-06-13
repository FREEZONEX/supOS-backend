package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum RoleEnum {

    /**
     * 1, 待激活
     */
    SUPER_ADMIN("7ca9f922-0d35-44cf-8747-8dcfd5e66f8e","super-admin", "user.role.supAdmin","超级管理员"),
    ADMIN("a22ce15f-7bef-4e2e-9909-78f51b91c799", "admin","user.role.admin","管理员"),
    NORMAL_USER("71dd6dc2-6b12-4273-9ec0-b44b86e5b500", "normal-user","user.role.normalUser","普通用户"),
    ;

    /**
     * 角色ID
     */
    private String id;

    private String name;

    private String i18nCode;

    /**
     * 注释
     */
    private String comment;

    public static Set<String> IGNORE_ROLE_ID = new HashSet<>();
    public static Set<String> IGNORE_ROLE_NAME = new HashSet<>();

    static {
        // 角色列表展示需要忽略的角色ID
        IGNORE_ROLE_ID.add("625d093d-1333-47d4-92fa-dded93a4f90a");//shimu
        IGNORE_ROLE_ID.add("831f62ab-d306-4b11-882e-b23c37ee8c7e");//uma_protection
        IGNORE_ROLE_ID.add("2152d19d-e4f9-488d-8509-e49cf239596a");//supos-default
        IGNORE_ROLE_ID.add("a22ce15f-7bef-4e2e-9909-78f51b91c799");//admin
        IGNORE_ROLE_ID.add("71dd6dc2-6b12-4273-9ec0-b44b86e5b500");//normal-user

        IGNORE_ROLE_NAME.add("supos-default");
    }

    public static RoleEnum parse(String id) {
        for (RoleEnum each : values()) {
            if (id.equals(each.id)) {
                return each;
            }
        }
        return null;
    }

    public static RoleEnum parseName(String name) {
        for (RoleEnum each : values()) {
            if (name.equals(each.name)) {
                return each;
            }
        }
        return null;
    }
}

