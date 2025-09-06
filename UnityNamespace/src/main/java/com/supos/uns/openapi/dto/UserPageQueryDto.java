package com.supos.uns.openapi.dto;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserPageQueryDto extends PaginationDTO {

    @Schema(description = "用户名，精准查询")
    private String username;

    @Schema(description = "显示名称 支持模糊查询")
    private String displayName;

    @Schema(description = "邮箱  支持模糊查询")
    private String email;

    @Schema(description = "手机号 支持模糊查询")
    private String phone;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
