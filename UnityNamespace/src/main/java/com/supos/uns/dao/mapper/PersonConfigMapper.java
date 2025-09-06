package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.uns.dao.po.PersonConfigPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PersonConfigMapper extends BaseMapper<PersonConfigPo> {


    @Select("SELECT * FROM " + PersonConfigPo.TABLE_NAME + " WHERE user_id = #{userId} ")
    PersonConfigPo getByUserId(@Param("userId") String userId);

    //INSERT INTO users (user_id, name)
//VALUES (#{userId}, #{name})
//ON CONFLICT (user_id) DO UPDATE SET name=EXCLUDED.name
    @Update("<script> INSERT INTO " + PersonConfigPo.TABLE_NAME + " (user_id, main_language) VALUES (#{userId}, #{mainLanguage}) " +
            " ON CONFLICT (user_id) DO  UPDATE SET main_language=#{mainLanguage} </script>")
    int updateByUserId(@Param("userId") String userId, @Param("mainLanguage") String mainLanguage);

}
