package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.uns.dao.po.UnsHistoryDeleteJobPo;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UnsHistoryDeleteJobMapper extends BaseMapper<UnsHistoryDeleteJobPo> {

    @Select("select * from " + UnsHistoryDeleteJobPo.TABLE_NAME + " where " +
            " create_at < NOW() - (#{days} || ' days')::INTERVAL " +
            " and status=1 ")
    List<UnsHistoryDeleteJobPo> selectOverDue(@Param("days") int days);

    @Select("<script>" +
            "select * from "  + UnsHistoryDeleteJobPo.TABLE_NAME + " where alias in " +
            "<foreach collection=\"aliases\" item=\"alias\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{alias} " +
            "</foreach>" +
            "</script>")
    List<UnsHistoryDeleteJobPo> selectByAlias(@Param("aliases") Collection<String> aliases);

    @Insert("<script>" +
            "insert into " + UnsHistoryDeleteJobPo.TABLE_NAME +
            "(id, alias, name, table_name, path, path_type, data_type, fields) values " +
            "<foreach collection=\"unsList\" item=\"fm\" separator=\",\">" +
            "      (#{fm.id}, #{fm.alias}, #{fm.name}, #{fm.tableName}, #{fm.path}, #{fm.pathType}, #{fm.dataType}, #{fm.fields}) " +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("unsList") Collection<UnsHistoryDeleteJobPo> unsList);

    @Delete("<script>" +
            "delete from "  + UnsHistoryDeleteJobPo.TABLE_NAME + " where alias in " +
            "<foreach collection=\"aliases\" item=\"alias\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{alias} " +
            "</foreach>" +
            "</script>")
    int deleteByAliases(@Param("aliases") Collection<String> aliases);
}
