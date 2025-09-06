package com.supos.uns.openapi.dto;

import com.supos.common.annotation.AliasValidator;
import com.supos.common.dto.FieldDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@NoArgsConstructor
@Data
public class CreateFolderDto {

    @Schema(description = "*文件名称，和文件夹显示名一致。支持修改。")
    @NotEmpty(message = "uns.name.empty")
    @Size(max = 63, message = "uns.external.name.length.limit.exceed")
    private String name;

    @Schema(description = "文件别名。最大长度63字符，允许字符包括英文、数字、下划线。为空则系统自动生成")
    @AliasValidator
    private String alias;

    @Schema(description = "*文件显示名。支持修改和重名。最大长度128字符")
    @Size(max = 128, message = "uns.display.name.length.limit.exceed")
    private String displayName;

    @Schema(description = "所属父文件夹别名")
    private String parentAlias;

    @Schema(description = "描述")
    @Size(max = 255, message = "uns.description.length.limit.exceed")
    private String description;

    @Schema(description = "字段定义")
    private FieldDefine[] definition;

    @Schema(description = "扩展属性 最大支持3个")
    @Size(max = 3, message = "uns.extend.size.limit.exceed")
    LinkedHashMap<String, Object> extendProperties;

    @Schema(description = "关联的模板别名")
    private String templateAlias;
}
