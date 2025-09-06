package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Hidden;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlugInfoYml implements Serializable {

    @Schema(description = "插件名称,唯一")
    private String name;

    @Schema(description = "插件显示名称")
    private String showName;

    @Hidden
    @JsonIgnore
    private String showNameI18nCode;

    @Schema(description = "插件描述")
    private String description;

    @Hidden
    @JsonIgnore
    private String descriptionI18nCode;

    @Schema(description = "插件版本")
    private String version;

    @Schema(description = "插件作者")
    private String vendorName;

    @Schema(description = "是否自动安装")
    private Boolean autoInstall;

    @Schema(description = "是否可卸载")
    private Boolean removable = true;

    @Schema(description = "插件路由")
    private PlugRoute route;

    @Schema(description = "插件资源")
    private List<PlugResource> resources;

    @Schema(description = "插件依赖")
    private List<PlugDependency> dependencies;

    @Data
    public static class PlugRoute implements Serializable {
        private String name;
        private String path;
    }

    @Data
    @NoArgsConstructor
    public static class PlugDependency implements Serializable {
        private String name;

        public PlugDependency(String name) {
            this.name = name;
        }
    }

    @Data
    @NoArgsConstructor
    public static class PlugResource implements Serializable {
        private String code;
        private Integer groupType;
        private Integer type;
        private String url;
        private Integer urlType;
        private Integer openType;
        private String icon;
        private String description;
        private Integer sort;
        private Long parentId;

        private List<PlugResource> children;
        private List<PlugOperation> operations;
    }

    @Data
    @NoArgsConstructor
    public static class PlugOperation implements Serializable {
        private String code;

        public PlugOperation(String code) {
            this.code = code;
        }
    }

//    public String getShowName() {
//        return this.showName != null ? I18nUtils.getMessage4Plugin(this.name, this.showName) : this.showName;
//    }
//
//    public String getDescription() {
//        return this.description != null ? I18nUtils.getMessage4Plugin(this.name, this.description) : this.description;
//    }

}
