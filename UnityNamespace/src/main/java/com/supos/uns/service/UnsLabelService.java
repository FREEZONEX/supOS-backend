package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.UnsLabelDto;
import com.supos.common.event.EventBus;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.UnsLabelSubscribeEvent;
import com.supos.common.event.UpdateInstanceEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.service.IUnsLabelService;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.common.utils.SuposIdUtil;
import com.supos.common.vo.LabelVo;
import com.supos.uns.bo.UnsLabels;
import com.supos.uns.bo.UnsPoLabels;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsLabelRefMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.MakeLabelDto;
import com.supos.uns.openapi.dto.UpdateLabelDto;
import com.supos.uns.util.PageUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.vo.FileVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsLabelService extends ServiceImpl<UnsLabelMapper, UnsLabelPo> implements IUnsLabelService {

    @Autowired
    private UnsMapper unsMapper;
    @Autowired
    private UnsLabelRefService unsLabelRefService;
    @Autowired
    private UnsDefinitionService unsDefinitionService;
    @Autowired
    private UnsLabelRefMapper unsLabelRefMapper;

    /**
     * 标签列表
     */
    public ResultVO<List<LabelVo>> allLabels(String key) {
        List<UnsLabelPo> list = this.baseMapper.selectList(new LambdaQueryWrapper<UnsLabelPo>()
                .like(StringUtils.isNotBlank(key), UnsLabelPo::getLabelName, key).orderByAsc(UnsLabelPo::getCreateAt)
        );
        if (CollectionUtils.isEmpty(list)) {
            return ResultVO.successWithData(Collections.emptyList());
        }
        return ResultVO.successWithData(formatPo2Vo(list));
    }


    public ResultVO<LabelVo> detail(Long id) {
        UnsLabelPo po = getById(id);
        if (null == po) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        LabelVo vo = BeanUtil.copyProperties(po, LabelVo.class);
        vo.setTopic("label/" + po.getLabelName());

        Integer flagsN = po.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            vo.setSubscribeEnable(Constants.withSubscribeEnable(flags));
            if (Boolean.TRUE.equals(vo.getSubscribeEnable())) {
                vo.setSubscribeFrequency(po.getSubscribeFrequency());
            }
        }
        if (po.getCreateAt() != null) {
            vo.setCreateTime(po.getCreateAt().getTime());
        }

        return ResultVO.successWithData(vo);
    }

    public ResultVO<LabelVo> create(String name) {
        if (!Constants.NAME_PATTERN.matcher(name).matches()) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.name.error"));
        }
        long c = count(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getLabelName, name));
        if (c > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        UnsLabelPo po = new UnsLabelPo(name);
        po.setCreateAt(new Date());
        save(po);
        LabelVo vo = BeanUtil.copyProperties(po, LabelVo.class);
        return ResultVO.successWithData(vo);
    }

    public void create(Set<String> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        List<UnsLabelPo> labelsPos = list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, labels));
        HashMap<String, UnsLabelPo> labelMap = new HashMap<>(labelsPos.size());
        for (UnsLabelPo po : labelsPos) {
            labelMap.put(po.getLabelName(), po);
        }
        List<UnsLabelPo> insertList = new ArrayList<>(labels.size());
        Date updateTime = new Date();
        for (String label : labels) {
            UnsLabelPo po = labelMap.get(label);
            if (po == null) {
                insertList.add(po = new UnsLabelPo(nextId(), label));
                po.setCreateAt(updateTime);
            }
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            saveBatch(insertList);
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
        long labelId = Long.parseLong(dto.getId());
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
        String freq = dto.getSubscribeFrequency();
        if (!StringUtils.isEmpty(freq)) {
            lambdaUpdateWrapper.set(UnsLabelPo::getSubscribeFrequency, freq);
        }
        Boolean enable = dto.getSubscribeEnable();
        if (enable != null) {
            Integer flags = po.getWithFlags();
            if (flags == null) {
                flags = 0;
            }
            flags = enable ? (flags | Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE) : (flags & ~Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE);
            lambdaUpdateWrapper.set(UnsLabelPo::getWithFlags, flags);
        }
        update(lambdaUpdateWrapper);
        unsMapper.updateUnsLabelNames(labelId, dto.getLabelName());// 更新uns冗余的标签 id->name 键值对
        return ResultVO.success("ok");
    }

    public ResultVO subscribeLabel(Long id, Boolean enable, String frequency) {
        if (enable == null && frequency == null) {
            return ResultVO.fail("enable and frequency not be null");
        }
        UnsLabelPo po = getById(id);
        if (null == po) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }

        Integer flags = po.getWithFlags();
        if (enable != null) {
            if (flags == null) {
                if (enable) {
                    flags = Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE;
                }
            } else {
                flags = enable ? (flags | Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE) : (flags & ~Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE);
            }
        }

        LambdaUpdateWrapper<UnsLabelPo> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(UnsLabelPo::getId, id);
        if (enable != null) {
            lambdaUpdateWrapper.set(UnsLabelPo::getWithFlags, flags);

            if (enable) {
                lambdaUpdateWrapper.set(UnsLabelPo::getSubscribeAt, new Date());
            }
        }
        if (frequency != null) {
            lambdaUpdateWrapper.set(UnsLabelPo::getSubscribeFrequency, frequency);
        }
        lambdaUpdateWrapper.set(UnsLabelPo::getUpdateAt, new Date());
        update(lambdaUpdateWrapper);

        UnsLabelPo afterPo = getOne(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getId, id));
        List<UnsLabelDto> labelList = new ArrayList<>();
        labelList.add(BeanUtil.copyProperties(afterPo, UnsLabelDto.class));
        UnsLabelSubscribeEvent subscribeEvent = new UnsLabelSubscribeEvent(this, labelList);
        EventBus.publishEvent(subscribeEvent);
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
                    Long labelId = Long.parseLong(create(labelVo.getLabelName()).getData().getId());
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

    public List<LabelVo> getLabelListByUnsId(Long unsId) {
        List<UnsLabelPo> poList = this.baseMapper.getLabelByUnsId(unsId);
        List<LabelVo> voList = poList.stream().map(po -> BeanUtil.copyProperties(po, LabelVo.class)).collect(Collectors.toList());
        return voList;
    }

    public ResultVO<Collection<String>> batchMakeLabel(List<MakeLabelDto> makeLabelList) {
        ResultVO<Collection<String>> resultVO = new ResultVO<>();
        resultVO.setCode(200);
        resultVO.setMsg("ok");
        List<UnsPoLabels> poLabelsList = new ArrayList<>();
        HashMap<String, String[]> fileLabels = new HashMap<>(makeLabelList.size());
        for (MakeLabelDto dto : makeLabelList) {
            fileLabels.put(dto.getFileAlias(), dto.getLabelNames() != null ? dto.getLabelNames().toArray(new String[0]) : new String[0]);
        }
        for (List<String> alias : Lists.partition(makeLabelList.stream().map(MakeLabelDto::getFileAlias).collect(Collectors.toList()), 500)) {
            for (UnsPo po : unsMapper.listByAlias(alias)) {
                UnsPoLabels poLabels = new UnsPoLabels(po, true, fileLabels.remove(po.getAlias()));
                poLabelsList.add(poLabels);
            }
        }
        Date updateTime = new Date();
        List<UnsLabelPo> labelPos = this.makeLabel(poLabelsList);
        if (!fileLabels.isEmpty()) {
            resultVO.setCode(400);
            resultVO.setMsg(I18nUtils.getMessage("uns.label.mark.error"));
            resultVO.setData(fileLabels.keySet());// data = notExists Alias list
        }
        //
        ArrayList<UnsPo> files = new ArrayList<>(poLabelsList.size());
        if (!poLabelsList.isEmpty()) {
            for (UnsPoLabels unsPoLabels : poLabelsList) {
                unsPoLabels.getUnsPo().setUpdateAt(updateTime);
                files.add(unsPoLabels.getUnsPo());
            }
            unsMapper.updateById(files);
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, files.stream().map(po -> UnsConverter.po2dto(po, false)).toList());
            EventBus.publishEvent(event);
        }


        return resultVO;
    }

    private static long nextId() {
        return SuposIdUtil.nextId();
    }

    private List<LabelVo> formatPo2Vo(List<UnsLabelPo> pos) {
        if (CollectionUtils.isEmpty(pos)) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::formatPo2Vo).toList();
    }

    private LabelVo formatPo2Vo(UnsLabelPo po) {
        LabelVo vo = BeanUtil.copyProperties(po, LabelVo.class);
        vo.setTopic("label/" + po.getLabelName());

        Integer flagsN = po.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            vo.setSubscribeEnable(Constants.withSubscribeEnable(flags));
            if (Boolean.TRUE.equals(vo.getSubscribeEnable())) {
                vo.setSubscribeFrequency(po.getSubscribeFrequency());
            }
        }
        if (po.getCreateAt() != null) {
            vo.setCreateTime(po.getCreateAt().getTime());
        }

        return vo;
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(2000)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        Set<Long> labelIds = event.topics.stream().filter(t -> !CollectionUtil.isEmpty(t.getLabelIds()))
                .flatMap(t -> t.getLabelIds().keySet().stream()).collect(Collectors.toSet());
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
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(1000)
    void labelSubscribeInit() {
        List<UnsLabelDto> unsLabelList = this.baseMapper.listSubscribe();
        if (CollectionUtils.isNotEmpty(unsLabelList)) {
            UnsLabelSubscribeEvent event = new UnsLabelSubscribeEvent(this, unsLabelList);
            EventBus.publishEvent(event);
            log.debug("labelSubscribeInit  publish event done!");
        }
    }

    @Override
    public UnsLabelDto getLabelById(Long id) {
        UnsLabelPo po = getById(id);
        if (po == null) {
            return null;
        }
        UnsLabelDto dto = BeanUtil.copyProperties(po, UnsLabelDto.class);
        dto.setRefUnsIds(unsLabelRefMapper.listByLabelId(id));
        return dto;
    }
}
