package com.supos.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/4/16 13:51
 */
@Data
public class FileBlobDataQueryDto implements Serializable {

    /**
     * 文件别名
     */
    private String fileAlias;

    /**
     * 相等条件
     */
    private List<EQCondition> eqConditions;

    @Data
    public static class EQCondition implements Serializable {
        private String fieldName;
        private String value;
    }
}
