package com.supos.uns.dao.po;

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
@TableName(AlarmHandlerPo.TABLE_NAME)
public class AlarmHandlerPo {

    public static final String TABLE_NAME = "uns_alarms_handler";

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报警规则 id
     */
    private String unsId;

    private String userId;

    private String username;

    private Date createAt;
}
