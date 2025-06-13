package com.supos.uns.openapi;

import cn.hutool.core.bean.BeanUtil;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.auth.AddUserDto;
import com.supos.common.dto.auth.UpdateUserDto;
import com.supos.common.dto.auth.UserQueryDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.openapi.dto.UserCreateDto;
import com.supos.uns.openapi.dto.UserUpdateDto;
import com.supos.uns.openapi.dto.UserUpdatePasswordDto;
import com.supos.uns.service.UserManageService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 用户openapi
 * @date 2025/4/18 13:38
 */
@Slf4j
@RestController
@Hidden
public class UserOpenApi {

    @Autowired
    private UserManageService userManageService;

    /**
     * 用户管理列表
     * @param params
     * @return
     */
    @Operation(summary = "用户管理列表",tags = "openapi.tag.user.management")
    @PostMapping({"/open-api/supos/userManage/pageList"})
    public PageResultDTO<UserManageVo> openUserPageList(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户查询条件定义") @Valid @RequestBody UserQueryDto params){
        return userManageService.userManageList(params);
    }

    /**
     * 创建用户
     * @param userCreateDto
     * @return
     */
    @Operation(summary = "创建用户",tags = "openapi.tag.user.management")
    @PostMapping({"/open-api/supos/userManage/user"})
    public ResultVO openCreateUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "创建用户定义") @Valid @RequestBody UserCreateDto userCreateDto){
        AddUserDto dto = BeanUtil.copyProperties(userCreateDto, AddUserDto.class);
        return userManageService.createUser(dto);
    }

    /**
     * 修改
     * @param userUpdateDto
     * @return
     */
    @Operation(summary = "修改用户",tags = "openapi.tag.user.management")
    @PutMapping({"/open-api/supos/userManage/user"})
    public ResultVO openUpdateUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "修改用户定义") @Valid @RequestBody UserUpdateDto userUpdateDto){
        UpdateUserDto dto = BeanUtil.copyProperties(userUpdateDto, UpdateUserDto.class);
        return userManageService.updateUserByUserName(dto);
    }

    /**
     * 删除用户
     * @param username 用户名称
     * @return
     */
    @Operation(summary = "删除用户",tags = "openapi.tag.user.management")
    @DeleteMapping({"/open-api/supos/userManage/user/{username}"})
    public ResultVO openUserDelete(@PathVariable("username") String username){
        return userManageService.deleteByUserName(username);
    }

    /**
     * 重置密码
     * @param userUpdatePasswordDto
     * @return
     */
    @Operation(summary = "重置密码",tags = "openapi.tag.user.management")
    @PutMapping({"/open-api/supos/userManage/resetPwd"})
    public ResultVO openUserResetPwd(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "重置密码定义") @Valid @RequestBody UserUpdatePasswordDto userUpdatePasswordDto){
        return userManageService.resetPwdByUserName(userUpdatePasswordDto.getUsername(),userUpdatePasswordDto.getPassword());
    }
}
