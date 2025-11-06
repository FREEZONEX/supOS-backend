package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挂载扩展信息
 * @author sunlifang
 * @version 1.0
 * @description: UnsMountPo
 * @date 2025/6/17 15:22
 */
@Data
@NoArgsConstructor
@TableName(UnsMountExtendPo.TABLE_NAME)
public class UnsMountExtendPo {

    public static final String TABLE_NAME = "uns_mount_extend";

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 挂载源数据子类
     * collector    -- 采集器
     * collector_device    -- 采集器设备
     */
    private String sourceSubType;

    /**
     * 挂载批次
     */
    private String mountSeq;

    /**
     * 挂载的目标别名
     */
    private String targetAlias;

    /**
     * 挂载源标识符1
     */
    private String firstSourceAlias;

    /**
     * 挂载源标识符2
     */
    private String secondSourceAlias;

    /**
     * 挂载源名称
     */
    private String sourceName;

    /**
     * 扩展
     */
    private String extend;
}
