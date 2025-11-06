package com.supos.adpter.nodered.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowExtendsPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface NodeFlowMapper extends BaseMapper<NodeFlowPO> {

    @Select("select * from supos_node_flows where template=#{templateName} ")
    List<NodeFlowPO> selectAll(@Param("templateName") String templateName);

    @Select("select * from supos_node_flows where id=#{id}")
    NodeFlowPO getById(@Param("id") long id);

    @Select("<script>" +
            "select id,flow_id,flow_name,flow_status,description,template  from supos_node_flows where id in " +
            "<foreach collection=\"ids\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> queryByIds(@Param("ids") Collection<Long> ids);

    @Select("<script>" +
            "select * from supos_node_flows where flow_id in " +
            "<foreach collection=\"flowIds\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> selectByFlowIds(@Param("flowIds") List<String> flowIds);
    @Select("<script>" +
            "select * from supos_node_flows where flow_name in " +
            "<foreach collection=\"flowNames\" item=\"flowName\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{flowName} " +
            "</foreach>" +
            "</script>")
    List<NodeFlowPO> selectByFlowNames(@Param("flowNames") List<String> flowNames);
    @Select("select id, flow_id, flow_name from supos_node_flows where flow_name=#{flowName} and template=#{templateName}")
    NodeFlowPO getByName(@Param("flowName") String flowName, @Param("templateName") String templateName);

    @Select("<script> " +
            "SELECT a.*, b.mark, b.mark_time  " +
            "FROM supos_node_flows a " +
            "LEFT JOIN supos_node_flow_top_recodes b ON a.id = b.id and b.user_id=#{userId} " +
            "where template=#{templateName} " +
            "<if test='fuzzyName != null'>" +
            "  and (flow_name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%'))" +
            "</if>" +
            " order by b.mark asc, " +
            "<if test='orderCode == null'>" +
                " b.mark_time desc, a.create_time desc " +
            "</if>" +
            "<if test='orderCode != null'>" +
                " ${orderCode} ${descOrAsc}  " +
            "</if>" +

            "LIMIT #{pageSize} OFFSET (#{pageNo} - 1) * #{pageSize} " +
            "</script>"
    )
    List<NodeFlowExtendsPO> selectFlows(@Param("userId") String userId,
                                        @Param("fuzzyName") String fuzzyName,
                                        @Param("templateName") String templateName,
                                        @Param("orderCode") String orderCode,
                                        @Param("descOrAsc") String descOrAsc,
                                        @Param("pageNo") int pageNo,
                                        @Param("pageSize") int pageSize);

    @Select("<script> " +
            "select count(*) from supos_node_flows " +
            "where template=#{templateName} " +
            "<if test='fuzzyName != null'>" +
            "  and (flow_name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%')) " +
            "</if>" +
            "</script>"
    )
    int selectTotal(@Param("fuzzyName") String fuzzyName, @Param("templateName") String templateName);

    @Insert("insert into supos_node_flows " +
            "(id, flow_id, flow_name, flow_data, flow_status, description, template, creator) values " +
            "(#{id}, #{flowId}, #{flowName}, #{flowData}, #{flowStatus}, #{description}, #{template}, #{creator})")
    int insert(NodeFlowPO flowPO);

    @Update("update supos_node_flows set " +
            "flow_id = #{flowId}, " +
            "flow_data = #{flowData}, " +
            "flow_status = #{flowStatus}, " +
            "update_time = now() " +
            "where id = #{id}")
    int deployUpdate(@Param("id") long id, @Param("flowId") String flowId, @Param("flowStatus") String flowStatus, @Param("flowData") String flowData);

    @Update("update supos_node_flows set " +
            "flow_data = #{flowData}, " +
            "flow_status = #{flowStatus}, " +
            "update_time = now() " +
            "where id = #{id}"
    )
    int saveFlowData(@Param("id") long id,  @Param("flowStatus") String flowStatus, @Param("flowData") String flowData);

    @Update("update supos_node_flows set " +
            "flow_name = #{flowName}, " +
            "description = #{description}, " +
            "update_time = now() " +
            "where id = #{id}")
    int updateBasicInfoById(@Param("id") long id, @Param("flowName") String flowName, @Param("description") String description);

    @Delete("delete from supos_node_flows where id = #{id}")
    int deleteById(@Param("id") long id);

    @Delete("<script>" +
            "delete from supos_node_flows where id in " +
            "<foreach collection=\"ids\" item=\"id\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{id} " +
            "</foreach>" +
            "</script>")
    void batchDeleteByIds(@Param("ids") Collection<Long> ids);

}
