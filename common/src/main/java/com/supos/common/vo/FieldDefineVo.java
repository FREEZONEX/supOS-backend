package com.supos.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.Constants;
import com.supos.common.annotation.FieldTypeValidator;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDefineVo {
    @NotEmpty
    @Schema(description = "字段名")
    private String name;//字段名
    @NotEmpty
    @FieldTypeValidator
    @Schema(description = "字段类型：int, long, float, string, boolean")
    private String type;// 字段类型：int, long, float, string, boolean
    @Schema(description = "是否唯一约束")
    private Boolean unique;// 是否唯一约束
    @Schema(description = "modeBus 协议时字段对应的数组下标")
    private String index; // modeBus 协议时字段对应的数组下标
    @Schema(description = "是否系统预置字段")
    private Boolean system; // 是否系统预置字段
    @Schema(description = "显式名")
    private String displayName;//显式名
    @Schema(description = "备注")
    private String remark;//备注
    @Schema(description = "最大长度")
    private String maxLen;//最大长度

    public FieldDefineVo() {
    }

    public FieldDefineVo(FieldDefine bo) {
        this.name = bo.getName();
        this.type = bo.getType().name;
        this.unique = bo.getUnique();
        this.index = bo.getIndex();
    }

    public FieldDefineVo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public FieldDefineVo(String name, String type, boolean unique) {
        this.name = name;
        this.unique = unique;
        this.type = type;
    }

    public FieldDefineVo(String name, String type, String index) {
        this.name = name;
        this.index = index;
        this.type = type;
    }

    public Boolean getSystem() {
        if (system == null && name != null && name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
            return Boolean.TRUE;
        }
        return system;
    }

    public boolean isUnique() {
        return unique != null && unique;
    }

    public FieldDefine convert() {
        return vo2bo(this);
    }

    public static FieldDefine[] convert(FieldDefineVo[] vfs) {
        if (vfs == null || vfs.length == 0) {
            return null;
        }
        FieldDefine[] fs = new FieldDefine[vfs.length];
        for (int i = 0; i < vfs.length; i++) {
            FieldDefineVo vo = vfs[i];
            FieldDefine define = vo2bo(vo);
            fs[i] = define;
        }
        return fs;
    }

    private static final FieldDefine vo2bo(FieldDefineVo vo) {
        FieldDefine define = new FieldDefine(vo.getName(), FieldType.getByName(vo.getType()), vo.getIndex());
        define.setUnique(vo.getUnique());
        define.setDisplayName(vo.getDisplayName());
        define.setRemark(vo.getRemark());
        return define;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldDefineVo that = (FieldDefineVo) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
