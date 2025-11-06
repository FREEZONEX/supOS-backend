package com.supos.common.dto.mount.meta.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/9/19 16:21
 */
@Data
public class CommonFolderMetaDto implements Serializable {

    private String code;

    private String name;

    private String displayName;

    private Integer mountType;

    /**
     * 挂载源
     */
    private String mountSource;
}
