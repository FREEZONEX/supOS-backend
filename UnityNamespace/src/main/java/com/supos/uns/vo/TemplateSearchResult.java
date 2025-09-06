package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSearchResult {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "模板名称")
    String name;

    /**
     * 模型描述
     */
    @Schema(description = "模型描述")
    String description;

    /**
     * 别名
     */
    @Schema(description = "别名")
    String alias;
}
