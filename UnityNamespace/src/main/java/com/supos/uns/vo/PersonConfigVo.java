package com.supos.uns.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author chenlizhong@supos.com
 * @description:
 * @date 2025/7/3 13:47
 */
@Data
public class PersonConfigVo {

    private String userId;
    private String mainLanguage;
    private Date createAt;
    private Date updateAt;

}
