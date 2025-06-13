package com.supos.adpter.kong.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 菜单
 * @date 2025/4/16 19:22
 */
@Data
public class MenuDto implements Serializable {

    /**
     * 所属服务名称
     */
    private String serviceName;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 显示名称
     */
    private String showName;

    /**
     * 描述
     */
    String description;

    /**
     * 基础url
     */
    String baseUrl;

    /**
     * 打开方式
     */
    Integer openType;

    /**
     * 菜单icon
     */
    MultipartFile icon;

    boolean isMenu;

    private List<String> tags;
}
