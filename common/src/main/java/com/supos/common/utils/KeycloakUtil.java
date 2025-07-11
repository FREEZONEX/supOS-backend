package com.supos.common.utils;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.dto.auth.*;
import com.supos.common.exception.BuzException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KeycloakUtil {


    private final Cache<String, String> keycloakCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final static String ADMIN_TOKEN_KEY = "admin_token";

    //supos client id
    private final static String CLIENT_ID = "a7b53e5e-3567-470a-9da1-94cc0c7f18e6";

    @Resource
    private OAuthKeyCloakConfig keyCloakConfig;

    private String getApiUrl() {
        return keyCloakConfig.getIssuerUri() + "/realms/" + keyCloakConfig.getRealm() + "/protocol/openid-connect";
    }

    private String getAdminApiUrl() {
        return keyCloakConfig.getIssuerUri() + "/admin/realms/" + keyCloakConfig.getRealm();
    }


    public HttpResponse userinfo(String accessToken) {
        //获取用户信息
        String url = getApiUrl() + "/userinfo";
        log.debug(">>>>>>>>>>>>Keycloak userinfo URL：{}", url);
        HttpResponse response = HttpUtil.createRequest(Method.GET, url).bearerAuth(accessToken).execute();
        log.debug(">>>>>>>>>>>>Keycloak userinfo response code：{},body:{}", response.getStatus(), response.body());
        return response;
    }


    public HttpResponse refreshToken(String refreshToken) {
        String url = getApiUrl() + "/token";
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", keyCloakConfig.getClientId());
        params.put("client_secret", keyCloakConfig.getClientSecret());
        log.debug(">>>>>>>>>>>>Keycloak refreshToken URL：{},params:{}", url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak refreshToken response code：{},body:{}", response.getStatus(), response.body());
        return response;
    }

    public JSONObject getKeyCloakToken(String code) {
        String url = getApiUrl() + "/token";
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", keyCloakConfig.getAuthorizationGrantType());
        params.put("code", code);
        params.put("redirect_uri", keyCloakConfig.getRedirectUri());
        params.put("client_id", keyCloakConfig.getClientId());
        params.put("client_secret", keyCloakConfig.getClientSecret());
        log.debug(">>>>>>>>>>>>Keycloak get token URL：{},params:{}", url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak get token response code：{},body:{}", response.getStatus(), response.body());
        if (200 != response.getStatus()) {
            log.error("getKeyCloakToken return error response:{}", response);
            return null;
        }
        return JSON.parseObject(response.body());
    }


    public String getAdminToken() {
        String adminToken = keycloakCache.getIfPresent(ADMIN_TOKEN_KEY);
        if (StringUtils.isNotBlank(adminToken)) {
            return adminToken;
        }

        String url = keyCloakConfig.getIssuerUri() + "/realms/master/protocol/openid-connect/token";
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "Supos1304@");
        params.put("grant_type", "password");
        params.put("client_id", "admin-cli");
        log.debug(">>>>>>>>>>>>Keycloak getAdminToken URL：{},params:{}", url, JSON.toJSON(params));

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak getAdminToken response code：{},body:{}", response.getStatus(), response.body());
        if (200 != response.getStatus()) {
            log.error("Keycloak getAdminToken return error response:{}", response);
            throw new RuntimeException("Keycloak getAdminToken 失败");
        }
        adminToken = JSON.parseObject(response.body()).getString("access_token");
        keycloakCache.put(ADMIN_TOKEN_KEY, adminToken);
        return adminToken;
    }


    public boolean deleteUser(String id) {
        String url = getAdminApiUrl() + "/users/" + id;
        log.debug(">>>>>>>>>>>>Keycloak deleteUser URL：{}", url);
        HttpResponse response = HttpRequest.delete(url).bearerAuth(getAdminToken()).execute();
        log.debug(">>>>>>>>>>>>Keycloak deleteUser response code：{},body:{}", response.getStatus(), response.body());
        return 204 == response.getStatus();
    }

    public boolean resetPwd(String userId, String password) {
        String url = getAdminApiUrl() + "/users/" + userId + "/reset-password";
        JSONObject params = new JSONObject();
        params.put("type", "password");
        params.put("temporary", false);
        params.put("value", password);
        log.debug(">>>>>>>>>>>>Keycloak resetPwd URL：{}", url);
        HttpResponse response = HttpRequest.put(url).bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.debug(">>>>>>>>>>>>Keycloak resetPwd response code：{},body:{}", response.getStatus(), response.body());
        return 204 == response.getStatus();
    }

    public boolean updateUser(String userId, JSONObject params) {
        String url = getAdminApiUrl() + "/users/" + userId;
        log.debug(">>>>>>>>>>>>Keycloak updateUser URL：{},params:{}", url, params.toString());
        HttpResponse response = HttpRequest.put(url).bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.debug(">>>>>>>>>>>>Keycloak updateUser response code：{},body:{}", response.getStatus(), response.body());
        return 204 == response.getStatus();
    }

//    public String getClientId {
//        String url = getAdminApiUrl() + "/clients?clientId=" + keyCloakConfig.CLIENT_ID;
//        log.debug(">>>>>>>>>>>>Keycloak getClientId URL：{}", url);
//        HttpResponse response = HttpRequest.get(url).bearerAuth(getAdminToken()).execute();
//        log.debug(">>>>>>>>>>>>Keycloak getClientId response code：{},body:{}", response.getStatus(), response.body());
//        if (200 != response.getStatus()) {
//            throw new RuntimeException("Keycloak getClientId 失败");
//        }
//        JSONArray array = JSON.parseArray(response.body());
//        if (null == array) {
//            throw new RuntimeException("Keycloak getClientId 失败");
//        }
//        return array.getJSONObject(0).getString("id");
//    }

    public boolean setRole(String userId, Integer type, JSONArray params) {
        String url = getAdminApiUrl() + "/users/" + userId + "/role-mappings/clients/" + CLIENT_ID;
        log.debug(">>>>>>>>>>>>Keycloak setRole URL：{},type:{},params:{}", url, type, params.toString());
        HttpRequest httpRequest = null;
        if (1 == type) {
            httpRequest = HttpUtil.createRequest(Method.POST, url);
        } else {
            httpRequest = HttpUtil.createRequest(Method.DELETE, url);
        }
        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.debug(">>>>>>>>>>>>Keycloak setRole response code：{},body:{}", response.getStatus(), response.body());
        return 204 == response.getStatus();
    }

    public String createUser(String body) {
        String url = getAdminApiUrl() + "/users";
        log.debug(">>>>>>>>>>>>Keycloak createUser URL：{},body:{}", url, body);
        HttpResponse response = HttpRequest.post(url).bearerAuth(getAdminToken()).body(body).execute();
        log.debug(">>>>>>>>>>>>Keycloak createUser response code：{},body:{}", response.getStatus(), response.body());
        //http://keycloak:8080/admin/realms/supos/users/a09d1625-3244-4cfb-ae86-f226caa44121
        String location = response.header("Location");
        if (response.getStatus() == 201 && StrUtil.isNotBlank(location)) {
            return StrUtil.subAfter(location, "/", true);
        } else if (response.getStatus() == 409) {
            throw new BuzException("user.create.already.exists");
        } else {
            throw new BuzException("user.create.failed");
        }
    }

    public static String removePortIfDefault(String url) {
        // 使用正则表达式匹配并处理端口号为80或443的情况
        String regex = "(http://[^:/]+)(:(80|443))?(/.*)?";

        // 如果匹配到端口为80或443的情况
        if (ReUtil.isMatch(regex, url)) {
            // 使用正则替换，去掉端口号为80或443的部分
            return ReUtil.replaceAll(url, regex, "$1$4");
        }

        // 如果没有匹配，返回原始 URL
        return url;
    }

    public AccessTokenDto login(String username, String password) {
        String url = getApiUrl() + "/token";
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("username", username);
        params.put("password", password);
        params.put("client_id", keyCloakConfig.getClientId());
        params.put("client_secret", keyCloakConfig.getClientSecret());
        log.debug(">>>>>>>>>>>>Keycloak login URL：{},params:{}", url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak login response code：{},body:{}", response.getStatus(), response.body());
        if (response.getStatus() != 200) {
            log.error("Keycloak login 返回异常:{}", response);
            return null;
        }
        return JSON.parseObject(response.body(), AccessTokenDto.class);
    }

    /**
     * 获取用户信息
     *
     * @param username
     * @return
     */
    public KeycloakUserInfoDto fetchUser(String username) {
        String url = getAdminApiUrl() + "/users";
        Map<String, Object> params = new HashMap<>();
        params.put("exact", true);
        params.put("username", username);
        log.debug(">>>>>>>>>>>>Keycloak fetchUser URL：{},params:{}", url, JSONObject.toJSONString(params));
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).form(params).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchUser response code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus()) {
            String body = response.body();
            if (StringUtils.isNotBlank(body) && JSONArray.isValidArray(body)) {
                JSONArray array = JSONArray.parseArray(body);
                if (array.size() > 0) {
                    return array.getJSONObject(0).toJavaObject(KeycloakUserInfoDto.class);
                }
            }
        }
        return null;
    }

    // 查询email
    public KeycloakUserInfoDto fetchUserByEmail(String email) {
        String url = getAdminApiUrl() + "/users";
        Map<String, Object> params = new HashMap<>();
        params.put("exact", true);
        params.put("email", email);
        log.debug(">>>>>>>>>>>>Keycloak fetchUserByEmail URL：{},params:{}", url, JSONObject.toJSONString(params));
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).form(params).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchUserByEmail response code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus()) {
            String body = response.body();
            if (StringUtils.isNotBlank(body) && JSONArray.isValidArray(body)) {
                JSONArray array = JSONArray.parseArray(body);
                if (array.size() > 0) {
                    return array.getJSONObject(0).toJavaObject(KeycloakUserInfoDto.class);
                }
            }
        }
        return null;
    }

    /**
     * 获取所有角色信息
     *
     * @return
     */
    public List<KeycloakRoleInfoDto> getAllRoles() {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/roles";
        log.debug(">>>>>>>>>>>>Keycloak getAllRoles URL：{}", url);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
        log.debug(">>>>>>>>>>>>Keycloak getAllRoles response code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus()) {
            String body = response.body();
            if (StringUtils.isNotBlank(body) && JSONArray.isValidArray(body)) {
                return JSONArray.parseArray(body).toJavaList(KeycloakRoleInfoDto.class);
            }
        } else {
            throw new BuzException("role.get.failed");
        }
        return new ArrayList<>();
    }

    /**
     * 获取角色信息
     *
     * @return
     */
    public KeycloakRoleInfoDto fetchRole(String roleName) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/roles/" + roleName;
        log.debug(">>>>>>>>>>>>Keycloak fetchRole URL：{}", url);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchRole response code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus()) {
            String body = response.body();
            if (StringUtils.isNotBlank(body)) {
                return JSONObject.parseObject(body, KeycloakRoleInfoDto.class);
            }
        } else if (404 == response.getStatus()) {
            return null;
        } else {
            throw new BuzException("role.get.failed");
        }
        return null;
    }

    /**
     * 获取角色信息
     *
     * @param id
     * @return
     */
    public KeycloakRoleInfoDto fetchRoleById(String id) {
        String url = getAdminApiUrl() + "/roles-by-id/" + id;
        log.debug(">>>>>>>>>>>>Keycloak fetchRoleById URL：{}", url);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchRoleById response code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus()) {
            String body = response.body();
            if (StringUtils.isNotBlank(body)) {
                return JSONObject.parseObject(body, KeycloakRoleInfoDto.class);
            }
        } else if (404 == response.getStatus()) {
            return null;
        } else {
            throw new BuzException("role.get.failed");
        }
        return null;
    }

    /**
     * 删除角色
     *
     * @param roleName
     * @return
     */
    public boolean deleteRole(String roleName) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/roles/" + roleName;
        log.debug(">>>>>>>>>>>>Keycloak deleteRole URL：{}", url);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.DELETE, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
        log.debug(">>>>>>>>>>>>Keycloak deleteRole response code：{},body:{}", response.getStatus(), response.body());
        if (204 == response.getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * 创建角色
     *
     * @param roleName
     * @param description
     * @return
     */
    public boolean createRole(String roleName, String description) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/roles";
        JSONObject body = new JSONObject();
        body.put("name", roleName);
        body.put("description", description);

        String bodyStr = body.toJSONString();
        log.debug(">>>>>>>>>>>>Keycloak createRole URL：{}, body:{}", url, bodyStr);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(bodyStr).execute();
        log.debug(">>>>>>>>>>>>Keycloak createRole code：{},body:{}", response.getStatus(), response.body());
        if (204 == response.getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * 根据名称获取资源信息
     *
     * @param name
     * @return
     */
    public JSONObject fetchResource(String name) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/resource/search";
        Map<String, Object> params = new HashMap<>();
        params.put("exact", true);
        params.put("name", name);

        log.debug(">>>>>>>>>>>>Keycloak fetchResource URL：{},params:{}", url, JSONObject.toJSONString(params));
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).form(params).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchResource code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus() && JSONObject.isValidObject(response.body())) {
            return JSONObject.parseObject(response.body());
        }
        return null;
    }

    /**
     * 创建资源
     *
     * @param name
     * @param type
     * @param uris
     * @return
     */
    public KeycloakResourceInfoDto createResource(String name, String type, Set<String> uris) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/resource";
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("displayName", name);
        body.put("type", type);
        body.put("uris", uris);

        String bodyStr = body.toJSONString();
        log.debug(">>>>>>>>>>>>Keycloak createResource URL：{}, body:{}", url, bodyStr);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(bodyStr).execute();
        log.debug(">>>>>>>>>>>>Keycloak createResource code：{},body:{}", response.getStatus(), response.body());
        if (201 == response.getStatus() && JSONObject.isValidObject(response.body())) {
            return JSONObject.parseObject(response.body(), KeycloakResourceInfoDto.class);
        }
        return null;
    }

    /**
     * 更新资源
     *
     * @param id
     * @param object
     * @return
     */
    public boolean updateResource(String id, JSONObject object) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/resource/" + id;

        String bodyStr = object.toJSONString();
        log.debug(">>>>>>>>>>>>Keycloak updateResource URL：{}, body:{}", url, bodyStr);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.PUT, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(bodyStr).execute();
        log.debug(">>>>>>>>>>>>Keycloak updateResource code：{},body:{}", response.getStatus(), response.body());
        if (204 == response.getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * 删除资源
     *
     * @param name
     * @return
     */
    public boolean deleteResource(String name) {
        JSONObject resource = fetchResource(name);
        if (resource != null) {
            String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/resource/" + resource.getString("_id");
            log.debug(">>>>>>>>>>>>Keycloak deleteResource URL：{}", url);
            HttpRequest httpRequest = HttpUtil.createRequest(Method.DELETE, url);
            HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
            log.debug(">>>>>>>>>>>>Keycloak deleteResource code：{},body:{}", response.getStatus(), response.body());
            if (204 == response.getStatus()) {
                return true;
            }
        }
        return true;
    }

    /**
     * 根据名称获取策略信息
     *
     * @param name
     * @return
     */
    public JSONObject fetchPolicy(String name) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/policy/search";
        Map<String, Object> params = new HashMap<>();
        params.put("exact", true);
        params.put("name", name);

        log.debug(">>>>>>>>>>>>Keycloak fetchPolicy URL：{},params:{}", url, JSONObject.toJSONString(params));
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).form(params).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchPolicy code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus() && JSONObject.isValidObject(response.body())) {
            return JSONObject.parseObject(response.body());
        }
        return null;
    }

    // 创建策略
    public KeycloakPolicyInfoDto createPolicy(String name, String description, String roleId) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/policy/role";
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("description", description);
        body.put("fetchRoles", false);
        body.put("logic", "POSITIVE");

        JSONArray roles = new JSONArray();
        JSONObject role = new JSONObject();
        role.put("id", roleId);
        role.put("required", false);
        roles.add(role);

        body.put("roles", roles);


        String bodyStr = body.toJSONString();
        log.debug(">>>>>>>>>>>>Keycloak createPolicy URL：{}, body:{}", url, bodyStr);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(bodyStr).execute();
        log.debug(">>>>>>>>>>>>Keycloak createPolicy code：{},body:{}", response.getStatus(), response.body());
        if (201 == response.getStatus() && JSONObject.isValidObject(response.body())) {
            return JSONObject.parseObject(response.body(), KeycloakPolicyInfoDto.class);
        }
        return null;
    }

    /**
     * 删除策略
     *
     * @param name
     * @return
     */
    public boolean deletePolicy(String name) {
        JSONObject policy = fetchPolicy(name);
        if (policy != null) {
            String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/policy/" + policy.getString("id");
            log.debug(">>>>>>>>>>>>Keycloak deletePolicy URL：{}", url);
            HttpRequest httpRequest = HttpUtil.createRequest(Method.DELETE, url);
            HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
            log.debug(">>>>>>>>>>>>Keycloak deletePolicy code：{},body:{}", response.getStatus(), response.body());
            if (204 == response.getStatus()) {
                return true;
            }
        }
        return true;
    }

    /**
     * 根据名称获取权限信息
     *
     * @param name
     * @return
     */
    public JSONObject fetchPermission(String name) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/permission/search";
        Map<String, Object> params = new HashMap<>();
        params.put("exact", true);
        params.put("name", name);

        log.debug(">>>>>>>>>>>>Keycloak fetchPermission URL：{},params:{}", url, JSONObject.toJSONString(params));
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).form(params).execute();
        log.debug(">>>>>>>>>>>>Keycloak fetchPermission code：{},body:{}", response.getStatus(), response.body());
        if (200 == response.getStatus() && JSONObject.isValidObject(response.body())) {
            return JSONObject.parseObject(response.body());
        }
        return null;
    }


    // 创建权限
    public boolean createPermission(String name, String description, String policyId, String resourceId) {
        String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/permission/resource";
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("description", description);
        body.put("decisionStrategy", "UNANIMOUS");

        JSONArray policies = new JSONArray();
        policies.add(policyId);
        body.put("policies", policies);

        JSONArray resources = new JSONArray();
        resources.add(resourceId);
        body.put("resources", resources);

        String bodyStr = body.toJSONString();
        log.debug(">>>>>>>>>>>>Keycloak createPermission URL：{}, body:{}", url, bodyStr);

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, url);

        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(bodyStr).execute();
        log.debug(">>>>>>>>>>>>Keycloak createPermission code：{},body:{}", response.getStatus(), response.body());
        if (201 == response.getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * 删除权限
     *
     * @param name
     * @return
     */
    public boolean deletePermission(String name) {
        JSONObject permission = fetchPermission(name);
        if (permission != null) {
            String url = getAdminApiUrl() + "/clients/" + CLIENT_ID + "/authz/resource-server/permission/resource/" + permission.getString("id");
            log.debug(">>>>>>>>>>>>Keycloak deletePermission URL：{}", url);
            HttpRequest httpRequest = HttpUtil.createRequest(Method.DELETE, url);
            HttpResponse response = httpRequest.bearerAuth(getAdminToken()).execute();
            log.debug(">>>>>>>>>>>>Keycloak deletePermission code：{},body:{}", response.getStatus(), response.body());
            if (204 == response.getStatus()) {
                return true;
            }
        }
        return true;
    }


    public void setLocale(String locale) {
        String token = getAdminToken();
        String url = getAdminApiUrl();
        HttpResponse response = HttpRequest.get(url).bearerAuth(token).execute();
        if (response.getStatus() != 200 || StringUtils.isBlank(response.body())) {
            log.warn("设置Keycloak 语言退出，获取realms信息失败");
            return;
        }

        JSONObject realmInfo = JSONObject.parseObject(response.body());
        String defaultLocale = realmInfo.getString("defaultLocale");
        JSONArray supportedLocales = realmInfo.getJSONArray("supportedLocales");
        //如果当前语言和keycloak语言是一致的，无需修改
        if (locale.equals(defaultLocale) || CollectionUtils.isEmpty(supportedLocales) || locale.equals(supportedLocales.getString(0))) {
            return;
        }

        realmInfo.put("internationalizationEnabled", true);
        realmInfo.put("defaultLocale", locale);

        JSONArray newSupportedLocales = new JSONArray();
        supportedLocales.add(locale);
        realmInfo.put("supportedLocales", newSupportedLocales);

        HttpResponse putResponse = HttpRequest.put(url).bearerAuth(token).body(realmInfo.toJSONString()).execute();
        if (putResponse.getStatus() == 204) {
            log.debug("设置Keycloak 语言完成");
        }
    }


    public String getUserExchangeTokenById(String userId) {
        String url = getApiUrl() + "/token";
        Map<String, Object> params = new HashMap<>();
        params.put("client_secret", "VaOS2makbDhJJsLlYPt4Wl87bo9VzXiO");
        params.put("grant_type", "client_credentials");
        params.put("client_id", "supos");
        log.debug(">>>>>>>>>>>>Keycloak getAdminToken URL：{},params:{}", url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak getAdminToken response code：{},body:{}", response.getStatus(), response.body());
        if (200 != response.getStatus()) {
            throw new RuntimeException("Keycloak getAdminToken 失败");
        }
        String adminToken = JSON.parseObject(response.body()).getString("access_token");

//        String url  = "http://100.100.100.22:33997/keycloak/home/realms/supos/protocol/openid-connect/token";
        Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        tokenParams.put("client_id", keyCloakConfig.getClientId());
        tokenParams.put("client_secret", keyCloakConfig.getClientSecret());
        tokenParams.put("subject_token", adminToken);
        tokenParams.put("requested_subject", userId);
        tokenParams.put("scope", "openid profile");
        HttpResponse responseToken = HttpRequest.post(url).form(tokenParams).execute();
        if (responseToken.getStatus() != 200) {
            log.error("getUserExchangeTokenById 返回异常:{}", responseToken);
            return null;
        }
        return responseToken.body();
    }

    public String getRoleListByUserId(String userId) {
        String url = getAdminApiUrl() + "/users/" + userId + "/role-mappings";
        HttpResponse response = HttpRequest.get(url).bearerAuth(getAdminToken()).execute();
        if (response.getStatus() != 200) {
            log.error("getRoleListByUserId 返回异常:{}", response);
            return null;
        }
        return response.body();
    }

    public void logout(String refreshToken){
        String url = getApiUrl() + "/logout";
        Map<String, Object> params = new HashMap<>();
        params.put("client_id", "supos");
        params.put("client_secret", "VaOS2makbDhJJsLlYPt4Wl87bo9VzXiO");
        params.put("refresh_token", refreshToken);
        log.debug(">>>>>>>>>>>>Keycloak logout URL：{},params:{}", url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.debug(">>>>>>>>>>>>Keycloak logout response code：{},body:{}", response.getStatus(), response.body());
        if (204 != response.getStatus()) {
            log.warn("Keycloak logout 失败");
        }
    }

    public void removeSession(String sessionState ){
        String url = getAdminApiUrl() + "/sessions/" + sessionState;
        log.info(">>>>>>>>>>>>Keycloak removeSession URL：{},params:{}", url, sessionState);
        HttpResponse response = HttpRequest.delete(url).bearerAuth(getAdminToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak removeSession response code：{}", response.getStatus());
    }
}
