package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 网关分页查询
 * @date 2025/4/7 18:43
 */
@Data
public class CollectorGatewayQueryDto extends PaginationDTO {

    @Schema(description = "搜索关键字，支持显示名称和描述的模糊搜索")
    private String keyword;
}
