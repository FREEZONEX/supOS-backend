package com.supos.uns.openapi.vo;

import com.supos.common.dto.FieldDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenTemplateVo {

    @Schema(description = "模板ID")
    Long id;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称")
    String name;

    /**
     * 别名
     */
    @Schema(description = "别名")
    String alias;

    /**
     * 字段定义
     */
    @Schema(description = "字段定义")
    FieldDefine[] definition;
    /**
     * 创建时间--单位：毫秒
     */
    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    String description;
}
