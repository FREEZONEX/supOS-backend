package com.supos.uns.openapi.dto;


import com.supos.common.Constants;
import com.supos.common.dto.FieldDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@NoArgsConstructor
@Data
public class UpdateFolderDto {

    @Schema(description = "*文件名称，和文件夹显示名一致。支持修改。")
    @Size(max = 63, message = "uns.external.name.length.limit.exceed")
    @Pattern(regexp = Constants.NAME_REG, message = "uns.name.format.error")
    private String name;

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
