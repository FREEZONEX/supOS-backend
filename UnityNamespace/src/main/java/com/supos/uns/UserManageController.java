package com.supos.uns;

import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.auth.*;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.service.UserManageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class UserManageController {

    @Resource
    private UserManageService userManageService;

    /**
     * 用户管理列表
     */
    @Operation(summary = "用户管理列表",tags = "用户管理")
    @PostMapping({"/inter-api/supos/userManage/pageList"})
    public PageResultDTO<UserManageVo> pageList(@RequestBody UserQueryDto params){
        return userManageService.userManageList(params);
    }

    /**
     * 删除用户
     * @param id 用户ID
     * @return
     */
    @DeleteMapping({"/inter-api/supos/userManage/deleteById/{id}"})
    public ResultVO delete(@PathVariable String id){
        return userManageService.delete(id);
    }

    /**
     * 重置密码
     * @param updateUserDto
     * @return
     */
    @PutMapping({"/inter-api/supos/userManage/resetPwd"})
    public ResultVO resetPwd(@Valid @RequestBody UpdateUserDto updateUserDto){
        return userManageService.resetPwd(updateUserDto.getUserId(),updateUserDto.getPassword());
    }

    /**
     * 用户设置密码
     * @param resetPasswordDto
     * @return
     */
    @PutMapping("/inter-api/supos/userManage/userResetPwd")
    public ResultVO userResetPwd(@Valid @RequestBody ResetPasswordDto resetPasswordDto){
        return userManageService.userResetPwd(resetPasswordDto);
    }

    /**
     * 用户管理-用户设置
     * @param updateUserDto
     * @return
     */
    @PutMapping({"/inter-api/supos/userManage/updateUser"})
    public ResultVO updateUser(@Valid @RequestBody UpdateUserDto updateUserDto){
        return userManageService.updateUser(updateUserDto);
    }

    /**
     * 设置角色
     * @param updateRoleDto
     * @return
     */
    @PostMapping("/inter-api/supos/userManage/setRole")
    public ResultVO setRole(@Valid @RequestBody UpdateRoleDto updateRoleDto){
        return userManageService.setRole(updateRoleDto);
    }

    /**
     * 创建用户
     * @param addUserDto
     * @return
     */
    @PostMapping({"/inter-api/supos/userManage/createUser"})
    public ResultVO createUser(@Valid @RequestBody AddUserDto addUserDto){
        return userManageService.createUser(addUserDto);
    }

    /**
     * 设置tips开关
     * @param tipsEnable 0关 1开
     * @return
     */
    @PutMapping("/inter-api/supos/userManage/tipsEnable")
    public ResultVO setTipsEnable(@RequestParam int tipsEnable){
        return userManageService.setTipsEnable(tipsEnable);
    }

    /**
     * 设置用户首页
     * @param homePage
     * @return
     */
    @PutMapping("/inter-api/supos/userManage/homePage")
    public ResultVO setHomePage(@RequestParam String homePage){
        return userManageService.setHomePage(homePage);
    }

    @PutMapping("/inter-api/supos/userManage/phone")
    public ResultVO setPhone(@RequestParam String phone){
        return userManageService.setPhone(phone);
    }

    @PutMapping("/inter-api/supos/userManage/email")
    public ResultVO setEmail(@RequestParam String email){
        return userManageService.setEmail(email);
    }
}
