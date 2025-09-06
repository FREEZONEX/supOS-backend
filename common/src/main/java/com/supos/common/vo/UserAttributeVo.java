package com.supos.common.vo;

import lombok.Data;

@Data
public class UserAttributeVo {


    /**
     * 是否首次登录
     * 1：是
     * 0：否
     */
    private Integer firstTimeLogin = 1;

    /**
     * 是否开启tips
     * 1：是
     * 0：否
     */
    private Integer tipsEnable = 1;

    /**
     * 用户自定义首页
     */
    private String homePage = "/home";

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户来源
     */
    private String source;
}
