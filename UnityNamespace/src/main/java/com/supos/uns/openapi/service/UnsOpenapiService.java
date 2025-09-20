package com.supos.uns.openapi.service;

import ch.qos.logback.core.joran.sanity.Pair;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.*;
import com.supos.common.vo.LabelVo;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.*;
import com.supos.uns.openapi.dto.CreateFileDto;
import com.supos.uns.openapi.dto.CreateFolderDto;
import com.supos.uns.openapi.vo.FileDetailVo;
import com.supos.uns.openapi.vo.FolderDetailVo;
import com.supos.uns.openapi.vo.LabelOpenVo;
import com.supos.uns.service.*;
import com.supos.uns.util.PageUtil;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.vo.InstanceFieldVo;
import com.supos.uns.vo.UnsDataResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsOpenapiService {

    @Autowired
    UnsMapper unsMapper;
    @Autowired
    UnsAddService unsAddService;
    @Autowired
    UnsDefinitionService unsDefinitionService;
    @Autowired
    UnsTemplateService unsTemplateService;
    @Autowired
    UnsQueryService unsQueryService;
    @Autowired
    UnsLabelService unsLabelService;

    private static final List<String> SYSTEM_KEY_WORDS = Arrays.asList("label","template");

    public ResultVO<FolderDetailVo> folderDetailByAlias(String alias){
        FolderDetailVo folderVo = new FolderDetailVo();
        UnsPo po = unsMapper.getByAlias(alias);
        if (po == null || ObjectUtil.notEqual(Constants.PATH_TYPE_DIR, po.getPathType())) {
            return ResultVO.fail(I18nUtils.getMessage("uns.model.not.found"));
        }
        po2FolderVo(folderVo, po);
        return ResultVO.successWithData(folderVo);
    }

    public ResultVO<FolderDetailVo> folderDetailByPath(String path){
        FolderDetailVo folderVo = new FolderDetailVo();
        UnsPo po = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                .eq(UnsPo::getPathType, Constants.PATH_TYPE_DIR)
                .eq(UnsPo::getPath, path));
        if (po == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.model.not.found"));
        }
        po2FolderVo(folderVo, po);
        return ResultVO.successWithData(folderVo);
    }

    public ResultVO<FileDetailVo> fileDetailByAlias(String alias){
        FileDetailVo fileVo = new FileDetailVo();
        UnsPo po = unsMapper.getByAlias(alias);
        if (po == null || ObjectUtil.notEqual(Constants.PATH_TYPE_FILE, po.getPathType())) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.not.found"));
        }
        po2FileVo(fileVo, po);
        return ResultVO.successWithData(fileVo);
    }

    public ResultVO<FileDetailVo> fileDetailByPath(String path){
        FileDetailVo fileVo = new FileDetailVo();
        UnsPo file = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                .eq(UnsPo::getPathType, Constants.PATH_TYPE_FILE)
                .eq(UnsPo::getPath, path));
        if (file == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.not.found"));
        }

        po2FileVo(fileVo, file);
        return ResultVO.successWithData(fileVo);
    }

    public ResultVO createFolder(CreateFolderDto dto){
        UnsPo uns = unsMapper.getByAlias(dto.getAlias());
        if (uns != null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.alias.has.exist"));
        }
        if (SYSTEM_KEY_WORDS.contains(dto.getName())) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.reserved.word"));
        }

        String parentAlias = dto.getParentAlias();
        if (StringUtils.isNotBlank(parentAlias)) {
            UnsPo parent = unsMapper.getByAlias(parentAlias);
            if (parent == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.parent.alias.not.found"));
            }
        }

        String templateAlias = dto.getTemplateAlias();
        if (StringUtils.isNotBlank(templateAlias)) {
            UnsPo template = unsMapper.getByAlias(templateAlias);
            if (template == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
            }
            dto.setDefinition(template.getFields());
        }
        //没有引用模板  校验
        if (StringUtils.isBlank(templateAlias) && ArrayUtils.isNotEmpty(dto.getDefinition())) {
            for (FieldDefine fieldDefine : dto.getDefinition()) {
                FieldType fieldType = fieldDefine.getType();
                if (fieldType == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.invalid.field.type"));
                }
                boolean hasSystemFiled = Constants.systemFields.contains(fieldDefine.getName());
                if (hasSystemFiled) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.has.system.field"));
                }
                if (!FieldUtils.FIELD_NAME_PATTERN.matcher(fieldDefine.getName()).matches()) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.name.format.invalid", fieldDefine.getName()));
                }
                //96998 【UNS-openapi】创建文件夹，字段定义非string类型传了长度，建议忽略
                if (fieldType != FieldType.STRING) {
                    fieldDefine.setMaxLen(null);
                }
            }
        }
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        createTopicDto.setPathType(Constants.PATH_TYPE_DIR);
        createTopicDto.setFields(dto.getDefinition());
        createTopicDto.setExtend(dto.getExtendProperties());
        createTopicDto.setModelAlias(dto.getTemplateAlias());
        if (StringUtils.isBlank(dto.getAlias())) {
            createTopicDto.setAlias(PathUtil.generateAlias(dto.getName(), Constants.PATH_TYPE_DIR));
        }
        JsonResult<String> jsonResult = unsAddService.createModelInstance(createTopicDto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        return ResultVO.success("ok");
    }

    public ResultVO createFile(CreateFileDto dto){
        UnsPo uns = unsMapper.getByAlias(dto.getAlias());
        if (uns != null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.alias.has.exist"));
        }

        String parentAlias = dto.getParentAlias();
        if (StringUtils.isNotBlank(parentAlias)) {
            UnsPo parent = unsMapper.getByAlias(parentAlias);
            if (parent == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.parent.alias.not.found"));
            }
        }

        String templateAlias = dto.getTemplateAlias();
        if (StringUtils.isNotBlank(templateAlias)) {
            UnsPo template = unsMapper.getByAlias(templateAlias);
            if (template == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
            }
            dto.setDefinition(template.getFields());
        }
        //没有引用模板  校验
        if (StringUtils.isBlank(templateAlias) && ArrayUtils.isNotEmpty(dto.getDefinition())) {
            for (FieldDefine fieldDefine : dto.getDefinition()) {
                FieldType fieldType = fieldDefine.getType();
                if (fieldType == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.invalid.field.type"));
                }
                boolean hasSystemFiled = Constants.systemFields.contains(fieldDefine.getName());
                if (hasSystemFiled) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.has.system.field"));
                }
                if (!FieldUtils.FIELD_NAME_PATTERN.matcher(fieldDefine.getName()).matches()) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.name.format.invalid", fieldDefine.getName()));
                }
                //96998 【UNS-openapi】创建文件夹，字段定义非string类型传了长度，建议忽略
                if (fieldType != FieldType.STRING) {
                    fieldDefine.setMaxLen(null);
                }
            }
        }

        String frequency = dto.getFrequency();
        if (StringUtils.isNotBlank(frequency)) {
            if (frequency.length() >= 2) {
                frequency = frequency.trim();
                Integer timeNum = IntegerUtils.parseInt(frequency.substring(0, frequency.length() - 1).trim());
                if (timeNum != null) {
                    char unit = frequency.charAt(frequency.length() - 1);
                    if ("smh".indexOf(unit) == -1) {
                        return ResultVO.fail(I18nUtils.getMessage("uns.frequency.unit.wrong"));
                    }
                }
            }
        }
        InstanceField[] refers = dto.getRefers();
        if (ArrayUtils.isNotEmpty(refers)) {
            for (InstanceField refer : refers) {
                String referAlias = refer.getAlias();
                CreateTopicDto ref = unsDefinitionService.getDefinitionByAlias(referAlias);
                if (ref == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", referAlias));
                }
            }
        }

        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setFields(dto.getDefinition());
        createTopicDto.setExtend(dto.getExtendProperties());
        createTopicDto.setModelAlias(dto.getTemplateAlias());
        createTopicDto.setSave2db(dto.getPersistence());
        createTopicDto.setAddDashBoard(dto.getDashBoard());
        createTopicDto.setAddFlow(dto.getAddFlow());
        if (StringUtils.isBlank(dto.getAlias())) {
            createTopicDto.setAlias(PathUtil.generateAlias(dto.getName(), Constants.PATH_TYPE_FILE));
        }
        JsonResult<String> jsonResult = unsAddService.createModelInstance(createTopicDto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        return ResultVO.success("ok");
    }

    public ResultVO updateFolder(String alias, UpdateFolderDto dto){
        UnsPo uns = unsMapper.getByAlias(alias);
        if (uns == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.or.file.not.found"));
        }
        if (SYSTEM_KEY_WORDS.contains(dto.getName())) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.reserved.word"));
        }

        String parentAlias = dto.getParentAlias();
        if (StringUtils.isNotBlank(parentAlias)) {
            UnsPo parent = unsMapper.getByAlias(parentAlias);
            if (parent == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.parent.alias.not.found"));
            }
        }

        Long templateId = uns.getModelId();
        //没有引用模板  校验
        if (templateId == null && ArrayUtils.isNotEmpty(dto.getDefinition())) {
            for (FieldDefine fieldDefine : dto.getDefinition()) {
                FieldType fieldType = fieldDefine.getType();
                if (fieldType == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.invalid.field.type"));
                }
                boolean hasSystemFiled = Constants.systemFields.contains(fieldDefine.getName());
                if (hasSystemFiled) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.has.system.field"));
                }
                if (!FieldUtils.FIELD_NAME_PATTERN.matcher(fieldDefine.getName()).matches()) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.name.format.invalid", fieldDefine.getName()));
                }
                //96998 【UNS-openapi】创建文件夹，字段定义非string类型传了长度，建议忽略
                if (fieldType != FieldType.STRING) {
                    fieldDefine.setMaxLen(null);
                }
            }
        }
        String name = dto.getName();
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        if (templateId != null) {
            createTopicDto.setFields(null);
        } else {
            createTopicDto.setFields(dto.getDefinition());
        }
        createTopicDto.setName(StringUtils.isNotBlank(name) ? name : uns.getName());
        createTopicDto.setPathType(Constants.PATH_TYPE_DIR);
        createTopicDto.setAlias(alias);
        createTopicDto.setExtend(dto.getExtendProperties());
        createTopicDto.setModelAlias(dto.getTemplateAlias());
        JsonResult<String> jsonResult = unsAddService.createModelInstance(createTopicDto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        return ResultVO.success("ok");
    }

    public ResultVO updateFile(String alias, UpdateFileDto dto){
        UnsPo uns = unsMapper.getByAlias(alias);
        if (uns == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.or.file.not.found"));
        }
        String parentAlias = dto.getParentAlias();
        if (StringUtils.isNotBlank(parentAlias)) {
            UnsPo parent = unsMapper.getByAlias(parentAlias);
            if (parent == null) {
                return ResultVO.fail(I18nUtils.getMessage("uns.parent.alias.not.found"));
            }
        }
        Long templateId = uns.getModelId();
        //没有引用模板  校验
        if (templateId == null && ArrayUtils.isNotEmpty(dto.getDefinition())) {
            for (FieldDefine fieldDefine : dto.getDefinition()) {
                FieldType fieldType = fieldDefine.getType();
                if (fieldType == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.invalid.field.type"));
                }
                boolean hasSystemFiled = Constants.systemFields.contains(fieldDefine.getName());
                if (hasSystemFiled) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.has.system.field"));
                }
                if (!FieldUtils.FIELD_NAME_PATTERN.matcher(fieldDefine.getName()).matches()) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.name.format.invalid", fieldDefine.getName()));
                }
                //96998 【UNS-openapi】创建文件夹，字段定义非string类型传了长度，建议忽略
                if (fieldType != FieldType.STRING) {
                    fieldDefine.setMaxLen(null);
                }
            }
        }

        InstanceField[] refers = dto.getRefers();
        if (ArrayUtils.isNotEmpty(refers)) {
            for (InstanceField refer : refers) {
                String referAlias = refer.getAlias();
                CreateTopicDto ref = unsDefinitionService.getDefinitionByAlias(referAlias);
                if (ref == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", referAlias));
                }
            }
        }

        String name = dto.getName();
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        if (templateId != null) {
            createTopicDto.setFields(null);
        } else {
            createTopicDto.setFields(dto.getDefinition());
        }
        createTopicDto.setName(StringUtils.isNotBlank(name) ? name : uns.getName());
        createTopicDto.setDataType(uns.getDataType());
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setAlias(alias);
        createTopicDto.setExtend(dto.getExtendProperties());
        createTopicDto.setModelAlias(dto.getTemplateAlias());
        createTopicDto.setSave2db(dto.getPersistence());
        createTopicDto.setAddDashBoard(dto.getDashBoard());
        createTopicDto.setAddFlow(dto.getAddFlow());
        Boolean save2db = dto.getPersistence();
        if (save2db != null) {
            int fl = uns.getWithFlags();
            fl = save2db ? (fl | Constants.UNS_FLAG_WITH_SAVE2DB) : (fl & ~Constants.UNS_FLAG_WITH_SAVE2DB);
            createTopicDto.setFlags(fl);
        }
        JsonResult<String> jsonResult = unsAddService.createModelInstance(createTopicDto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        return ResultVO.success("ok");
    }

    public ResultVO batchQueryFileByPath(List<String> pathList) {
        if (CollectionUtils.isEmpty(pathList)) {
            return ResultVO.fail("路径集合为空");
        }
        List<String> notExists = new ArrayList<>();
        JSONObject resultData = new JSONObject();
        for (String path : pathList) {
            CreateTopicDto dto = unsDefinitionService.getDefinitionByPath(path);
            if (dto == null) {
                notExists.add(path);
                continue;
            }
            String msgInfo = unsQueryService.getLastMsgByPath(path, true).getData();
            JSONObject data;
            if (org.springframework.util.StringUtils.hasText(msgInfo)) {
                data = JSON.parseObject(msgInfo).getJSONObject("data");
//                standardizingData(alias, data);
                if (data.containsKey(Constants.QOS_FIELD)) {
                    Object qos = data.get(Constants.QOS_FIELD);
                    if (qos != null) {
                        long v = 0;
                        if (qos instanceof Number n) {
                            v = n.longValue();
                        } else {
                            try {
                                v = Long.parseLong(qos.toString());
                            } catch (NumberFormatException ex) {
                                log.error("qos isNaN:{}, path={}", qos, path);
                            }
                        }
                        qos = Long.toHexString(v);
                    }
                    data.put(Constants.QOS_FIELD, qos);
                }
            } else {
                //pride：如果所订查询的文件当前无实时值，则status=通讯异常（0x80 00 00 00 00 00 00 00），timeStampe=当前服务器时间戳，value=各数据类型的初始值（详见文章节：支持数据类型）
                CreateTopicDto uns = unsDefinitionService.getDefinitionByPath(path);
                data = DataUtils.transEmptyValue(uns, true);
            }
            resultData.put(path, data);
        }
        if (!CollectionUtils.isEmpty(notExists)) {
            ResultVO resultVO = new ResultVO();
            resultVO.setCode(206);
            UnsDataResponseVo res = new UnsDataResponseVo();
            res.setNotExists(notExists);
            resultVO.setData(res);
            return resultVO;
        }
        return ResultVO.successWithData("ok", resultData);
    }

    public ResultVO updateTemplate(String alias, UpdateTemplateDto dto){
        UnsPo template = unsMapper.getByAlias(alias);
        if (template == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        ResultVO resultVO = null;
        String name = StrUtil.blankToDefault(dto.getName(),template.getName());
        if (StringUtils.isNotBlank(dto.getName()) || StringUtils.isNotBlank(dto.getDescription())){
            resultVO = unsTemplateService.updateTemplate(template.getId(), name, dto.getDescription());
            if (resultVO.getCode() != 200){
                return resultVO;
            }
        }

        if (ArrayUtils.isNotEmpty(dto.getFields())) {
            for (FieldDefine fieldDefine : dto.getFields()) {
                FieldType fieldType = fieldDefine.getType();
                if (fieldType == null) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.invalid.field.type"));
                }
                boolean hasSystemFiled = Constants.systemFields.contains(fieldDefine.getName());
                if (hasSystemFiled) {
                    return ResultVO.fail(I18nUtils.getMessage("uns.field.has.system.field"));
                }
                //96998 【UNS-openapi】创建文件夹，字段定义非string类型传了长度，建议忽略
                if (fieldType != FieldType.STRING) {
                    fieldDefine.setMaxLen(null);
                }
            }
            resultVO = unsTemplateService.updateFields(alias, dto.getFields());
        }
        return resultVO;
    }

    private void po2FileVo(FileDetailVo fileVo, UnsPo file) {
        String expression = file.getExpression();
        InstanceField[] fs = file.getRefers();
        UnsPo origPo = null;//被引用的uns
        if (ArrayUtils.isNotEmpty(fs)) {
            if (file.getDataType() == Constants.CITING_TYPE) {
                Long origId = fs[0].getId();
                UnsPo orig = unsMapper.selectById(origId);
                if (orig != null) {
                    origPo = orig;
                    file.setFields(orig.getFields());
                    file.setWithFlags(orig.getWithFlags());
                }
            }
            List<Long> ids = Arrays.stream(fs).map(InstanceField::getId).toList();
            Map<Long, UnsPo> unsMap = unsMapper.listInstanceByIds(ids).stream().collect(Collectors.toMap(UnsPo::getId, k -> k));
            Map<Integer, Map<String, Pair<String, String>>> variableFieldMap = new HashMap<>();
            InstanceFieldVo[] refers = Arrays.stream(fs).map(field -> {
                InstanceFieldVo instanceFieldVo = new InstanceFieldVo();
                instanceFieldVo.setId(field.getId().toString());
                instanceFieldVo.setField(field.getField());
                instanceFieldVo.setUts(field.getUts());
                UnsPo ref = unsMap.get(field.getId());
                if (ref != null) {
                    instanceFieldVo.setAlias(ref.getAlias());
                    instanceFieldVo.setPath(ref.getPath());
                }
                return instanceFieldVo;
            }).toArray(InstanceFieldVo[]::new);
            fileVo.setRefers(refers);
            String protocol = file.getProtocol();
            if (protocol != null && protocol.startsWith("{")) {
                fileVo.setProtocol(JsonUtil.fromJson(protocol, Map.class));
            }
            Map<String, Object> protocolMap = fileVo.getProtocol();
            Object whereExpr;
            if (expression != null) {
                fileVo.setExpression(expression);
            } else if (protocolMap != null && (whereExpr = protocolMap.get("whereCondition")) != null) {
                expression = ExpressionUtils.replaceExpression(whereExpr.toString(), var -> String.format("$\"%s\".%s#", fs[0].getTopic(), var));
                fileVo.setExpression(expression);
            }
        }
        Long fileId = file.getId();
        fileVo.setId(fileId);
        fileVo.setDataType(file.getDataType());
        UnsPo unsPo = origPo != null ? origPo : file;
        if (unsPo.getFields() != null) {
            FieldDefine[] fields = unsPo.getFields();
            FieldDefine[] fieldDefines = new FieldDefine[0];
            int dataType = unsPo.getDataType();
            if (dataType == Constants.TIME_SEQUENCE_TYPE || dataType == Constants.CALCULATION_REAL_TYPE) {
                LinkedList<FieldDefine> fsList = new LinkedList<>(Arrays.asList(fields));
                Iterator<FieldDefine> itr = fsList.iterator();
                CreateTopicDto dtp = UnsConverter.po2dto(unsPo);
                String tbF = dtp.getTbFieldName();
                if (tbF != null) {
                    FieldDefine dvf = dtp.getFieldDefines().getFieldsMap().get(tbF);
                    FieldDefine vf = dtp.getFieldDefines().getFieldsMap().get(Constants.SYSTEM_SEQ_VALUE);
                    vf.setName(dvf.getTbValueName());
                }
                while (itr.hasNext()) {
                    FieldDefine fd = itr.next();
                    String name = fd.getName();
                    if (name.startsWith(Constants.SYSTEM_FIELD_PREV) || fd.getTbValueName() != null) {
                        itr.remove();
                    }
                }
                fieldDefines = fsList.toArray(new FieldDefine[0]);
            } else {
                SrcJdbcType jdbcType = SrcJdbcType.getById(unsPo.getDataSrcId());
                if (jdbcType != null) {
                    FieldDefine ct = FieldUtils.getTimestampField(fields), qos = FieldUtils.getQualityField(fields, jdbcType.typeCode);
                    fieldDefines = Arrays.stream(fields)
                            .filter(fd -> fd != ct && fd != qos && !fd.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toList().toArray(new FieldDefine[0]);
                }
            }
            fileVo.setDefinition(fieldDefines);
        }
        fileVo.setAlias(file.getAlias());
        fileVo.setPath(file.getPath());
//        dto.setTopic(Constants.useAliasAsTopic ? file.getAlias() : file.getPath());

        fileVo.setDescription(file.getDescription());
        fileVo.setCreateTime(getDatetime(file.getCreateAt()));
        fileVo.setUpdateTime(getDatetime(file.getUpdateAt()));
        fileVo.setAlias(file.getAlias());
        fileVo.setName(file.getName());
        fileVo.setDisplayName(file.getDisplayName());
        fileVo.setPathName(PathUtil.getName(file.getPath()));
        fileVo.setExtendProperties(file.getExtend());

        Integer flagsN = file.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            fileVo.setPersistence(Constants.withSave2db(flags));
        }

        Long templateId = file.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                fileVo.setTemplateAlias(template.getAlias());
            }
        }
    }

    private static final Long getDatetime(Date date) {
        return date != null ? date.getTime() : null;
    }

    private void po2FolderVo(FolderDetailVo folderVo, UnsPo po) {
        folderVo.setId(po.getId());
        folderVo.setName(po.getName());
        folderVo.setDisplayName(po.getDisplayName());
        folderVo.setAlias(po.getAlias());
        folderVo.setParentAlias(po.getParentAlias());
        folderVo.setPathType(po.getPathType());
        folderVo.setPath(po.getPath());
        folderVo.setPathName(PathUtil.getName(po.getPath()));
        folderVo.setDescription(po.getDescription());
        folderVo.setCreateTime(getDatetime(po.getCreateAt()));
        folderVo.setUpdateTime(getDatetime(po.getUpdateAt()));
        folderVo.setDescription(po.getDescription());

        folderVo.setExtendProperties(po.getExtend());
        FieldDefine[] fs = po.getFields();
        if (fs != null) {
            for (FieldDefine f : fs) {
                f.setIndex(null);// 模型的定义 给前端消除掉 index
            }
        }
        folderVo.setDefinition(fs);
        Long templateId = po.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                folderVo.setTemplateAlias(template.getAlias());
            }
        }
    }

    public ResultVO<LabelOpenVo> create(String name) {
        ResultVO<UnsLabelPo> resultVO = unsLabelService.create(name);
        if (resultVO.getCode() != 200) {
            return ResultVO.fail(resultVO.getMsg());
        }
        LabelOpenVo vo = BeanUtil.copyProperties(resultVO.getData(),LabelOpenVo.class);
        return ResultVO.successWithData(vo);
    }

    public ResultVO<LabelOpenVo> detail(Long id) {
        UnsLabelPo po = unsLabelService.getById(id);
        if (null == po) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        return ResultVO.successWithData(labelPo2Vo(po));
    }

    public PageResultDTO<LabelOpenVo> allLabels(LabelQueryDto queryDto) {
        Page<UnsLabelPo> page = new Page<>(queryDto.getPageNo(), queryDto.getPageSize());
        LambdaQueryWrapper<UnsLabelPo> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.isNotBlank(queryDto.getKey()), UnsLabelPo::getLabelName, queryDto.getKey());
        IPage<UnsLabelPo> iPage = unsLabelService.page(page,qw);
        if (CollectionUtils.isEmpty(iPage.getRecords())) {
            return PageUtil.build(iPage, Collections.emptyList());
        }
        List<LabelOpenVo> voList = iPage.getRecords().stream().map(this::labelPo2Vo).collect(Collectors.toList());
        PageResultDTO<LabelOpenVo> pageResultDTO = PageUtil.build(iPage, voList);
        return pageResultDTO;
    }

    private LabelOpenVo labelPo2Vo(UnsLabelPo po){
        LabelOpenVo vo = BeanUtil.copyProperties(po,LabelOpenVo.class);
        vo.setCreateTime(po.getCreateAt().getTime());
        return vo;
    }

}
