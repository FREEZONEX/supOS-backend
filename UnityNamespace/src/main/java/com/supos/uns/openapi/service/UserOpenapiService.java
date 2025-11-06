package com.supos.uns.openapi.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.UserAttributeDto;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.enums.RoleEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.UserManageMapper;
import com.supos.uns.openapi.dto.RoleOpenDto;
import com.supos.uns.openapi.dto.UserDetailDto;
import com.supos.uns.openapi.dto.UserPageQueryDto;
import com.supos.uns.openapi.vo.UserDetailVo;
import com.supos.uns.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserOpenapiService {

    @Autowired
    private UserManageMapper userMapper;
    @Autowired
    private RoleService roleService;


    public PageResultDTO<UserDetailVo> userManageList(UserPageQueryDto params) {
        Page<UserDetailVo> page = new Page<>(params.getPageNo(), params.getPageSize());
        IPage<UserDetailDto> iPage = userMapper.userOpenapiList(page, params);
        List<UserDetailVo> userList = new ArrayList<>(iPage.getRecords().size());
        iPage.getRecords().forEach(userDetail -> {
            UserDetailVo user = BeanUtil.copyProperties(userDetail, UserDetailVo.class);
            user.setEnable(userDetail.getEnabled());
            List<RoleDto> roleList = roleService.getRoleListByUserId(user.getId());
            List<UserAttributeDto> attrList = userMapper.getUserAttribute(user.getId());
            if (CollectionUtil.isNotEmpty(attrList)) {
                Map<String, Object> attrMap = attrList.stream().collect(Collectors.toMap(UserAttributeDto::getName, UserAttributeDto::getValue));
                BeanUtil.copyProperties(attrMap, user, CopyOptions.create().ignoreNullValue());
            }
            if (CollectionUtil.isNotEmpty(roleList)) {
                List<RoleOpenDto> roleOpenList = roleList.stream()
                        .filter(r -> !RoleEnum.IGNORE_ROLE_ID.contains(r.getRoleId()) && !RoleEnum.IGNORE_ROLE_NAME.contains(r.getRoleName()) && !r.getRoleName().startsWith("deny-"))
                        .map(r -> {
                            RoleEnum roleEnum = RoleEnum.parse(r.getRoleId());
                            if (roleEnum != null) {
                                r.setRoleName(I18nUtils.getMessage(roleEnum.getI18nCode()));
                            } else {
                                r.setRoleName(r.getRoleDescription());
                            }
                            RoleOpenDto role = BeanUtil.copyProperties(r, RoleOpenDto.class);
                            return role;
                        }).collect(Collectors.toList());
                user.setRoleList(roleOpenList);
            }
            userList.add(user);
        });
        PageResultDTO.PageResultDTOBuilder<UserDetailVo> pageBuilder = PageResultDTO.<UserDetailVo>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(200).data(userList).build();
    }


}
