package com.supos.uns.vo;

import com.supos.common.dto.FieldDefine;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class OuterStructureVo {
    String dataPath;
    List<FieldDefine> fields;// 解析出的结果

    Map<String, Object> valueMap;

    public OuterStructureVo(String dataPath, List<FieldDefine> fields) {
        this.dataPath = dataPath;
        this.fields = fields;
    }

    public OuterStructureVo(String dataPath, List<FieldDefine> fields, Map<String, Object> valueMap) {
        this.dataPath = dataPath;
        this.fields = fields;
        this.valueMap = valueMap;
    }
}
