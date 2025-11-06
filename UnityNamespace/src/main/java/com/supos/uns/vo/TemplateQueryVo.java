package com.supos.uns.vo;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateQueryVo extends PaginationDTO {


    @Schema(description = "关键字查询，模版名称模糊匹配")
    String key;

    /**
     * 是否订阅
     */
    @Schema(description = "是否订阅")
    Boolean subscribeEnable;
}
