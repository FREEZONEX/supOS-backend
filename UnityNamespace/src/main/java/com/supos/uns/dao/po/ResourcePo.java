package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对应表：supos_menu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("supos_resource")
public class ResourcePo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父级ID
     */
//    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;

    /**
     * 资源类型（1-目录 2-菜单 3-按钮 4-Tab 5-子菜单）
     */
    private Integer type;

    /**
     * 来源：平台-platform  插件编码或APP编码
     */
    private String source;

    /**
     * 资源编码（国际化KEY）
     */
    private String code;

    /**
     * 名称国际化code
     */
    private String nameCode;

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
//    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    /**
     * 描述国际化Key
     */
//    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String descriptionCode;

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
