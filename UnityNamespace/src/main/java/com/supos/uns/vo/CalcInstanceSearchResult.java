package com.supos.uns.vo;

import com.supos.common.vo.FieldDefineVo;
import lombok.Data;

import java.util.List;

@Data
public class CalcInstanceSearchResult {
    String topic;
    List<FieldDefineVo> fields;
}
