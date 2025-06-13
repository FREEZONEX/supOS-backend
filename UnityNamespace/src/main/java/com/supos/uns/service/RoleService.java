package com.supos.uns.service;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.dto.auth.*;
import com.supos.common.enums.ActionEnum;
import com.supos.common.enums.EventMetaEnum;
import com.supos.common.enums.RoleEnum;
import com.supos.common.enums.ServiceEnum;
import com.supos.common.event.EventBus;
import com.supos.common.event.SysEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.KeycloakUtil;
import com.supos.gateway.dao.mapper.AuthMapper;
import com.supos.uns.vo.RoleVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sunlifang
 * @version 1.0
 * @description: RoleService
 * @date 2025/4/21 9:31
 */
@Slf4j
@Service
public class RoleService {

    @Resource
    private AuthMapper authMapper;

    @Resource
    private KeycloakUtil keycloakUtil;

    private static Set<String> DEFAULT_RESOURCE_URI = new HashSet<>();

    static {
        DEFAULT_RESOURCE_URI.add("/inter-api/supos");
    }

    public ResultVO<List<RoleVo>> getRoleList(){
        // 获取默认资源集
/*        List<ResourceDto> defResources = new ArrayList<>();
        List<String> defPolicyIds = authMapper.getPolicyIdsByRoleId(authMapper.getDefaultRoleId());
        if (CollectionUtils.isNotEmpty(defPolicyIds)){
            defResources = authMapper.getResourceListByPolicyIds(defPolicyIds);
        }*/

        //获取角色列表
        List<KeycloakRoleInfoDto> keycloakRoleInfoDtos = keycloakUtil.getAllRoles();
        Map<String, KeycloakRoleInfoDto> roleMap = keycloakRoleInfoDtos.stream().collect(Collectors.toMap(KeycloakRoleInfoDto::getName, Function.identity(), (k1, k2) -> k2));
        Map<KeycloakRoleInfoDto, KeycloakRoleInfoDto> rolePairMap = new HashMap<>();// role -> denyRole;
        for (KeycloakRoleInfoDto role : keycloakRoleInfoDtos) {
            if (RoleEnum.IGNORE_ROLE_ID.contains(role.getId()) || RoleEnum.IGNORE_ROLE_NAME.contains(role.getName())) {
                continue;
            }
            if (!role.getName().startsWith("deny-")) {
                KeycloakRoleInfoDto denyRole = roleMap.get(String.format("deny-%s", role.getName()));
                rolePairMap.put(role, denyRole);
            }
        }

        List<RoleVo> list = new ArrayList<>(rolePairMap.size());
        List<RoleVo> tempRoleList = new ArrayList<>(rolePairMap.size());
        for (Map.Entry<KeycloakRoleInfoDto, KeycloakRoleInfoDto> rolePair : rolePairMap.entrySet()) {
            KeycloakRoleInfoDto keycloakRole = rolePair.getKey();
            RoleVo role = new RoleVo();
            role.setRoleId(keycloakRole.getId());

            RoleEnum roleEnum = RoleEnum.parse(keycloakRole.getId());
            if (roleEnum != null) {
                role.setRoleName(I18nUtils.getMessage(roleEnum.getI18nCode()));
            } else {
                role.setRoleName(keycloakRole.getDescription());
            }

            // 封装接受资源
            List<String> policyIds = authMapper.getPolicyIdsByRoleId(keycloakRole.getId());
            if (CollectionUtils.isEmpty(policyIds)){
                role.setResourceList(new ArrayList<>());
            } else {
                List<ResourceDto> roleResources = authMapper.getResourceListByPolicyIds(policyIds);
                role.setResourceList(unionResources(new ArrayList<>(),roleResources));
            }

            // 封装拒绝资源
            KeycloakRoleInfoDto keycloakDenyRole = rolePair.getValue();
            if (keycloakDenyRole != null) {
                List<String> denyPolicyIds = authMapper.getPolicyIdsByRoleId(keycloakDenyRole.getId());
                if (CollectionUtils.isEmpty(denyPolicyIds)){
                    role.setDenyResourceList(new ArrayList<>());
                } else {
                    List<ResourceDto> roleResources = authMapper.getResourceListByPolicyIds(denyPolicyIds);
                    role.setDenyResourceList(unionResources(new ArrayList<>(),roleResources));
                }
            } else {
                role.setDenyResourceList(new ArrayList<>());
            }
            if (role.getRoleId().equals(RoleEnum.SUPER_ADMIN.getId())) {
                list.add(role);
            } else {
                tempRoleList.add(role);
            }
        }
        list.addAll(tempRoleList);

        return ResultVO.successWithData(list);
    }

    private static List<ResourceDto> unionResources(List<ResourceDto> defResources,List<ResourceDto> roleResources) {
        return new ArrayList<>(Stream.concat(defResources.stream(), roleResources.stream())
                .collect(Collectors.toMap(ResourceDto::getUri, dto -> dto, (existing, replacement) -> existing))
                .values());
    }

    /**
     * 新建角色
     * @param roleSaveDto
     * @return
     */
    public RoleVo createRole(RoleSaveDto roleSaveDto){
        List<KeycloakRoleInfoDto> roles = keycloakUtil.getAllRoles();
        if (CollectionUtils.isNotEmpty(roles)) {
            roles = roles.stream().filter(r -> !RoleEnum.IGNORE_ROLE_ID.contains(r.getId()) && !r.getName().startsWith("deny-")).collect(Collectors.toList());
            if (roles.size() >= 10) {
                throw new BuzException("role.max.limit");
            }
        }

        // 1. 角色名校验
        String roleName = roleSaveDto.getName();
        String denyRoleName = String.format("deny-%s", roleName);

        KeycloakRoleInfoDto keycloakRole = keycloakUtil.fetchRole(roleName);
        if (keycloakRole != null) {
            throw new BuzException("role.name.exist");
        }

        // 2.创建角色
        keycloakUtil.createRole(roleName, roleName);
        keycloakRole = keycloakUtil.fetchRole(roleName);
        keycloakUtil.createRole(denyRoleName, denyRoleName);
        KeycloakRoleInfoDto denyKeycloakRole = keycloakUtil.fetchRole(denyRoleName);


        // 3.创建资源
        // 3.1 创建接受资源
        String resourceName = String.format("%s-resource", roleName);
        Set<String> allowResourceUris = new HashSet<>();
        if (CollectionUtils.isNotEmpty(roleSaveDto.getAllowResourceList())) {
            allowResourceUris.addAll(roleSaveDto.getAllowResourceList().stream().map(ResourceDto::getUri).collect(Collectors.toSet()));
        }
        allowResourceUris.addAll(DEFAULT_RESOURCE_URI);
        KeycloakResourceInfoDto allowResource = keycloakUtil.createResource(resourceName, "URL", allowResourceUris);
        // 3.2 创建拒绝资源
        String denyResourceName = String.format("%s-resource", denyRoleName);
        Set<String> denyResourceUris = new HashSet<>();
        if (CollectionUtils.isNotEmpty(roleSaveDto.getDenyResourceList())) {
            denyResourceUris.addAll(roleSaveDto.getDenyResourceList().stream().map(ResourceDto::getUri).collect(Collectors.toSet()));
        }
        KeycloakResourceInfoDto denyResource = keycloakUtil.createResource(denyResourceName, "URL", denyResourceUris);

        // 4.创建策略
        String policyName = String.format("%s-policy", roleName);
        KeycloakPolicyInfoDto policy = keycloakUtil.createPolicy(policyName, policyName, keycloakRole.getId());
        String denyPolicyName = String.format("%s-policy", denyRoleName);
        KeycloakPolicyInfoDto denyPolicy = keycloakUtil.createPolicy(denyPolicyName, denyPolicyName, denyKeycloakRole.getId());

        // 5.创建权限
        String permissionName = String.format("%s-permission", roleName);
        keycloakUtil.createPermission(permissionName, permissionName, policy.getId(), allowResource.getId());
        String denyPermissionName = String.format("%s-permission", denyRoleName);
        keycloakUtil.createPermission(denyPermissionName, denyPermissionName, denyPolicy.getId(), denyResource.getId());

        RoleVo role = new RoleVo();
        role.setRoleId(keycloakRole.getId());
        role.setRoleName(roleName);
        role.setResourceList(roleSaveDto.getAllowResourceList());
        role.setDenyResourceList(roleSaveDto.getDenyResourceList());

        sendWebhook(role, ActionEnum.ADD);
        return role;
    }

    public boolean updateRole(RoleSaveDto roleSaveDto){
        // 1. 角色名校验
        KeycloakRoleInfoDto keycloakRole = keycloakUtil.fetchRoleById(roleSaveDto.getId());
        if (keycloakRole == null) {
            throw new BuzException("role.no.exist");
        }
        // 超级管理员角色不允许修改
        RoleEnum roleEnum = RoleEnum.parse(keycloakRole.getId());
        if (RoleEnum.SUPER_ADMIN == roleEnum) {
            throw new BuzException("role.super.update");
        }
        String roleName = roleSaveDto.getName();
        String denyRoleName = String.format("deny-%s", roleName);

        boolean result = true;
        // 2.更新资源
        // 2.1 更新接受资源
        String resourceName = String.format("%s-resource", roleName);
        JSONObject allowResource = keycloakUtil.fetchResource(resourceName);
        Set<String> allowResourceUris = new HashSet<>();
        if (CollectionUtils.isNotEmpty(roleSaveDto.getAllowResourceList())) {
            allowResourceUris.addAll(roleSaveDto.getAllowResourceList().stream().map(ResourceDto::getUri).collect(Collectors.toSet()));
        }
        allowResourceUris.addAll(DEFAULT_RESOURCE_URI);
        allowResource.put("uris", allowResourceUris);
        result &= keycloakUtil.updateResource(allowResource.getString("_id"), allowResource);
        // 2.2 更新拒绝资源
        String denyResourceName = String.format("%s-resource", denyRoleName);
        JSONObject denyResource = keycloakUtil.fetchResource(denyResourceName);
        Set<String> denyResourceUris = new HashSet<>();
        if (CollectionUtils.isNotEmpty(roleSaveDto.getDenyResourceList())) {
            denyResourceUris.addAll(roleSaveDto.getDenyResourceList().stream().map(ResourceDto::getUri).collect(Collectors.toSet()));
        }
        denyResource.put("uris", denyResourceUris);
        result &= keycloakUtil.updateResource(denyResource.getString("_id"), denyResource);


        RoleVo role = new RoleVo();
        role.setRoleId(keycloakRole.getId());
        role.setRoleName(roleName);
        role.setResourceList(roleSaveDto.getAllowResourceList());
        role.setDenyResourceList(roleSaveDto.getDenyResourceList());

        sendWebhook(role, ActionEnum.MODIFY);
        return result;
    }

    public boolean deleteRole(String id){
        KeycloakRoleInfoDto role = keycloakUtil.fetchRoleById(id);
        if (role != null) {
            // 超级管理员角色不允许删除
            RoleEnum roleEnum = RoleEnum.parse(role.getId());
            if (RoleEnum.SUPER_ADMIN == roleEnum) {
                throw new BuzException("role.super.delete");
            }
            String roleName = role.getName();
            String denyRoleName = String.format("deny-%s", roleName);
            // 删除权限
            String permissionName = String.format("%s-permission", roleName);
            keycloakUtil.deletePermission(permissionName);
            String denyPermissionName = String.format("%s-permission", denyRoleName);
            keycloakUtil.deletePermission(denyPermissionName);

            // 删除策略
            String policyName = String.format("%s-policy", roleName);
            keycloakUtil.deletePolicy(policyName);
            String denyPolicyName = String.format("%s-policy", denyRoleName);
            keycloakUtil.deletePolicy(denyPolicyName);

            // 删除资源
            String resourceName = String.format("%s-resource", roleName);
            keycloakUtil.deleteResource(resourceName);
            String denyResourceName = String.format("%s-resource", denyRoleName);
            keycloakUtil.deleteResource(denyResourceName);

            // 删除角色
            keycloakUtil.deleteRole(roleName);
            keycloakUtil.deleteRole(denyRoleName);

            RoleVo roleVo = new RoleVo();
            roleVo.setRoleId(role.getId());
            roleVo.setRoleName(roleName);

            sendWebhook(role, ActionEnum.DELETE);
        }

        return true;
    }

    public void sendWebhook(Object payload, ActionEnum action){
        ThreadUtil.execAsync(() -> {
            EventBus.publishEvent(
                    new SysEvent(this, ServiceEnum.AUTH_SERVICE, EventMetaEnum.ROLE_CHANGE,
                            action, payload));
        });
    }
}
