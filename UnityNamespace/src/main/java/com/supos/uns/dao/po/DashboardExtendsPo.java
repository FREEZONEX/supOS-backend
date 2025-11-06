package com.supos.uns.dao.po;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/29 10:08
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("uns_dashboard")
public class DashboardExtendsPo extends DashboardPo {

    private Integer mark;
}
