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
        params.put("password","Supos1304@");
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

    @Test
    public void getRoleListByUserId() {
        String url = "http://100.100.100.22:33997/keycloak/home/auth/realms/master/protocol/openid-connect/token";
        Map<String,Object> params = new HashMap<>();
        params.put("username","admin");
        params.put("password","Supos1304@");
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
        String u2 = "http://100.100.100.22:33997/keycloak/home/admin/realms/supos/users/746b5f5a-6ce1-4aa8-bee1-4a1e7afe2029/role-mappings";
        HttpResponse r1 = HttpRequest.get(u2).bearerAuth(token).execute();
        System.out.println();
    }

    @Test
    public void testLdap() {
        String url = "http://100.100.100.22:33997/keycloak/home/auth/admin/realms/supos/components?parentId=8920b375-d705-4d30-8a71-52d9c14ec4ba&type=org.keycloak.storage.UserStorageProvider";
        String adminToken = adminToken();
        HttpResponse response = HttpRequest.get(url).bearerAuth(adminToken).execute();

//        JSONArray res = JSONArray.parseArray(response.body());
//        String ldapParentId =  res.getJSONObject(0).getString("id");


        String json = "{\"config\":{\"allowKerberosAuthentication\":[\"false\"],\"authType\":[\"simple\"],\"batchSizeForSync\":[\"\"],\"bindCredential\":[\"P@ssword.supcon\"],\"bindDn\":[\"cn=manager,dc=supcon,dc=com\"],\"cachePolicy\":[\"DEFAULT\"],\"changedSyncPeriod\":[86400],\"connectionPooling\":[\"false\"],\"connectionTimeout\":[\"\"],\"connectionUrl\":[\"ldaps://192.168.235.134:17089\"],\"customUserSearchFilter\":[\"\"],\"editMode\":[\"READ_ONLY\"],\"enabled\":[\"true\"],\"fullSyncPeriod\":[604800],\"importEnabled\":[\"true\"],\"pagination\":[\"false\"],\"rdnLDAPAttribute\":[\"uid\"],\"readTimeout\":[\"\"],\"referral\":[\"\"],\"searchScope\":[\"1\"],\"startTls\":[\"false\"],\"syncRegistrations\":[\"true\"],\"trustEmail\":[\"false\"],\"useKerberosForPasswordAuthentication\":[\"false\"],\"usePasswordModifyExtendedOp\":[\"false\"],\"useTruststoreSpi\":[\"always\"],\"userObjectClasses\":[\"person, organizationalPerson, user\"],\"usernameLDAPAttribute\":[\"uid\"],\"usersDn\":[\"o=accounts,dc=supcon,dc=com\"],\"uuidLDAPAttribute\":[\"entryUUID\"],\"validatePasswordPolicy\":[\"false\"],\"vendor\":[\"ad\"]},\"name\":\"ldap\",\"parentId\":\"8920b375-d705-4d30-8a71-52d9c14ec4ba\",\"providerId\":\"ldap\",\"providerType\":\"org.keycloak.storage.UserStorageProvider\"}";


        String componentUrl = "http://100.100.100.22:33997/keycloak/home/auth/admin/realms/supos/components";

        HttpResponse comRes = HttpRequest.post(componentUrl).bearerAuth(adminToken).body(json).execute();

        if (comRes.getStatus() == 201){
            // ok
        }

        System.out.println();

    }

    private String adminToken(){
        String url = "http://100.100.100.22:33997/keycloak/home/auth/realms/master/protocol/openid-connect/token";
        Map<String,Object> params = new HashMap<>();
        params.put("username","admin");
        params.put("password","Supos1304@");
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
        return token;
    }

    @Test
    public void removeSession(){
        String sessionState = "ac379e27-af5a-44e9-8f99-207bec82f27f";
        String url = "http://100.100.100.22:33997/keycloak/home/auth/admin/realms/supos/sessions/" + sessionState;
        log.info(">>>>>>>>>>>>Keycloak removeSession URL：{}", url);
        HttpResponse response = HttpRequest.delete(url).bearerAuth(adminToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak removeSession response code：{}", response.getStatus());
    }
}
