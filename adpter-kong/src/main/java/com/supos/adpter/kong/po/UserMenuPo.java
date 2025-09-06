package com.supos.adpter.kong.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("supos_user_menu")
public class UserMenuPo {

    private Long id;

    private String userId;

    private String menuName;

    private Boolean picked;

    private Date updateTime;

    private Date createTime;
}
