package com.supos.i18n.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nResourceModuleVO
 * @date 2025/9/4 16:41
 */
@Data
public class I18nResourceModuleVO implements Serializable {

    @Schema(description = "模块ID")
    private Long id;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "模块类型，1-内置；2-自定义")
    private Integer moduleType;
}
