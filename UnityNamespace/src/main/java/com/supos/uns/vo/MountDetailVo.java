package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sunlifang@supos.supcon.com
 * @title MountDetailVo
 * @description
 * @create 2025/6/21 下午3:40
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MountDetailVo implements Serializable {

    private Integer mountType;

    /**
     * 挂载源
     */
    private String mountSource;

    /**
     * 挂载源显示名
     */
    private String displayName;
}
