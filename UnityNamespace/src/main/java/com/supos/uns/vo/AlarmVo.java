package com.supos.uns.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmVo {

    private String id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 报警规则 id
     */
    private Long uns;

    /**
     * uns 路径
     */
    private String unsPath;


    /**
     * 关联的实例信息
     * [{"field":"tag1","topic":"/位号写值测试/ins2"}]
     */
    private String refers;

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
    private boolean readStatus;

    private Date createAt;

    private String expression;

    /**
     * 条件
     */
    private String condition;

    /**
     * 是否可处理
     */
    boolean canHandler;
}
