package com.supos.adpter.kong.dto;


import lombok.Data;

@Data
public class ResourceQuery {

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 资源类型（1-目录 2-菜单 3-按钮 4-Tab 5-子菜单）
     */
    private Integer type;

}
