package com.supos.common.dto.mount.meta.connection;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ConnectionDto
 * @date 2025/9/29 14:34
 */
@Data
public class ConnectionDto implements Serializable {

    /**
     * 名称
     */
    private String name;

    private LinkedHashMap<String, Object> config;
}
