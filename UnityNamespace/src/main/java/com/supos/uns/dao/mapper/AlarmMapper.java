package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.AlarmQueryVo;
import com.supos.uns.vo.AlarmVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.checkerframework.checker.index.qual.SameLenBottom;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AlarmMapper extends BaseMapper<AlarmPo> {


    IPage<AlarmVo> pageList(Page<?> page, AlarmQueryVo params);

    @Delete("<script> delete from " + AlarmPo.TABLE_NAME +
            " where uns in " +
            "  <foreach collection=\"unsIds\" item=\"unsId\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{unsId}" +
            "  </foreach>" +
            "</script>")
    void deleteByUnsIds(@Param("unsIds") Collection<Long> unsIds);

    @Select("select * from "+ AlarmPo.TABLE_NAME + " where uns = #{unsId} and (read_status IS NULL OR read_status = false)")
    List<AlarmPo> getNoReadListByUnsId(@Param("unsId") Long unsId);


    @Select("SELECT a.* from uns_alarms_data a LEFT JOIN uns_alarms_handler b on a.uns = b.uns_id " +
            "where a.uns = #{unsId} and b.user_id = #{userId} and read_status = false ")
    List<AlarmPo> getNoReadListByUnsIdAndUserId(@Param("unsId") Long unsId,@Param("userId") String userId);
}
