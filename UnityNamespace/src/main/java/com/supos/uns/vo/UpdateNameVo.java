package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateNameVo {

    @NotNull
    @Schema(description = "ID")
    Long id;

    @Schema(description = "名称")
    @NotBlank
    String name;

}
