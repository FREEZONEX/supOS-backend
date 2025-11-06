package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.RunningStatus;
import com.supos.common.SrcJdbcType;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.*;
import com.supos.common.enums.*;
import com.supos.common.event.*;
import com.supos.common.event.multicaster.EventStatusAware;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.service.IUnsManagerService;
import com.supos.common.utils.*;
import com.supos.uns.bo.CreateModelInstancesArgs;
import com.supos.uns.bo.UnsPoLabels;
import com.supos.uns.dao.mapper.UnsHistoryDeleteJobMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsHistoryDeleteJobPo;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.WebhookDataDTO;
import com.supos.uns.util.LayRecUtils;
import com.supos.uns.util.LeastTopNodeUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.util.WebhookUtils;
import com.supos.uns.vo.CreateTemplateVo;
import com.supos.uns.vo.UpdateNameVo;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.supos.uns.util.DependencySorter.buildReverseGraph;
import static com.supos.uns.util.DependencySorter.calculateLevels;
import static com.supos.uns.util.UnsFlags.generateFlag;

@Slf4j
@Service
public class UnsAddService extends ServiceImpl<UnsMapper, UnsPo> implements IUnsManagerService {

    private SrcJdbcType relationType = SrcJdbcType.Postgresql, timeDataType;
    @Resource
    UnsLabelService unsLabelService;
    @Autowired
    UnsCalcService unsCalcService;
    @Autowired
    UnsHistoryDeleteJobMapper unsHistoryDeleteJobMapper;
    @Autowired
    UnsTemplateService unsTemplateService;
    @Resource
    private IUnsDefinitionService unsDefinitionService;
    @Autowired
    private UnsMapper unsMapper;
    @Autowired
    private SystemConfig systemConfig;

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public JsonResult<Map<String, String>> createCategoryModelInstance(CreateTopicDto dto) {
        List<CreateTopicDto> dtoList = new ArrayList<>();
        dtoList.add(dto);

        boolean isParentFolderCategory = false;
        UnsPo parentFolder = null;
        if (dto.getParentId() != null || dto.getParentAlias() != null) {
            parentFolder = dto.getParentId() != null ? baseMapper.selectById(dto.getParentId()) : baseMapper.getByAlias(dto.getParentAlias());
            if (parentFolder == null) {
                return new JsonResult<>(400, I18nUtils.getMessage("uns.folder.not.found"));
            }
            isParentFolderCategory = parentFolder.getDataType() != null && parentFolder.getDataType() > 0;
            log.info("当前文件(name={})的父级文件夹是否为分类文件夹： {}", dto.getName(), isParentFolderCategory);
            // 分类文件夹下不能再建文件夹
            if (isParentFolderCategory && dto.getPathType() == Constants.PATH_TYPE_DIR) {
                return new JsonResult<>(400, I18nUtils.getMessage("uns.folder.create.forbidden"));
            }
            dto.setParentAlias(parentFolder.getAlias());
        }

        // 当其父节点类型设置为非普通文件夹，需要自动创建一层特殊文件夹
        if (systemConfig.getEnableAutoCategorization() && !isParentFolderCategory && dto.getParentDataType() != null && dto.getParentDataType() > 0) {
            if (!FolderDataType.isTypeMatched(dto.getParentDataType(), dto.getDataType())) {
                String msg = I18nUtils.getMessage("uns.category.type.not.eq");
                throw new BuzException(400, msg);
            }
            UnsPo fixedCategoryFolder = unsMapper.getFixedCategoryFolder(dto.getParentAlias(), dto.getParentDataType());
            String mountSource = parentFolder == null ? null : parentFolder.getMountSource();
            Integer mountType = parentFolder == null ? null : parentFolder.getMountType();
            if (fixedCategoryFolder == null) {
                CreateTopicDto categoryFolder = buildCategoryFolderDto(dto.getParentAlias(), mountType, mountSource, dto.getParentDataType());
                dto.setParentAlias(categoryFolder.getAlias());
                dtoList.add(categoryFolder);
            } else {
                dto.setParentAlias(fixedCategoryFolder.getAlias());
                dto.setParentId(fixedCategoryFolder.getId());
            }
        }

        boolean isSuccess = false;
        String errorMsg = "";
        if (dtoList.size() == 1) {
            JsonResult<String> result = createModelInstance(dtoList.get(0));
            isSuccess = result.getCode() == 0;
            errorMsg = result.getMsg();
        } else {
            Map<String, String> errorMap = createModelAndInstance(dtoList, false);
            isSuccess = errorMap.isEmpty();
            if (!isSuccess) {
                errorMsg = errorMap.values().iterator().next();
            }
        }
        JsonResult<Map<String, String>> resultMap = new JsonResult<>(0, "ok");
        if (isSuccess) {
            UnsPo unsFile = unsMapper.getByAlias(dto.getAlias());
            Map<String, String> resultData = new HashMap<>();
            resultData.put("parentId", unsFile.getParentId() == null ? "" : unsFile.getParentId().toString());
            resultData.put("id", unsFile.getId().toString());
            resultMap.setData(resultData);
        } else {
            resultMap.setCode(400);
            resultMap.setMsg(errorMsg);
        }
        return resultMap;
    }

    /**
     * 创建目录或文件 -- 前端界面创建单个实例发起
     *
     * @param dto
     * @return
     */
    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public JsonResult<String> createModelInstance(CreateTopicDto dto) {
        JsonResult<String> result = new JsonResult<>(0, "ok");
        Long folderId = dto.getParentId();
        if (folderId != null || dto.getParentAlias() != null) {
            UnsPo folder = folderId != null ? baseMapper.selectById(folderId) : baseMapper.getByAlias(dto.getParentAlias());
            if (folder == null) {
                return new JsonResult<>(400, I18nUtils.getMessage("uns.folder.not.found"));
            }
            dto.setParentAlias(folder.getAlias());
        }

        // 查询文件是否存在，因为修改也是走这个方法。。
        UnsPo fileUns = baseMapper.getByAlias(dto.getAlias());
        if (fileUns != null) {
            dto.setDataType(fileUns.getDataType());
            dto.setParentDataType(fileUns.getParentDataType());
            dto.setParentAlias(fileUns.getParentAlias());
        }

        //是文件夹 并且 需要创建模板
        if (Constants.PATH_TYPE_DIR == dto.getPathType() && Boolean.TRUE.equals(dto.getCreateTemplate())) {
            CreateTemplateVo templateVo = new CreateTemplateVo();
            templateVo.setName(dto.getName());
            templateVo.setFields(dto.getFields());
            ResultVO<String> templateResult = unsTemplateService.createTemplate(templateVo);
            if (templateResult.getCode() != 200) {
                result.setCode(400);
                result.setMsg(templateResult.getMsg());
                return result;
            }
            dto.setModelId(Long.valueOf(templateResult.getData()));
        }

        dto.setIndex(0);
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = Collections.singletonList(dto);
        args.fromImport = false;
        args.throwModelExistsErr = true;
        if (dto.getDataType() != null && (dto.getDataType() == Constants.CALCULATION_HIST_TYPE || dto.getDataType() == Constants.CALCULATION_REAL_TYPE)) {
            dto.setAddFlow(false);
        }
        Map<String, String> rs = createModelAndInstancesInner(args);
        if (rs != null && !rs.isEmpty()) {
            throw new BuzException(400, rs.values().toString());
        } else {
            result.setData(dto.getId().toString());
        }
        return result;
    }

    private CreateTopicDto buildCategoryFolderDto(String parentAlias, Integer mountType, String mountSource, Integer folderDataType) {
        CreateTopicDto dto = new CreateTopicDto();
        FolderDataType fdt = FolderDataType.getFolderDataType(folderDataType);
        if (StringUtils.hasText(parentAlias)) {
            dto.setAlias(fdt.name().toLowerCase() + "_" + parentAlias);
            dto.setParentAlias(parentAlias);
            dto.setMountSource(mountSource);
            dto.setMountType(mountType);
        } else {
            dto.setAlias("_" + fdt.name().toLowerCase() + "_");
            dto.setParentAlias(null);
        }
        dto.setId(SuposIdUtil.nextId());
        String name = I18nUtils.getMessage(fdt.getI18nName());
        dto.setName(name);
        dto.setDisplayName(name);
        dto.setDataType(folderDataType);
        dto.setPathType(Constants.PATH_TYPE_DIR);
        return dto;

    }

    public JsonResult<String> updateModelInstance(UpdateUnsDto dto) {
        UnsPo unsPo = null;
        if (null != dto.getId()) {
            unsPo = this.baseMapper.selectById(dto.getId());
        } else if (StringUtils.hasText(dto.getAlias())) {
            unsPo = this.baseMapper.getByAlias(dto.getAlias());
        }

        if (null == unsPo) {
            return new JsonResult<>(400, "uns.folder.or.file.not.found");
        }

        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        if (!StringUtils.hasText(createTopicDto.getName())) {
            createTopicDto.setName(unsPo.getName());
        }

        createTopicDto.setPathType(unsPo.getPathType());
        Integer flags = createTopicDto.getFlags();
        Boolean dash = createTopicDto.getAddDashBoard(), flow = createTopicDto.getAddFlow(), save2db = createTopicDto.getSave2db();
        String accessLevel = dto.getAccessLevel();
        if (flags == null && (dash != null || flow != null || save2db != null)) {
            flags = unsPo.getWithFlags();
            if (flags == null) {
                flags = 0;
            }
            int fl = flags;
            if (dash != null) {
                fl = dash ? (fl | Constants.UNS_FLAG_WITH_DASHBOARD) : (fl & ~Constants.UNS_FLAG_WITH_DASHBOARD);
            }
            if (flow != null) {
                fl = flow ? (fl | Constants.UNS_FLAG_WITH_FLOW) : (fl & ~Constants.UNS_FLAG_WITH_FLOW);
            }
            if (save2db != null) {
                fl = save2db ? (fl | Constants.UNS_FLAG_WITH_SAVE2DB) : (fl & ~Constants.UNS_FLAG_WITH_SAVE2DB);
            }
            if (StringUtils.hasText(accessLevel)) {
                if (accessLevel.equals(FileReadWriteMode.READ_ONLY.getMode())) {
                    fl = (fl & ~Constants.UNS_FLAG_ACCESS_LEVEL_READ_WRITE)
                            | Constants.UNS_FLAG_ACCESS_LEVEL_READ_ONLY;
                } else {
                    fl = (fl & ~Constants.UNS_FLAG_ACCESS_LEVEL_READ_ONLY)
                            | Constants.UNS_FLAG_ACCESS_LEVEL_READ_WRITE;
                }
            }
            flags = fl;
        } else {
            flags = unsPo.getWithFlags();
        }

        createTopicDto.setFlags(flags);
        createTopicDto.setParentAlias(unsPo.getParentAlias());
        createTopicDto.setDataType(unsPo.getDataType());
        JsonResult<String> modelInstance = createModelInstance(createTopicDto);
        JsonResult<String> newResult = new JsonResult<>();
        newResult.setCode(modelInstance.getCode());
        newResult.setMsg(modelInstance.getMsg());
        if (modelInstance.getData() != null) {
            newResult.setData(modelInstance.getData());
        }
        return newResult;
    }


    public JsonResult<String> updateName(UpdateNameVo updateNameVo) {
        UnsPo unsPo = baseMapper.selectById(updateNameVo.getId());
        if (null == unsPo) {
            return new JsonResult<>(400, I18nUtils.getMessage("uns.folder.or.file.not.found"));
        }
        CreateTopicDto createTopicDto = new CreateTopicDto();
        createTopicDto.setName(updateNameVo.getName());
        createTopicDto.setFlags(unsPo.getWithFlags());
        createTopicDto.setPathType(unsPo.getPathType());
        createTopicDto.setAlias(unsPo.getAlias());
        createTopicDto.setParentAlias(unsPo.getParentAlias());
        createTopicDto.setDataType(unsPo.getDataType());
        JsonResult<String> modelInstance = createModelInstance(createTopicDto);
        JsonResult<String> newResult = new JsonResult<>();
        newResult.setCode(modelInstance.getCode());
        newResult.setMsg(modelInstance.getMsg());
        if (modelInstance.getData() != null) {
            newResult.setData(modelInstance.getData());
        }
        return newResult;
    }

    public ResultVO subscribeModel(Long id, Boolean enable, String frequency) {
        if (enable == null && frequency == null) {
            return ResultVO.fail("enable and frequency not be null");
        }
        CreateTopicDto dto = unsDefinitionService.getDefinitionById(id);
        if (dto == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.or.file.not.found"));
        }

        Integer flags = dto.getFlags();
        if (enable != null) {
            if (flags == null) {
                if (enable) {
                    flags = Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE;
                }
            } else {
                flags = enable ? (flags | Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE) : (flags & ~Constants.UNS_FLAG_WITH_SUBSCRIBE_ENABLE);
            }
        }

        if (enable != null) {
            dto.setFlags(flags);
        }
        if (frequency != null) {
            Map<String, Object> protocol = new HashMap<>();
            protocol.put("frequency", frequency);
            dto.setProtocol(protocol);
        }
        dto.setSubscribeAt(new Date());
        JsonResult jsonResult = createModelInstance(dto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        UnsPo afterUnsPo = baseMapper.selectById(id);
        CreateTopicDto createTopicDto = UnsConverter.po2dto(afterUnsPo);
        Integer pathType = afterUnsPo.getPathType();
        CreateTopicDto[] dtos = new CreateTopicDto[1];
        dtos[0] = createTopicDto;
        List<CreateTopicDto> emptyList = Collections.emptyList();
        CreateTopicDto[] emptyArray = new CreateTopicDto[0];
        if (pathType == 0) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, emptyList, dtos, emptyArray);
            EventBus.publishEvent(event);
        } else if (pathType == 1) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, emptyList, emptyArray, dtos);
            EventBus.publishEvent(event);
        } else if (pathType == 2) {
            UpdateInstanceEvent event = new UpdateInstanceEvent(this, Arrays.asList(createTopicDto), null, null);
            EventBus.publishEvent(event);
        }
        return ResultVO.success("ok");
    }


    /**
     * 创建模型和实例 -- excel 导入发起
     *
     * @param topicList
     * @param flowName
     * @param statusConsumer
     * @return Map &lt; key=数组index, value=ErrTip &gt;
     */
    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public Map<String, String> createModelAndInstance(List<CreateTopicDto> topicList, Map<String, String[]> labelsMap, String flowName, Consumer<RunningStatus> statusConsumer) {
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = topicList;
        args.fromImport = true;
        args.throwModelExistsErr = true;
        args.flowName = flowName;
        args.statusConsumer = statusConsumer;
        args.labelsMap = labelsMap;

        return createModelAndInstancesInner(args);
    }

    private List<CreateTopicDto> modelTransfer(List<CreateUnsNodeRedDto> requestDtoList) {
        List<CreateTopicDto> dtos = new ArrayList<>();
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, List<FieldDefine>> fileMap = new HashMap<>();
        // 根据路径，对文件属性进行归类
        for (CreateUnsNodeRedDto requestDto : requestDtoList) {
            if (StringUtils.hasText(requestDto.getFieldName())) {
                String fullpath = requestDto.getPath();
                if (requestDto.getPath().endsWith("/")) {
                    fullpath = requestDto.getPath().substring(0, requestDto.getPath().length() - 1);
                }
                FieldDefine fd = new FieldDefine();
                fd.setType(FieldType.getByNameIgnoreCase(requestDto.getFieldType()));
                fd.setName(requestDto.getFieldName());
                List<FieldDefine> fieldDefines = fileMap.getOrDefault(fullpath, new ArrayList<>());
                // 如果传了alias，那么就以参数为准
                if (StringUtils.hasText(requestDto.getAlias())) {
                    aliasMap.put(fullpath, requestDto.getAlias());
                }
                fieldDefines.add(fd);
                fileMap.put(fullpath, fieldDefines);
            }
        }
        if (fileMap.isEmpty()) {
            return dtos;
        }
        // 构建path和alias的关系
        for (String path : fileMap.keySet()) {
            String[] folders = path.split("/");
            for (int i = 0; i < folders.length; i++) {
                String tmp = folders[i];
                int ii = i;
                while (--ii >= 0) {
                    tmp = folders[ii] + "/" + tmp;
                }
                if (!aliasMap.containsKey(tmp)) {
                    String falias = baseMapper.selectAliasByPath(tmp);
                    if (!StringUtils.hasText(falias)) {
                        falias = PathUtil.generateFileAlias(tmp);
                    }
                    aliasMap.put(tmp, falias);
                }
            }
        }

        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            CreateTopicDto dto = new CreateTopicDto();
            dto.setAlias(entry.getValue());
            dto.setPath(entry.getKey());
            dto.setDataType(1);
            List<FieldDefine> fields = fileMap.get(entry.getKey());
            if (fields != null && !fields.isEmpty()) {
                dto.setFields(list2Array(fields));
                dto.setPathType(Constants.PATH_TYPE_FILE);
            } else {
                dto.setPathType(Constants.PATH_TYPE_DIR);
            }
            // 根据当前path获取父级path
            int lastSlashIndex = entry.getKey().lastIndexOf('/'); // 最后一个 "/" 的位置
            if (lastSlashIndex > 0) {
                String parentPath = entry.getKey().substring(0, lastSlashIndex); // 截取到最后一个 "/" 前
                dto.setParentAlias(aliasMap.get(parentPath));
            }
            int index = lastSlashIndex + 1;
            dto.setName(entry.getKey().substring(index));
            dtos.add(dto);
        }
        return dtos;
    }

    private FieldDefine[] list2Array(List<FieldDefine> fieldList) {
        FieldDefine[] fs = new FieldDefine[fieldList.size()];
        for (int i = 0; i < fieldList.size(); i++) {
            fs[i] = fieldList.get(i);
        }
        return fs;
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    public List<String[]> createModelsForNodeRed(List<CreateUnsNodeRedDto> requestDto) {
        List<CreateTopicDto> createTopicDtos = modelTransfer(requestDto);
        if (!createTopicDtos.isEmpty()) {
            createModelAndInstance(createTopicDtos, false);
        }
        List<String[]> results = new ArrayList<>();
        // 填充别名返回
        for (CreateUnsNodeRedDto dto : requestDto) {
            String fullpath = dto.getPath();
            if (dto.getPath().endsWith("/")) {
                fullpath = dto.getPath().substring(0, dto.getPath().length() - 1);
            }
            String alias = dto.getAlias();
            if (!StringUtils.hasText(alias) && StringUtils.hasText(dto.getFieldName())) {
                alias = PathUtil.generateFileAlias(fullpath);
            }
            // path, alias, fname, ftype, tag
            String[] row = {dto.getPath(), alias, dto.getFieldName(), dto.getFieldType(), dto.getTag()};
            results.add(row);
        }
        return results;
    }

    /**
     * 文件或者文件夹黏贴
     * @param sourceId 被复制的顶级文件（夹）ID
     * @param targetParentId 粘贴的目的地顶层文件夹ID
     * @param newFolderOrFile 源顶层文件夹重命名
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResultVO pasteFolderOrFile(String sourceId, String targetParentId, CreateTopicDto newFolderOrFile) {
        UnsPo targetFolder = null;
        // 挂载的目录不允许粘贴
        if (StringUtils.hasText(targetParentId)) {
            targetFolder = unsMapper.getById(Long.parseLong(targetParentId));
            if (targetFolder.getMountType() != null && targetFolder.getMountType() > 0) {
                throw new BuzException(400, "uns.paste.mount.not.allow");
            }
        }
        // 查询被复制顶层文件以及其下面的所有文件和文件夹
        List<UnsPo> unsPos = unsMapper.selectAllByLayRec(sourceId);
        if (unsPos.isEmpty()) {
            throw new BuzException(400, "uns.paste.empty.not.allow");
        }
        // 归类文件夹下不能创建文件夹，且只能创建同类型文件
        if (systemConfig.getEnableAutoCategorization()) {
            if (targetFolder != null) {
                Integer pDataType = targetFolder.getDataType();
                if (pDataType != null && pDataType > 0) {
                    List<UnsPo> forbiddenFiles = unsPos.stream().filter(po ->
                                    (po.getPathType() == Constants.PATH_TYPE_DIR && po.getDataType().intValue() != pDataType.intValue()) ||
                                            (po.getPathType() == Constants.PATH_TYPE_FILE && (po.getParentDataType() == null || po.getParentDataType().intValue() != pDataType.intValue())))
                            .toList();
                    if (!forbiddenFiles.isEmpty()) {
                        throw new BuzException(400, "uns.paste.category.not.allow");
                    }
                }
            }
            UnsPo sourceFile = unsMapper.getById(Long.parseLong(sourceId));
            if (sourceFile.getPathType() == Constants.PATH_TYPE_DIR && sourceFile.getDataType() > 0) {
                // 删除顶层归类文件夹，并且不支持传入新的归类文件夹
                unsPos.removeIf(item -> item.getId().longValue() == sourceFile.getId().longValue());
                newFolderOrFile = null;
            }
        }
        List<CreateTopicDto> createTopicDtos = unsPo2Dto(unsPos, sourceId, targetParentId, newFolderOrFile);
        if (createTopicDtos.size() == 1) {
            JsonResult<Map<String, String>> modelInstance = createCategoryModelInstance(createTopicDtos.get(0));
            if (modelInstance.getCode() > 200) {
                throw new BuzException(modelInstance.getMsg());
            }
            return ResultVO.successWithData(modelInstance.getData());
        }
        Map<String, String> modelAndInstance = createModelAndInstance(createTopicDtos, false);
        // 当超过一半文件失败则回滚
        if (modelAndInstance.size() >= (createTopicDtos.size() / 2)) {
            // 取第一个报错信息返回
            throw new BuzException(modelAndInstance.values().iterator().next());
        }
        // 查询新文件的ID和parentId,并返回前端用于定位
        UnsPo anyOne = unsMapper.getByAlias(createTopicDtos.get(0).getAlias());
        Map<String, String> resultData = new HashMap<>();
        resultData.put("parentId", anyOne.getParentId() == null ? "" : anyOne.getParentId().toString());
        resultData.put("id", anyOne.getId().toString());
        // 当前方法只能处理5000条数据，超过5000需要给前端一个反馈
        if (createTopicDtos.size() > 2000) { // 避免每次都查总数
            int count = unsMapper.countAllByLayRec(sourceId);
            if (count > 5000) {
                modelAndInstance.put("0", I18nUtils.getMessage("uns.paste.total.limit"));
            }
        }

        if (modelAndInstance.isEmpty()) {
            return ResultVO.successWithData(resultData);
        }

        return ResultVO.builder()
                .code(206)
                .msg(modelAndInstance.values().iterator().next())
                .data(resultData)
                .build();
    }

    private List<CreateTopicDto> unsPo2Dto(List<UnsPo> unsPos, String sourceId, String targetParentId, CreateTopicDto newFolderOrFile) {
        LinkedList<CreateTopicDto> topicDtos = new LinkedList<>();
        Map<String, String> aliasMapping = new HashMap<>();
        for (UnsPo unsPo : unsPos) {
            CreateTopicDto topicDto = new CreateTopicDto();
            BeanUtils.copyProperties(unsPo, topicDto);
            topicDto.setId(SuposIdUtil.nextId());
            String newAlias = "";
            if (newFolderOrFile == null) {
                newAlias = PathUtil.generateAliasWithRandom(topicDto.getName(), topicDto.getPathType());
            } else {
                newAlias = PathUtil.generateAliasWithRandom(newFolderOrFile.getName(), newFolderOrFile.getPathType());
            }
            topicDto.setAlias(newAlias);
            topicDto.setTemplate(unsPo.getTemplateAlias());
            if (unsPo.getLabelIds() != null) {
                topicDto.setLabelNames(unsPo.getLabelIds().values().toArray(new String[0]));
            }
            if (StringUtils.hasText(unsPo.getProtocol())) {
                Map<String, Object> protoMap = JSON.parseObject(unsPo.getProtocol(), Map.class);
                topicDto.setFrequency(protoMap.get("frequency").toString());
                topicDto.setProtocol(protoMap);
            }
            if (Constants.withFlow(unsPo.getWithFlags())) {
                // 粘贴的文件不需要mock数据
                topicDto.setFlags(unsPo.getWithFlags() & ~Constants.UNS_FLAG_WITH_FLOW);
            } else {
                topicDto.setFlags(unsPo.getWithFlags());
            }
            topicDto.setAccessLevel(Constants.withReadOnly(unsPo.getWithFlags()));
            aliasMapping.put(unsPo.getAlias(), newAlias);
            // 顶层文件添加到列表第一位置
            if (sourceId.equals(unsPo.getId().toString())) {
                // 粘贴并修改得情况，顶层采用输入的文件
                if (newFolderOrFile != null) {
                    aliasMapping.put(newFolderOrFile.getAlias(), newAlias);
                    newFolderOrFile.setId(SuposIdUtil.nextId());
                    newFolderOrFile.setAlias(newAlias);
                    newFolderOrFile.setMountType(0);
                    newFolderOrFile.setMountSource(null);
                    topicDtos.addFirst(newFolderOrFile);
                } else {
                    topicDtos.addFirst(topicDto);
                }
            } else {
                topicDtos.add(topicDto);
            }
        }
        // 重新设置parent alias
        resetParentAlias(topicDtos, targetParentId, aliasMapping);

        // 判断文件夹name是否和同级的师兄弟是否有重名，如果重名需要对name进行重命名
        if (topicDtos.get(0).getPathType() == Constants.PATH_TYPE_DIR) {
            renameDuplicateFolderName(topicDtos.get(0), targetParentId);
        }
        return topicDtos;
    }

    private void renameDuplicateFolderName(CreateTopicDto topDto, String targetParentId) {
        Long fatherId = StringUtils.hasText(targetParentId) ? Long.valueOf(targetParentId) : null;
        Set<String> brotherNameList = unsMapper.listBrotherFileName(fatherId);
        String unsName = topDto.getName();
        if (brotherNameList.contains(unsName)) {
            String newName = LayRecUtils.genNewPath(unsName, brotherNameList);
            topDto.setName(newName);
        }
    }

    private void resetParentAlias(LinkedList<CreateTopicDto> topicDtos, String targetParentId, Map<String, String> aliasMapping) {
        for (CreateTopicDto topicDto : topicDtos) {
            String newParentAlias = aliasMapping.get(topicDto.getParentAlias());
            if (newParentAlias != null) {
                topicDto.setParentAlias(newParentAlias);
            } else {
                if (StringUtils.hasText(targetParentId)) {
                    UnsPo newParent = unsMapper.getById(Long.parseLong(targetParentId));
                    topicDto.setParentAlias(newParent.getAlias());
                } else {
                    topicDto.setParentAlias(null);
                }
            }
        }
    }

    @Transactional(rollbackFor = Throwable.class, timeout = 300)
    @Override
    public Map<String, String> createModelAndInstance(List<CreateTopicDto> topicDtos, boolean fromImport) {
        final int TASK_ID = System.identityHashCode(topicDtos);

        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = topicDtos;
        args.fromImport = fromImport;
        args.throwModelExistsErr = false;
        args.flowName = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        args.statusConsumer = runningStatus -> {
            Long spend = runningStatus.getSpendMills();
            if (spend != null) {
                Integer i = runningStatus.getI(), n = runningStatus.getN();
                log.info("[{}] {}/{} 已处理， {}：耗时{} ms", TASK_ID, i, n, runningStatus.getTask(), spend);
            }
        };
        AtomicInteger index = new AtomicInteger(0);
        args.topics = topicDtos.stream().map(topicDto -> {
            topicDto.setBatch(0);
            topicDto.setIndex(index.getAndIncrement());
            return topicDto;
        }).collect(Collectors.toList());

        Map<String, String[]> labelsMap = new HashMap<>();
        topicDtos.stream().filter(topicDto -> ObjectUtil.isNotEmpty(topicDto.getLabelNames())).forEach(topicDto -> {
            labelsMap.put(topicDto.getAlias(), topicDto.getLabelNames());
        });
        args.labelsMap = labelsMap;

        Map<String, String> rs = createModelAndInstancesInner(args);
        log.info("[{}] UNS 处理完毕.", TASK_ID);
        return rs;
    }

    private List<CreateTopicDto> appendCategoryFolders(List<CreateTopicDto> topicDtos, Map<String, String> errorTip) {
        List<CreateTopicDto> newTopicDtos = new ArrayList<>(topicDtos);
        Set<String> parentAliasSet = newTopicDtos.stream().map(CreateTopicDto::getParentAlias).filter(StringUtils::hasText).collect(Collectors.toSet());
        Map<String, UnsPo> parentAliasMap = new HashMap<>();
        Map<String, List<UnsPo>> categoryFolderMap = new HashMap<>();
        if (!parentAliasSet.isEmpty()) {
            List<UnsPo> parentUnsPoList = unsMapper.listByAlias(parentAliasSet);
            parentAliasMap = parentUnsPoList.stream()
                    .collect(Collectors.toMap(
                            UnsPo::getAlias, // Key Mapper: 提取 alias 作为 key
                            unsPo -> unsPo   // Value Mapper: 对象本身作为 value
                    ));
            // 查询数据库父节点下的归类子文件夹
            List<UnsPo> categoryFolders = unsMapper.listCategoryFolders(parentAliasSet);
            categoryFolderMap = categoryFolders.stream()
                    .collect(Collectors.groupingBy(UnsPo::getParentAlias));
        }
        List<UnsPo> rootUnsPoList = unsMapper.listRootCategoryFolders();
        categoryFolderMap.put(null, rootUnsPoList);

        Map<String, CreateTopicDto> aliasMap = newTopicDtos.stream().collect(Collectors.toMap(
                CreateTopicDto::getAlias, // Key Mapper: 提取 alias 作为 key
                dto -> dto   // Value Mapper: 对象本身作为 value
        ));
        Map<String, CreateTopicDto> newCategoryAliasMap = new HashMap<>();
        Iterator<CreateTopicDto> iterator = newTopicDtos.iterator();
        while(iterator.hasNext()) {
            CreateTopicDto topicDto = iterator.next();
            if (topicDto.getPathType() == Constants.PATH_TYPE_FILE && topicDto.getDataType() != Constants.ALARM_RULE_TYPE) {
                // 验证parentDataType是否为空
                if (topicDto.getParentDataType() == null || topicDto.getParentDataType() < 1 || topicDto.getParentDataType() > 3) {
                    errorTip.put(topicDto.gainBatchIndex(), I18nUtils.getMessage("uns.file.type.invalid"));
                    iterator.remove();
                    continue;
                }
                // 判断父级类型和文件类型是否属于包含关系，例如操作文件夹下只能包含JSONB类型
                if (!FolderDataType.isTypeMatched(topicDto.getParentDataType(), topicDto.getDataType())) {
                    errorTip.put(topicDto.gainBatchIndex(), I18nUtils.getMessage("uns.category.type.not.eq"));
                    iterator.remove();
                    continue;
                }
                UnsPo parentUnsPo = parentAliasMap.get(topicDto.getParentAlias());
                // 如果父级目录是归类文件夹并且已经在数据库中已存在，则跳过
                if (parentUnsPo != null && parentUnsPo.getDataType() != null) {
                    // 检查文件类型和父级文件夹类型是否一致
                    if (parentUnsPo.getDataType() > 0 && parentUnsPo.getDataType().intValue() != topicDto.getParentDataType()) {
                        errorTip.put(topicDto.gainBatchIndex(), I18nUtils.getMessage("uns.category.type.not.eq"));
                        iterator.remove();
                        continue;
                    }
                    if (parentUnsPo.getDataType().intValue() == topicDto.getParentDataType()) {
                        continue;
                    }
                }
                // 如果数据库不存在则从当前列表中查询
                CreateTopicDto parentDto = aliasMap.get(topicDto.getParentAlias());
                // 如果当前列表中已存在父级文件夹，并且类型为归类文件夹，则跳过
                if (parentDto != null && parentDto.getDataType() != null) {
                    if (parentDto.getDataType() > 0 && parentDto.getDataType().intValue() != topicDto.getParentDataType()) {
                        errorTip.put(topicDto.gainBatchIndex(), I18nUtils.getMessage("uns.category.type.not.eq"));
                        iterator.remove();
                        continue;
                    }
                    if (parentDto.getDataType().intValue() == topicDto.getParentDataType()) {
                        continue;
                    }
                }
                // 判断父节点下面有没有归类子文件夹，如果有则将当前文件的父级设置为已经存在的归类子文件夹
                List<UnsPo> existsCategoryFolders = categoryFolderMap.get(topicDto.getParentAlias());
                boolean bingo = false;
                if (existsCategoryFolders != null) {
                    for (UnsPo existsCategoryFolder : existsCategoryFolders) {
                        if (existsCategoryFolder.getDataType() != null && topicDto.getParentDataType() != null
                                && existsCategoryFolder.getDataType().intValue() == topicDto.getParentDataType().intValue()) {
                            topicDto.setParentAlias(existsCategoryFolder.getAlias());
                            bingo = true;
                            break;
                        }
                    }
                    if (bingo) {
                        continue;
                    }
                }
                String mountSource = parentUnsPo == null ? (parentDto == null ? null : parentDto.getMountSource()) : parentUnsPo.getMountSource();
                Integer mountType = parentUnsPo == null ? (parentDto == null ? null : parentDto.getMountType()) : parentUnsPo.getMountType();;

                // 在文件和其父节点之间插入一层归类文件夹
                CreateTopicDto categoryDto = buildCategoryFolderDto(topicDto.getParentAlias(), mountType, mountSource, topicDto.getParentDataType());
                topicDto.setParentAlias(categoryDto.getAlias());
                if (!newCategoryAliasMap.containsKey(categoryDto.getAlias())) {
                    newCategoryAliasMap.put(categoryDto.getAlias(), categoryDto);
                }
            }
        }
        if (!newCategoryAliasMap.isEmpty()) {
            newTopicDtos.addAll(newCategoryAliasMap.values());
        }
        return newTopicDtos;
    }

    static class FileMap extends HashMap<String, CreateTopicDto> {
        public FileMap(int initialCapacity) {
            super(initialCapacity);
        }

        Consumer<CreateTopicDto> removeConsumer;

        @Override
        public CreateTopicDto remove(Object key) {
            CreateTopicDto old = super.remove(key);
            if (removeConsumer != null && old != null) {
                removeConsumer.accept(old);
            }
            return old;
        }
    }

    public Map<String, String> createModelAndInstancesInner(final CreateModelInstancesArgs args) {
        final Map<String, String> errTipMap = new HashMap<>();
        if (systemConfig.getEnableAutoCategorization()) {
            // 对文件进行归类
            args.topics = appendCategoryFolders(args.topics, errTipMap);
        }
        List<CreateTopicDto> topicDtos = args.topics;
        if (log.isDebugEnabled()) {
            log.debug("createModelAndInstances args:{}", args);
        } else if (topicDtos.size() == 1) {
            log.info("UnsAdd: {}", topicDtos.get(0));
        } else if (topicDtos.size() > 1) {
            log.info("UnsAdd[{}]: {} ~ {}", topicDtos.size(), topicDtos.get(0).getAlias(), topicDtos.get(topicDtos.size() - 1).getAlias());
        }

        FileMap paramFiles = new FileMap(topicDtos.size());
        Map<String, CreateTopicDto> paramFolders = initParamsUns(topicDtos, errTipMap, paramFiles);
        if (paramFolders.isEmpty() && paramFiles.isEmpty()) {
            log.warn("不存在任何文件夹或文件, 无法继续保存");
            return errTipMap;
        }
        //
        HashSet<Long> ids = new HashSet<>();
        HashSet<String> aliasSet = new HashSet<>(topicDtos.size());
        addAlias(paramFolders.values(), aliasSet, ids);
        addAlias(paramFiles.values(), aliasSet, ids);
        HashMap<Long, UnsPo> dbFiles = new HashMap<>(topicDtos.size());
        Map<String, UnsPo> existsUns = this.listUnsByAliasAndIds(aliasSet, ids, dbFiles);
        tryFillIdOrAlias(paramFiles, existsUns, dbFiles, errTipMap);
        HashMap<String, Long> histAliasIdMap = new HashMap<>(8 + aliasSet.size() / 8);

        if (aliasSet.size() <= 1000) {
            addOldId(aliasSet, histAliasIdMap);
        } else {
            for (List<String> aliasList : Lists.partition(new ArrayList<>(aliasSet), 1000)) {
                addOldId(aliasList, histAliasIdMap);
            }
        }

        HashMap<Long, UnsPo> addFiles = new HashMap<>(topicDtos.size());
        HashMap<String, UnsPo> aliasMap = new HashMap<>(topicDtos.size());

        ArrayList<CreateTopicDto> folders = new ArrayList<>(paramFolders.values());
        // 对目录按树的层级降序排列（根节点在最高层），确保层级高的先处理，子孙可简单获取 parentId
        if (folders.size() > 1) {
            Map<String, List<String>> reverseGraph = buildReverseGraph(folders, CreateTopicDto::getAlias, CreateTopicDto::getParentAlias);
            Map<String, Integer> levelMap = calculateLevels(reverseGraph);
            folders.sort((o1, o2) -> {
                String a1 = o1.getAlias(), a2 = o2.getAlias();
                return levelMap.get(a2) - levelMap.get(a1);
            });
        }
        // 找出 parentAlias 或 name 有修改的最高层目录，后面需要获取它的整个子树，为更新 layRec 做准备
        tryAddLayRecOrPathChangedChildren(folders, paramFiles.values(), existsUns, dbFiles);

        final Function<String, UnsPo> allUns = alias -> {
            UnsPo unsPo = aliasMap.get(alias);
            if (unsPo == null) {
                unsPo = existsUns.get(alias);
            }
            return unsPo;
        };

        for (CreateTopicDto bo : folders) {
            UnsPo po = trySetId(args, bo, histAliasIdMap, allUns, dbFiles, errTipMap);
            if (po != null) {
                addFiles.put(po.getId(), po);
                aliasMap.put(po.getAlias(), po);
            }
        }
        Map<Long, UnsPoLabels> unsPoLabels = new HashMap<Long, UnsPoLabels>(paramFiles.size());
        for (CreateTopicDto bo : paramFiles.values()) {
            UnsPo po = trySetId(args, bo, histAliasIdMap, allUns, dbFiles, errTipMap);
            if (po != null) {
                addFiles.put(po.getId(), po);
                aliasMap.put(po.getAlias(), po);
                if (bo.getLabelNames() != null) {
                    unsPoLabels.put(po.getId(), new UnsPoLabels(po, dbFiles.containsKey(po.getId()), bo.getLabelNames()));
                }
            }
        }
        paramFiles.removeConsumer = dto -> addFiles.remove(dto.getId());
        // 校验和更新计算实例引用
        final Function<Long, UnsPo> allIdUns = id -> {
            UnsPo unsPo = addFiles.get(id);
            if (unsPo == null) {
                unsPo = dbFiles.get(id);
            }
            return unsPo;
        };
        final Date createTime = new Date();
        List<UnsPo> refUpdates = unsCalcService.tryUpdateCalcRefUns(errTipMap, paramFiles, allIdUns, dbFiles::get, createTime);
        if (refUpdates != null) {
            for (UnsPo refUpdate : refUpdates) {
                dbFiles.put(refUpdate.getId(), refUpdate);
            }
        }
        if (!args.fromImport && !errTipMap.isEmpty() && addFiles.isEmpty()) {
            log.warn("Won't save when errTips: {}", errTipMap);
            return errTipMap;
        }

        this.aliasToId(addFiles, allUns);

        LayRecUtils.SaveOrUpdate rs = LayRecUtils.setLayRecAndPath(addFiles, dbFiles);

        ArrayList<CreateTopicDto> createList = new ArrayList<>(addFiles.size());
        ArrayList<CreateTopicDto> dtoUpdateList = new ArrayList<>(addFiles.size());
        for (UnsPo file : addFiles.values()) {
            CreateTopicDto createTopicDto = UnsConverter.po2dto(file);
            UnsPo dbF = dbFiles.get(file.getId());
            if (dbF == null) {
                dbF = existsUns.get(file.getAlias());
            }
            UnsPoLabels labels = unsPoLabels.get(file.getId());
            if (labels != null) {
                labels.setDto(createTopicDto);
            }
            if (dbF != null) {
                if (file.getPathType() == Constants.PATH_TYPE_FILE) {
                    createTopicDto.setFieldsChanged(!Arrays.deepEquals(file.getFields(), dbF.getFields()));
                }
                dtoUpdateList.add(createTopicDto);
            } else {
                createList.add(createTopicDto);
            }
        }
        for (UnsPo po : rs.updateList) {
            Long id = po.getId();
            if (!addFiles.containsKey(id)) {
                dtoUpdateList.add(UnsConverter.po2dto(po, false));
            }
        }
        if (refUpdates != null && !refUpdates.isEmpty()) {
            for (UnsPo refPo : refUpdates) {
                Long id = refPo.getId();
                UnsPo po = dbFiles.get(id);
                rs.updateList.add(po);
                if (!addFiles.containsKey(id)) {
                    dtoUpdateList.add(UnsConverter.po2dto(po, false));
                }
            }
        }
        this.saveBatchAndSendEvent(args, rs.insertList, rs.updateList, createList, dtoUpdateList, unsPoLabels.values());

        return errTipMap;
    }

    private void addOldId(Collection<String> alias, HashMap<String, Long> histAliasIdMap) {
        unsHistoryDeleteJobMapper.selectList(new QueryWrapper<UnsHistoryDeleteJobPo>()
                .in("alias", alias)
                .select("id", "alias")).forEach(po -> {
            histAliasIdMap.put(po.getAlias(), po.getId());
        });
    }

    private void tryAddLayRecOrPathChangedChildren(Collection<CreateTopicDto> paramFolders, Collection<CreateTopicDto> paramFiles, Map<String, UnsPo> existsUns, HashMap<Long, UnsPo> dbFiles) {
        LinkedList<UnsPo> changedSubTree = new LinkedList<>();
        HashSet<String> parentAliasSet = new HashSet<>();
        scanChangedNodes(paramFolders, existsUns, parentAliasSet, changedSubTree);
        scanChangedNodes(paramFiles, existsUns, parentAliasSet, changedSubTree);
        final int sizeTree = changedSubTree.size();
        final int sizeSiblings = parentAliasSet.size();
        if (sizeTree + sizeSiblings == 0) {
            return;
        }
        QueryWrapper<UnsPo> subTreeQuery = new QueryWrapper<>();
        if (sizeTree > 0) {
            List<UnsPo> topNodes = changedSubTree.size() > 1 ? LeastTopNodeUtil.getLeastTopNodes(changedSubTree) : changedSubTree;
            for (UnsPo po : topNodes) {
                subTreeQuery = subTreeQuery.or().likeRight("lay_rec", po.getLayRec() + "/");
            }
        }
        boolean hitchhike = false;
        if (sizeSiblings > 0) {
            boolean hasNull = parentAliasSet.remove(null);
            if (sizeSiblings <= 500) {
                hitchhike = true;
                if (hasNull) {
                    subTreeQuery = subTreeQuery.or().isNull("parent_alias");
                }
                if (!parentAliasSet.isEmpty()) {
                    subTreeQuery = subTreeQuery.or().in("parent_alias", parentAliasSet);
                }
            } else {
                if (hasNull) {
                    List<UnsPo> siblings = list(new QueryWrapper<UnsPo>().isNull("parent_alias"));
                    add2Exists(siblings, dbFiles, existsUns);
                }
                for (List<String> alias : Lists.partition(new ArrayList<>(parentAliasSet), 500)) {
                    List<UnsPo> siblings = this.list(new QueryWrapper<UnsPo>().in("parent_alias", alias));
                    add2Exists(siblings, dbFiles, existsUns);
                }
            }
        }
        if (sizeTree > 0 || hitchhike) {
            List<UnsPo> needChangeLayRecOrPathChildren = this.list(subTreeQuery);
            add2Exists(needChangeLayRecOrPathChildren, dbFiles, existsUns);
        }
    }

    private static void add2Exists(List<UnsPo> fs, HashMap<Long, UnsPo> dbFiles, Map<String, UnsPo> existsUns) {
        for (UnsPo po : fs) {
            dbFiles.put(po.getId(), po);
            existsUns.put(po.getAlias(), po);
        }
    }

    private static void scanChangedNodes(Collection<CreateTopicDto> files, Map<String, UnsPo> existsUns, HashSet<String> parentAliasSet, LinkedList<UnsPo> changedSubTree) {
        for (CreateTopicDto bo : files) {
            String alias = bo.getAlias();
            UnsPo dbo = existsUns.get(alias);
            String parentAlias = bo.getParentAlias();
            if (dbo == null) {
                parentAliasSet.add(parentAlias);
            } else if (!Objects.equals(parentAlias, dbo.getParentAlias())) {
                if (bo.getPathType() == Constants.PATH_TYPE_DIR) {
                    changedSubTree.add(dbo);
                }
                parentAliasSet.add(parentAlias);
            } else if (!Objects.equals(bo.getName(), dbo.getName())) {
                if (bo.getPathType() == Constants.PATH_TYPE_DIR) {
                    changedSubTree.add(dbo);
                }
                parentAliasSet.add(parentAlias);
            } else {
                parentAliasSet.add(parentAlias);
            }
        }
    }

    private static void tryFillIdOrAlias(FileMap paramFiles, Map<String, UnsPo> existsUns, HashMap<Long, UnsPo> dbFiles, Map<String, String> errTipMap) {
        Iterator<Map.Entry<String, CreateTopicDto>> itr = paramFiles.entrySet().iterator();
        while (itr.hasNext()) {
            CreateTopicDto bo = itr.next().getValue();
            Long id = bo.getId();
            String alias = bo.getAlias();
            if (id != null && alias == null) {
                UnsPo po = dbFiles.get(id);
                if (po != null) {
                    bo.setAlias(po.getAlias());
                }
            } else if (id == null && alias != null) {
                UnsPo po = existsUns.get(alias);
                if (po != null) {
                    bo.setId(po.getId());
                }
            }
            Long pid = bo.getParentId();
            String parentAlias = bo.getParentAlias();
            if (pid == null && parentAlias != null) {
                UnsPo parent = existsUns.get(parentAlias);
                if (parent != null) {
                    bo.setParentId(parent.getId());
                }
            } else if (parentAlias == null && pid != null) {
                UnsPo parent = dbFiles.get(pid);
                if (parent != null) {
                    bo.setParentAlias(parent.getAlias());
                }
            }
            InstanceField[] refers = bo.getRefers();
            if (ArrayUtils.isNotEmpty(refers)) {
                for (InstanceField field : refers) {
                    Long refId = field.getId();
                    String refAlias = field.getAlias();
                    if (refId == null && refAlias != null) {
                        UnsPo refPo = existsUns.get(refAlias);
                        if (refPo != null) {
                            field.setId(refPo.getId());
                        } else {
                            itr.remove();
                            errTipMap.put(bo.gainBatchIndex(), I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", bo.getAlias()));
                        }
                    } else if (refId != null && refAlias == null) {
                        UnsPo refPo = dbFiles.get(refId);
                        if (refPo != null) {
                            field.setAlias(refPo.getAlias());
                        } else {
                            itr.remove();
                            errTipMap.put(bo.gainBatchIndex(), I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", bo.getAlias()));
                        }
                    }
                }
            }
        }
    }

    private void aliasToId(HashMap<Long, UnsPo> addFiles, Function<String, UnsPo> aliasMap) {
        for (UnsPo file : addFiles.values()) {

            String modelAlias = file.getModelAlias();
            if (modelAlias != null) {
                UnsPo model = aliasMap.apply(modelAlias);
                if (model != null) {
                    file.setModelId(model.getId());
                }
            }
            String parentAlias = file.getParentAlias();
            if (parentAlias != null) {
                UnsPo parent = aliasMap.apply(parentAlias);
                if (parent != null) {
                    file.setParentId(parent.getId());
                }
            }
        }
    }

    private UnsPo trySetId(CreateModelInstancesArgs args, CreateTopicDto bo, HashMap<String, Long> histAliasIdMap, Function<String, UnsPo> existsUns, HashMap<Long, UnsPo> dbFiles, Map<String, String> errTipMap) {
        final String batchIndex = bo.gainBatchIndex();
        boolean[] invalid = new boolean[1];
        UnsPo template = this.getTemplate(bo, existsUns, dbFiles, batchIndex, errTipMap, invalid);
        if (invalid[0]) {
            return null;
        }
        this.setJdbcType(bo);

        UnsPo dbPo = existsUns.apply(bo.getAlias());
        if (dbPo != null) {
            if (dbPo.getPathType().intValue() != bo.getPathType()) {
                String msg = I18nUtils.getMessage("uns.alias.has.exist.type",
                        I18nUtils.getMessage("uns.type." + dbPo.getPathType())
                        , I18nUtils.getMessage("uns.type." + bo.getPathType())
                );
                errTipMap.put(batchIndex, msg);
                return null;
            }
            bo.setId(dbPo.getId());
        } else {
            Long oldId = histAliasIdMap.get(bo.getAlias());
            bo.setId(oldId != null ? oldId : SuposIdUtil.nextId());
        }

        UnsPo newUns = newUnsFile(args, bo);
        if ((dbPo == null && bo.getDataType() != Constants.CITING_TYPE) || ArrayUtils.isNotEmpty(bo.getFields())) {
            if (setFieldsErr(bo, errTipMap, batchIndex, newUns, template)) {
                return null;
            }
        }
        if (bo.getDataType() == Constants.CITING_TYPE && bo.getFields() != null) {
            FieldDefine[] EMPTY = new FieldDefine[0];
            bo.setFields(EMPTY);
            newUns.setFields(EMPTY);
        }

        if (dbPo != null) {
            UnsPo tar = dbPo.clone();
            BeanUtil.copyProperties(newUns, tar, copyOptions);
            String expression = newUns.getExpression();
            boolean expChanged = (expression != null && !expression.equals(dbPo.getExpression()));
            boolean hasRefer = bo.getRefers() != null || bo.getReferIds() != null;
            if (expChanged || hasRefer) {
                bo = UnsConverter.po2dto(tar, false);
                if (hasRefer) {
                    String err = UnsCalcService.checkRefers(bo);
                    if (err != null) {
                        errTipMap.put(batchIndex, err);
                        return null;
                    }
                }
                if (expChanged) {
                    String err = UnsCalcService.checkExpression(bo);
                    if (err != null) {
                        errTipMap.put(batchIndex, err);
                        return null;
                    }
                }
            }
            newUns = tar;
        } else {
            if (bo.getFlags() == null) {
                newUns.setWithFlags(generateFlag(bo.getAddFlow(), bo.getSave2db(), bo.getAddDashBoard(), bo.getRetainTableWhenDeleteInstance(), bo.getAccessLevel()));
            }
            String err = UnsCalcService.checkRefers(bo);
            if (err == null) {
                err = UnsCalcService.checkExpression(bo);
            }
            if (err != null) {
                errTipMap.put(batchIndex, err);
                newUns = null;
            } else {
                newUns.setRefers(bo.getRefers());
                newUns.setExpression(bo.getExpression());
            }
        }
        return newUns;
    }

    private static final CopyOptions copyOptions = new CopyOptions();

    static {
        copyOptions.setIgnoreNullValue(true);
    }

    private static boolean setFieldsErr(CreateTopicDto bo, Map<String, String> errTipMap, String batchIndex, UnsPo instance, UnsPo template) {
        FieldDefine[] insFs = bo.getFields();//
        String path = bo.getAlias();
        SrcJdbcType jdbcType = bo.getDataSrcId();
        final boolean addSystemField = jdbcType != null && bo.getPathType() == Constants.PATH_TYPE_FILE && bo.getDataType() != Constants.ALARM_RULE_TYPE;
        if (ArrayUtil.isNotEmpty(insFs)) {
            String[] err = new String[1];
            FieldUtils.TableFieldDefine tfd = FieldUtils.processFieldDefines(path, jdbcType, insFs, err, true, addSystemField);
            if (err[0] != null) {
                errTipMap.put(batchIndex, err[0]);
                return true;
            }
            insFs = tfd.fields;
            instance.setTableName(tfd.tableName);
            instance.setFields(insFs);
        } else {
            insFs = null;
        }
        if (template != null && template.getFields() != null) {
            FieldDefine[] fields = template.getFields();
            if (ArrayUtil.isNotEmpty(insFs)) {
                String checkError = bo.getPathType() == Constants.PATH_TYPE_FILE ? checkInstanceFields(fields, insFs) : null;
                if (checkError != null) {
                    errTipMap.put(batchIndex, checkError);
                    return true;
                } else if (instance.getFields() == null) {
                    String[] err = new String[1];
                    FieldUtils.TableFieldDefine tfd = FieldUtils.processFieldDefines(path, jdbcType, fields, err, true);
                    if (err[0] != null) {
                        errTipMap.put(batchIndex, err[0]);
                        return true;
                    }
                    insFs = tfd.fields;
                    instance.setTableName(tfd.tableName);
                    instance.setFields(insFs);
                }
            } else if (addSystemField) {
                String[] err = new String[1];
                FieldUtils.TableFieldDefine tfd = FieldUtils.processFieldDefines(path, jdbcType, fields, err, true);
                if (err[0] != null) {
                    errTipMap.put(batchIndex, err[0]);
                    return true;
                }
                insFs = tfd.fields;
                instance.setTableName(tfd.tableName);
                bo.setFields(insFs);
                instance.setFields(insFs);
            }
        }
        if (bo.getPathType() == Constants.PATH_TYPE_FILE && ArrayUtils.isEmpty(instance.getFields())) {
            errTipMap.put(batchIndex, I18nUtils.getMessage("uns.field.empty"));
            return true;
        }
        return false;
    }

    private UnsPo getTemplate(CreateTopicDto bo, Function<String, UnsPo> existsUns, HashMap<Long, UnsPo> dbFiles, String batchIndex, Map<String, String> errTipMap, boolean[] err) {
        UnsPo template = null;
        err[0] = false;
        Long modelId = bo.getModelId();
        String modelAlias = bo.getModelAlias();
        String folderAlias;
        if (modelId != null) {
            template = dbFiles.get(modelId);
            if (template != null && template.getPathType() != 1) {
                String msg = I18nUtils.getMessage("uns.alias.has.exist.type",
                        I18nUtils.getMessage("uns.type." + template.getPathType())
                        , I18nUtils.getMessage("uns.type.1")
                );
                errTipMap.put(batchIndex, msg);
                err[0] = true;
            }
        } else if (modelAlias != null) {
            template = existsUns.apply(modelAlias);
            if (template != null && template.getPathType() != 1) {
                String msg = I18nUtils.getMessage("uns.alias.has.exist.type",
                        I18nUtils.getMessage("uns.type." + template.getPathType())
                        , I18nUtils.getMessage("uns.type.1")
                );
                errTipMap.put(batchIndex, msg);
                err[0] = true;
            } else if (template == null) {
                String msg = I18nUtils.getMessage("uns.template.not.exists");
                errTipMap.put(batchIndex, msg);
                err[0] = true;
            }
        } else if ((folderAlias = bo.getParentAlias()) != null) {
            UnsPo folder = existsUns.apply(folderAlias);
            if (folder == null) {
                String msg = I18nUtils.getMessage("uns.folder.not.found");
                errTipMap.put(batchIndex, msg);
                err[0] = true;
            } else if (folder.getPathType() != 0) {
                String msg = I18nUtils.getMessage("uns.alias.has.exist.type",
                        I18nUtils.getMessage("uns.type." + folder.getPathType())
                        , I18nUtils.getMessage("uns.type.0")
                );
                errTipMap.put(batchIndex, msg);
                err[0] = true;
            }
        }
        return template;
    }

    private void setJdbcType(CreateTopicDto bo) {
        final Integer DATA_TYPE = bo.getDataType();
        SrcJdbcType jdbcType = bo.getDataSrcId();
        if (jdbcType == null && DATA_TYPE != null && Constants.PATH_TYPE_FILE == bo.getPathType()) {
            switch (DATA_TYPE) {
                case Constants.CALCULATION_HIST_TYPE:
                case Constants.CALCULATION_REAL_TYPE:
                case Constants.TIME_SEQUENCE_TYPE:
                    jdbcType = timeDataType;
                    break;
                case Constants.ALARM_RULE_TYPE:
                case Constants.RELATION_TYPE:
                case Constants.MERGE_TYPE:
                case Constants.JSONB_TYPE:
                    jdbcType = relationType;
                    break;
            }
            bo.setDataSrcId(jdbcType);
        }
    }

    private static UnsPo newUnsFile(CreateModelInstancesArgs args, CreateTopicDto bo) {
        String alias = bo.getAlias();
        UnsPo instance = new UnsPo(bo.getId(), alias, bo.getName(), bo.getPathType(), bo.getDataType(), null, null, bo.getDescription());
        SrcJdbcType jdbcType = bo.getDataSrcId();
        if (jdbcType != null) {
            instance.setDataSrcId(jdbcType.id);
        }
        instance.setDisplayName(bo.getDisplayName());
        instance.setDataPath(bo.getDataPath());
        instance.setAlias(alias);
        instance.setName(bo.getName());
        instance.setModelAlias(bo.getModelAlias());
        instance.setParentAlias(bo.getParentAlias());
        instance.setTableName(bo.getTableName());
        instance.setWithFlags(bo.getFlags());
        instance.setModelId(bo.getModelId());
        instance.setModelAlias(bo.getModelAlias());
        instance.setSubscribeAt(bo.getSubscribeAt());
        if (bo.getPathType() == Constants.PATH_TYPE_FILE) {
            if (bo.getParentDataType() != null) {
                instance.setParentDataType(bo.getParentDataType());
            }
            if (ArrayUtils.isNotEmpty(bo.getFields())) {
                instance.setNumberFields(bo.countNumberFields());
            }
        }
        instance.setExtend(bo.getExtend());
        if (bo.getRefers() != null) {
            instance.setRefers(bo.getRefers());
        }
        instance.setExpression(bo.getExpression());
        Map<String, Object> protocol = bo.getProtocol();
        if (protocol != null && protocol.size() > 0) {
            Object protocolType = protocol.get("protocol");
            if (protocolType != null) {
                instance.setProtocolType(protocolType.toString());
            }
            Object protocolBean = bo.getProtocolBean();
            instance.setProtocol(JsonUtil.toJson(Objects.requireNonNullElse(protocolBean, protocol)));
        }
        if (bo.getExtendFieldUsed() != null) {
            instance.setExtendFieldFlags(FieldUtils.generateFlag(bo.getExtendFieldUsed()));
        }
        instance.setMountType(bo.getMountType());
        instance.setMountSource(bo.getMountSource());
        return instance;
    }

    private void saveBatchAndSendEvent(CreateModelInstancesArgs args,
                                       Collection<UnsPo> insertList, Collection<UnsPo> updateList,
                                       List<CreateTopicDto> notifyCreateList, List<CreateTopicDto> notifyUpdateList,
                                       Collection<UnsPoLabels> unsLabels) {
        Consumer<RunningStatus> statusConsumer = args.statusConsumer;

        List<UnsLabelPo> labelPos = unsLabelService.makeLabel(unsLabels);
        Map<SrcJdbcType, CreateTopicDto[]> topics = Collections.emptyMap();
        if (!notifyCreateList.isEmpty()) {
            Map<SrcJdbcType, ArrayList<CreateTopicDto>> jdbcFiles = new HashMap<>(2);
            for (CreateTopicDto f : notifyCreateList) {
                jdbcFiles.computeIfAbsent(f.getDataSrcId(), k -> new ArrayList<>(notifyCreateList.size())).add(f);
            }
            Map tmp = jdbcFiles;
            for (Map.Entry<SrcJdbcType, ArrayList<CreateTopicDto>> entry : jdbcFiles.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue().toArray(new CreateTopicDto[0]));
            }
            topics = tmp;
        }
        BatchCreateTableEvent event = new BatchCreateTableEvent(this, args.fromImport, topics).setFlowName(args.flowName);
        AtomicInteger total = new AtomicInteger();
        if (statusConsumer != null) {
            setEventStatusCallback(statusConsumer, event, total);
        }
        UnsRemoveService.batchRemoveExternalTopic(event.topics.values()); // 删除在uns已经存在的topic
        EventBus.publishEvent(event);
        if (!CollectionUtils.isEmpty(insertList) || !CollectionUtils.isEmpty(updateList)) {
            if (statusConsumer != null) {
                long tStart = System.currentTimeMillis();
                String task = I18nUtils.getMessage("uns.create.task.name.uns");
                String startMsg = I18nUtils.getMessage("uns.create.status.running");
                final int N = total.get() + 1;
                statusConsumer.accept(new RunningStatus(N, N, task, startMsg).setProgress(98.0));
                Throwable err = null;
                try {
                    this.saveBatch(insertList);
                    this.updateBatchById(updateList);
                } catch (Throwable ex) {
                    err = ex;
                    throw ex;
                } finally {
                    String msg;
                    if (err == null) {
                        msg = I18nUtils.getMessage("uns.create.status.finished");
                    } else {
                        msg = I18nUtils.getMessage("uns.create.status.error") + err.getMessage();
                    }
                    statusConsumer.accept(new RunningStatus(N, N, task, msg)
                            .setSpendMills(System.currentTimeMillis() - tStart).setCode(err == null ? 0 : 400).setProgress(99.0));
                }
            } else {
                this.saveBatch(insertList);
                this.updateBatchById(updateList);
            }
        }
        if (!CollectionUtils.isEmpty(notifyUpdateList)) {
            EventBus.publishEvent(new UpdateInstanceEvent(this, notifyUpdateList));
            List<WebhookDataDTO> webhookData = WebhookUtils.transfer(updateList);
            if (!webhookData.isEmpty()) {
                EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                        ActionEnum.MODIFY, webhookData));
            }
        }
        EventBus.publishEvent(new NamespaceChangeEvent(this));
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(insertList);
        if (!webhookData.isEmpty()) {
            EventBus.publishEvent(new SysEvent(this, ServiceEnum.UNS_SERVICE, EventMetaEnum.UNS_FILED_CHANGE,
                    ActionEnum.ADD, webhookData));
        }
    }

    private void setEventStatusCallback(Consumer<RunningStatus> statusConsumer, BatchCreateTableEvent event, AtomicInteger total) {
        final String START_MSG = I18nUtils.getMessage("uns.create.status.running");
        final String END_MSG = I18nUtils.getMessage("uns.create.status.finished");
        final String ERR_MSG = I18nUtils.getMessage("uns.create.status.error");
        event.setDelegateAware(new EventStatusAware() {
            long t0;

            @Override
            public void beforeEvent(int N, int i, String listenerName) {
                total.compareAndSet(0, N);
                double progress = 0;
                if (i > 1 && N > 0) {
                    progress = ((int) (1000.0 * (i - 1) / N)) / 10.0;
                }
                statusConsumer.accept(new RunningStatus(N + 1, i, listenerName, START_MSG).setProgress(progress));
                t0 = System.currentTimeMillis();
            }

            @Override
            public void afterEvent(int N, int i, String listenerName, Throwable ex) {
                String msg;
                if (ex == null) {
                    msg = END_MSG;
                } else {
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        msg = cause.getMessage();
                    } else {
                        msg = ex.getMessage();
                    }
                    if (msg == null) {
                        msg = ERR_MSG;
                    } else {
                        msg = ERR_MSG + msg;
                    }
                }
                statusConsumer.accept(new RunningStatus(N + 1, i, listenerName, msg)
                        .setSpendMills(System.currentTimeMillis() - t0).setCode(ex == null ? 0 : 500));
            }
        });
    }

    private static String checkInstanceFields(FieldDefine[] modelFields, FieldDefine[] insFields) {
        if (modelFields == null) {
            modelFields = new FieldDefine[0];
        }
        if (insFields == null) {
            insFields = new FieldDefine[0];
        }
        HashMap<String, FieldDefine> insMap = new HashMap<>(insFields.length);
        for (FieldDefine insField : insFields) {
            String name = insField.getName();
            if (!insField.isSystemField() && insMap.put(name, insField) != null) {
                return "fields name duplicate: " + name;
            }
        }
        for (FieldDefine mf : modelFields) {
            String name = mf.getName();
            if (!mf.isSystemField()) {
                FieldDefine insF = insMap.remove(name);
                if (insF == null) {
                    return "instance need field: " + name;
                } else if (!mf.getType().equals(insF.getType())) {
                    return String.format("instance field type changed: %s, %s -> %s", name, mf.getType(), insF.getType());
                }
            }
        }
        if (!insMap.isEmpty()) {
            return "instance has unknown Fields in model: " + insMap.values();
        }
        return null;
    }

    private static Map<String, CreateTopicDto> initParamsUns(List<CreateTopicDto> topicDtos,
                                                             Map<String, String> errTipMap,
                                                             Map<String, CreateTopicDto> paramFiles) {
        Map<String, CreateTopicDto> paramFolders = new HashMap<>(2 + topicDtos.size() / 2);
        for (CreateTopicDto dto : topicDtos) {
            checkTopicDto(errTipMap, paramFolders, paramFiles, dto);
        }
        return paramFolders;
    }

    private static void addAlias(Collection<CreateTopicDto> bos, HashSet<String> aliasSet, HashSet<Long> ids) {
        for (CreateTopicDto dto : bos) {
            {
                String alias = dto.getAlias();
                if (alias != null) {
                    aliasSet.add(alias);
                }
            }
            {
                String refAlias = dto.getReferUns();
                if (refAlias != null) {
                    aliasSet.add(refAlias);
                }
            }
            {
                String modelAlis = dto.getModelAlias();
                if (modelAlis != null) {
                    aliasSet.add(modelAlis);
                }
            }
            {
                String folderAlias = dto.getParentAlias();
                if (folderAlias != null) {
                    aliasSet.add(folderAlias);
                }
            }
            {
                Long[] referIds = dto.getReferIds();
                if (ArrayUtils.isNotEmpty(referIds)) {
                    ids.addAll(Arrays.asList(referIds));
                    InstanceField[] refers = dto.getRefers();
                    if (ArrayUtils.isEmpty(refers)) {
                        refers = new InstanceField[referIds.length];
                        for (int i = 0; i < referIds.length; i++) {
                            refers[i] = new InstanceField(referIds[i], null);
                        }
                        dto.setRefers(refers);
                    }
                }
            }
            Long unsId = dto.getId(), pid = dto.getParentId(), mid = dto.getModelId();
            if (unsId != null) {
                ids.add(unsId);
            }
            if (pid != null) {
                ids.add(pid);
            }
            if (mid != null) {
                ids.add(mid);
            }
            InstanceField[] refers = dto.getRefers();
            if (ArrayUtils.isNotEmpty(refers)) {
                for (InstanceField field : refers) {
                    Long id = field.getId();
                    if (id != null) {
                        ids.add(id);
                    }
                    String alias = field.getAlias();
                    if (alias != null) {
                        aliasSet.add(alias);
                    }
                }
            }
        }
    }

    private static void checkTopicDto(Map<String, String> errTipMap,
                                      Map<String, CreateTopicDto> paramFolders,
                                      Map<String, CreateTopicDto> paramFiles,
                                      final CreateTopicDto dto) {
        final int pathType = dto.getPathType();
        /*if (pathType == Constants.PATH_TYPE_DIR) {
            dto.setDataType(null);
        }*/
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        String batchIndex = dto.gainBatchIndex();
        if (!violations.isEmpty()) {
            StringBuilder er = new StringBuilder(128);
            addValidErrMsg(er, violations);
            errTipMap.put(batchIndex, er.toString());
            return;
        }
        String alias = dto.getAlias();
        if (paramFolders.containsKey(alias) || paramFiles.containsKey(alias)) {
            String msg = I18nUtils.getMessage("uns.alias.duplicate");
            errTipMap.put(batchIndex, msg);
            return;
        }
        if (pathType == Constants.PATH_TYPE_DIR) {// current is folder
            if (dto.getDataType() == null) {
                dto.setDataType(0);
            }
            paramFolders.put(alias, dto);
        } else if (pathType == Constants.PATH_TYPE_FILE) { // current is file
            Integer dataType = dto.getDataType();
            if (dataType == null) {
                String msg = I18nUtils.getMessage("uns.file.dataType.empty");
                errTipMap.put(batchIndex, msg);
                return;
            } else if (!Constants.isValidDataType(dataType)) {
                String msg = I18nUtils.getMessage("uns.file.dataType.invalid", dataType);
                errTipMap.put(batchIndex, msg);
                return;
            }
            FieldDefine[] fields = dto.getFields();
            if (ArrayUtil.isEmpty(fields) && dataType == Constants.MERGE_TYPE) {
                FieldDefine mergeField = new FieldDefine("data_json", FieldType.STRING);
                mergeField.setMaxLen(512 * 1024);// 聚合的字段总长度限制改大，不能超过mqtt消息长度限制
                fields = new FieldDefine[]{mergeField};
                dto.setFields(fields);
            }
            if (dto.getFrequency() != null) {
                Map<String, Object> protocol = dto.getProtocol();
                if (protocol == null) {
                    protocol = new HashMap<>();
                }
                String frequency = dto.getFrequency();
                protocol.put("frequency", frequency);
                dto.setProtocol(protocol);
                dto.setFrequencySeconds(getFrequencySeconds(frequency));
            }
            paramFiles.put(alias, dto);
        } else {
            dto.setDataType(0);
        }
    }

    private static Long getFrequencySeconds(String frequency) {
        Long nano = TimeUnits.toNanoSecond(frequency);
        if (nano != null) {
            long frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            return frequencySeconds;
        }
        return null;
    }

    /*private static final Snowflake UNS_SNOW = new Snowflake();

    public static long nextId() {
        return UNS_SNOW.nextId();
    }*/

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(100)
    void onStartup(ContextRefreshedEvent event) {
        {
            DataStorageServiceHelper storageServiceHelper = event.getApplicationContext().getBean(DataStorageServiceHelper.class);
            if (storageServiceHelper.getRelationDbEnabled() == null) {
                log.error("storageServiceHelper RelationDbEnabled is null");
                return;
            }
            relationType = storageServiceHelper.getRelationDbEnabled().getJdbcType();
            timeDataType = storageServiceHelper.getSequenceDbEnabled().getJdbcType();

            log.info("** timeDataType={}, relationType={}", timeDataType, relationType);
            reSyncCache();
        }

    }

    public void reSyncCache() {
        int cacheSize = unsDefinitionService.getTopicDefinitionMap().size();
        Long dbSize = baseMapper.selectCount(new QueryWrapper<>());
        if (dbSize != null && dbSize.intValue() != cacheSize) {
            log.info("尝试重新同步缓存...");
            HashMap<SrcJdbcType, List<CreateTopicDto>> typeListMap = new HashMap<>();
            Constants.readOnlyMode.set(true);
            unsDefinitionService.getTopicDefinitionMap().clear();
            unsDefinitionService.getAliasMap().clear();
            unsDefinitionService.getPathMap().clear();
            List<UnsPo> instances = baseMapper.selectList(new QueryWrapper<>());
            if (!CollectionUtils.isEmpty(instances)) {
                for (UnsPo p : instances) {
                    CreateTopicDto dto = UnsConverter.po2dto(p);
                    typeListMap.computeIfAbsent(dto.getDataSrcId(), k -> new LinkedList<>()).add(dto);
                }
            }
            EventBus.publishEvent(new InitTopicsEvent(this, typeListMap));
            log.info("重新同步缓存完毕.");
        }
    }

    @EventListener(classes = UnsFirstDataSavedEvent.class)
    void onUnsFirstDataSavedEvent(UnsFirstDataSavedEvent event) {
        UnsPo po = new UnsPo();
        po.setId(event.unsId);
        po.setWithFlags(event.unsFlags);
        baseMapper.updateById(po);
    }

    private Map<String, UnsPo> listUnsByAliasAndIds(Set<String> alias, Set<Long> ids, HashMap<Long, UnsPo> dbFiles) {
        HashMap<String, UnsPo> aliasMap = new HashMap<>(alias.size() + (ids != null ? ids.size() : 16));
        for (List<String> list : Lists.partition(new ArrayList<>(alias), Constants.SQL_BATCH_SIZE)) {
            List<UnsPo> unsPos = baseMapper.listByAlias(list);
            addDbPo(unsPos, dbFiles, aliasMap);
        }
        if (!CollectionUtils.isEmpty(ids)) {
            for (List<Long> list : Lists.partition(new ArrayList<>(ids), Constants.SQL_BATCH_SIZE)) {
                List<UnsPo> unsPos = baseMapper.selectByIds(list);
                addDbPo(unsPos, dbFiles, aliasMap);
            }
        }
        return aliasMap;
    }

    private static void addDbPo(List<UnsPo> unsPos, HashMap<Long, UnsPo> dbFiles, HashMap<String, UnsPo> aliasMap) {
        for (UnsPo po : unsPos) {
            aliasMap.put(po.getAlias(), po);
            dbFiles.put(po.getId(), po);
        }
    }

    static final Validator validator;
    static final Pattern ALIAS_PATTERN;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);
    }

    private static void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            er.append('[').append(t).append('.').append(v.getPropertyPath()).append(' ').append(I18nUtils.getMessage(v.getMessage())).append(']');
        }
    }

    //        @EventListener(classes = ContextRefreshedEvent.class)
//    @Order
    void benchAddUns5w() {//启动时造数据
        Long pid = null;
        {
            CreateTopicDto po = new CreateTopicDto();
            po.setAlias("bench4del");
            po.setName(po.getAlias());
            po.setPathType(0);
            createModelAndInstance(List.of(po), false);
            pid = baseMapper.listByAlias(List.of("bench4del")).get(0).getId();
        }
        for (int i = 1, k = 0; i <= 200; i++) {
            ArrayList<UnsPo> list = new ArrayList<>(1000);
            for (int n = 1; n <= 1000; n++) {
                UnsPo po = new UnsPo();
                list.add(po);
                po.setId(SuposIdUtil.nextId());
                po.setParentId(pid);
                po.setParentAlias("bench4del");
                po.setAlias("mock4d_" + (++k));
                po.setName(po.getAlias());
                po.setPathType(2);
                po.setDataType(1);
                po.setLayRec(pid + "/" + po.getId());
                po.setPath("bench4del/" + po.getName());
                po.setNumberFields(1);
                po.setWithFlags(0);
                po.setFields(new FieldDefine[]{
                        new FieldDefine("value", FieldType.DOUBLE),
                });
            }
            saveBatch(list);
            log.info("造数据: batch: {}", i);
        }
    }
}
