package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.dto.TodoQuery;
import com.supos.uns.dao.po.TodoPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TodoMapper extends BaseMapper<TodoPo> {

    @Update("update " + TodoPo.TABLE_NAME + " set status = #{status} ,handler_user_id = #{userId}, " +
            "handler_username = #{username} , handler_time = now() " +
            "where module_code = #{moduleCode} and business_id = #{businessId} and link = #{link}")
    int updateTodoStatus(@Param("moduleCode") String moduleCode, @Param("businessId")Long businessId,
                         @Param("status")int status, @Param("username")String username,
                         @Param("userId")String userId,@Param("link")String link);



    IPage<TodoPo> pageList(Page<?> page, TodoQuery params);

    @Select("SELECT module_code,module_name from supos_todo GROUP BY module_code,module_name")
    List<TodoPo> getModuleList();
}
