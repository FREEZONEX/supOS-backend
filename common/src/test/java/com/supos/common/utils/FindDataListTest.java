package com.supos.common.utils;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.supos.common.annotation.DateTimeConstraint;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.FieldDefines;
import com.supos.common.enums.FieldType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.junit.Test;

import java.util.*;

public class FindDataListTest {
    @Test
    public void test_DataType() {
        String pkQuery = " SELECT \n" +
                "  tc.table_name,\n" +
                "  kcu.column_name,\n" +
                "  CASE WHEN tc.constraint_type = 'PRIMARY KEY' THEN true ELSE false END AS is_primary\n" +
                "FROM  information_schema.table_constraints tc  \n" +
                " JOIN information_schema.key_column_usage kcu \n" +
                "  ON tc.constraint_name = kcu.constraint_name\n" +
                "WHERE tc.table_name in (";
        System.out.println(pkQuery);
    }

    @Test
    public void test_findDataNum() {
        FieldDefine f1 = new FieldDefine("_id", FieldType.LONG, true);
        FieldDefine f2 = new FieldDefine("int1", FieldType.INTEGER);
        FieldDefine[] fs = new FieldDefine[]{f1, f2};

        Object vo = JsonUtil.fromJson("23.01");
        FindDataListUtils.SearchResult rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs));

        System.out.println(JsonUtil.toJson(rs));
    }

    @Test
    public void test_gsonObj() {
        String[] jss = new String[]{
                "abc",
                "{\"name\": \"Lite\"}",
                "[1,2,3]",
                "[1.1,2,3]",
                "false",
                "{\"enable\": false}",
//                "{\"a\":1",
        };
        Gson gson = new Gson();
        for (String s : jss) {
            try {
                Object gObj = gson.fromJson(s, Object.class);
                System.out.println("gObj = " + gObj.getClass() + ", " + gObj);
                if (List.class.isAssignableFrom(gObj.getClass())) {
                    List list = (List) gObj;
                    System.out.println("list[0].class = " + list.get(0).getClass() + ", obj=" + list.get(0));
                } else if (Map.class.isAssignableFrom(gObj.getClass())) {
                    Map<String, Object> map = (Map) gObj;
                    for (Map.Entry entry : map.entrySet()) {
                        System.out.printf("%s=%s, class=%s\n", entry.getKey(), entry.getValue().getClass(), entry.getValue());
                    }
                }
            } catch (Exception ex) {
                System.out.println("ERR parse: " + s);
            }
        }
        Object vo = JsonUtil.fromJson("{\"a\": false}");
        System.out.println("vo.class = " + vo.getClass());
    }

    @Test
    public void test_findStr() {
        FieldDefine f1 = new FieldDefine("_id", FieldType.LONG, true);
        FieldDefine f2 = new FieldDefine("str", FieldType.STRING);
        FieldDefine[] fs = new FieldDefine[]{f1, f2};

        Gson gson = new Gson();
        Object gObj = gson.fromJson("aaaa", Object.class);
        System.out.println("gObj = " + gObj.getClass());

        Object vo = JsonUtil.fromJson("aaaa");
        FindDataListUtils.SearchResult rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs));

        System.out.println(JsonUtil.toJson(rs));
    }

    @Test
    public void test_findDataBoolean() {
        FieldDefine f1 = new FieldDefine("_id", FieldType.LONG, true);
        FieldDefine f2 = new FieldDefine("foo", FieldType.BOOLEAN);
        FieldDefine[] fs = new FieldDefine[]{f1, f2};

        Object vo = JsonUtil.fromJson("23.01");
        FindDataListUtils.SearchResult rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs));

        System.out.println(rs);

        vo = JsonUtil.fromJson("true");
        rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs));
        System.out.println(rs);
    }

    @Test
    public void test_findDataIndexMap() {
        FieldDefine f1 = new FieldDefine("id", FieldType.INTEGER, true);
        FieldDefine f2 = new FieldDefine("tag", FieldType.STRING);
        FieldDefine f3 = new FieldDefine("desc", FieldType.STRING);
        f2.setIndex("0");
        f3.setIndex("16");
        FieldDefine[] fs = new FieldDefine[]{f1, f2, f3};

        HashMap<Object, Map<String, Object>> data = new HashMap<>();
        data.put(0L, vqt("study", 1L));
        data.put(16, vqt("for study only", 2L));
        String json = JsonUtil.toJson(data);
        Map vo = JsonUtil.fromJson(json, Map.class);
        List<Map<String, Object>> rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs)).list;

        System.out.println(rs);

        System.out.println(ExpressionFunctions.FIXED(3.1415, 2));
        System.out.println(ExpressionFunctions.FIXED(3.1415, -1));
        System.out.println(DateTimeConstraint.parseDate("2024-01-21 02"));
        System.out.println(DateTimeConstraint.parseDate("2024-01-21"));
    }

    private static Map<String, Object> vqt(Object v, Long q) {
        Map<String, Object> m = new HashMap<>(4);
        m.put("v", v);
        if (q != null) {
            m.put("q", q);
        }
        m.put("t", System.currentTimeMillis());
        return m;
    }

    @Test
    public void test_fastJsonRead() {
        FieldDefine[] fs = JsonUtil.fromJson("[{\"name\":\"#ct\",\"type\":\"datetime\",\"unique\":true},{\"index\":0,\"name\":\"t0\",\"type\":\"int\"},{\"index\":1,\"name\":\"t1\",\"type\":\"long\"},{\"name\":\"t2\",\"type\":\"float\"}]",
                FieldDefine[].class);
        System.out.println(JsonUtil.toJson(fs));

        String[] ds = new String[]{"1", "{", "}", "[1,2", "[12.3."};

        System.out.println("--- fast json--");
        for (String s : ds) {
            try {
                Object vo = JsonUtil.fromJson(s);
                System.out.println(s + ", " + vo + ", vo.class = " + vo.getClass());
            } catch (Exception ex) {
                System.err.println("fastJson Failed: " + s);
            }
        }
        System.out.println("--- hutool json--");
        for (String s : ds) {
            try {
                Object vo = JSONUtil.parse(s);
                System.out.println(s + ", " + vo);
            } catch (Exception ex) {
                System.err.println("hutool Failed: " + s);
            }
        }
    }

    @Test
    public void test_fastJsonW() {
        FieldDefine f1 = new FieldDefine("tag", FieldType.STRING);
        FieldDefine f2 = new FieldDefine("id", FieldType.INTEGER, true);
        FieldDefine f3 = new FieldDefine("desc", FieldType.STRING);
        f3.setIndex("16");
        FieldDefine[] fs = new FieldDefine[]{f1, f2, f3};

        System.out.println(JsonUtil.toJson(fs));

        CreateTopicDto dto = new CreateTopicDto();
        dto.setPath("/a/b/1F");
        System.out.println(JsonUtil.toJson(dto));
        //
        Map<String, Object> bean = new HashMap<>();
        bean.put("name", "Lucy");
        String payload = "he says: \"Gun!\" ";
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"updateTime\":").append(System.currentTimeMillis());
        sb.append(",\"payload\":\"").append(payload.replace("\"", "\\\""))
                .append("\",\"data\":{ ");
        for (Map.Entry<String, Object> entry : bean.entrySet()) {
            String name = entry.getKey();
            Object v = entry.getValue();
            if (name.charAt(0) != '@' && v != null) {
                sb.append('"').append(name).append("\":");
                if (Number.class.isAssignableFrom(v.getClass())) {
                    sb.append(v).append(',');
                } else {
                    sb.append("\"").append(v).append("\",");
                }
            }
        }
        sb.setCharAt(sb.length() - 1, '}');
        sb.append('}');
        System.out.println(sb);
    }

    @Test
    public void test_json() {
        JSON vo = JSONUtil.parse("{\"data\":[1,2,3] }");
        System.out.println(vo.getByPath("data").getClass());
    }

    @Test
    public void test_findData() {
        List<LinkedHashMap<String, Object>> list = new ArrayList<>();
        {
            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
            m.put("id", 100);
            m.put("Tag2", "3.00.00");
            m.put("remark", "test100");
            list.add(m);
        }
        {
            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
            m.put("id", 130);
            m.put("tag", "3.60.00");
            list.add(m);
        }
        {
            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
            m.put("id", 150);
            m.put("Tag2", "3.60.02");
            list.add(m);
        }
        HashMap payload = new HashMap();
        payload.put("topic", "t11");

        payload.put("id", 11);
        payload.put("tag", "121");

        HashMap em = new HashMap();
        em.put("list", list);
        payload.put("data", em);

        String json = JsonUtil.toJson(payload);
        Object vo = JsonUtil.fromJson(json);
//        vo = JsonUtil.fromJson(json, Map.class);

        FieldDefine[] fs = new FieldDefine[]{new FieldDefine("id", FieldType.INTEGER), new FieldDefine("tag", FieldType.STRING, "Tag2")};
        FindDataListUtils.SearchResult rs = FindDataListUtils.
                findDataList(vo, 1, new FieldDefines(fs));

        System.out.println(JsonUtil.toJson(rs));
    }

    @Data
    public static class SaveDataVO {
        @NotEmpty
        String topic;
        /**
         * 数据列表
         */
        @NotNull
        Object list;

        public String toString() {
            return JsonUtil.toJsonUseFields(this);
        }
    }
}
