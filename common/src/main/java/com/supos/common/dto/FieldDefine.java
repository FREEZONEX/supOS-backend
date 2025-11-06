package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDefine implements Cloneable {
    @NotEmpty(message = "uns.invalid.emptyFieldName")
    @Schema(description = "字段名")
    private String name;//字段名
    @NotNull
    @Schema(description = "字段类型：int, long, float, string, boolean")
    private FieldType type;// 字段类型：int, long, float, string, boolean
    @Schema(description = "是否唯一约束，新建模板时，此参数不生效")
    private Boolean unique;// 是否唯一约束
    @Schema(description = "对应的协议字段key，新建模板时，此参数不生效")
    private String index; // 对应的协议字段key
    @Schema(description = "显式名")
    private String displayName;//显式名
    @Schema(description = "备注")
    private String remark;//备注
    @Schema(description = "最大长度(string字段类型生效)")
    private Integer maxLen;// 最大长度
    @Hidden
    private String tbValueName;

    @Schema(description = "位号单位")
    private String unit;

    @Schema(description = "原始上限")
    private Double upperLimit;

    @Schema(description = "原始下限")
    private Double lowerLimit;

    @Schema(description = "小数精度位数")
    private Integer decimal;

    public boolean isUnique() {
        return unique != null && unique;
    }

    public FieldDefine() {
    }

    public FieldDefine(String name, FieldType type) {
        this.setName(name);
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, boolean unique) {
        this.setName(name);
        this.unique = unique;
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, String index) {
        this.setName(name);
        this.setIndex(index);
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, Boolean unique, String index, String displayName, String remark) {
        this.setName(name);
        this.setIndex(index);
        this.type = type;
        this.unique = unique;
        this.displayName = displayName;
        this.remark = remark;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public void setIndex(String index) {
        this.index = index != null ? index.trim() : null;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    //    @JsonIgnore
    @Schema(description = "是否系统参数，新建模板时，此参数不生效")
    public boolean isSystemField() {
        return name.startsWith(Constants.SYSTEM_FIELD_PREV) || Constants.systemFields.contains(name);
    }

    public FieldDefine clone() {
        try {
            return (FieldDefine) super.clone();
        } catch (CloneNotSupportedException e) {
            FieldDefine f = new FieldDefine();
            f.setName(name);
            f.setType(type);
            f.setUnique(unique);
            return f;
        }
    }
}
