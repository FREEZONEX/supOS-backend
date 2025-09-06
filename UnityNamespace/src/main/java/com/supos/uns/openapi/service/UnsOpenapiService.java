package com.supos.uns.openapi.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.JsonResult;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.*;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.openapi.dto.*;
import com.supos.uns.openapi.vo.FileDetailVo;
import com.supos.uns.openapi.vo.FolderDetailVo;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsDefinitionService;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.util.UnsConverter;
import com.supos.uns.vo.InstanceFieldVo;
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
    UnsConverter unsConverter;
    @Autowired
    UnsTemplateService unsTemplateService;
    @Autowired
    UnsQueryService unsQueryService;

    private static final List<String> SYSTEM_KEY_WORDS = Arrays.asList("label","template");

    public ResultVO<FolderDetailVo> folderDetailByAlias(String alias){
        FolderDetailVo folderVo = new FolderDetailVo();
        UnsPo po = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                .eq(UnsPo::getPathType, Constants.PATH_TYPE_DIR)
                .eq(UnsPo::getAlias, alias));
        if (po == null) {
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
        UnsPo file = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>()
                .eq(UnsPo::getPathType, Constants.PATH_TYPE_FILE)
                .eq(UnsPo::getAlias, alias));
        if (file == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.not.found"));
        }

        po2FileVo(fileVo, file);
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
        if (SYSTEM_KEY_WORDS.contains(dto.getName())) {
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.name.illegality"));
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
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.name.illegality"));
        }
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        createTopicDto.setPathType(Constants.PATH_TYPE_DIR);
        createTopicDto.setAlias(alias);
        createTopicDto.setFields(dto.getDefinition());
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
        CreateTopicDto createTopicDto = BeanUtil.copyProperties(dto, CreateTopicDto.class);
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setAlias(alias);
        createTopicDto.setFields(dto.getDefinition());
        createTopicDto.setExtend(dto.getExtendProperties());
        createTopicDto.setModelAlias(dto.getTemplateAlias());
        createTopicDto.setSave2db(dto.getPersistence());
        createTopicDto.setAddDashBoard(dto.getDashBoard());
        createTopicDto.setAddFlow(dto.getAddFlow());
        JsonResult<String> jsonResult = unsAddService.createModelInstance(createTopicDto);
        if (jsonResult.getCode() != 0) {
            return ResultVO.fail(jsonResult.getMsg());
        }
        return ResultVO.success("ok");
    }

    public ResultVO updateTemplate(String alias, UpdateTemplateDto dto){
        UnsPo template = unsMapper.getByAlias(alias);
        if (template == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }
        ResultVO resultVO = null;
        if (StringUtils.isNotBlank(dto.getName()) || StringUtils.isNotBlank(dto.getDescription())){
            resultVO = unsTemplateService.updateTemplate(template.getId(), template.getName(), dto.getDescription());
            if (resultVO.getCode() != 200){
                return resultVO;
            }
        }

        if (dto.getFields() != null && dto.getFields().length > 0) {
            resultVO = unsTemplateService.updateFields(alias, dto.getFields());
        }
        return resultVO;
    }

    public ResultVO batchQueryFileByPath(List<String> pathList) {
        if (CollectionUtils.isEmpty(pathList)) {
            return ResultVO.fail("路径集合为空");
        }
        JSONObject resultData = new JSONObject();
        for (String path : pathList) {
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
        return ResultVO.successWithData("ok", resultData);
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
                Map<String, String> varReplacer = new HashMap<>(8);
                Map<String, String> showVarReplacer = new HashMap<>(8);
                for (int i = 0; i < fs.length; i++) {
                    InstanceField field = fs[i];
                    if (field != null) {
                        Long citingId = field.getId();
                        if (citingId != null) {
                            String var = Constants.VAR_PREV + (i + 1);
                            varReplacer.put(var, String.format("$\"%s\".%s#", citingId, field.getField()));
                            CreateTopicDto citingInfo = unsDefinitionService.getDefinitionById(citingId);
                            if (citingInfo != null) {
                                showVarReplacer.put(var, String.format("$\"%s\".%s#", citingInfo.getPath(), field.getField()));
                            }
                        }
                    }
                }
                if (!varReplacer.isEmpty()) {
                    String showExpression = ExpressionUtils.replaceExpression(expression, showVarReplacer);
                    fileVo.setExpression(showExpression);
                    expression = ExpressionUtils.replaceExpression(expression, varReplacer);
                }
            } else if (protocolMap != null && (whereExpr = protocolMap.get("whereCondition")) != null) {
                expression = ExpressionUtils.replaceExpression(whereExpr.toString(), var -> String.format("$\"%s\".%s#", fs[0].getTopic(), var));
            }
        }
        fileVo.setExpression(expression);

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
                CreateTopicDto dtp = unsConverter.po2dto(unsPo);
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
            fileVo.setAddFlow(Constants.withFlow(flags));
            fileVo.setDashBoard(Constants.withDashBoard(flags));
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



}
