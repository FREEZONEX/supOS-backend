package com.supos.gateway.service;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.dto.auth.ResourceDto;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.KeycloakUtil;
import com.supos.common.vo.UserInfoVo;
import com.supos.gateway.dao.mapper.AuthMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {
    @Resource
    private KeycloakUtil keycloakUtil;
    @Resource
    private OAuthKeyCloakConfig keyCloakConfig;
    @Resource
    private AuthMapper authMapper;

    private static final List<String> DEF_METHODS = Arrays.asList("get","post","put","delete","patch","head","options");

    /**
     * key:supos_community_token
     * value:token_info json
     * 默认1小时
     */
    @Resource
    private TimedCache<String, JSONObject> tokenCache;

    /**
     * key:sub
     * value:user info vo
     */
    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;

    public UserInfoVo getUserInfoVoByToken(String token){
        JSONObject tokenObj = tokenCache.get(token);
        if (null == tokenObj) {
            return null;
        }
        String accessToken = tokenObj.getString("access_token");
        UserInfoVo userInfoVo = getUserInfoVoByCache(accessToken,true);
        return userInfoVo;
    }

    /**
     * 获取用户信息（保活）
     * @param token
     * @return
     */
    public ResponseEntity getUserInfoByToken(String token){
        //获取token json info
        JSONObject tokenObj = tokenCache.get(token);
        if (null == tokenObj) {
            return ResponseEntity.status(401).body("can not find token obj from cache");
        }
        String accessToken = tokenObj.getString("access_token");
        JWT jwt = JWT.of(accessToken);
        long exp = jwt.getPayloads().getLong("exp");
        //单位：秒
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        // 判断是否过期
        if (currentTimeInSeconds > exp) {
            return ResponseEntity.status(401).body("Token is expired.");
        }
        //如果过期时间小于5分钟，刷新token
        if ((exp - currentTimeInSeconds) <= keyCloakConfig.getRefreshTokenTime()) {
//        if (true) {
            log.info(">>>>>>>>>>>>token：{}过期时间小于RefreshTokenTime，进行refreshToken",token);
            //使用refresh刷新token
            HttpResponse refreshRes = keycloakUtil.refreshToken(tokenObj.getString("refresh_token"));
            if (200 == refreshRes.getStatus()) {
                JSONObject refreshTokenObj = JSON.parseObject(refreshRes.body());
                tokenCache.put(token, refreshTokenObj,refreshTokenObj.getLong("expires_in") * 1000);
                log.info(">>>>>>>>>>>>token：{}，完成保活",token);
            }
        }
        UserInfoVo userInfoVo = getUserInfoVoByCache(accessToken,true);
        if (null == userInfoVo){
            return ResponseEntity.status(401).body("keycloak token获取用户信息失败");
        }
        return ResponseEntity.ok(ResultVO.successWithData(userInfoVo));
    }

    public String getTokenByCode(String code){
        JSONObject tokenObj = keycloakUtil.getKeyCloakToken(code);
        if (null == tokenObj){
            return null;
        }
        //设置 token与token_info
        String token = IdUtil.fastUUID();
        tokenCache.put(token, tokenObj,tokenObj.getLong("expires_in") * 1000);
        //设置用户信息缓存：key:sub   value:user_info
        String accessToken = tokenObj.getString("access_token");
        getUserInfoVoByCache(accessToken,false);
        return token;
    }

    /**
     * 从缓存获取用户信息 获取不到重新从keycloak获取并设置缓存
     * @param accessToken
     * @return
     */
    private UserInfoVo getUserInfoVoByCache(String accessToken,boolean getCache){
        JWT jwt = JWT.of(accessToken);
        String sub = jwt.getPayloads().getStr("sub");
        UserInfoVo userInfoVo = null;
        if (getCache){
            userInfoVo = userInfoCache.get(sub);
            if (null != userInfoVo){
                return userInfoVo;
            }
        }
        HttpResponse response = keycloakUtil.userinfo(accessToken);
        if (200 != response.getStatus()){
            log.warn("accessToken:{}查询keycloak用户信息失败",accessToken);
            return null;
        }
        //设置用户信息缓存 key = sub   value = user_info
        userInfoVo = JSON.parseObject(response.body(),UserInfoVo.class);

        //首次登录
        if (1 == userInfoVo.getFirstTimeLogin()){
            JSONObject attributes = new JSONObject();
            JSONObject params = new JSONObject();
            params.put("firstTimeLogin",0);

            params.put("tipsEnable",userInfoVo.getTipsEnable());
            attributes.put("attributes",params);
            if (StrUtil.isNotBlank(userInfoVo.getEmail())){
                attributes.put("email",userInfoVo.getEmail());
            }
            keycloakUtil.updateUser(userInfoVo.getSub(),attributes);
        }
        userInfoVo = getUserRolesResources(userInfoVo);
        userInfoCache.put(sub, userInfoVo);
        log.debug("获取用户信息成功：{}",userInfoVo);
        return userInfoVo;
    }

    public UserInfoVo getUserRolesResources(UserInfoVo userInfoVo){
        //查询用户的所有角色  包含组合角色
        List<RoleDto> roleList = authMapper.roleListByUserId(keyCloakConfig.getRealm(),userInfoVo.getSub());
        if (CollectionUtil.isEmpty(roleList)){
            return userInfoVo;
        }

        //默认角色(组合角色)
        List<String> compositeRoleIds = roleList.stream().filter(r -> !r.getClientRole()).map(RoleDto::getRoleId).collect(Collectors.toList());
        //查询组合角色下的子角色
        List<RoleDto> compositeRoleList = authMapper.getChildRoleListByCompositeRoleId(compositeRoleIds);

        //client role
        List<RoleDto> clientRoleList = roleList.stream().filter(RoleDto::getClientRole).collect(Collectors.toList());

        List<RoleDto> allRoleList = new ArrayList<>();
        allRoleList.addAll(compositeRoleList);
        allRoleList.addAll(clientRoleList);

        userInfoVo.setRoleList(allRoleList);

        //分别获取允许角色和拒绝策略角色
        List<String> denyRoles = allRoleList.stream()
                .filter(role -> role.getRoleName().startsWith("deny"))
                .map(RoleDto::getRoleId)
                .collect(Collectors.toList());

        List<String> allowRoles = allRoleList.stream()
                .filter(role -> !role.getRoleName().startsWith("deny"))
                .map(RoleDto::getRoleId)
                .collect(Collectors.toList());
        //获取资源列表
        List<ResourceDto> denyResourceList = new ArrayList<>();
        List<ResourceDto> allowResourceList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(denyRoles)){
            denyResourceList = getResourceListByRoles(denyRoles);
        }
        if (CollectionUtil.isNotEmpty(allowRoles)) {
            allowResourceList = getResourceListByRoles(allowRoles);
        }
        userInfoVo.setDenyResourceList(denyResourceList);
        userInfoVo.setResourceList(allowResourceList);
        return userInfoVo;
    }

    public List<ResourceDto> getResourceListByRoles(List<String> roleIds){
        List<String> policyIds = new ArrayList<>();
        for (String roleId : roleIds) {
            List<String> pList = authMapper.getPolicyIdsByRoleId(roleId);
            if (CollectionUtil.isNotEmpty(pList)){
                policyIds.addAll(pList);
            }
        }
        if (CollectionUtil.isEmpty(policyIds)){
            return null;
        }
        List<ResourceDto> resourceList = authMapper.getResourceListByPolicyIds(policyIds);
        if (CollectionUtil.isNotEmpty(resourceList)){
            resourceList.forEach(res -> {
                res.setMethods(transMethodList(res.getUri()));
                removeIfUriSuffix(res);
            });
        }
        removeRepeat(resourceList);
        return resourceList;
    }

    private void removeIfUriSuffix(ResourceDto resource){
        if (StrUtil.contains(resource.getUri(),"$")){
            String uri = StrUtil.subBefore(resource.getUri(),"$",true);
            resource.setUri(uri);
        }
    }

    public static List<String> transMethodList(String uri){
        // /dashboard/test$get,post,put,delete
        if (!StrUtil.contains(uri,"$")){
            return DEF_METHODS;
        }

        String methodsStr =  StrUtil.subAfter(uri,"$",true);
        if (StrUtil.isBlank(methodsStr)){
            return DEF_METHODS;
        }

        return Arrays.stream(methodsStr.toLowerCase().split(",")).collect(Collectors.toList());
    }

    private void removeRepeat(List<ResourceDto> resourceList){
        new ArrayList<>(resourceList.stream()
                .collect(Collectors.toMap(ResourceDto::getUri, obj -> obj, (existing, replacement) -> {
                    if (existing.getMethods().size() > replacement.getMethods().size()) {
                        return existing;
                    } else {
                        return replacement;
                    }
                }))
                .values());
    }
}
