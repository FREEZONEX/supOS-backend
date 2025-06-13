package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.BaseResult;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.JsonResult;
import com.supos.common.enums.FieldType;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.common.utils.PostgresqlTypeUtils;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.CreateAlarmRuleVo;
import com.supos.uns.vo.UpdateAlarmRuleVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.*;

@Service
@Slf4j
public class UnsAlarmService extends ServiceImpl<UnsMapper, UnsPo> {

    @Autowired
    AlarmService alarmService;
    @Autowired
    UnsAddService unsAddService;
    @Autowired
    IUnsDefinitionService unsDefinitionService;

    public BaseResult createAlarmRule(CreateAlarmRuleVo vo) {
        String name = vo.getName();
        String nameAlias;
        if (StringUtils.hasText(name)) {
            nameAlias = PathUtil.generateAlias(PathUtil.escapeName(vo.getName()),2);
        } else {
            nameAlias = IdUtil.fastSimpleUUID();
        }
        String alias = "alarm_" + nameAlias;

        if (unsDefinitionService.getDefinitionByAlias(alias) != null){
            return new BaseResult(400, I18nUtils.getMessage("uns.alarm.rule.already.exists"));
        }

        return saveOrUpdateAlarmRule(vo, name, alias);
    }

    private BaseResult saveOrUpdateAlarmRule(CreateAlarmRuleVo vo, String name, String alias) {
        CreateTopicDto createTopicDto = new CreateTopicDto();
        createTopicDto.setPathType(Constants.PATH_TYPE_FILE);
        createTopicDto.setDataPath(vo.getName());
        createTopicDto.setDescription(vo.getDescription());
        createTopicDto.setRefers(vo.getRefers());
        createTopicDto.setExpression(vo.getExpression());
        createTopicDto.setAlarmRuleDefine(vo.getProtocol());
        Map<String, Object> protocolMap = new HashMap<>();
        BeanUtil.copyProperties(vo.getProtocol(), protocolMap);
        createTopicDto.setProtocol(protocolMap);

        //  不需要 path，只需要 name, alias, parentAlais
        createTopicDto.setName(name);
        FieldDefine[] fields = getAlarmTableFields();
        createTopicDto.setFields(fields);
        createTopicDto.setParentAlias(ALARM_FOLDER_ALAIS);
        createTopicDto.setModelAlias(ALARM_MODEL_ALAIS);
        createTopicDto.setDataType(Constants.ALARM_RULE_TYPE);
        createTopicDto.setDataSrcId(SrcJdbcType.Postgresql);
        createTopicDto.setAlias(alias);
        createTopicDto.setTableName(ALARM_TABLE);
        createTopicDto.setFlags(ALARM_FLAGS | vo.getWithFlags()); //接收方式 1-人员 2-工作流程
        createTopicDto.setExtend(vo.getExtend());//工作流ID

        createTopicDto.setBatch(0);
        createTopicDto.setIndex(1);
        createTopicDto.setModelId(ALARM_MODEL_ID);
        JsonResult<String> rs = unsAddService.createModelInstance(createTopicDto);

        //16人员  32工作流
        if (rs.getData() != null && Constants.UNS_FLAG_ALARM_ACCEPT_PERSON == vo.getWithFlags()) {
            alarmService.createAlarmHandler(Long.valueOf(rs.getData()), vo.getUserList());
        }
        //工作流只保存ext字段  工作流流程ID
        return rs;
    }

    public BaseResult updateAlarmRule(UpdateAlarmRuleVo alarmRuleVo) {
        BaseResult result = new BaseResult(0, "ok");
        UnsPo unsPo = this.baseMapper.selectById(alarmRuleVo.getId());
        if (null == unsPo) {
            result.setCode(404);
            result.setMsg(I18nUtils.getMessage("uns.alarm.rule.not.exist"));
            return result;
        }
        return saveOrUpdateAlarmRule(alarmRuleVo, alarmRuleVo.getName(), unsPo.getAlias());
    }

    private FieldDefine[] getAlarmTableFields() {
        List<FieldDefineVo> fieldDefineVos = baseMapper.describeTableFieldInfo(AlarmPo.TABLE_NAME);
        ArrayList<FieldDefine> list = new ArrayList<>(fieldDefineVos.size());
        for (FieldDefineVo vo : fieldDefineVos) {
            String col = vo.getName(), type = vo.getType();
            String fieldTypeStr = PostgresqlTypeUtils.dbType2FieldTypeMap.get(type.toLowerCase());
            FieldType fieldType = FieldType.getByName(fieldTypeStr);
            FieldDefine fieldDefine = new FieldDefine(col.toLowerCase(), fieldType);
            if ("_id".equalsIgnoreCase(col)){
                fieldDefine.setUnique(true);
            }
            list.add(fieldDefine);
        }
        list.sort(Comparator.comparing(FieldDefine::getName));
        return list.toArray(new FieldDefine[0]);
    }

    private static final String ALARM_FOLDER_ALAIS = "_alarm_folder";
    private static final String ALARM_MODEL_ALAIS = "_alarm_model";
    private static final Long ALARM_MODEL_ID = 5L;
    private static final Long ALARM_FOLDER_ID = 6L;
    private static final String ALARM_TABLE = "supos." + AlarmPo.TABLE_NAME;
    private static final int ALARM_FLAGS = Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE;

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(90)
    void onStartup(ContextRefreshedEvent event) {
        final FieldDefine[] fields = getAlarmTableFields();
        UnsPo alarmModel = new UnsPo(ALARM_MODEL_ID, ALARM_MODEL_ALAIS, "AlarmTemplate", NodeType.Model.code, Constants.ALARM_RULE_TYPE, SrcJdbcType.Postgresql.id, fields, "Alarm Model");
        {
            alarmModel.setPath(alarmModel.getName());
            alarmModel.setTableName("supos." + AlarmPo.TABLE_NAME);
            alarmModel.setLayRec(String.valueOf(ALARM_MODEL_ID));
            alarmModel.setWithFlags(Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE);
        }
        UnsPo existsAlarmModel = baseMapper.getAlarmTemplate(ALARM_MODEL_ID);
        if (existsAlarmModel != null && (!alarmModel.getTableName().equals(existsAlarmModel.getTableName())
                || !Integer.valueOf(SrcJdbcType.Postgresql.id).equals(existsAlarmModel.getDataSrcId())//
                || !Arrays.equals(fields, existsAlarmModel.getFields())//
        )) {
            int updated = baseMapper.fixAlarmFields(fields);
            log.info("修复 alarm实例: {}", updated);
            saveOrUpdateBatch(Collections.singletonList(folderAlarm()));
        } else if (existsAlarmModel == null) {
            // public UnsPo(String id, String path, int pathType, int dataType, int dataSrcId, String fields, String description) {
            saveOrUpdateBatch(Arrays.asList(alarmModel, folderAlarm()));
        }

    }

    private UnsPo folderAlarm() {
        UnsPo folderAlarm = new UnsPo();
        folderAlarm.setId(ALARM_FOLDER_ID);
        folderAlarm.setLayRec(String.valueOf(ALARM_FOLDER_ID));
        folderAlarm.setName("_alarms_");
        folderAlarm.setPath(folderAlarm.getName());
        folderAlarm.setPathType(0);
        folderAlarm.setDataType(Constants.ALARM_RULE_TYPE);
        folderAlarm.setDescription("alarm folder");
        folderAlarm.setModelId(ALARM_MODEL_ID);
        folderAlarm.setAlias(ALARM_FOLDER_ALAIS);
        return folderAlarm;
    }
}
