//package com.supos.adpter.pg;
//
//import com.supos.common.SrcJdbcType;
//import com.supos.common.dto.FieldDefine;
//import com.supos.common.dto.FieldDefines;
//import com.supos.common.dto.SaveDataDto;
//import com.supos.common.event.SaveDataEvent;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = PostgresqlEventHandlerTest.InitConfiguration.class)
//@RunWith(SpringRunner.class)
//public class PostgresqlEventHandlerTest {
//
//    @ContextConfiguration
//    @ComponentScan("com.supos.adpter.pg")
//    static class InitConfiguration {
//    }
//
//    @Autowired
//    PostgresqlEventHandler handler;
//    @Autowired
//    @Qualifier("pgJdbcTemplate")
//    JdbcTemplate jdbcTemplate;
//
//    @Test
//    public void testBatchAlterTable() {
//        jdbcTemplate.batchUpdate("alter table \"/a/b/\" rename to \"/biz/a/b\"", "alter table \"/a/b/c\" rename to \"/biz/a/b/c\" ");
//    }
//
//    @Test
//    public void testCreateTable() {
////        CreateTopicDto dto = new CreateTopicDto();
////        dto.setSrcType("pg");
////        dto.setTopic("dev5./a/b/test_dev3");
////        dto.setFields(Arrays.asList(
////                        new FieldDefine("id", "number")
////                        , new FieldDefine("devTag", "string")
////                        , new FieldDefine("remark", "string")s
////                        , new FieldDefine("enabled", "boolean")
////                        , new FieldDefine("sw", "int")
////                )
////                .toArray(new FieldDefine[0]));
////        handler.onCreateTable(new CreateTableEvent(this, SrcJdbcTypes.Postgresql.jdbcType, dto));
//    }
//
//    @Test
//    public void testInsertData() {
////        SaveDataDto saveDataDto = getSaveDataDto();
////        saveDataDto.setTopic("test_dev4");
////        saveDataDto.getList().get(0).put("pk_score", 0.55);
////        FieldDefines defines;
////        {
////            Map<String, String> fieldTypes = new LinkedHashMap<>();
////            fieldTypes.put("id", "bigint");
////            fieldTypes.put("devTag", "text");
////            fieldTypes.put("remark", "text");
//////            fieldTypes.put("enable", "boolean");
////            defines = new FieldDefines(fieldTypes, new String[]{"id"});
////        }
////        handler.insertData(saveDataDto, defines);
//    }
//
//    private SaveDataDto getSaveDataDto() {
//        SaveDataDto saveDataDto = new SaveDataDto();
//        saveDataDto.setTopic("test_dev2");
//        List<Map<String, Object>> list = new ArrayList<>();
//        saveDataDto.setList(list);
//        {
//            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
//            m.put("id", 100);
//            m.put("devTag", "3.00.00");
//            m.put("remark", "test100");
//            m.put("enable", true);
//            list.add(m);
//        }
//        {
//            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
//            m.put("id", 130);
//            m.put("devTag", "3.60.00");
//            list.add(m);
//        }
//        {
//            LinkedHashMap<String, Object> m = new LinkedHashMap<>(4);
//            m.put("id", 150);
//            m.put("devTag", "3.60.02");
//            list.add(m);
//        }
//        return saveDataDto;
//    }
//
//
//}
