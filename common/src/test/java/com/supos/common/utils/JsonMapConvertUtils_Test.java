package com.supos.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.supos.common.annotation.UrlValidator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Assert;
import org.junit.Test;

import jakarta.validation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonMapConvertUtils_Test {
    @Test
    public void testMapConvert() {
        Map<String, Object> map = new HashMap<>();
//        map.put("homePage", "http://yy.cc");
        map.put("name", " Lucy\r\n");
        map.put("desc", "selfTest");
//        map.put("city.name", "HangZhou");
        map.put("city.province", "ZheJiang");

        map.put("params[0].key", "id");
        map.put("params[0].value", 0);
        map.put("params[1].key", "id");
        map.put("params[1].value", 123);

        map.put("legs[0]", 1);
        map.put("legs[1]", 3);
        map.put("legs[2]", 5);

//        map.put("params[2].id", 125);
//        map.put("params[2].tag.value", "dev");
//        map.put("params[2].tag.desc", "dev123");

        Map<String, Object> rs = JsonMapConvertUtils.convertMap(map);
        System.out.println("mapRs: \n" + JSON.toJSONString(rs, true));
        User user = BeanUtil.toBean(rs, User.class);
        System.out.println("user: \n" + JSON.toJSONString(user, true));

        Assert.assertEquals(map.get("name").toString().trim(), user.name);
        Assert.assertEquals("id", user.params.get(0).key);
        Assert.assertEquals(123, user.params.get(1).value);

//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        Validator validator = factory.getValidator();
//        Set<ConstraintViolation<User>> violations = validator.validate(user);
//
//        if (!violations.isEmpty()) {
//            for (ConstraintViolation<User> violation : violations) {
//                System.out.println(violation);
//            }
//        } else {
//            System.out.println("校验通过");
//        }
    }
}





@Data
@AllArgsConstructor
@NoArgsConstructor
@Valid class User {

    @NotEmpty
    @UrlValidator
    String homePage;
    @NotEmpty String name;

    @NotEmpty String desc;

    @Valid @NotNull City city;

    @Valid @NotEmpty List<Params> params;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Valid
    public static class Params {
        String key;
        @Min(1) int value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Valid
    public static class City {
        @NotEmpty String name;
        String province;
    }
}
