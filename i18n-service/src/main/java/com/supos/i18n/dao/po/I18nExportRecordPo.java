package com.supos.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(I18nExportRecordPo.TABLE_NAME)
public class I18nExportRecordPo {
    public static final String TABLE_NAME = "supos_i18n_export_record";
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String filePath;
    private Boolean confirm;
    private Date createTime;
    private Date updateTime;
}