package com.supos.adpter.kong.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.kong.po.UserMenuPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author xinwangji@supos.com
 * @date 2024/12/10 10:32
 * @description
 */
@Mapper
public interface UserMenuMapper extends BaseMapper<UserMenuPo> {


    @Select("SELECT * FROM supos_user_menu where user_id = #{userId} and menu_name = #{menuName}")
    UserMenuPo getByMenuName(@Param("userId")String userId, @Param("menuName")String menuName);
}
