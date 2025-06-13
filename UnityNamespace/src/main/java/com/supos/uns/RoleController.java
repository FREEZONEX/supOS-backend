package com.supos.uns;

import com.supos.common.dto.auth.RoleSaveDto;
import com.supos.common.dto.auth.UpdateRoleDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.group.Create;
import com.supos.common.group.Update;
import com.supos.uns.service.RoleService;
import com.supos.uns.vo.RoleVo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 角色Controller
 * @date 2025/4/18 13:06
 */
@Slf4j
@RestController
public class RoleController {

    @Autowired
    private RoleService roleService;

    // 角色列表
    @GetMapping("/inter-api/supos/userManage/roleList")
    public ResultVO<List<RoleVo>> interRoleList(){
        return roleService.getRoleList();
    }

    /**
     * 创建角色
     * @param roleSaveDto
     * @return
     */
    @PostMapping("/inter-api/supos/role")
    public ResultVO<RoleVo> interCreateRole(@Validated(Create.class) @RequestBody RoleSaveDto roleSaveDto){
        return ResultVO.successWithData(roleService.createRole(roleSaveDto));
    }

    /**
     * 修改角色
     * @param roleSaveDto
     * @return
     */
    @PutMapping("/inter-api/supos/role")
    public ResultVO<RoleVo> interUpdateRole(@Validated(Update.class) @RequestBody RoleSaveDto roleSaveDto){
        return ResultVO.successWithData(roleService.updateRole(roleSaveDto));
    }

    /**
     * 删除角色
     * @param id
     * @return
     */
    @DeleteMapping("/inter-api/supos/role/{id}")
    public ResultVO<Boolean> interDeleteRole(@PathVariable("id") String id){
        return ResultVO.successWithData(roleService.deleteRole(id));
    }
}
