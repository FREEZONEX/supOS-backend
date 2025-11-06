package com.supos.common.dto.mount.meta.common;

import com.supos.common.dto.FieldDefine;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: CommonFileMetaDto
 * @date 2025/9/19 16:21
 */
@Data
public class CommonFileMetaDto implements Serializable {

    private String alias;

    private String name;

    private String displayName;

    private String description;

    private Boolean save2db;

    private String originalAlias;

    private FieldDefine[] fields;
}
