package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(UnsLabelRefPo.TABLE_NAME)
public class UnsLabelRefPo {

    public static final String TABLE_NAME = "uns_label_ref";

    private Long labelId;

    private Long unsId;

    private Date createAt;

    public UnsLabelRefPo(Long labelId, Long unsId) {
        this.labelId = labelId;
        this.unsId = unsId;
    }
}
