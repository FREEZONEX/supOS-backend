package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(AlarmPo.TABLE_NAME)
public class AlarmPo {

    public static final String TABLE_NAME = "uns_alarms_data";

    @TableId(value = "_id", type = IdType.INPUT)
    private Long id;

    /**
     * 报警规则 uns Id
     */
    private Long uns;

    /**
     * uns 路径
     */
    private String unsPath;

    /**
     * 当前值
     */
    private Double currentValue;

    /**
     * 限值
     */
    private Double limitValue;

    /**
     * 是否为报警：true报警，false报警消除
     */
    private Boolean isAlarm;

    /**
     * 是否已读
     */
    private Boolean readStatus;

    @TableField("_ct")
    private Date createAt;

    /**
     * 未读数
     */
    @TableField(exist = false)
    private Long noReadCount;

    public AlarmPo(Long id, Boolean readStatus) {
        this.id = id;
        this.readStatus = readStatus;
    }
}
