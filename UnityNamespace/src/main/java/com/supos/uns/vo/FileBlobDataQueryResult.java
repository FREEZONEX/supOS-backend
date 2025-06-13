package com.supos.uns.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 文件数据查询结果
 * @date 2025/4/16 13:09
 */
@Data
public class FileBlobDataQueryResult implements Serializable {


    private List<Map<String, String>> datas;
}
