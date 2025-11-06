package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挂载信息
 * @author sunlifang
 * @version 1.0
 * @description: UnsMountPo
 * @date 2025/6/17 15:22
 */
@Data
@NoArgsConstructor
@TableName(UnsMountPo.TABLE_NAME)
public class UnsMountPo {

    public static final String TABLE_NAME = "uns_mount";

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 挂载批次
     */
    private String mountSeq;

    /**
     * 挂载的目标类型
     * folder    -- 文件夹
     * file    -- 文件
     */
    private String targetType;

    /**
     * 挂载的目标别名
     */
    private String targetAlias;

    /**
     * 挂载模式
     * collector    -- 采集器挂载
     */
    private String mountModel;

    /**
     * 挂载源标识符
     */
    private String sourceAlias;

    private Integer sourceType;

    /**
     * 初始挂载状态
     */
    private Integer mountStatus;

    /**
     * 挂载的文件类型
     */
    private Integer dataType;

    private String version;

    private String nextVersion;

    /**
     * 状态
     */
    private String status;

    /**
     * 开关标识：
     */
    private Integer withFlags;
}
