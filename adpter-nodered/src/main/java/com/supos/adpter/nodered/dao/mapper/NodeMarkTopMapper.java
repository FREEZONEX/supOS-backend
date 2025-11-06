package com.supos.adpter.nodered.dao.mapper;

import com.supos.adpter.nodered.dao.po.NodeMarkTopPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NodeMarkTopMapper {

    @Insert("insert into supos_node_flow_top_recodes " +
            "(id, user_id) values " +
            "(#{id}, #{userId})")
    int insert(NodeMarkTopPO flowMarkPO);

    @Delete("delete from supos_node_flow_top_recodes where id=#{id} and user_id=#{userId}")
    int delete(@Param("id") long id, @Param("userId") String userId);

    @Delete("delete from supos_node_flow_top_recodes where id=#{id}")
    int deleteById(@Param("id") long id);
}
