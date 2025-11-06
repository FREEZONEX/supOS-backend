package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.dao.po.NodeMarkTopPO;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.uns.dao.po.DashboardExtendsPo;
import com.supos.uns.dao.po.DashboardMarkTopPO;
import com.supos.uns.dao.po.DashboardPo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DashboardMapper extends BaseMapper<DashboardPo> {


    @Insert("<script> INSERT INTO uns_dashboard (id,name,description,update_time,create_time) VALUES" +
            " <foreach collection=\"beans\" item=\"db\" separator=\",\">" +
            "     (#{db.id},#{db.name},#{db.description},CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)" +
            " </foreach> ON CONFLICT (id) DO NOTHING " +
            "</script>")
    int saveOrIgnoreBatch(@Param("beans") List<DashboardDto> dashboardDtoList);

    @Select("<script>" +
            "select * from uns_dashboard where name in " +
            "<foreach collection=\"names\" item=\"dsName\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{dsName} " +
            "</foreach>" +
            "</script>")
    List<DashboardPo> selectByFlowNames(@Param("names") List<String> names);

    @Select("<script> " +
            "SELECT a.*, b.mark, b.mark_time  " +
            "FROM uns_dashboard a " +
            "LEFT JOIN uns_dashboard_top_recodes b ON a.id = b.id and b.user_id=#{userId} " +
            "where 1=1 " +
            "<if test='fuzzyName != null'>" +
            "  and (name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%'))" +
            "</if>" +
            "<if test='type != null'>" +
            "  and type=#{type}" +
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
    List<DashboardExtendsPo> selectDashboard(@Param("userId") String userId,
                                             @Param("fuzzyName") String fuzzyName,
                                             @Param("type") Integer type,
                                             @Param("orderCode") String orderCode,
                                             @Param("descOrAsc") String descOrAsc,
                                             @Param("pageNo") long pageNo,
                                             @Param("pageSize") long pageSize);
    @Select("<script> " +
            "SELECT count(*) " +
            "FROM uns_dashboard " +
            "where 1=1 " +
            "<if test='fuzzyName != null'>" +
            "  and (name like concat('%', #{fuzzyName}, '%') or description like concat('%', #{fuzzyName}, '%'))" +
            "</if>" +
            "<if test='type != null'>" +
            "  and type=#{type}" +
            "</if>" +
            "</script>"
    )
    long selectDashboardCount(@Param("fuzzyName") String fuzzyName, @Param("type") Integer type);
}
