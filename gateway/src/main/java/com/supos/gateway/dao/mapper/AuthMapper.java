package com.supos.gateway.dao.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.dto.UserAttributeDto;
import com.supos.common.dto.auth.ResourceDto;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@DS("keycloak")
@Mapper
public interface AuthMapper extends BaseMapper {


    /**
     * 通过用户名获取用户的角色列表
     * @param realm
     * @param userId
     * @return
     */
    List<RoleDto> roleListByUserId(@Param("realm") String realm, @Param("userId") String userId);

    /**
     * 通过组合角色ID获取子角色列表
     * @param compositeRoleIds
     * @return
     */
    List<RoleDto> getChildRoleListByCompositeRoleId(@Param("compositeRoleIds") List<String> compositeRoleIds);


    /**
     * 通过roleId查询策略ID集合
     * @param roleId
     * @return
     */
    List<String> getPolicyIdsByRoleId(@Param("roleId") String roleId);

    /**
     * t通过策略ID集合获取资源集合
     * @param policyIds
     * @return
     */
    List<ResourceDto> getResourceListByPolicyIds(@Param("policyIds") List<String> policyIds);

    @Select("SELECT id FROM keycloak_role where name = 'supos-default' LIMIT 1")
    String getDefaultRoleId();

    @Select("select name,value from user_attribute where user_id = #{userId}")
    List<UserAttributeDto> getUserAttribute(@Param("userId") String userId);

    @Select("select ue.*,ue.id AS sub,ue.username AS preferredUsername from user_entity ue where ue.id =#{userId}")
    UserInfoVo getById(@Param("userId") String userId);
}
