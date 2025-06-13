package com.supos.uns.vo;

import com.supos.common.annotation.AliasValidator;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.PathUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateTemplateVo {

    @Hidden
    int batch;
    @Hidden
    int index;
    @AliasValidator
    @Schema(description = "别名，唯一，最长63，可用字符：a-zA-Z0-9_")
    String alias;

    public String getAlias() {
        if (alias == null && name != null) {
            alias = PathUtil.generateAlias(name,1);
        }
        return alias;
    }

    /**
     * 模板名称
     */
    @NotEmpty(message = "模板名称不可为空")
    @Size(max = 63 , message = "名称最多63个字符")
    @Schema(description = "模板名称")
    String name;
    /**
     * 字段定义
     */
    @NotEmpty(message = "字段定义不可为空")
    @Schema(description = "字段定义")
    FieldDefine[] fields;
    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    @Size(max = 255 , message = "模板描述最多255个字符")
    String description;

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public CreateTemplateVo(String name, @NotNull(message = "The fields cannot be null") FieldDefine[] fields) {
        this.name = name;
        this.fields = fields;
    }
}
