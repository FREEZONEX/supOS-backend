package com.supos.uns.openapi.dto;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LabelQueryDto extends PaginationDTO {

    @Schema(description = "标签名称查询，支持模糊匹配")
    String key;
}
