package com.supos.uns.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/10/13 14:09
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Data
public class UserDetailDto {
    @Hidden
    @JsonIgnore
    private String id;

    @Schema(description = "用户名 英文+数组 长度3-20位")
    private String username;

    @Schema(description = "显示名")
    private String displayName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "角色列表")
    private List<RoleOpenDto> roleList;




}
