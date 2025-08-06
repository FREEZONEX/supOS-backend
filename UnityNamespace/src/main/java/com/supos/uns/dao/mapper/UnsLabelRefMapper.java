package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.uns.dao.po.UnsLabelRefPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/10 13:58
 */
@Mapper
public interface UnsLabelRefMapper extends BaseMapper<UnsLabelRefPo> {
    @Insert("<script> insert into uns_label_ref(label_id,uns_id) VALUES " +
            " <foreach collection=\"beans\" item=\"po\" separator=\",\">" +
            "     (#{po.labelId},#{po.unsId})" +
            " </foreach> ON CONFLICT (label_id,uns_id) DO NOTHING; " +
            "</script>")
    void saveOrIgnore(@Param("beans") Collection<UnsLabelRefPo> pos);
}
