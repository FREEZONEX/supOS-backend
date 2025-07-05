package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 9:23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(GlobalExportRecordPo.TABLE_NAME)
public class GlobalExportRecordPo {
    public static final String TABLE_NAME = "global_export_record";
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String filePath;
    private Boolean confirm;
    private Date createTime;
    private Date updateTime;
}
