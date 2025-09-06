package com.supos.common.dto;

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
public class SaveResourceDto {

    /**
     * 菜单分组 1-导航 2-菜单 3-tab
     */
    @NotNull(message = "菜单分组不可为空")
    private Integer groupType;

    /**
     * 资源编码（国际化KEY）
     */
    @NotBlank(message = "资源编码不可为空")
    private String code;

    /**
     * 父级ID
     * null 为顶级
     * @see com.supos.common.enums.ParentResourceEnum
     */
    private Long parentId;

    /**
     * 资源类型1-目录 2-菜单 3-按钮（操作权限）
     */
    private Integer type;

    /**
     * 地址
     */
    private String url;

    /**
     * 类型 1-内部地址 2-外部链接
     */
    private Integer urlType;

    /**
     * 打开方式：1-当前页面跳转 2-新窗口打开
     */
    private Integer openType;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述国际化Key
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;
}
