package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UnsLabelMapper extends BaseMapper<UnsLabelPo> {


    @Select("<script>select label_name from " + UnsLabelPo.TABLE_NAME + " group by label_name</script>")
    List<String> allLabelList();

    @Select("<script>select label_name from " + UnsLabelPo.TABLE_NAME + " where topic = #{topic} and is_deleted = false</script>")
    List<String> labelListByTopic(@Param("topic") String topic);

    @Select("SELECT * FROM " + UnsLabelPo.TABLE_NAME + " WHERE topic = #{topic} AND label_name = #{labelName}")
    UnsLabelPo findByTopicAndLabelName(@Param("topic") String topic, @Param("labelName") String labelName);

    @Select("SELECT b.* from "+ UnsLabelRefPo.TABLE_NAME + " a LEFT JOIN " + UnsPo.TABLE_NAME + " b on a.uns_id = b.id where a.label_id = #{labelId} and b.id is not null")
    IPage<UnsPo> getUnsByLabel(Page<?> page, @Param("labelId") Long labelId);

    @Select("<script>SELECT n.*, " +
            " (SELECT COUNT(*) FROM uns_namespace c WHERE c.parent_id = n.id) AS count_direct_children" +
            " from "+ UnsPo.TABLE_NAME + " n " +
            " where n.path_type=2 " +
            "<if test=\"keyword!=null and keyword!='' \"> and (lower(n.path) like '${'%' + keyword.toLowerCase() + '%'}' or lower(n.alias) like '${'%' + keyword.toLowerCase() + '%'}' )  </if> " +
            " and exists (select 1 from uns_label_ref ulr where n.id = ulr.uns_id) " +
            "GROUP BY n.id </script>")
    List<UnsPo> getUnsByKeyword(@Param("keyword") String keyword);

    @Select("<script>SELECT a.* from uns_label a LEFT JOIN uns_label_ref b on a.id = b.label_id where uns_id = #{unsId}</script>")
    List<UnsLabelPo> getLabelByUnsId(@Param("unsId") Long unsId);

    @Delete("delete from " + UnsLabelRefPo.TABLE_NAME + " where label_id = #{labelId}")
    void deleteRefByLabelId(@Param("labelId") Long labelId);

    @Delete("delete from " + UnsLabelRefPo.TABLE_NAME + " where uns_id = #{unsId}")
    void deleteRefByUnsId(@Param("unsId") Long unsId);

    @Insert("insert into " + UnsLabelRefPo.TABLE_NAME +
            "(label_id, uns_id) values " +
            "(#{ref.labelId}, #{ref.unsId})")
    int saveRef(@Param("ref") UnsLabelRefPo ref);

    void deleteRefByUnsIdLabelNames(@Param("unsId") Long unsId, List<String> labelNames);
}
