package com.supos.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class KeycloakUtilTest {


    @Test
    public void testSetLocale() {
        String url = "http://100.100.100.22:33893/keycloak/home/realms/master/protocol/openid-connect/token";
        Map<String,Object> params = new HashMap<>();
        params.put("username","admin");
        params.put("password","admin");
        params.put("grant_type","password");
        params.put("client_id","admin-cli");
        log.info(">>>>>>>>>>>>Keycloak getAdminToken URL：{},params:{}",url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak getAdminToken response code：{},body:{}",response.getStatus(),response.body());
        if(200 != response.getStatus()){
            throw new RuntimeException("Keycloak getAdminToken 失败");
        }

        String token = JSON.parseObject(response.body()).getString("access_token");

        String u1 = "http://100.100.100.22:33893/keycloak/home/admin/realms/supos";

        HttpResponse response1 = HttpRequest.get(u1).bearerAuth(token).execute();
        System.out.println(response1.body());

        JSONObject realmInfo = JSONObject.parseObject(response1.body());
        String locale = "en-US";
        realmInfo.put("defaultLocale",locale);

        JSONArray supportedLocales = new JSONArray();
        supportedLocales.add(locale);
        realmInfo.put("supportedLocales",supportedLocales);

        //204
        HttpResponse response2 = HttpRequest.put("http://100.100.100.22:33893/keycloak/home/admin/realms/supos").bearerAuth(token).body(realmInfo.toJSONString()).execute();

        System.out.println(response2.body());



    }
}
