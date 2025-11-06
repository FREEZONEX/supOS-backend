package com.supos.adpter.nodered.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface NodeFlowModelMapper extends BaseMapper<NodeFlowModelPO> {

    @Select("<script>" +
            "select parent_id, alias from supos_node_flow_models where alias in " +
            "<foreach collection=\"aliases\" item=\"alias\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{alias} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowModelPO> selectByAliases(@Param("aliases") Collection<String> aliases);

    @Select("select parent_id, alias from supos_node_flow_models")
    List<NodeFlowModelPO> selectAll();

    @Select("select * from supos_node_flow_models where alias=#{alias} order by create_time desc LIMIT 1")
    NodeFlowModelPO queryLatestByAlias(@Param("alias") String alias);

    @Select("<script>" +
            "select * from supos_node_flow_models where parent_id in " +
            "<foreach collection=\"parentIds\" item=\"parentId\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{parentId} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowModelPO> selectByParentIds(@Param("parentIds") Collection<Long> parentIds);

    @Insert("<script>" +
            "insert into supos_node_flow_models " +
            "(parent_id, alias, topic) values " +
            "<foreach collection=\"flowModels\" item=\"fm\" separator=\",\">" +
            "      (#{fm.parentId}, #{fm.alias}, #{fm.topic}) " +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("flowModels") Collection<NodeFlowModelPO> flowModels);

    @Delete("delete from supos_node_flow_models where parent_id=#{parentId} ")
    int deleteById(@Param("parentId") long parentId);

    @Delete("<script>" +
            "delete from supos_node_flow_models where alias in " +
            "<foreach collection=\"aliases\" item=\"alias\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{alias} " +
            "</foreach>" +
            "</script>")
    int deleteByAliases(@Param("aliases") Collection<String> aliases);
}
