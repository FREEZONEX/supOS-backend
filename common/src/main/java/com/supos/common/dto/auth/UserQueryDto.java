package com.supos.common.dto.auth;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 用户查询参数
 * @date 2025/4/16 15:56
 */
@Data
public class UserQueryDto extends PaginationDTO {

    @Schema(description = "账号")
    private String preferredUsername;

    @Schema(description = "名称")
    private String firstName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
