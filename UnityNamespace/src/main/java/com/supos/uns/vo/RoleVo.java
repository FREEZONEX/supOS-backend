package com.supos.uns.vo;

import com.supos.common.dto.auth.ResourceDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/26 10:17
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleVo {


    /**
     * 角色ID
     */
    private String roleId;

    /**
     * 角色名称
     */
    private String roleName;


    private List<ResourceDto> resourceList;

    private List<ResourceDto> denyResourceList;
}
