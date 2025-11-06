package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.dao.po.DashboardRefPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardRefMapper extends BaseMapper<DashboardRefPo> {


    @Select("select a.* from uns_dashboard a LEFT JOIN uns_dashboard_ref b ON a.id = b.dashboard_id where b.uns_alias = #{unsAlias} limit 1")
    DashboardPo getByUns(@Param("unsAlias") String unsAlias);
}
