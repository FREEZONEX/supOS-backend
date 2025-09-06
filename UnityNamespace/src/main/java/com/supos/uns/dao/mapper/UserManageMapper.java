package com.supos.uns.dao.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.dto.UserAttributeDto;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.vo.UserAttributeVo;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.UserPageQueryDto;
import com.supos.uns.openapi.vo.UserDetailVo;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/27 20:59
 * @description
 */
@DS("keycloak")
@Mapper
public interface UserManageMapper extends BaseMapper {

    IPage<UserManageVo> userManageList(Page<?> page,@Param("realm") String realm, @Param("preferredUsername") String preferredUsername,
                                       @Param("firstName") String firstName, @Param("email") String email, @Param("enabled") Boolean enabled,
                                       @Param("roleName") String roleName);

    /**
     * 通过用户名获取用户的角色列表
     * @param realm
     * @param userId
     * @return
     */
    List<RoleDto> roleListByUserId(@Param("realm") String realm, @Param("userId") String userId);

    List<UserManageVo> userList();

    @Select("select ue.*,ue.username AS preferredUsername from user_entity ue where ue.id =#{userId}")
    UserManageVo getById(@Param("userId") String userId);

    @Select("select ue.id,ue.username AS preferredUsername from user_entity ue where ue.username =#{username} and ue.realm_id = '8920b375-d705-4d30-8a71-52d9c14ec4ba'")
    UserManageVo getByUsername(@Param("username") String username);



    @Select("<script> select ue.id,ue.username AS preferredUsername from user_entity ue " +
            " where ue.id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> " +
            "</script>")
    List<UserManageVo> listUserById(@Param("ids") List<String> ids);

    @Select("<script> select ue.*,ue.username AS preferredUsername from user_entity ue " +
            " where ue.username in " +
            "  <foreach collection=\"usernameList\" item=\"username\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{username}" +
            "  </foreach> " +
            "</script>")
    List<UserManageVo> listUserByUsernames(@Param("usernameList") List<String> usernameList);

    @Select("select name,value from user_attribute where user_id = #{userId}")
    List<UserAttributeDto> getUserAttribute(@Param("userId") String userId);

    @Select("select value from user_attribute where name = 'phone' and user_id = #{userId} limit 1")
    String getUserPhoneById(@Param("userId") String userId);


    @Select("<script> SELECT a.value FROM user_attribute a LEFT JOIN user_entity b on a.user_id = b.id where a.name = 'phone'  " +
            " and b.username in  " +
            "  <foreach collection=\"usernameList\" item=\"username\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{username}" +
            "  </foreach> " +
            "</script>")
    List<String> getUsersPhoneByUsernames(@Param("usernameList") List<String> usernameList);


    List<UserManageVo> getNotInitializedLdapUsers();


    IPage<UserDetailVo> userOpenapiList(Page<?> page, @Param("params")UserPageQueryDto params);
}

