package com.supos.uns.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/13 9:08
 */
@Data
@Getter
@Setter
public class ExportParam {

    public final static String EXPORT_TYPE_ALL = "ALL";

    private String userId;

    private String exportType;
    private String fileType;

    private List<String> models;
    private List<String> instances;

    private String fileFlag;

    Boolean async = true;
}
