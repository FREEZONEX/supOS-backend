package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoVo {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不可为空")
    private String username;
    /**
     * 模块编码
     *
     * @see com.supos.common.enums.SysModuleEnum
     */
    @Schema(description = "模块编码")
    @NotBlank(message = "模块编码不可为空")
    @Size(max = 32, message = "模块编码长度不能超过32")
    private String moduleCode;

    @Schema(description = "模块名称")
    @NotBlank(message = "模块名称不可为空")
    @Size(max = 32, message = "模块名称长度不能超过32")
    private String moduleName;

    /**
     * 事项信息
     */
    @Schema(description = "事项信息")
    @NotBlank(message = "事项信息不可为空")
    @Size(max = 256, message = "事项信息长度不能超过256")
    private String todoMsg;

    /**
     * 业务主键
     */
    @Schema(description = "业务主键")
    @Hidden
    private String businessId;

    /**
     * 链接
     */
    @Schema(description = "链接")
    @Hidden
    private String link;
}
