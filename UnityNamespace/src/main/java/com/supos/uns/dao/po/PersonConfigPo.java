package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/3 13:37
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(PersonConfigPo.TABLE_NAME)
public class PersonConfigPo {

    public static final String TABLE_NAME = "uns_person_config";

    @TableId(value = "_id", type = IdType.INPUT)
    private Long id;

    private String userId;
    /**
     * default : zh_CN
     */
    private String mainLanguage;
    private Date createAt;
    private Date updateAt;
}
                 