package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: PlugInfoYml
 * @date 2025/5/20 18:46
 */
@Data
public class PlugInfoYml implements Serializable {

    @Schema(description = "插件名称,唯一")
    private String name;

    @Schema(description = "插件显示名称")
    private String showName;

    @Schema(description = "插件描述")
    private String description;

    @Schema(description = "插件版本")
    private String version;

    @Schema(description = "插件作者")
    private String vendorName;

    @Schema(description = "是否自动安装")
    private Boolean autoInstall;

    @Schema(description = "插件路由")
    private PlugRoute route;

    @Schema(description = "插件依赖")
    private List<PlugDependency> dependencies;

    @Data
    public static class PlugRoute implements Serializable {
        private String name;
        private String description;
        private Integer sort;
        private String parentName;
        private String path;
        private String icon;
        private String moduleName;
        private String homeParentName;
        private String homeIconUrl;
    }

    @Data
    @NoArgsConstructor
    public static class PlugDependency implements Serializable {
        private String name;

        public PlugDependency(String name) {
            this.name = name;
        }
    }
}
