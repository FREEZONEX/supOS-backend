package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.adpter.eventflow.dao.po.NodeFlowPO;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.uns.dao.po.DashboardPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
