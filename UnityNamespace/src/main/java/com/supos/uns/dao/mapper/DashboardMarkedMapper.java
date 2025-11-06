package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.uns.dao.po.DashboardMarkTopPO;
import com.supos.uns.dao.po.DashboardPo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DashboardMarkedMapper extends BaseMapper<DashboardPo> {

    @Insert("insert into uns_dashboard_top_recodes " +
            "(id, user_id) values " +
            "(#{id}, #{userId})")
    int insert(DashboardMarkTopPO dashboardMarkTopPO);

    @Delete("delete from uns_dashboard_top_recodes where id=#{id} and user_id=#{userId}")
    int delete(@Param("id") String id, @Param("userId") String userId);

    @Delete("delete from uns_dashboard_top_recodes where id=#{id}")
    int deleteById(@Param("id") String id);
}
