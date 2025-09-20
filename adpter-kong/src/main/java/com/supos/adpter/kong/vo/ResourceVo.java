package com.supos.adpter.kong.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceVo {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 资源类型（1-目录 2-菜单 3-按钮 4-Tab 5-子菜单）
     */
    private Integer type;

    /**
     * 资源编码
     */
    private String code;

    /**
     * 名称国际化code
     */
    private String nameCode;

    /**
     * 显示名称
     */
    private String showName;


    /**
     * 路由来源 1-手工 2-Kong
     */
    private Integer routeSource;

    /**
     * 地址
     */
    private String url;

    /**
     * 类型 1-内部地址 2-外部链接
     */
    private Integer urlType;

    /**
     * 打开方式：0-当前页面跳转 1-新窗口打开
     */
    private Integer openType;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述国际化Key
     */
    private String descriptionCode;

    /**
     * 描述内容
     */
    private String showDescription;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否可编辑
     */
    private Boolean editEnable;

    /**
     * 是否显示在首页
     */
    private Boolean homeEnable;

    /**
     * 是否固定
     */
    private Boolean fixed;

    /**
     * 启用状态
     */
    private Boolean enable;

    /**
     * 更新时间
     */
    private Date updateAt;

    /**
     * 创建时间
     */
    private Date createAt;
}
