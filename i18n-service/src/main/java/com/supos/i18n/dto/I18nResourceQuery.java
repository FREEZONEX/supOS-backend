package com.supos.i18n.dto;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化资源搜索条件
 * @date 2025/9/1 14:36
 */
@Data
public class I18nResourceQuery extends PaginationDTO {

    /**
     * 模块编码
     */
    @Schema(description = "模块编码")
    private String moduleCode;

    /**
     * 搜索关键字
     */
    @Schema(description = "搜索关键字，模糊国际化资源得键或值")
    private String keyword;
}
