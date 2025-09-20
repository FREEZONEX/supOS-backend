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
    @NotBlank(message = "user.username.null")
    private String username;
    /**
     * 模块编码
     *
     * @see com.supos.common.enums.SysModuleEnum
     */
    @Schema(description = "模块编码")
    @NotBlank(message = "todo.moduleCode.null")
    @Size(max = 32, message = "todo.moduleCode.length.error")
    private String moduleCode;

    @Schema(description = "模块名称")
    @NotBlank(message = "todo.moduleName.null")
    @Size(max = 32, message = "todo.moduleName.length.error")
    private String moduleName;

    /**
     * 事项信息
     */
    @Schema(description = "事项信息")
    @NotBlank(message = "todo.todoMsg.null")
    @Size(max = 256, message = "todo.todoMsg.length.error")
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
