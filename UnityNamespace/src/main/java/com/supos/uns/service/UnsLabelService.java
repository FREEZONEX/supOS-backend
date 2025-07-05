package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.PageUtil;
import com.supos.uns.vo.FileVo;
import com.supos.common.vo.LabelVo;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UnsLabelService extends ServiceImpl<UnsLabelMapper, UnsLabelPo> {

    @Resource
    private UnsMapper unsMapper;
    @Resource
    private UnsLabelRefService unsLabelRefService;


    /**
     * 标签列表
     */
    public ResultVO<List<UnsLabelPo>> allLabels(String key) {
        List<UnsLabelPo> list = this.baseMapper.selectList(new LambdaQueryWrapper<UnsLabelPo>().like(StringUtils.isNotBlank(key), UnsLabelPo::getLabelName, key));
        return ResultVO.successWithData(list);
    }


    public ResultVO<LabelVo> detail(Long id) {
        UnsLabelPo po = getById(id);
        if (null == po) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        LabelVo vo = BeanUtil.copyProperties(po, LabelVo.class);
        return ResultVO.successWithData(vo);
    }

    public ResultVO<UnsLabelPo> create(String name) {
        if (name.contains(",")) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.name.error"));
        }
        long c = count(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getLabelName, name));
        if (c > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        UnsLabelPo po = new UnsLabelPo(name);
        save(po);
        return ResultVO.successWithData(po);
    }

    public void create(Set<String> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        List<UnsLabelPo> existLabels = list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, labels));
        if (CollectionUtils.isNotEmpty(existLabels)) {
            List<String> labelNames = existLabels.stream().map(UnsLabelPo::getLabelName).collect(Collectors.toList());
            labels.removeAll(labelNames);
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            List<UnsLabelPo> labelPos = labels.stream().map(UnsLabelPo::new).collect(Collectors.toList());
            saveBatch(labelPos);
        }
    }

    public ResultVO delete(Long id) {
        removeById(id);
        this.baseMapper.deleteRefByLabelId(id);
        return ResultVO.success("ok");
    }

    public ResultVO deleteByName(String name) {
        UnsLabelPo unsLabelPo = getOne(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getLabelName, name));
        if (null != unsLabelPo) {
            this.baseMapper.deleteRefByLabelId(unsLabelPo.getId());
            baseMapper.deleteById(unsLabelPo.getId());
        }
        return ResultVO.success("ok");
    }

    public ResultVO update(LabelVo labelVo) {
        long labelId = Long.parseLong(labelVo.getId());
        long c = count(new LambdaQueryWrapper<UnsLabelPo>()
                .eq(UnsLabelPo::getLabelName, labelVo.getLabelName())
                .ne(UnsLabelPo::getId, labelId));
        if (c > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        LambdaUpdateWrapper<UnsLabelPo> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(UnsLabelPo::getId, labelId);
        lambdaUpdateWrapper.set(UnsLabelPo::getLabelName, labelVo.getLabelName());
        update(lambdaUpdateWrapper);
        return ResultVO.success("ok");
    }

    public ResultVO makeLabel(Long unsId, List<LabelVo> labelList) {
        UnsPo uns = unsMapper.selectById(unsId);
        if (null == uns) {
            return ResultVO.success("ok");
        }
        this.baseMapper.deleteRefByUnsId(uns.getId());
        if (CollectionUtils.isNotEmpty(labelList)) {
            for (LabelVo labelVo : labelList) {
                UnsLabelRefPo ref = null;
                if (null != labelVo.getId()) {
                    ref = new UnsLabelRefPo(Long.valueOf(labelVo.getId()), uns.getId());
                } else {
                    //不存在标签，先创建
                    Long labelId = create(labelVo.getLabelName()).getData().getId();
                    ref = new UnsLabelRefPo(labelId, uns.getId());
                }
                this.baseMapper.saveRef(ref);
            }
        }
        return ResultVO.success("ok");
    }

    public ResultVO makeSingleLabel(Long unsId, Long labelId) {
        LambdaQueryWrapper<UnsLabelRefPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UnsLabelRefPo::getUnsId,unsId).eq(UnsLabelRefPo::getLabelId,labelId);
        UnsLabelRefPo refPo = unsLabelRefService.getOne(qw);
        if (refPo == null){
            refPo = new UnsLabelRefPo(labelId, unsId);
            this.baseMapper.saveRef(refPo);
        }
        return ResultVO.success("ok");
    }

    /**
     * 批量新增标签绑定关系。
     * 标签不存在时，先新增标签
     *
     * @param labelListMap
     * @return
     */
    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public ResultVO makeLabel(Map<Long, String[]> labelListMap) {
        if (labelListMap != null) {
            Set<String> labels = new HashSet<>();
            Map<String, Set<Long>> labelUnsMap = new HashMap<>();//label绑定了哪些uns节点
            for (Map.Entry<Long, String[]> e : labelListMap.entrySet()) {
                if (ArrayUtils.isNotEmpty(e.getValue())) {
                    for (String label : e.getValue()) {
                        labels.add(label);
                        labelUnsMap.computeIfAbsent(label, k -> new HashSet<>()).add(e.getKey());
                    }
                }
            }

            ArrayList<UnsLabelPo> saveLabels = new ArrayList<>(labels.size());
            List<UnsLabelRefPo> saveLabelRef = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(labels)) {
                List<UnsLabelPo> existLabels = baseMapper.selectList(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, labels));
                Map<String, UnsLabelPo> existLabelMap = existLabels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
                for (String label : labels) {
                    Set<Long> unsIds = labelUnsMap.get(label);
                    UnsLabelPo existLabel = existLabelMap.get(label);
                    if (existLabel == null) {
                        // 新增标签
                        UnsLabelPo labelPo = new UnsLabelPo(label);
                        labelPo.setId(nextId());
                        saveLabels.add(labelPo);
                        for (Long unsId : unsIds) {
                            saveLabelRef.add(new UnsLabelRefPo(labelPo.getId(), unsId));
                        }
                    } else {
                        saveLabelRef.addAll(unsIds.stream().map(unsId -> new UnsLabelRefPo(existLabel.getId(), unsId)).toList());
                    }
                }
            }

            if (!saveLabels.isEmpty()) {
                this.saveBatch(saveLabels);
            }

            if (CollectionUtils.isNotEmpty(saveLabelRef)) {
                unsLabelRefService.saveBatch(saveLabelRef);
            }
        }
        return ResultVO.success("ok");
    }

    public ResultVO cancelLabel(Long unsId, List<Long> labelIds) {
        UnsPo uns = unsMapper.selectById(unsId);
        if (null == uns) {
            return ResultVO.success("ok");
        }
        unsLabelRefService.remove(new LambdaQueryWrapper<UnsLabelRefPo>().eq(UnsLabelRefPo::getUnsId,uns.getId()).in(UnsLabelRefPo::getLabelId,labelIds));
        return ResultVO.success("ok");
    }

    public PageResultDTO<FileVo> pageListUnsByLabel(Long labelId, Long pageNo, Long pageSize) {
        Page<UnsPo> page = new Page<>(pageNo, pageSize, true);
        IPage<UnsPo> iPage = this.baseMapper.getUnsByLabel(page, labelId);
        List<FileVo> fileList = iPage.getRecords().stream().map(uns -> {
            FileVo fileVo = new FileVo();
            fileVo.setUnsId(uns.getId().toString());
            fileVo.setPathType(uns.getPathType());
            fileVo.setPath(uns.getPath());
            fileVo.setName(PathUtil.getName(uns.getPath()));
            return fileVo;
        }).collect(Collectors.toList());
        return PageUtil.build(iPage, fileList);
    }

    private static final Snowflake LABEL_SNOW = new Snowflake(2);

    private static long nextId() {
        return LABEL_SNOW.nextId();
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(2000)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        Set<Long> unsIds = event.topics.keySet();
        LambdaQueryWrapper<UnsLabelRefPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CollectionUtils.isNotEmpty(unsIds), UnsLabelRefPo::getUnsId, unsIds);
        unsLabelRefService.remove(queryWrapper);
    }
}
