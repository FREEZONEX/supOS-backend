package com.supos.common.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.mvel2.asm.Handle;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class VQT <T> {

    private long timeStamp;

    private long quality;

    private T value;

    public static String getVQTJson(Long t, long q, Object v, CreateTopicDto metaDef) {
        Map<String, Object> vqt = new HashMap<>();
        String timestampField = metaDef.getTimestampField();
        String qualityField = metaDef.getQualityField();
        vqt.put(timestampField, t);
        vqt.put(qualityField, q);
        for (FieldDefine fd : metaDef.getFields()) {
            if (!metaDef.getTimestampField().equals(fd.getName()) && !metaDef.getQualityField().equals(fd.getName())) {
                vqt.put(fd.getName(), v);
                break;
            }
        }
        return JSON.toJSONString(vqt);
    }

    public static Map<String, Object> getVQTJson2(Long t, long q, Object v, CreateTopicDto metaDef) {
        Map<String, Object> vqt = new HashMap<>();
        String timestampField = metaDef.getTimestampField();
        String qualityField = metaDef.getQualityField();
        vqt.put(timestampField, t);
        vqt.put(qualityField, q);
        for (FieldDefine fd : metaDef.getFields()) {
            if (!metaDef.getTimestampField().equals(fd.getName()) && !metaDef.getQualityField().equals(fd.getName())) {
                vqt.put(fd.getName(), v);
                break;
            }
        }
        return vqt;
    }

}
