package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum ParentResourceEnum {

    NAV_APPSPACE(4L,"menu.tag.appspace","应用集", 1),
    NAV_DEVTOOLS(50L,"menu.tag.devtools", "工具集",1),
    NAV_SYSTEM(60L,"menu.tag.system", "系统管理",1),

    MENU_UNS(1L,"menu.tag.uns.svg","数据管理", 2),
    MENU_DEVTOOLS(2L,"menu.tag.devtools.svg", "工具集",2),
    MENU_SYSTEM(3L,"menu.tag.system.svg", "系统管理",2),
    MENU_APPSPACE(90L,"menu.tag.appspace.svg", "应用集",2),
    ;

    private Long id;

    /**
     * 资源编码
     */
    private String code;

    /**
     * 注释
     */
    private String comment;

    /**
     * 菜单分组 1-导航 2-菜单 3-tab
     */
    private Integer groupType;




    public static ParentResourceEnum parse(String code) {
        for (ParentResourceEnum each : values()) {
            if (code.equals(each.code)) {
                return each;
            }
        }
        return null;
    }
}

