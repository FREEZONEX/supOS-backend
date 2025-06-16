package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.Constants;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.UnsSearchCondition;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.SimpleUns;
import org.apache.ibatis.annotations.*;

import java.util.*;

@Mapper
public interface UnsMapper extends BaseMapper<UnsPo> {

    @Select("select * from " + UnsPo.TABLE_NAME + " where id = #{id}")
    @ResultMap("unsResultMap")
    UnsPo getById(@Param("id") Long id);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where alias in " +
            "  <foreach collection=\"alias\" item=\"a\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{a}" +
            "  </foreach>" +
            "</script>")
    @ResultMap("unsResultMap")
    List<UnsPo> listByAlias(@Param("alias") Collection<String> alias);


    @Select("select alias from " +  UnsPo.TABLE_NAME + " where path = #{path}")
    String selectAliasByPath(@Param("path") String path);

    @Select("select * from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type !=" + Constants.CALCULATION_HIST_TYPE +
            " and status=1")
    @ResultMap("unsResultMap")
    List<UnsPo> listAllInstance();

    String filterPaths = " where path_type=#{pathType} " +
            "<if test=\"modelId!=null\"> and model_id = #{modelId} </if>  <if test=\"k!=null\"> and path like #{k} </if>" +
            "<choose><when test=\"dataTypes!=null and dataTypes.size() > 0\"> and data_type in <foreach collection=\"dataTypes\" item=\"dt\" index=\"index\" open=\"(\" close=\")\" separator=\",\">#{dt} </foreach></when>" +
            "<otherwise> and (data_type !=" + Constants.ALARM_RULE_TYPE + " or data_type is null)</otherwise></choose>" +
            " and status=1";

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + filterPaths + "</script>")
    int countPaths(@Param("modelId") String modelId, @Param("k") String key, @Param("pathType") int pathType, @Param("dataTypes") Collection<Integer> dataTypes);

    @Select("<script> select id,alias,path from " + UnsPo.TABLE_NAME + filterPaths + " order by path asc, create_at desc limit #{size} offset #{offset} </script>")
    ArrayList<SimpleUns> listPaths(@Param("modelId") String modelId, @Param("k") String key, @Param("pathType") int pathType, @Param("dataTypes") Collection<Integer> dataTypes, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where data_type=#{dataType} and path_type=2 <if test=\"k!=null\"> and path like #{k} </if> " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    @ResultMap("unsResultMap")
    ArrayList<UnsPo> listByDataType(@Param("k") String key, @Param("dataType") int dataType, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select count(*) from " + UnsPo.TABLE_NAME + " where data_type=#{dataType} and path_type=2 <if test=\"k!=null\"> and path like #{k} </if> " +
            "and status=1" +
            "</script>")
    int countByDataType(@Param("k") String key, @Param("dataType") int dataType);

    @Select("<script> select id from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and path_type=2 and status=1" +
            "</script>")
    Set<Long> listInstanceIds(@Param("ids") Collection<Long> instanceIds);

    @Select("<script> select id,alias,path from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and path_type=2 and status=1" +
            "</script>")
    List<UnsPo> listInstanceByIds(@Param("ids") Collection<Long> id);


    @Update("update " + UnsPo.TABLE_NAME + " set fields=#{fs,typeHandler=com.supos.uns.config.FieldsTypeHandler},data_src_id=2,table_name='supos." + AlarmPo.TABLE_NAME +
            "', update_at=now(),with_flags=" + (Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE) +
            "  where data_type=" + Constants.ALARM_RULE_TYPE)
    int fixAlarmFields(@Param("fs") FieldDefine[] fields);

    @ResultMap("unsResultMap")
    @Select("select fields, table_name, data_src_id from " + UnsPo.TABLE_NAME + " where id=#{id}")
    UnsPo getAlarmTemplate(@Param("id") Long id);

    @Select("select * from " + UnsPo.TABLE_NAME + " where alias=#{alias}")
    @ResultMap("unsResultMap")
    UnsPo getByAlias(@Param("alias") String alias);

    @Update("update " + UnsPo.TABLE_NAME + " set fields=#{fields,typeHandler=com.supos.uns.config.FieldsTypeHandler}, number_fields=#{numberCount}, update_at=now() where id=#{id}")
    int updateModelFieldsById(@Param("id") Long id, @Param("fields") FieldDefine[] fields, @Param("numberCount") int numberCount);

    @Update("<script> update " + UnsPo.TABLE_NAME + " set fields=#{fields,typeHandler=com.supos.uns.config.FieldsTypeHandler}, number_fields=#{numberCount}, update_at=now() where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach>" +
            "</script>")
    int updateInstanceFieldsByIds(@Param("ids") List<Long> ids, @Param("fields") FieldDefine[] fields, @Param("numberCount") int numberCount);

    @Update("update " + UnsPo.TABLE_NAME + " set description=#{description}, update_at=now() where alias=#{alias}")
    int updateDescByAlias(@Param("alias") String alias, @Param("description") String description);

    @Select("<script> select * from " + UnsPo.TABLE_NAME +
            " where id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and status=1" +
            "</script>")
    @ResultMap("unsResultMap")
    List<UnsPo> listInstancesById(@Param("ids") Collection<Long> ids);

    @Select("select * from " + UnsPo.TABLE_NAME + " where model_id=#{mid} and path_type=2 and status=1")
    @ResultMap("unsResultMap")
    List<UnsPo> listInstancesByModel(@Param("mid") Long modelId);

    @Select("SELECT column_name as name, udt_name as type FROM information_schema.columns WHERE table_name = #{t} and table_schema = 'supos'")
    List<FieldDefineVo> describeTableFieldInfo(@Param("t") String table);


    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type=" + Constants.TIME_SEQUENCE_TYPE +
            " <if test=\"k!=null\"> and path like #{k} </if>" +
            " <if test=\"nfc!=null\"> and number_fields >=  #{nfc} </if>" +
            " and status=1 </script>")
    int countNotCalcSeqInstance(@Param("k") String key, @Param("nfc") Integer minNumFields);

    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where path_type=2 and (data_type=" + Constants.TIME_SEQUENCE_TYPE + " OR data_type= " + Constants.RELATION_TYPE + ")" +
            "<if test=\"k!=null\"> and path like #{k} </if> " +
            " <if test=\"nfc!=null\"> and number_fields >=  #{nfc} </if>" +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    @ResultMap("unsResultMap")
    ArrayList<UnsPo> listNotCalcSeqInstance(@Param("k") String key, @Param("nfc") Integer minNumFields, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type in (" + Constants.TIME_SEQUENCE_TYPE + "," + Constants.CALCULATION_REAL_TYPE + ")" +
            " <if test=\"k!=null\"> and path like #{k} </if> and number_fields > 0 and status=1 </script>")
    int countTimeSeriesInstance(@Param("k") String key);

    @Select("<script> select count(1) from " + UnsPo.TABLE_NAME + " where path_type=2  and data_type = " + Constants.ALARM_RULE_TYPE +
            " <if test=\"k!=null\"> and (data_path like #{k} or description like #{k}) </if> and status=1 </script>")
    int countAlarmRules(@Param("k") String key);


    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type in (" + Constants.TIME_SEQUENCE_TYPE + "," + Constants.CALCULATION_REAL_TYPE + ")" +
            "<if test=\"k!=null\"> and path like #{k} </if> and number_fields > 0 " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    @ResultMap("unsResultMap")
    ArrayList<UnsPo> listTimeSeriesInstance(@Param("k") String key, @Param("offset") int offset, @Param("size") int size);

    @Select("<script> select * from " + UnsPo.TABLE_NAME + " where path_type=2 and data_type = " + Constants.ALARM_RULE_TYPE +
            "<if test=\"k!=null\"> and (data_path like #{k} or description like #{k}) </if> " +
            "and status=1 order by create_at desc limit #{size} offset #{offset} </script>")
    @ResultMap("unsResultMap")
    ArrayList<UnsPo> listAlarmRules(@Param("k") String key, @Param("offset") int offset, @Param("size") int size);

    @UpdateProvider(type = UnsRefUpdateProvider.class, method = "updateRefUns")
    void updateRefUns(@Param("id") Long id, @Param("ids") Map<Long, Integer> calcIds);

    @UpdateProvider(type = UnsRefUpdateProvider.class, method = "removeRefUns")
    void removeRefUns(@Param("id") Long id, @Param("ids") Collection<Long> calcIds);

    @Select("<script> select a.* , " +
            "(SELECT COUNT(*) FROM uns_namespace c WHERE c.parent_id = a.id) AS count_direct_children" +
            " from " + UnsPo.TABLE_NAME + " a "+
            " where a.id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> and a.status=1" +
            "</script>")
    @ResultMap("unsResultMap")
    List<UnsPo> listUnsByIds(@Param("ids") Collection<Long> ids);

    @Select("<script> SELECT n.* FROM " + UnsPo.TABLE_NAME + " n" +
            " where (n.path_type=0 or n.path_type=2) and (n.data_type !=" + Constants.ALARM_RULE_TYPE + " or n.data_type is null or n.data_type=0)" +
            " and n.model_id is not null" +
            " <if test=\"name!=null and name!='' \"> and (lower(n.path) like '${'%' + name.toLowerCase() + '%'}' or lower(n.alias) like '${'%' + name.toLowerCase() + '%'}' )  </if> " +
            " order by n.path_type asc,n.id asc</script>")
    @ResultMap("unsResultMap")
    List<UnsPo> listInTemplate(@Param("name") String name);

    List<UnsPo> listByConditions(@Param("params") UnsSearchCondition params);

    IPage<UnsPo> pageListByConditions(Page<?> page, @Param("params") UnsSearchCondition params);

    @Select("SHOW timezone")
    String timeZone();

    IPage<UnsPo> pageListByLazy(Page<?> page, @Param("params") UnsSearchCondition params);

    @Select("SELECT COUNT(*) FROM uns_namespace WHERE path_type = 2 AND lay_rec LIKE CONCAT(#{layRec}, '/%')")
    int countAllChildrenByLayRec(@Param("layRec") String layRec);

    @Select("SELECT COUNT(*) FROM uns_namespace WHERE parent_id = #{parentId}")
    int countDirectChildrenByParentId(@Param("parentId") Long parentId);

    List<UnsPo> selectByLayRecPrefixes(@Param("prefixes") Set<String> prefixes);

    List<UnsPo> selectByLayRecPrefix(@Param("layRecPrefix") String layRecPrefix);

    class UnsRefUpdateProvider {
        public static String updateRefUns(@Param("id") Long id, @Param("ids") Map<Long, Integer> idDataTypes) {
            StringBuilder sql = new StringBuilder(128 + idDataTypes.size());
            sql.append("UPDATE ").append(UnsPo.TABLE_NAME).append(" SET ref_uns = ");
            for (int i = 0, sz = idDataTypes.size(); i < sz; i++) {
                sql.append("jsonb_set(");
            }
            sql.append("case when ref_uns is null then '{}' else ref_uns end");
            for (Map.Entry<Long, Integer> e : idDataTypes.entrySet()) {
                Long unsId = e.getKey();
                Integer dataType = e.getValue();
                sql.append(",'{\"").append(unsId).append("\"}','").append(dataType).append("')");
            }
            sql.append(" where id=#{id}");
            return sql.toString();
        }

        public static String removeRefUns(@Param("id") Long id, @Param("ids") Collection<Long> calcIds) {
            StringBuilder sql = new StringBuilder(128 + calcIds.size());
            sql.append("UPDATE ").append(UnsPo.TABLE_NAME).append(" SET ref_uns = ");
            for (int i = 0, sz = calcIds.size(); i < sz; i++) {
                sql.append("jsonb_set_lax(");
            }
            sql.append("ref_uns");
            for (Long calcId : calcIds) {
                sql.append(",'{\"").append(calcId).append("\"}',null,true,'delete_key')");
            }
            sql.append(" where id=#{id}");
            return sql.toString();
        }

    }


}
