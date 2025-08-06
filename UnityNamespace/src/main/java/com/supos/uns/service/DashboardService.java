package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.*;
import com.supos.common.dto.grafana.DashboardDto;
import com.supos.common.dto.grafana.PgDashboardParam;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.common.event.CreateDashboardEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.FuxaUtils;
import com.supos.common.utils.GrafanaUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.dao.mapper.DashboardMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.DashboardPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.core.DashboardExportContext;
import com.supos.uns.service.exportimport.core.DashboardImportContext;
import com.supos.uns.service.exportimport.json.DashboardDataExporter;
import com.supos.uns.service.exportimport.json.DashboardDataImporter;
import com.supos.uns.util.FileUtils;
import com.supos.uns.util.UnsFlags;
import com.supos.uns.vo.DashboardExportParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 仪表盘服务
 *
 * @author xinwangji@supos.com
 * @date 2024/10/29 10:37
 * @description
 */
@Service
@Slf4j
public class DashboardService extends ServiceImpl<DashboardMapper, DashboardPo> {

    @Autowired
    private DashboardMapper dashboardMapper;
    @Autowired
    private SystemConfig systemConfig;
    @Autowired
    private IUnsDefinitionService unsDefinitionService;
    @Autowired
    UnsMapper unsMapper;

    public PageResultDTO<DashboardDto> pageList(String keyword, Integer type, PaginationDTO params) {
        Page<DashboardPo> page = new Page<>(params.getPageNo(), params.getPageSize());
        LambdaQueryWrapper<DashboardPo> qw = new LambdaQueryWrapper<>();
        if (type != null) {
            qw.eq(DashboardPo::getType, type);
        }
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(DashboardPo::getName, keyword).or().like(DashboardPo::getDescription, keyword));
        }
        qw.orderByDesc(DashboardPo::getCreateTime);
        Page<DashboardPo> iPage = page(page, qw);
        List<DashboardDto> list = BeanUtil.copyToList(iPage.getRecords(), DashboardDto.class);
        PageResultDTO.PageResultDTOBuilder<DashboardDto> pageBuilder = PageResultDTO.<DashboardDto>builder().total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(0).data(list).build();
    }

    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public JsonResult<DashboardPo> create(DashboardDto dashboardDto) {
        LambdaQueryWrapper<DashboardPo> qw = new LambdaQueryWrapper<>();
        qw.eq(DashboardPo::getName, dashboardDto.getName());
        qw.eq(DashboardPo::getType, dashboardDto.getType());
        if (count(qw) > 0) {
            return new JsonResult(500, I18nUtils.getMessage("uns.dashboard.name.duplicate"));
        }

        Date now = new Date();
        dashboardDto.setId(IdUtil.fastUUID());
        DashboardPo po = BeanUtil.copyProperties(dashboardDto, DashboardPo.class);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        if (dashboardDto.getType() == 1) {
            String template = ResourceUtil.readUtf8Str("templates/dashboard-blank.json");
            PgDashboardParam pgParams = new PgDashboardParam();
            pgParams.setUid(dashboardDto.getId());
            pgParams.setTitle(dashboardDto.getName());
            Map<String, Object> dbParams = BeanUtil.beanToMap(pgParams);
            String dashboardJson = StrUtil.format(template, dbParams);
            log.info(">>>>>>>>>>>>>>>dashboardJson :{}", dashboardJson);
            HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dashboardJson).execute();
            log.info(dashboardResponse.body());
            if (200 != dashboardResponse.getStatus()) {
                return new JsonResult(500, I18nUtils.getMessage("uns.dashboard.create.failed"));
            }
        }
        save(po);
        return new JsonResult(0, "success", po);
    }

    public BaseResult edit(DashboardDto dashboardDto) {
        DashboardPo dashboardPo = getById(dashboardDto.getId());
        if (null == dashboardPo) {
            throw new BuzException("uns.dashboard.not.exit");
        }
        if (dashboardPo.getType() == 1) {
            JSONObject dbJson = GrafanaUtils.getDashboardByUuid(dashboardDto.getId());
            if (null == dbJson) {
                throw new BuzException("uns.dashboard.not.exit");
            }

            dbJson.getJSONObject("dashboard").put("title", dashboardDto.getName());
            dbJson.getJSONObject("dashboard").put("description", dashboardDto.getDescription());

            log.info(">>>>>>>>>>>>>>>dashboardJson :{}", dbJson);
            HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dbJson.toJSONString()).execute();
            log.info(dashboardResponse.body());
        }
        BeanUtil.copyProperties(dashboardDto, dashboardPo, "id", "type");
        updateById(dashboardPo);
        return new BaseResult(0, "success");
    }

    public boolean delete(String uid) {
        DashboardPo dashboardPo = getById(uid);
        if (null == dashboardPo) {
            return true;
        }
        if (1 == dashboardPo.getType() && null != systemConfig.getContainerMap().get("grafana")) {
            GrafanaUtils.deleteDashboard(uid);
        } else if (2 == dashboardPo.getType() && null != systemConfig.getContainerMap().get("fuxa")) {
            String url = Constants.FUXA_API_URL + "/api/project/" + uid;
            HttpResponse response = HttpRequest.delete(url).execute();
            log.info(">>>>>>>>>>>>>>>dashboard fuxa delete response code:{}", response.getStatus());
        }
        return removeById(uid);
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    public void onRemoveTopics(RemoveTopicsEvent event) {
        if (CollectionUtil.isEmpty(event.topics) || !event.withDashboard ||event.jdbcType==null||event.jdbcType==SrcJdbcType.None) {
            return;
        }
        long t0 = System.currentTimeMillis();
        Collection<String> aliasList = event.topics.values().stream()
                .filter(SimpleUnsInstance::isRemoveDashboard)
                .map(SimpleUnsInstance::getAlias).collect(Collectors.toSet());
        List<String> ids = aliasList.stream().map(alias -> {
            //tableName = alias
            return GrafanaUtils.getDashboardUuidByAlias(alias);
        }).collect(Collectors.toList());

        for (List<String> idList : Lists.partition(ids, Constants.SQL_BATCH_SIZE)) {
            dashboardMapper.deleteBatchIds(idList);
        }
        long t1 = System.currentTimeMillis();
        log.info("Dashboard删除耗时 : {} ms, size={}", t1-t0, ids.size());
    }


    /**
     * 获取grafana详情
     */
    public ResultVO<JSONObject> getByUuid(String uuid) {
        JSONObject dbJson = GrafanaUtils.getDashboardByUuid(uuid);
        if (null == dbJson) {
            return ResultVO.fail(I18nUtils.getMessage("uns.dashboard.not.exit"));
        }
        return ResultVO.successWithData(dbJson);
    }

    @EventListener(classes = CreateDashboardEvent.class)
    public void createByHandler(CreateDashboardEvent event) {
        log.info("创建数据看板参数：{}", event);
        Date now = new Date();
        DashboardPo po = new DashboardPo();
        po.setId(event.uuid);
        po.setName(event.name);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        boolean flag = dashboardMapper.insert(po) > 0;
        log.info("结束创建数据看板 name：{}，创建状态：{}", event.name, flag);
    }

    public void asyncImport(File file, Consumer<RunningStatus> consumer) {
        if (!file.exists()) {
            String message = I18nUtils.getMessage("global.import.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }
        DashboardImportContext context = new DashboardImportContext(file.toString());
        DashboardDataImporter dataImporter = new DashboardDataImporter(context, dashboardMapper);
        try {
            dataImporter.importData(file);
        } catch (Throwable ex) {
            log.error("UnsImportErr:{}", file.getPath(), ex);
            importFinish(dataImporter, consumer, file, context, ex);
            return;
        }
        importFinish(dataImporter, consumer, file, context, null);
    }

    private void importFinish(DashboardDataImporter dataImporter, Consumer<RunningStatus> consumer, File file, DashboardImportContext context, Throwable ex) {
        try {
            if (context.dataEmpty()) {
                // todo wsz
                String message = I18nUtils.getMessage("dashboard.import.excel.empty");
                consumer.accept(new RunningStatus(400, message));
            } else {
                String finalTask = I18nUtils.getMessage("dashboard.create.task.name.final");

                if (ex != null) {
                    Throwable cause = ex.getCause();
                    String errMsg;
                    if (cause != null) {
                        errMsg = cause.getMessage();
                    } else {
                        errMsg = ex.getMessage();
                    }
                    if (errMsg == null) {
                        errMsg = I18nUtils.getMessage("dashboard.create.status.error");
                    }
                    consumer.accept(new RunningStatus(500, errMsg)
                            .setTask(finalTask)
                            .setProgress(0.0)
                    );
                    return;
                }

                if (context.getCheckErrorMap().isEmpty()) {
                    String message = I18nUtils.getMessage("dashboard.import.rs.ok");
                    RunningStatus runningStatus = new RunningStatus(200, message)
                            .setTask(finalTask)
                            .setProgress(100.0);
                    runningStatus.setTotalCount(context.getTotal());
                    runningStatus.setSuccessCount(context.getTotal());
                    runningStatus.setErrorCount(0);
                    consumer.accept(runningStatus);
                    return;
                }
                String fileName = "err_" + file.getName().replace(' ', '-');
                String targetPath = String.format("%s%s%s/%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT_ERROR, GlobalExportModuleEnum.DASHBOARD.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), fileName);
                File outFile = FileUtil.touch(targetPath);
                log.info("dashboard create error file:{}", outFile.toString());
                dataImporter.writeError(outFile);

                String message = I18nUtils.getMessage("dashboard.import.rs.hasErr");
                RunningStatus runningStatus = new RunningStatus(206, message, FileUtils.getRelativePath(targetPath))
                        .setTask(finalTask)
                        .setProgress(100.0);
                runningStatus.setTotalCount(context.getTotal());
                runningStatus.setErrorCount(context.getCheckErrorMap().keySet().size());
                runningStatus.setSuccessCount(runningStatus.getTotalCount()-runningStatus.getErrorCount());
                if(Objects.equals(runningStatus.getSuccessCount(),0)){
                    runningStatus.setMsg(I18nUtils.getMessage("global.import.rs.allErr"));
                }
                consumer.accept(runningStatus);
            }
        } catch (Throwable e) {
            log.error("Dashboard导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    private void fetchData(DashboardExportContext context, DashboardExportParam exportParam, StopWatch stopWatch) {
        List<DashboardPo> dashboardPos = null;
        if (CollUtil.isNotEmpty(exportParam.getIds())) {
            dashboardPos = dashboardMapper.selectByIds(exportParam.getIds());
        } else if ("ALL".equals(exportParam.getExportType())) {
            dashboardPos = dashboardMapper.selectList(new LambdaQueryWrapper<>());
        }
        if (CollUtil.isNotEmpty(dashboardPos)) {
            stopWatch.start("global dashboard export get jsonContent");
            for (DashboardPo dashboardPo : dashboardPos) {
                if (Objects.equals(dashboardPo.getType(), 1)) {
                    dashboardPo.setJsonContent(GrafanaUtils.get(dashboardPo.getId()));
                } else if (Objects.equals(dashboardPo.getType(), 2)) {
                    dashboardPo.setJsonContent(FuxaUtils.get(dashboardPo.getId()));
                }
                dashboardPo.setCreateTime(null);
                dashboardPo.setUpdateTime(null);
            }
            stopWatch.stop();
            context.setDashboardPos(dashboardPos);
        }
    }

    public JsonResult<String> dataExport(DashboardExportParam exportParam) {
        StopWatch stopWatch = new StopWatch();
        try {
            DashboardExportContext context = new DashboardExportContext();
            // 1.获取数据
            fetchData(context, exportParam, stopWatch);
            // 2.开始将数据写入json文件
            stopWatch.start("global dashboard export write data");
            String path = new DashboardDataExporter().exportData(context);
            stopWatch.stop();
            return new JsonResult<String>().setData(FileUtils.getRelativePath(path));
        } catch (Exception e) {
            log.error("global dashboard export error", e);
            String msg = I18nUtils.getMessage("global.dashboard.export.error");
            return new JsonResult<>(500, msg);
        } finally {
            log.info("global dashboard export time:{}", stopWatch.prettyPrint());
        }
    }


    public ResultVO createGrafanaByUns(String alias) {
        CreateTopicDto uns = unsDefinitionService.getDefinitionByAlias(alias);
        if (uns == null) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.not.exist"));
        }

        if (uns.getAddDashBoard()) {
            return ResultVO.fail(I18nUtils.getMessage("uns.dashboard.already.created"));
        }

        int dataType = uns.getDataType();
        if (dataType != Constants.TIME_SEQUENCE_TYPE && dataType != Constants.RELATION_TYPE) {
            return ResultVO.fail(I18nUtils.getMessage("uns.file.dataType.invalid", dataType));
        }

        SrcJdbcType jdbcType = null;
        if (dataType == Constants.RELATION_TYPE) {
            jdbcType = SrcJdbcType.Postgresql;
        } else {
            jdbcType = systemConfig.getContainerMap().containsKey("tdengine") ? SrcJdbcType.TdEngine : SrcJdbcType.TimeScaleDB;
        }
        FieldDefine[] fields = uns.getFields();

        String columns = GrafanaUtils.fields2Columns(jdbcType, fields);
        String title = alias;
        String schema = "public";
        String table = alias;
        String tagNameCondition = "";
        if (StringUtils.isNotBlank(uns.getTbFieldName())) {
            table = uns.getTableName();
            tagNameCondition =   " and " + Constants.SYSTEM_SEQ_TAG + "='" + uns.getId() + "' ";
        }
        log.debug(">>>>>> create grafana dashboard columns:{},title:{},schema:{},table:{},tagNameCondition:{}", columns, title, schema, table, tagNameCondition);
        int dot = table.indexOf('.');
        if (dot > 0) {
            schema = table.substring(0, dot);
            table = table.substring(dot + 1);
        }
        String uuid = GrafanaUtils.createDashboard(table, tagNameCondition, jdbcType, schema, title, columns, Constants.SYS_FIELD_CREATE_TIME);

        DashboardPo dashboardPo = getById(uuid);
        if (dashboardPo == null) {
            Date now = new Date();
            DashboardPo po = new DashboardPo();
            po.setId(uuid);
            po.setName(alias);
            po.setCreateTime(now);
            po.setUpdateTime(now);
            dashboardMapper.insert(po);
        }

        int flag = UnsFlags.generateFlag(uns.getAddFlow(), uns.getSave2db(), true, uns.getRetainTableWhenDeleteInstance(), uns.getAccessLevel());
        LambdaUpdateWrapper<UnsPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UnsPo::getId, uns.getId());
        updateWrapper.set(UnsPo::getWithFlags, flag);
        unsMapper.update(updateWrapper);
        return ResultVO.success("ok");
    }
}
