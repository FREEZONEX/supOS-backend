package com.supos.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sunlifang
 * @version 1.0
 * @description: I18nVersionPO
 * @date 2025/9/1 9:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(I18nVersionPO.TABLE_NAME)
public class I18nVersionPO {

    public static final String TABLE_NAME = "supos_i18n_version";

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 模块版本
     */
    private String moduleVersion;
}
