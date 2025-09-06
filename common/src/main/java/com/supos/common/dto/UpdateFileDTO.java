package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateFileDTO implements Serializable {

    @Schema(description = "文件别名")
    private String alias;

    @Schema(description = "字段名称，字段值的键值对；日期类型支持：时间戳、UTC日期格式（2021-03-04T05:04:11Z、2021-04-06T16:00:00.000+08:00）")
    private Map<String, Object> data;
}