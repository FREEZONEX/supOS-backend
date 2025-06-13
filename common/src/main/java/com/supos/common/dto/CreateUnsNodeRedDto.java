package com.supos.common.dto;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.DataTypeValidator;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUnsNodeRedDto {

    @NotEmpty(message = "uns.name.empty")
    String name;// 文件名

    @Hidden
    String path;// 文件路径

    @AliasValidator
    @Schema(description = "别名")
    String alias;

    String fieldType;

    String fieldName;

    String tag;


    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
