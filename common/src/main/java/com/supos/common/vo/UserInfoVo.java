package com.supos.common.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supos.common.dto.auth.ResourceDto;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.enums.RoleEnum;
import com.supos.common.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/19 15:55
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVo extends UserAttributeVo{


    /**
     * 用户的唯一标识符（用户ID）
     */
    private String sub;

    /**
     * 用户名
     */
    private String preferredUsername;

    /**
     * 邮箱
     */
    private String email;
    /**
     * 邮箱验证状态
     */
    @JSONField(name = "email_verified")
    private Boolean emailVerified;

    /**
     * 名字
     */
    private String firstName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 角色列表
     */
    private List<RoleDto> roleList;

    /**
     * 资源列表
     */
    private List<ResourceDto> resourceList;

    /**
     * 拒绝策略资源列表
     */
    private List<ResourceDto> denyResourceList;

    public UserInfoVo(String sub, String preferredUsername) {
        this.sub = sub;
        this.preferredUsername = preferredUsername;
    }

    public boolean isSuperAdmin(){
        if (CollectionUtils.isEmpty(this.roleList)){
            return false;
        }
        RoleDto roleDto = this.roleList.stream().filter(role -> RoleEnum.SUPER_ADMIN.getId().equals(role.getRoleId())).findFirst().orElse(null);
        return null != roleDto;
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
