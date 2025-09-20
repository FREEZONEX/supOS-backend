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
 * @description: I18nLanguagePO
 * @date 2025/8/28 14:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(I18nLanguagePO.TABLE_NAME)
public class I18nLanguagePO {
    public static final String TABLE_NAME = "supos_i18n_language";

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 语言code码 eg:zn_CH
     */
    private String languageCode;

    /**
     * 语言类型(code自己对应的语言描述)
     */
    private String languageName;

    /**
     *
     */
    private Integer languageType;

    /**
     * 是否启用 0 不使用 1使用
     */
    private Boolean hasUsed;

}
