package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.po.AlarmHandlerPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlarmHandlerMapper extends BaseMapper<AlarmHandlerPo> {


    @Insert("<script> INSERT INTO uns_alarms_handler (uns_id,user_id,username,create_at) VALUES" +
            " <foreach collection=\"beans\" item=\"db\" separator=\",\">" +
            "     (#{unsId},#{db.id},#{db.preferredUsername},CURRENT_TIMESTAMP)" +
            " </foreach> " +
            "</script>")
    int saveBatch(@Param("unsId") Long unsId, @Param("beans") List<UserManageVo> list);

    @Select("select * from uns_alarms_handler where uns_id = #{unsId}")
    List<AlarmHandlerPo> getByUnsId(@Param("unsId") Long unsId);
}
