package com.supos.uns.vo;

import com.supos.common.dto.FieldDefine;
import com.supos.common.vo.FieldDefineVo;
import lombok.Data;

import java.util.List;

@Data
public class TimeseriesInstanceSearchResult {
    String id;
    String name;
    String path;
    Integer parentDataType;
    List<FieldDefine> fields;
}
