package com.supos.common.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
public class BatchRemoveUnsDto {

    @Schema(description = "别名集合")
    List<String> aliasList;

    @Hidden
    Boolean withFlow = false;

    @Hidden
    Boolean withDashboard = false;

    @Hidden
    Boolean removeRefer = false;

    @Hidden
    Boolean checkMount;

    @Hidden
    Boolean onlyRemoveChild;
}
