package com.supos.adpter.eventflow.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.eventflow.dao.po.NodeFlowPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EventFlowMapper extends BaseMapper<NodeFlowPO> {
    @Select("select * from supos_event_flows")
    List<NodeFlowPO> selectAll();
    @Select("select * from supos_event_flows where id=#{id}")
    NodeFlowPO getById(@Param("id") long id);

    @Select("<script>" +
            "select * from supos_event_flows where id in " +
            "<foreach collection=\"ids\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> selectByIds(@Param("ids") List<Long> ids);
    @Select("<script>" +
            "select * from supos_event_flows where flow_id in " +
            "<foreach collection=\"flowIds\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> selectByFlowIds(@Param("flowIds") List<String> flowIds);
    @Select("<script>" +
            "select * from supos_event_flows where flow_name in " +
            "<foreach collection=\"flowNames\" item=\"flowName\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{flowName} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> selectByFlowNames(@Param("flowNames") List<String> flowNames);
    @Select("select id, flow_id, flow_name from supos_event_flows where flow_name=#{flowName}")
    NodeFlowPO getByName(@Param("flowName") String flowName);

    @Select("<script> " +
            "select id, flow_id, flow_name, template, flow_status, description from supos_event_flows " +
            "where 1=1 " +
            "<if test='fuzzyName != null'>" +
            "  and (flow_name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%'))" +
            "</if>" +
            "order by create_time desc LIMIT #{pageSize} OFFSET (#{pageNo} - 1) * #{pageSize} " +
            "</script>"
    )
    List<NodeFlowPO> selectFlows(@Param("fuzzyName") String fuzzyName, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize);

    @Select("<script> " +
            "select count(*) from supos_event_flows " +
            "where 1=1 " +
            "<if test='fuzzyName != null'>" +
            "  and (flow_name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%')) " +
            "</if>" +
            "</script>"
    )
    int selectTotal(@Param("fuzzyName") String fuzzyName);

    @Insert("insert into supos_event_flows " +
            "(id, flow_id, flow_name, flow_data, flow_status, description) values " +
            "(#{id}, #{flowId}, #{flowName}, #{flowData}, #{flowStatus}, #{description})")
    int insert(NodeFlowPO flowPO);

    @Update("update supos_event_flows set " +
            "flow_id = #{flowId}, " +
            "flow_data = #{flowData}, " +
            "flow_status = #{flowStatus}, " +
            "update_time = now() " +
            "where id = #{id}")
    int deployUpdate(@Param("id") long id, @Param("flowId") String flowId, @Param("flowStatus") String flowStatus, @Param("flowData") String flowData);

    @Update("update supos_event_flows set " +
            "flow_data = #{flowData}, " +
            "flow_status = #{flowStatus}, " +
            "update_time = now() " +
            "where id = #{id}"
    )
    int saveFlowData(@Param("id") long id,  @Param("flowStatus") String flowStatus, @Param("flowData") String flowData);

    @Update("update supos_event_flows set " +
            "flow_name = #{flowName}, " +
            "description = #{description}, " +
            "update_time = now() " +
            "where id = #{id}")
    int updateBasicInfoById(@Param("id") long id, @Param("flowName") String flowName, @Param("description") String description);

    @Delete("delete from supos_event_flows where id = #{id}")
    int deleteById(@Param("id") long id);

    @Delete("<script>" +
            "delete from supos_event_flows where id in " +
            "<foreach collection=\"ids\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
