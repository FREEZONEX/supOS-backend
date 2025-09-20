package com.supos.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 国际化资源
 * @date 2025/8/28 14:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(I18nResourcePO.TABLE_NAME)
public class I18nResourcePO {

    public static final String TABLE_NAME = "supos_i18n_resource";

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 国际化key
     */
    private String i18nKey;

    /**
     * 国际化值
     */
    private String i18nValue;

    /**
     * 语言编码
     */
    private String languageCode;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 模块版本
     */
    private String moduleVersion;

    /**
     * 修改标识
     */
    private String modifyFlag;

    private Date createAt;

    private Date modifyAt;
}
