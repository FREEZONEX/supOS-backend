package com.supos.adpter.tdengine;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Maps;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.StreamWindowType;
import org.junit.Assert;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2024/12/13 15:44
 */
public class TdQueryTest {
    static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");


    @Test
    public void test_com() {
        HashMap<String, Integer> map = new HashMap<>();
        System.out.println(map.computeIfAbsent("1", k -> 123));
    }

    @Test
    public void test_timeMills() {
        final Instant nowInstant = Instant.now();
        final long nowInMills = nowInstant.toEpochMilli();
        final int micro = nowInstant.get(ChronoField.MICRO_OF_SECOND);
        long nowInMicroSecond = nowInMills * 100_0000 + micro;// 精确到微秒
        System.out.println("nowInstant = " + nowInstant.atOffset(ZoneOffset.UTC).format(fmt));
        System.out.println("130d (micro) = " + convertToMills(micro));
        long nanoSecond = System.nanoTime();
        long[] times = new long[]{nowInMills / 1000, nowInMills, System.currentTimeMillis(), nowInMicroSecond, nanoSecond};
        String[] names = new String[]{"  秒", " 毫秒", "J毫秒", " 微秒", "J纳秒"};
        for (int i = 0; i < times.length; i++) {
            System.out.printf(" %s：,len=%d, %d, convertToMills=%d\n", names[i], String.valueOf(times[i]).length(), times[i], convertToMills(times[i]));
        }
    }

    static long convertToMills(long timestamp) {
        if (timestamp > 10000000000000L) {
            timestamp = Long.parseLong(String.valueOf(timestamp).substring(0, 13));
        } else if (timestamp < 1000000000000L) {
            StringBuilder sr = new StringBuilder(16).append(timestamp);
            while (sr.length() < 13) {
                sr.append('0');
            }
            timestamp = Long.parseLong(sr.toString());
        }
        return timestamp;
    }

    @Test
    public void test_getCreateStreamSQL() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();


        CreateTopicDto dto = new CreateTopicDto();
        StreamOptions streamOptions = new StreamOptions();
        StreamWindowOptions ops = new StreamWindowOptions();


        StreamWindowOptionsCountWindow countWindow = new StreamWindowOptionsCountWindow();
        countWindow.setCountValue(100);
        countWindow.setSlidingValue(30);

        HashMap<String, Object> opMap = BeanUtil.copyProperties(countWindow, HashMap.class);
        ops.setOptions(opMap);
        ops.setWindowType(StreamWindowType.COUNT_WINDOW.name());
        streamOptions.setWindow(ops);
        streamOptions.setWaterMark("100s");
        streamOptions.setWhereCondition("current_val > 1");
        FieldDefine[] fs = new FieldDefine[]{
                new FieldDefine("avgVal", FieldType.DOUBLE, "avg(current_val)")
        };
        dto.setFields(fs);

        Set<ConstraintViolation<Object>> violations = validator.validate(streamOptions);
        System.out.println(violations);
        Assert.assertEquals(0, violations.size());
        dto.setStreamCalculation("/his/test", streamOptions);
        dto.setReferTable("_5d5ee305b15742e296e915c4e8db7044");
        String sql = TdEngineEventHandler.getCreateStreamSQL("public", "cs2100c", dto);
        System.out.println(sql);
    }

    @Test
    public void testMapEquals() {
//        HttpUtil.createPost("http://100.100.100.20:31016/rest/sql/public")
//                .header("Authorization", "Basic " + Base64.encode("root:" + "taosdata"))
//                .body("select _ct from _liu_ceshid64e28951ea74d618a2a8366585cac9b").then(httpResponse -> {
//                    final int status = httpResponse.getStatus();
//                    final String respBody = httpResponse.body();
//                    System.out.println(respBody);
//                });
        System.out.println(System.currentTimeMillis());
        //1735126485003
        //1000000000000
        {
            final Instant nowInstant = Instant.now();
            final long nowInMills = nowInstant.toEpochMilli();
            final int micro = nowInstant.get(ChronoField.MICRO_OF_SECOND);
            long nowInMicroSecond = nowInMills * 100_0000 + micro;// 精确到微秒
            System.out.println(nowInMicroSecond);
        }

        {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("A", 1);
            map1.put("B", 2);

            Map<String, Integer> map2 = new LinkedHashMap<>();
            map2.put("A", 1);
            map2.put("B", 2);

            Map<String, Integer> map3 = new ConcurrentHashMap<>();
            map2.put("A", 1);
            map2.put("B", 2);

            boolean areEqual = map1.equals(map2);
            System.out.println("Maps are equal: " + areEqual);
            System.out.println("Maps are equal: " + map1.equals(map3));
            System.out.println("Maps are equal: " + Maps.difference(map1, map3).areEqual());
        }
        FieldDefine[] fields = new FieldDefine[]{
                new FieldDefine("A", FieldType.INTEGER), new FieldDefine("B", FieldType.DOUBLE),
        };
        Map<String, String> fmap = new LinkedHashMap<>();
        for (FieldDefine define : fields) {
            fmap.put(define.getName(), define.getType().name);
        }
        Map<String, String> fieldTypes = Arrays.stream(fields).collect(Collectors.toMap(d -> d.getName(), d -> d.getType().name));
        System.out.println("Maps fieldTypes are equal: " + fmap.equals(fieldTypes));
        System.out.println("Maps fieldTypes equal Null: " + fmap.equals(null));
    }
}
