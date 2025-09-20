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
 * @description: I18nResourceModulePO
 * @date 2025/8/28 14:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(I18nResourceModulePO.TABLE_NAME)
public class I18nResourceModulePO {

    public static final String TABLE_NAME = "supos_i18n_module";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String moduleCode;

    private String moduleName;

    private Integer moduleType;
}
