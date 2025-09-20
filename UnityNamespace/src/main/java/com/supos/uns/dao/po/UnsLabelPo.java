package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(UnsLabelPo.TABLE_NAME)
public class UnsLabelPo {

    public static final String TABLE_NAME = "uns_label";

    @TableId(type = IdType.AUTO)
    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String labelName;

    private Date createAt;

    public UnsLabelPo(String labelName) {
        this.labelName = labelName;
    }

    public UnsLabelPo(Long id, String labelName) {
        this.id = id;
        this.labelName = labelName;
    }
}
