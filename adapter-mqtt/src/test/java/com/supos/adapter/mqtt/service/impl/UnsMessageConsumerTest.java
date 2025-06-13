package com.supos.adapter.mqtt.service.impl;

import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.FieldDefines;
import com.supos.common.enums.FieldType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class UnsMessageConsumerTest {
//    Map<String, TopicDefinition> topicDefinitionMap = new HashMap<>();
//
//    {
//        for (int i = 1; i <= 3; i++) {
//            String topic = "/dev/" + i;
//            FieldDefine[] fields = new FieldDefine[]{new FieldDefine("tm", FieldType.DOUBLE, false, "1", null, null)};
//            CreateTopicDto dto = new CreateTopicDto(topic, topic, fields);
//            dto.setDataType(Constants.TIME_SEQUENCE_TYPE);
//            UnsMessageConsumer.addTopicFields(topicDefinitionMap, dto);
//        }
//        FieldDefine[] fields = new FieldDefine[]{new FieldDefine("cv", FieldType.DOUBLE, false, "1", null, null)};
//        CreateTopicDto dto = new CreateTopicDto("/calc/test", "calc", fields);
//        dto.setDataType(Constants.CALCULATION_REAL_TYPE);
//        InstanceField[] refers = new InstanceField[]{
//                new InstanceField("/dev/1", "tm"),
//                new InstanceField("/dev/2", "tm"),
//        };
//        dto.setRefers(refers);
//        dto.setCompileExpression(ExpressionFunctions.compileExpression("a1 * 100 + a2"));
//        UnsMessageConsumer.addTopicFields(topicDefinitionMap, dto);
//    }

    @Test
    public void test_add2Json() {
        FieldDefine[] fields = new FieldDefine[]{
                new FieldDefine("ct", FieldType.LONG),
                new FieldDefine("tm", FieldType.DOUBLE),
                new FieldDefine("desc", FieldType.STRING),
        };
        FieldDefines fieldDefines = new FieldDefines(fields);
        Map<String, Object> bean = new LinkedHashMap<>();
        bean.put("ct", 123.45);
        bean.put("tm", 3.1415);
        bean.put("desc", "LiLei \"me mei\"");

        StringBuilder sb = new StringBuilder();
        add2Json(fieldDefines, bean, sb);

        System.out.println(sb);
    }

    static void add2Json(FieldDefines fieldDefines, Map<String, Object> bean, StringBuilder sb) {
        sb.append('{');
        final int len = sb.length();
        Map<String, FieldDefine> fieldDefineMap = fieldDefines.getFieldsMap();
        for (Map.Entry<String, Object> entry : bean.entrySet()) {
            String name = entry.getKey();
            Object v = entry.getValue();
            FieldDefine fieldDefine = fieldDefineMap.get(name);
            if (fieldDefine != null && v != null) {
                sb.append("\\\"").append(name).append("\\\":");
                FieldType fieldType = fieldDefine.getType();
                boolean isZ = fieldType == FieldType.INT || fieldType == FieldType.LONG;
                if (Number.class.isAssignableFrom(v.getClass())) {
                    sb.append(isZ ? ((Number) v).longValue() : v).append(',');
                } else {
                    sb.append("\\\"").append(v.toString().replace("\"", "\\\\\\\"")).append("\\\",");
                }
            }
        }
        if (sb.length() > len) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
    }

}
