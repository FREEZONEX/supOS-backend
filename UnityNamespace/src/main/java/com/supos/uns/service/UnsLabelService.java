package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.common.utils.SuposIdUtil;
import com.supos.common.vo.LabelVo;
import com.supos.uns.bo.UnsLabels;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.MakeLabelDto;
import com.supos.uns.openapi.dto.UpdateLabelDto;
import com.supos.uns.util.PageUtil;
import com.supos.uns.vo.FileVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsLabelService extends ServiceImpl<UnsLabelMapper, UnsLabelPo> {

    @Autowired
    private UnsMapper unsMapper;
    @Autowired
    private UnsLabelRefService unsLabelRefService;
    @Autowired
    private UnsDefinitionService unsDefinitionService;


    /**
     * 标签列表
     */
    public ResultVO<List<LabelVo>> allLabels(String key) {
        List<UnsLabelPo> list = this.baseMapper.selectList(new LambdaQueryWrapper<UnsLabelPo>().like(StringUtils.isNotBlank(key), UnsLabelPo::getLabelName, key));
        if (CollectionUtils.isEmpty(list)){
            return ResultVO.successWithData(Collections.emptyList());
        }
        List<LabelVo> voList = list.stream().map(po -> BeanUtil.copyProperties(po,LabelVo.class)).collect(Collectors.toList());
        return ResultVO.successWithData(voList);
    }


    public ResultVO<LabelVo> detail(Long id) {
        UnsLabelPo po = getById(id);
        if (po == null) {
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
        UnsLabelPo po = getById(id);
        if (po == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        removeById(id);
        List<Long> unsIds = unsLabelRefService.listObjs(new LambdaQueryWrapper<UnsLabelRefPo>()
                        .eq(UnsLabelRefPo::getLabelId, id).select(UnsLabelRefPo::getUnsId)).stream()
                .map(o -> ((Number) o).longValue()).toList();
        Date updateTime = new Date();
        if (CollectionUtils.isNotEmpty(unsIds)) {
            this.baseMapper.deleteRefByLabelId(id);
            for (List<Long> parUnsIds : Lists.partition(unsIds, 500)) {
                unsMapper.unlinkLabelsByIds(id, parUnsIds, updateTime);
            }
        }
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

    public ResultVO update(UpdateLabelDto dto) {
        long labelId = dto.getId();
        UnsLabelPo po = getById(labelId);
        if (po == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        long c = count(new LambdaQueryWrapper<UnsLabelPo>()
                .eq(UnsLabelPo::getLabelName, dto.getLabelName())
                .ne(UnsLabelPo::getId, labelId));
        if (c > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        LambdaUpdateWrapper<UnsLabelPo> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(UnsLabelPo::getId, labelId);
        lambdaUpdateWrapper.set(UnsLabelPo::getLabelName, dto.getLabelName());
        update(lambdaUpdateWrapper);
        unsMapper.updateUnsLabelNames(labelId, dto.getLabelName());// 更新uns冗余的标签 id->name 键值对
        return ResultVO.success("ok");
    }

    public ResultVO makeLabel(Long unsId, List<LabelVo> labelList) {
        UnsPo uns = unsMapper.selectById(unsId);
        if (null == uns) {
            return ResultVO.success("ok");
        }
        TreeMap<Long, String> labelIdNameMap = new TreeMap<>();
        uns.setLabelIds(labelIdNameMap);
        this.baseMapper.deleteRefByUnsId(uns.getId());

        if (CollectionUtils.isNotEmpty(labelList)) {
            Map<Long, LabelVo> noNames = labelList.stream().filter(v -> v.getId() != null && v.getLabelName() == null).collect(Collectors.toMap(k -> Long.parseLong(k.getId()), v -> v));
            if (!noNames.isEmpty()) {
                this.listByIds(noNames.keySet()).forEach(p -> {
                    LabelVo vo = noNames.get(p.getId());
                    if (vo != null) {
                        vo.setLabelName(p.getLabelName());
                    }
                });
            }
            for (LabelVo labelVo : labelList) {
                UnsLabelRefPo ref;
                String lid = labelVo.getId();
                if (lid != null) {
                    ref = new UnsLabelRefPo(Long.parseLong(lid), uns.getId());
                } else {
                    //不存在标签，先创建
                    Long labelId = create(labelVo.getLabelName()).getData().getId();
                    ref = new UnsLabelRefPo(labelId, uns.getId());
                }
                labelIdNameMap.put(ref.getLabelId(), labelVo.getLabelName());
                this.baseMapper.saveRef(ref);
            }
        }
        uns.setUpdateAt(new Date());
        unsMapper.updateById(uns);
        return ResultVO.success("ok");
    }

    public ResultVO makeSingleLabel(Long unsId, Long labelId) {
        UnsLabelPo labelPo = baseMapper.selectById(labelId);
        if (labelPo == null) {
            throw new BuzException("uns.label.not.exists");
        }
        LambdaQueryWrapper<UnsLabelRefPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UnsLabelRefPo::getUnsId, unsId).eq(UnsLabelRefPo::getLabelId, labelId);
        UnsLabelRefPo refPo = unsLabelRefService.getOne(qw);
        if (refPo == null) {
            unsMapper.linkLabelOnUns(unsId, labelId, labelPo.getLabelName(), new Date());
            UnsPo uns = unsMapper.selectById(unsId);
            if (uns == null) {
                throw new BuzException("uns.file.not.exist");
            }
            refPo = new UnsLabelRefPo(labelId, unsId);
            this.baseMapper.saveRef(refPo);
        }
        return ResultVO.success("ok");
    }

    /**
     * 批量新增标签绑定关系。
     * 标签不存在时，先新增标签
     *
     * @param unsLabels
     * @return
     */
    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public <T extends UnsLabels> List<UnsLabelPo> makeLabel(Collection<T> unsLabels) {
        if (CollectionUtils.isEmpty(unsLabels)) {
            return Collections.emptyList();
        }
        List<Long> resetUnsIds = null;
        Map<String, List<UnsLabels>> labelUnsMap = new HashMap<>();//label绑定了哪些uns节点
        for (UnsLabels unsLabel : unsLabels) {
            if (unsLabel.isResetLabels()) {
                if (resetUnsIds == null) {
                    resetUnsIds = new ArrayList<>(unsLabels.size());
                }
                resetUnsIds.add(unsLabel.unsId());
            }
            String[] labelNames = unsLabel.labelNames();
            if (ArrayUtils.isNotEmpty(labelNames)) {
                for (String label : labelNames) {
                    labelUnsMap.computeIfAbsent(label, k -> new LinkedList<>()).add(unsLabel);
                }
            }
        }
        if (resetUnsIds != null && !resetUnsIds.isEmpty()) {
            unsLabelRefService.remove(new LambdaQueryWrapper<UnsLabelRefPo>().in(UnsLabelRefPo::getUnsId, resetUnsIds));
        }
        Set<String> labels = labelUnsMap.keySet();
        ArrayList<UnsLabelPo> allLabels = new ArrayList<>(labels.size());
        ArrayList<UnsLabelPo> saveLabels = null;
        List<UnsLabelRefPo> saveLabelRef = new ArrayList<>(labels.size());
        if (CollectionUtils.isNotEmpty(labels)) {
            List<UnsLabelPo> existLabels = baseMapper.selectList(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, labels));
            Map<String, UnsLabelPo> existLabelMap = existLabels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
            for (String label : labels) {
                UnsLabelPo existLabel = existLabelMap.get(label);
                UnsLabelPo labelPo = existLabel != null ? existLabel : new UnsLabelPo(label);
                if (existLabel == null) {
                    // 新增标签
                    labelPo.setId(nextId());
                    if (saveLabels == null) {
                        saveLabels = new ArrayList<>(labels.size());
                    }
                    saveLabels.add(labelPo);
                    allLabels.add(labelPo);
                }
                final Long labelId = labelPo.getId();
                List<UnsLabels> unsLabelsList = labelUnsMap.get(label);
                for (UnsLabels ul : unsLabelsList) {
                    ul.setLabelId(label, labelId);
                }
                saveLabelRef.addAll(unsLabelsList.stream().map(ul -> new UnsLabelRefPo(labelPo.getId(), ul.unsId())).toList());
            }
        }

        if (saveLabels != null && !saveLabels.isEmpty()) {
            this.saveBatch(saveLabels);
        }

        if (CollectionUtils.isNotEmpty(saveLabelRef)) {
            unsLabelRefService.saveOrIgnoreBatch(saveLabelRef);
        }
        return allLabels;
    }

    public ResultVO cancelLabel(Long unsId, List<Long> labelIds) {
        UnsPo uns = unsMapper.selectById(unsId);
        if (null == uns) {
            return ResultVO.success("ok");
        }
        unsLabelRefService.remove(new LambdaQueryWrapper<UnsLabelRefPo>().eq(UnsLabelRefPo::getUnsId, uns.getId()).in(UnsLabelRefPo::getLabelId, labelIds));
        return ResultVO.success("ok");
    }

    public ResultVO cancelLabel(String unsAlias, List<String> labelNames) {
        UnsPo uns = unsMapper.getByAlias(unsAlias);
        if (uns == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.not.exist"));
        }
        this.baseMapper.deleteRefByUnsIdLabelNames(uns.getId(), labelNames);
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

    public List<LabelVo> getLabelListByUnsId(Long unsId){
        List<UnsLabelPo> poList = this.baseMapper.getLabelByUnsId(unsId);
        List<LabelVo> voList = poList.stream().map(po -> BeanUtil.copyProperties(po,LabelVo.class)).collect(Collectors.toList());
        return voList;
    }

    public ResultVO batchMakeLabel(List<MakeLabelDto> makeLabelList) {
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(200);
        resultVO.setMsg("ok");
        List<String> notExists = new ArrayList<>();
        for (MakeLabelDto dto : makeLabelList) {
            String alias = dto.getFileAlias();
            CreateTopicDto createTopicDto = unsDefinitionService.getDefinitionByAlias(alias);
            if (createTopicDto == null){
                notExists.add(alias);
                continue;
            }
            List<LabelVo> labelList = dto.getLabelNames().stream().map(name ->{
                LabelVo vo = new LabelVo();
                vo.setLabelName(name);
                return vo;
            }).collect(Collectors.toList());
            makeLabel(createTopicDto.getId(), labelList);
        }

        if (CollectionUtils.isNotEmpty(notExists)) {
            resultVO.setCode(400);
            resultVO.setData(notExists);
        }
        return resultVO;
    }

    private static long nextId() {
        return SuposIdUtil.nextId();
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(2000)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        long t0 = System.currentTimeMillis();
        Set<Long> labelIds = event.topics.values().stream().filter(t -> !CollectionUtils.isEmpty(t.getLabelIds()))
                .flatMap(t -> t.getLabelIds().stream()).collect(Collectors.toSet());
        if (!labelIds.isEmpty()) {
            if (labelIds.size() <= 1000) {
                LambdaQueryWrapper<UnsLabelRefPo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.in(UnsLabelRefPo::getLabelId, labelIds);
                unsLabelRefService.remove(queryWrapper);
            } else {
                for (List<Long> list : Lists.partition(new ArrayList<>(labelIds), 1000)) {
                    LambdaQueryWrapper<UnsLabelRefPo> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.in(UnsLabelRefPo::getLabelId, list);
                    unsLabelRefService.remove(queryWrapper);
                }
            }
        }
        long t1 = System.currentTimeMillis();
        log.info("标签删除耗时: {}ms, size={}", t1 - t0, labelIds.size());
    }
}
