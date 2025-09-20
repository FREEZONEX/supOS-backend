package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SimpleUnsInfo;
import com.supos.uns.config.FieldsTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(UnsHistoryDeleteJobPo.TABLE_NAME)
public class UnsHistoryDeleteJobPo implements SimpleUnsInfo {

    public static final String TABLE_NAME = "uns_history_delete_job";

    @TableId
    private Long id;

    private String alias;

    private String name;

    @TableField("data_name")
    private String tableName;

    private String path;

    @TableField("path_type")
    private Integer pathType;

    @TableField("data_type")
    private Integer dataType;

    @TableField(typeHandler = FieldsTypeHandler.class)
    FieldDefine[] fields;

    private int status;

    private Date createAt;
}
