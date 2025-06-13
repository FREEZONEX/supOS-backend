package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.supos.uns.dao.po.UnsPo;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;

public class DbToUnsServiceTest {

    @Test
    public void testCopy() {
        UnsPo src = new UnsPo();
        Map<Long, Integer> m1 = new HashMap<>();
        m1.put(1L,1);
        src.setRefUns(m1);
        src.setPath("/a");
        UnsPo tar = new UnsPo();
        String pro = "{}";
        tar.setProtocol(pro);
        Map<Long, Integer> m2 = new HashMap<>();
        m2.put(2L, 2);
        tar.setRefUns(m2);

        BeanUtils.copyProperties(src, tar);
        Assert.assertEquals(pro, tar.getProtocol());
        Assert.assertEquals(tar.getPath(), src.getPath());
        Assert.assertEquals(m1, tar.getRefUns());
    }

    @Test
    public void testCopyHutool() {
        UnsPo src = new UnsPo();
        Map<Long, Integer> m1 = new HashMap<>();
        m1.put(1L,1);
        src.setRefUns(m1);
        src.setPath("/a");
        UnsPo tar = new UnsPo();
        String pro = "{}";
        tar.setProtocol(pro);
        Map<Long, Integer> m2 = new HashMap<>();
        m2.put(2L, 2);
        tar.setRefUns(m2);


        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreNullValue(true);

        BeanUtil.copyProperties(src, tar, copyOptions);
        Assert.assertEquals(pro, tar.getProtocol());
        Assert.assertEquals(tar.getPath(), src.getPath());
        Assert.assertEquals(m1, tar.getRefUns());
    }

//    @Test
//    public void testParseDsFields() {
//        DbToUnsService service = new DbToUnsService();
//        String json = "{\"success\":true,\"errorCode\":null,\"errorMessage\":null,\"data\":[{\"oldName\":null,\"name\":\"id\",\"tableName\":\"uns_namespace\",\"columnType\":\"CHAR\",\"dataType\":1,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":32,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":32,\"ordinalPosition\":1,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"path\",\"tableName\":\"uns_namespace\",\"columnType\":\"TEXT\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":2147483647,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":2147483647,\"ordinalPosition\":2,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"path_type\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT2\",\"dataType\":5,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":5,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":5,\"ordinalPosition\":3,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"data_type\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT2\",\"dataType\":5,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":5,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":5,\"ordinalPosition\":4,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"fields\",\"tableName\":\"uns_namespace\",\"columnType\":\"TEXT\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":2147483647,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":2147483647,\"ordinalPosition\":5,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"create_at\",\"tableName\":\"uns_namespace\",\"columnType\":\"TIMESTAMPTZ\",\"dataType\":93,\"defaultValue\":\"now()\",\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":35,\"bufferLength\":null,\"decimalDigits\":6,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":35,\"ordinalPosition\":6,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"status\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT2\",\"dataType\":5,\"defaultValue\":\"1\",\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":5,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":5,\"ordinalPosition\":7,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"description\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":255,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":255,\"ordinalPosition\":8,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"update_at\",\"tableName\":\"uns_namespace\",\"columnType\":\"TIMESTAMPTZ\",\"dataType\":93,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":35,\"bufferLength\":null,\"decimalDigits\":6,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":35,\"ordinalPosition\":9,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"protocol\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":2000,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":2000,\"ordinalPosition\":10,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"data_path\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":128,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":128,\"ordinalPosition\":11,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"with_flags\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT4\",\"dataType\":4,\"defaultValue\":\"0\",\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":10,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":10,\"ordinalPosition\":12,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"data_src_id\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT2\",\"dataType\":5,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":5,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":5,\"ordinalPosition\":13,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"ref_uns\",\"tableName\":\"uns_namespace\",\"columnType\":\"JSONB\",\"dataType\":1111,\"defaultValue\":\"'{}'::jsonb\",\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":2147483647,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":2147483647,\"ordinalPosition\":14,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"refers\",\"tableName\":\"uns_namespace\",\"columnType\":\"TEXT\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":2147483647,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":2147483647,\"ordinalPosition\":15,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"expression\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":255,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":255,\"ordinalPosition\":16,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"table_name\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":190,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":190,\"ordinalPosition\":17,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"number_fields\",\"tableName\":\"uns_namespace\",\"columnType\":\"INT2\",\"dataType\":5,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":5,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":5,\"ordinalPosition\":18,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"model_id\",\"tableName\":\"uns_namespace\",\"columnType\":\"CHAR\",\"dataType\":1,\"defaultValue\":\"NULL::bpchar\",\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":32,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":32,\"ordinalPosition\":19,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"alias\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":128,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":128,\"ordinalPosition\":20,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null},{\"oldName\":null,\"name\":\"protocol_type\",\"tableName\":\"uns_namespace\",\"columnType\":\"VARCHAR\",\"dataType\":12,\"defaultValue\":null,\"autoIncrement\":null,\"comment\":null,\"primaryKey\":null,\"schemaName\":\"supos\",\"databaseName\":null,\"typeName\":null,\"columnSize\":64,\"bufferLength\":null,\"decimalDigits\":0,\"numPrecRadix\":10,\"nullableInt\":null,\"sqlDataType\":null,\"sqlDatetimeSub\":null,\"charOctetLength\":64,\"ordinalPosition\":21,\"nullable\":null,\"generatedColumn\":null,\"extent\":null,\"editStatus\":null}],\"traceId\":null,\"errorDetail\":null,\"solutionLink\":null}";
//        JSONObject object = JSON.parseObject(json);
//        String data = object.getJSONArray("data").toJSONString();
//        List<DbFieldDefineVo> vos = JSON.parseArray(data, DbFieldDefineVo.class);
//
//        JsonResult<FieldDefine[]> fos = service.parseDatabaseFields(new DbFieldsInfoVo("POSTGRESQL", vos.toArray(new DbFieldDefineVo[0])));
//        System.out.println(JSON.toJSONString(fos.getData(), true));
//        Assert.assertEquals(vos.size(), fos.getData().length);
//
//        Map vo = JsonUtil.fromJson("{\n" +
//                "    \"connections\": 32,\n" +
//                "    \"topics\": 21,\n" +
//                "    \"cluster\": false,\n" +
//                "    \"subscriptions\": 26,\n" +
//                "    \"live_connections\": 32,\n" +
//                "    \"disconnected_durable_sessions\": 0,\n" +
//                "    \"subscriptions_durable\": 0,\n" +
//                "    \"subscriptions_ram\": 2611111111111111,\n" +
//                "    \"retained_msg_count\": 3,\n" +
//                "    \"shared_subscriptions\": 0,\n" +
//                "    \"dropped_msg_rate\": 20,\n" +
//                "    \"persisted_rate\": 0,\n" +
//                "    \"received_msg_rate\": 20,\n" +
//                "    \"sent_msg_rate\": 0.12,\n" +
//                "    \"transformation_failed_rate\": 0,\n" +
//                "    \"transformation_succeeded_rate\": 0,\n" +
//                "    \"validation_failed_rate\": 0,\n" +
//                "    \"validation_succeeded_rate\": 0\n" +
//                "}", Map.class);
//        System.out.println(vo.get("connections").getClass());
//        System.out.println(vo.get("subscriptions_ram").getClass());
//        System.out.println(vo.get("sent_msg_rate").getClass());
//        System.out.println(vo.get("cluster").getClass());
//    }
//
//    @Test
//    public void test_parseJson2uns() {
//        {
//            JsonResult<List<OuterStructureVo>> rs = UnsQueryService.parseJson2uns("[{\n" +
//                    "    \"connections\": 32,\n" +
//                    "    \"topics\": 21,\n" +
//                    "    \"cluster\": false,\n" +
//                    "    \"subscriptions\": 26,\n" +
//                    "    \"live_connections\": 32,\n" +
//                    "    \"disconnected_durable_sessions\": 0,\n" +
//                    "    \"subscriptions_durable\": 0,\n" +
//                    "    \"subscriptions_ram\": 2611111111111111,\n" +
//                    "    \"retained_msg_count\": 3,\n" +
//                    "    \"shared_subscriptions\": 0,\n" +
//                    "    \"dropped_msg_rate\": 20,\n" +
//                    "    \"persisted_rate\": 0,\n" +
//                    "    \"received_msg_rate\": 20,\n" +
//                    "    \"sent_msg_rate\": 0.12,\n" +
//                    "    \"transformation_failed_rate\": 0,\n" +
//                    "    \"transformation_succeeded_rate\": 0,\n" +
//                    "    \"validation_failed_rate\": 0,\n" +
//                    "    \"validation_succeeded_rate\": 0\n" +
//                    "},{ \"received_msg_rate\": 202611111111111111}]");
//            Assert.assertEquals(1, rs.getData().size());
////            System.out.println(JSON.toJSONString(rs.getData(), true));
//        }
//        {
//            JsonResult<List<OuterStructureVo>> rs = UnsQueryService.parseJson2uns("{\"da\":[{\n" +
//                    "    \"connections\": 32,\n" +
//                    "    \"topics\": 21,\n" +
//                    "    \"cluster\": false,\n" +
//                    "    \"subscriptions\": 26,\n" +
//                    "    \"live_connections\": 32,\n" +
//                    "    \"disconnected_durable_sessions\": 0,\n" +
//                    "    \"subscriptions_durable\": 0,\n" +
//                    "    \"subscriptions_ram\": 2611111111111111,\n" +
//                    "    \"retained_msg_count\": 3,\n" +
//                    "    \"shared_subscriptions\": 0,\n" +
//                    "    \"dropped_msg_rate\": 20,\n" +
//                    "    \"persisted_rate\": 0,\n" +
//                    "    \"received_msg_rate\": 20,\n" +
//                    "    \"sent_msg_rate\": 0.12,\n" +
//                    "    \"transformation_failed_rate\": 0,\n" +
//                    "    \"transformation_succeeded_rate\": 0,\n" +
//                    "    \"validation_failed_rate\": 0,\n" +
//                    "    \"validation_succeeded_rate\": 0\n" +
//                    "},{ \"received_msg_rate\": 202611111111111111}] ,\"zdx\": {\"validation_succeeded_rate\": 100} }");
//            Assert.assertEquals(1, rs.getData().size());
////            System.out.println(JSON.toJSONString(rs.getData(), true));
//        }
//        {
//            String json = "{\"da\":[{\n" +
//                    "    \"connections\": 32,\n" +
//                    "    \"topics\": 21,\n" +
//                    "    \"cluster\": false,\n" +
//                    "    \"received_msg_rate\": 20,\n" +
//                    "    \"sent_msg_rate\": 0.12,\n" +
//                    "    \"transformation_failed_rate\": 0,\n" +
//                    "    \"transformation_succeeded_rate\": 0,\n" +
//                    "    \"validation_failed_rate\": 0,\n" +
//                    "    \"validation_succeeded_rate\": 0\n" +
//                    "},{ \"received_msg_rate\": 202611111111111111}] ,\"zdx\": { \"xlist\": [ {\"tickTalk\": 100}] } }";
//
//            System.out.println(json);
//            JsonResult<List<OuterStructureVo>> rs = UnsQueryService.parseJson2uns(json);
//            Assert.assertEquals(2, rs.getData().size());
//            System.out.println(JSON.toJSONString(rs.getData(), true));
//        }
//    }
//
//
//    @Test
//    public void test_parseJson2uns2() {
//        String json = "{\n" +
//                "  \"name\": \"张三\",\n" +
//                "  \"age\": 25,\n" +
//                "  \"gender\": \"男\",\n" +
//                "  \"isStudent\": true,\n" +
//                "  \"hobbies\": [\"阅读\", \"运动\", \"音乐\"],\n" +
//                "  \"address\": {\n" +
//                "    \"city\": \"北京\",\n" +
//                "    \"country\": \"中国\",\n" +
//                "    \"postalCode\": \"100000\"\n" +
//                "  }\n" +
//                "}";
//        System.out.println(JSONUtil.toJsonStr(ParserUtil.parserJson2Tree(json)));
//    }
//
//    @Test
//    public void testParserJsonFromFile() {
//        String json = FileUtil.readString("E:\\工作\\supos2\\22157ce4-05ba-4c88-b531-a32b632f9970_2supos.txt", StandardCharsets.UTF_8);
//        System.out.println(JSONUtil.toJsonStr(ParserUtil.parserJson2Tree(json)));
//    }
}
