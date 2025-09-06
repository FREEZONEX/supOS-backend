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
     * 菜单分组 1-导航 2-菜单
     */
    private Integer groupType;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 资源类型（1-目录 2-菜单 3-按钮）
     */
    private Integer type;

    /**
     * 资源编码（国际化KEY）
     */
    private String code;

    /**
     * 显示名称
     */
    private String showName;

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
     * 描述内容
     */
    private String showDescription;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

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
