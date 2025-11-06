package com.supos.common.dto.mount.meta.connection;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ConnectionResp
 * @date 2025/9/29 14:32
 */
@Data
public class ConnectionResp implements Serializable {

    /**
     * 连接信息
     */
    private List<ConnectionDto> connections;
}
