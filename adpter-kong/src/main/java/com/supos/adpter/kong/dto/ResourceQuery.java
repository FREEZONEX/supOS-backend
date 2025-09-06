package com.supos.adpter.kong.dto;


import lombok.Data;

@Data
public class ResourceQuery {


    /**
     * 菜单分组 1-导航 2-菜单
     */
    private Integer groupType;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 资源类型（1-目录 2-菜单 3-按钮）
     */
    private Integer type;

}
