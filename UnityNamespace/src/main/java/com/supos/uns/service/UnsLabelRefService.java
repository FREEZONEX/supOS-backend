package com.supos.uns.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.uns.dao.mapper.UnsLabelRefMapper;
import com.supos.uns.dao.po.UnsLabelRefPo;
import org.springframework.stereotype.Service;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/10 14:01
 */
@Service
public class UnsLabelRefService extends ServiceImpl<UnsLabelRefMapper, UnsLabelRefPo> {


    boolean revmoveUnsLabelRefByLabelId(Long unsId,Long labelId){
        LambdaQueryWrapper<UnsLabelRefPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UnsLabelRefPo::getUnsId,unsId).eq(UnsLabelRefPo::getLabelId,labelId);
        return remove(qw);
    }

}
