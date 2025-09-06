package com.supos.uns.service;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.UserAttributeDto;
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
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserAttributeVo;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.mapper.UserManageMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/27 20:54
 * @description
 */
@Service
public class UserManageService {

    @Resource
    private UserManageMapper userMapper;
    @Resource
    private KeycloakUtil keycloakUtil;
    @Resource
    private OAuthKeyCloakConfig keyCloakConfig;
    @Resource
    private RoleService roleService;
    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;
    @Resource
    private SystemConfig systemConfig;


    public PageResultDTO<UserManageVo> userManageList(UserQueryDto params) {
        Page<UserManageVo> page = new Page<>(params.getPageNo(), params.getPageSize());
        // 超级管理员特殊处理
        if (StringUtils.isNotBlank(params.getRoleName())) {
            if (RoleEnum.SUPER_ADMIN.getComment().equals(params.getRoleName()) || RoleEnum.SUPER_ADMIN.getName().equals(params.getRoleName())) {
                params.setRoleName(RoleEnum.SUPER_ADMIN.getName());
            }
        }
        IPage<UserManageVo> iPage = userMapper.userManageList(page, keyCloakConfig.getRealm(), params.getPreferredUsername(),
                params.getFirstName(), params.getEmail(), params.getEnabled(), params.getRoleName());
        iPage.getRecords().forEach(user -> {
            List<RoleDto> roleList = roleService.getRoleListByUserId(user.getId());
            List<UserAttributeDto> attrList = userMapper.getUserAttribute(user.getId());
            if (CollectionUtil.isNotEmpty(attrList)) {
                Map<String, Object> attrMap = attrList.stream().collect(Collectors.toMap(UserAttributeDto::getName, UserAttributeDto::getValue));
                BeanUtil.copyProperties(attrMap, user, CopyOptions.create().ignoreNullValue());
            }
            if (CollectionUtil.isNotEmpty(roleList)) {
                roleList = roleList.stream()
                        .filter(r -> !RoleEnum.IGNORE_ROLE_ID.contains(r.getRoleId()) && !RoleEnum.IGNORE_ROLE_NAME.contains(r.getRoleName()) && !r.getRoleName().startsWith("deny-"))
                        .peek(r -> {
                            RoleEnum roleEnum = RoleEnum.parse(r.getRoleId());
                            if (roleEnum != null) {
                                r.setRoleName(I18nUtils.getMessage(roleEnum.getI18nCode()));
                            } else {
                                r.setRoleName(r.getRoleDescription());
                            }
                        }).collect(Collectors.toList());
                user.setRoleList(roleList);
            }
        });
        PageResultDTO.PageResultDTOBuilder<UserManageVo> pageBuilder = PageResultDTO.<UserManageVo>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(0).data(iPage.getRecords()).build();
    }

    public ResultVO delete(String id) {
        UserManageVo userManageVo = userMapper.getById(id);
        if (userManageVo != null){
            sendWebhook(userManageVo,ActionEnum.DELETE);
        }
        return new ResultVO(keycloakUtil.deleteUser(id));
    }

    public ResultVO deleteByUserName(String username) {
        if (StringUtils.equals("supos", username)) {
            throw new BuzException(I18nUtils.getMessage("user.supos.delete"));
        }
        KeycloakUserInfoDto user = keycloakUtil.fetchUser(username);
        if (user != null) {
            sendWebhook(user,ActionEnum.DELETE);
            return delete(user.getId());
        }
        sendWebhook(user,ActionEnum.DELETE);
        return new ResultVO(true);
    }

    public ResultVO resetPwd(String userId, String password) {
        boolean result = keycloakUtil.resetPwd(userId, password);

        sendWebhook(wrapUserInfo(userId), ActionEnum.MODIFY);
        return new ResultVO(result);
    }

    public ResultVO resetPwdByUserName(String username, String password) {
        KeycloakUserInfoDto user = keycloakUtil.fetchUser(username);
        if (user == null) {
            throw new BuzException("user.not.exist");
        }
        boolean result = keycloakUtil.resetPwd(user.getId(), password);
        sendWebhook(wrapUserInfo(user.getId()), ActionEnum.MODIFY);
        return new ResultVO(result);
    }

    public ResultVO userResetPwd(ResetPasswordDto resetPasswordDto) {
        AccessTokenDto accessTokenDto = keycloakUtil.login(resetPasswordDto.getUsername(), resetPasswordDto.getPassword());
        if (null == accessTokenDto) {
            return ResultVO.fail(I18nUtils.getMessage("user.login.password.error"));
        }
        boolean result = keycloakUtil.resetPwd(resetPasswordDto.getUserId(), resetPasswordDto.getNewPassword());
        sendWebhook(wrapUserInfo(resetPasswordDto.getUserId()), ActionEnum.MODIFY);
        return new ResultVO(result);
    }

    public ResultVO updateUser(UpdateUserDto updateUserDto) {
        String userId = updateUserDto.getUserId();
        UserManageVo userManageVo = userMapper.getById(userId);
        if (userManageVo == null) {
            return ResultVO.fail(I18nUtils.getMessage("user.not.exist"));
        }
        JSONObject params = new JSONObject();
        params.put("firstName", userManageVo.getFirstName());
        params.put("enabled", userManageVo.getEnabled());
        params.put("email", userManageVo.getEmail());
        if (ObjectUtil.isNotNull(updateUserDto.getEnabled())) {
            params.put("enabled", updateUserDto.getEnabled());
        }
        if (ObjectUtil.isNotNull(updateUserDto.getFirstName())) {
            params.put("firstName", updateUserDto.getFirstName());
        }
        if (ObjectUtil.isNotNull(updateUserDto.getEmail())) {
            KeycloakUserInfoDto user = keycloakUtil.fetchUserByEmail(updateUserDto.getEmail());
            if (user != null && !user.getId().equals(userId)) {
                return ResultVO.fail(I18nUtils.getMessage("user.email.already.exists"));
            }
            params.put("email", updateUserDto.getEmail());
        }
        UserAttributeVo userAttribute = getUserAttributeById(userId);
        if (userAttribute == null) {
            userAttribute = new UserAttributeVo();
        }
        if (StringUtils.isNotBlank(updateUserDto.getPhone())) {
            userAttribute.setPhone(updateUserDto.getPhone());
        }
        if (StringUtils.isNotBlank(updateUserDto.getHomePage())) {
            userAttribute.setHomePage(updateUserDto.getHomePage());
        }
        if (updateUserDto.getTipsEnable() != null) {
            userAttribute.setTipsEnable(updateUserDto.getTipsEnable());
        }
        if (updateUserDto.getFirstTimeLogin() != null) {
            userAttribute.setFirstTimeLogin(updateUserDto.getFirstTimeLogin());
        }
        if (StringUtils.isNotBlank(updateUserDto.getSource())) {
            userAttribute.setSource(updateUserDto.getSource());
        }
        params.put("attributes", userAttribute);
        boolean isUpdate = keycloakUtil.updateUser(userId, params);
        if (!isUpdate) {
            return ResultVO.fail(I18nUtils.getMessage("user.update.failed"));
        }

        List<RoleDto> roleList = updateUserDto.getRoleList();

        if (CollectionUtil.isNotEmpty(roleList)) {

            //先删除原有角色
            List<RoleDto> currentRoles = userMapper.roleListByUserId(keyCloakConfig.getRealm(), userId);
            if (CollectionUtil.isNotEmpty(currentRoles)) {
                UpdateRoleDto updateRole = new UpdateRoleDto();
                updateRole.setUserId(userId);
                updateRole.setRoleList(currentRoles);
                updateRole.setType(2);//取消角色
                setRole(updateRole);
            }

            //默认角色标记  for ldap
            if (Boolean.TRUE.equals(systemConfig.getLdapEnable())){
                RoleDto ldapInitialized = new RoleDto("c5921f89-9745-4c8a-9c69-b3015c94f2ea","ldap-initialized");
                roleList.add(ldapInitialized);
            }

            //设置新的角色
            UpdateRoleDto updateRole = new UpdateRoleDto();
            updateRole.setUserId(userId);
            updateRole.setRoleList(roleList);
            updateRole.setType(1);//设置角色
            setRole(updateRole);
        } else {
            if (Boolean.TRUE.equals(updateUserDto.getOperateRole())) {
                List<RoleDto> currentRoles = roleService.getRoleListByUserId(userId);
                if (CollectionUtil.isNotEmpty(currentRoles)) {
                    UpdateRoleDto updateRole = new UpdateRoleDto();
                    updateRole.setUserId(userId);
                    updateRole.setRoleList(currentRoles);
                    updateRole.setType(2);//取消角色
                    setRole(updateRole);
                }
            }
        }
        UserInfoVo userCache = userInfoCache.get(userId);
        //flush cache
        if (userCache != null){
            CopyOptions copyOptions = CopyOptions.create().ignoreError().ignoreNullValue();
            BeanUtil.copyProperties(userAttribute,userCache,copyOptions);
            BeanUtil.copyProperties(params,userCache,copyOptions);
            userInfoCache.put(userId,userCache);
        }
        sendWebhook(updateUserDto,ActionEnum.MODIFY);
        return ResultVO.success("ok");
    }

    public ResultVO updateUserByUserName(UpdateUserDto updateUserDto) {
        KeycloakUserInfoDto user = keycloakUtil.fetchUser(updateUserDto.getUsername());
        if (user == null) {
            throw new BuzException("user.not.exist");
        }
        updateUserDto.setUserId(user.getId());
        sendWebhook(updateUserDto,ActionEnum.MODIFY);
        return updateUser(updateUserDto);
    }

    public ResultVO setRole(UpdateRoleDto updateRoleDto) {
        JSONArray array = new JSONArray();
        if (CollectionUtil.isNotEmpty(updateRoleDto.getRoleList())) {
            for (RoleDto roleDto : updateRoleDto.getRoleList()) {
                KeycloakRoleInfoDto keycloakRole = null;

                if (roleDto.getRoleId() != null) {
                    keycloakRole = keycloakUtil.fetchRoleById(roleDto.getRoleId());
                } else if (roleDto.getRoleName() != null) {
                    keycloakRole = keycloakUtil.fetchRole(roleDto.getRoleName());
                    if (null == keycloakRole
                            && (RoleEnum.SUPER_ADMIN.getComment().equals(roleDto.getRoleName()) || RoleEnum.ADMIN.getName().equals(roleDto.getRoleName()))) {
                        keycloakRole = keycloakUtil.fetchRoleById(RoleEnum.SUPER_ADMIN.getId());
                    }
                }

                if (null == keycloakRole) {
                    continue;
                }

                if (RoleEnum.SUPER_ADMIN != RoleEnum.parse(keycloakRole.getId())) {
                    // 非超级管理员角色需要对deny角色进行处理
                    KeycloakRoleInfoDto denyKeycloakRole = keycloakUtil.fetchRole(String.format("deny-%s", keycloakRole.getName()));
                    if (null != denyKeycloakRole) {
                        // deny角色处理
                        JSONObject role = new JSONObject();
                        role.put("id", denyKeycloakRole.getId());
                        role.put("name", denyKeycloakRole.getName());
                        array.add(role);
                    }
                }
                JSONObject role = new JSONObject();
                role.put("id", keycloakRole.getId());
                role.put("name", keycloakRole.getName());
                array.add(role);
            }
        }
        boolean result = keycloakUtil.setRole(updateRoleDto.getUserId(), updateRoleDto.getType(), array);
        sendWebhook(wrapUserInfo(updateRoleDto.getUserId()), ActionEnum.MODIFY);
        return new ResultVO(result);
    }

    public ResultVO createUser(AddUserDto addUserDto) {
        KeycloakCreateUserDto createUserDto = BeanUtil.copyProperties(addUserDto, KeycloakCreateUserDto.class);
        if (StringUtils.isNotBlank(addUserDto.getEmail())) {
            KeycloakUserInfoDto user = keycloakUtil.fetchUserByEmail(addUserDto.getEmail());
            if (user != null) {
                return ResultVO.fail(I18nUtils.getMessage("user.email.already.exists"));
            }
        }
        UserAttributeVo userAttribute = new UserAttributeVo();
        if (StringUtils.isNotBlank(addUserDto.getPhone())){
            userAttribute.setPhone(addUserDto.getPhone());
        }

        if (StringUtils.isNotBlank(addUserDto.getSource())){
            userAttribute.setSource(addUserDto.getSource());
        }

        createUserDto.setAttributes(userAttribute);
        String userId = keycloakUtil.createUser(JSONObject.toJSONString(createUserDto));
        addUserDto.setId(userId);
        boolean isReset = keycloakUtil.resetPwd(userId, addUserDto.getPassword());
        if (!isReset) {
            return ResultVO.fail(I18nUtils.getMessage("user.set.password.failed"));
        }
        UpdateRoleDto updateRoleDto = new UpdateRoleDto();
        updateRoleDto.setUserId(userId);
        updateRoleDto.setType(1);
        updateRoleDto.setRoleList(addUserDto.getRoleList());
        sendWebhook(createUserDto,ActionEnum.ADD);
        return setRole(updateRoleDto);
    }

    public ResultVO setEmail(String email) {
        UserInfoVo userInfoVo = UserContext.get();
        UpdateUserDto updateUserDto = new UpdateUserDto();
        updateUserDto.setUserId(userInfoVo.getSub());
        updateUserDto.setEmail(email);
        ResultVO resultVO = updateUser(updateUserDto);
        if (resultVO.getCode() != 200) {
            return resultVO;
        }
        userInfoVo.setEmail(email);
        userInfoCache.put(userInfoVo.getSub(), userInfoVo);
        return ResultVO.success("ok");
    }

    public ResultVO setPhone(String phone) {
        UserInfoVo userInfoVo = UserContext.get();
        UpdateUserDto updateUserDto = new UpdateUserDto();
        updateUserDto.setUserId(userInfoVo.getSub());
        updateUserDto.setPhone(phone);
        ResultVO resultVO = updateUser(updateUserDto);
        if (resultVO.getCode() != 200) {
            return resultVO;
        }
        userInfoVo.setEmail(phone);
        userInfoCache.put(userInfoVo.getSub(), userInfoVo);
        return ResultVO.success("ok");
    }

    public ResultVO setTipsEnable(int tipsEnable) {
        UserInfoVo userInfoVo = UserContext.get();
        UpdateUserDto updateUserDto = new UpdateUserDto();
        updateUserDto.setUserId(userInfoVo.getSub());
        updateUserDto.setTipsEnable(tipsEnable);
        ResultVO resultVO = updateUser(updateUserDto);
        if (resultVO.getCode() != 200) {
            return resultVO;
        }
        userInfoVo.setTipsEnable(tipsEnable);
        userInfoCache.put(userInfoVo.getSub(), userInfoVo);
        return ResultVO.success("ok");
    }


    public ResultVO setHomePage(String homePage) {
        UserInfoVo userInfoVo = UserContext.get();
        UpdateUserDto updateUserDto = new UpdateUserDto();
        updateUserDto.setUserId(userInfoVo.getSub());
        updateUserDto.setHomePage(homePage);
        ResultVO resultVO = updateUser(updateUserDto);
        if (resultVO.getCode() != 200) {
            return resultVO;
        }
        userInfoVo.setHomePage(homePage);
        userInfoCache.put(userInfoVo.getSub(), userInfoVo);
        return ResultVO.success("ok");
    }


    public List<UserManageVo> getUserList() {
        return userMapper.userList();
    }

    private UserAttributeVo getUserAttributeById(String userId) {
        List<UserAttributeDto> attrList = userMapper.getUserAttribute(userId);
        if (CollectionUtil.isNotEmpty(attrList)) {
            Map<String, Object> attrMap = attrList.stream().collect(Collectors.toMap(UserAttributeDto::getName, UserAttributeDto::getValue));
            return BeanUtil.toBean(attrMap, UserAttributeVo.class);
        }
        return null;
    }

    private UpdateUserDto wrapUserInfo(String userId) {
        UserManageVo userManageVo = userMapper.getById(userId);
        UserAttributeVo userAttribute = getUserAttributeById(userId);

        UpdateUserDto updateUserDto = new UpdateUserDto();
        updateUserDto.setUserId(userId);
        if (userManageVo != null) {
            updateUserDto.setUsername(userManageVo.getPreferredUsername());
            updateUserDto.setFirstName(userManageVo.getFirstName());
            updateUserDto.setEmail(userManageVo.getEmail());
        }

        if (userAttribute != null) {
            updateUserDto.setPhone(userAttribute.getPhone());
        }

        List<RoleDto> currentRoles = userMapper.roleListByUserId(keyCloakConfig.getRealm(), userId);
        if (CollectionUtil.isNotEmpty(currentRoles)) {
            updateUserDto.setRoleList(currentRoles);
        }
        return updateUserDto;
    }

    public void sendWebhook(Object payload,ActionEnum action){
        ThreadUtil.execAsync(() -> {
            EventBus.publishEvent(
                new SysEvent(this, ServiceEnum.AUTH_SERVICE, EventMetaEnum.USER_CHANGE,
                            action, payload));
        });
    }
}
