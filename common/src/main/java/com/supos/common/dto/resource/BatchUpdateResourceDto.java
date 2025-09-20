package com.supos.common.dto.resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Valid
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateResourceDto {

    private Long id;

    /**
     * 资源编码
     */
    @NotBlank(message = "资源编码不可为空")
    private String code;

    /**
     * 资源名称
     */
    @NotBlank(message = "资源名称不可为空")
    private String name;

    /**
     * 名称国际化code
     */
    private String nameCode;

    private String descriptionCode;

    /**
     * 父级ID
     * null 为顶级
     * @see com.supos.common.enums.ParentResourceEnum
     */
    private Long parentId;

    /**
     * 资源类型1-目录 2-菜单 3-按钮（操作权限）4-Tab
     */
    @NotNull(message = "资源类型不可为空")
    private Integer type;

    /**
     * 来源：平台-platform  插件编码或APP编码
     */
    private String source;

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
     * 描述
     */
    private String description;

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
}
