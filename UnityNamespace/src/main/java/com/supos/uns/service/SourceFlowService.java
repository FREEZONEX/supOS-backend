package com.supos.uns.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowModelMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.service.NodeRedAdapterService;
import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.NodeRedTagsDTO;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.NodeRedUtils;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.service.exportimport.core.SourceFlowExportContext;
import com.supos.uns.service.exportimport.core.SourceFlowImportContext;
import com.supos.uns.service.exportimport.json.SourceFlowExporter;
import com.supos.uns.service.exportimport.json.SourceFlowImporter;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.SourceFlowExportParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月21日 14:26
 */
@Service
@Slf4j
public class SourceFlowService {
    @Autowired
    private NodeFlowMapper nodeFlowMapper;
    @Autowired
    private NodeFlowModelMapper nodeFlowModelMapper;
    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;

    public void asyncImport(File file, Consumer<RunningStatus> consumer) {
        if (!file.exists()) {
            String message = I18nUtils.getMessage("global.import.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }
        SourceFlowImportContext context = new SourceFlowImportContext(file.toString());
        context.setNodeRedHost(nodeRedAdapterService.getNodeRedHost());
        context.setNodeRedPort(nodeRedAdapterService.getNodeRedPort());
        SourceFlowImporter dataImporter = new SourceFlowImporter(context, nodeFlowMapper, nodeFlowModelMapper);
        try {
            dataImporter.importData(file);
        } catch (Throwable ex) {
            log.error("eventFlow ImportErr:{}", file.getPath(), ex);
            importFinish(dataImporter, consumer, file, context, ex);
            return;
        }
        importFinish(dataImporter, consumer, file, context, null);
    }

    private void importFinish(SourceFlowImporter dataImporter, Consumer<RunningStatus> consumer, File file, SourceFlowImportContext context, Throwable ex) {
        try {
            if (context.dataEmpty()) {
                // todo wsz
                String message = I18nUtils.getMessage("sourceFlow.import.excel.empty");
                consumer.accept(new RunningStatus(400, message));
            } else {
                String finalTask = I18nUtils.getMessage("sourceFlow.create.task.name.final");
                if (ex != null) {
                    Throwable cause = ex.getCause();
                    String errMsg;
                    if (cause != null) {
                        errMsg = cause.getMessage();
                    } else {
                        errMsg = ex.getMessage();
                    }
                    if (errMsg == null) {
                        errMsg = I18nUtils.getMessage("sourceFlow.create.status.error");
                    }
                    consumer.accept(new RunningStatus(500, errMsg)
                            .setTask(finalTask)
                            .setProgress(0.0)
                    );
                    return;
                }

                if (context.getCheckErrorMap().isEmpty()) {
                    String message = I18nUtils.getMessage("sourceFlow.import.rs.ok");
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
                String targetPath = String.format("%s%s%s/%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT_ERROR, GlobalExportModuleEnum.SOURCE_FLOW.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), fileName);
                File outFile = FileUtil.touch(targetPath);
                log.info("sourceFlow create error file:{}", outFile.toString());
                dataImporter.writeError(outFile);

                String message = I18nUtils.getMessage("sourceFlow.import.rs.hasErr");
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
            log.error("EventFlow导入失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    private void fetchData(SourceFlowExportContext context, SourceFlowExportParam exportParam, StopWatch stopWatch) {
        List<NodeFlowPO> flows = null;
        if(CollUtil.isNotEmpty(exportParam.getIds())){
            List<Long> ids = exportParam.getIds().stream().map(Long::parseLong).toList();
            flows = nodeFlowMapper.selectByIds(ids);
        }else if("ALL".equals(exportParam.getExportType())){
            flows = nodeFlowMapper.selectAll();
        }
        if (CollUtil.isNotEmpty(flows)) {
            stopWatch.start("global eventFlow export load data");
            List<Long> parentIds = new ArrayList<>();
            for (NodeFlowPO flowPO : flows) {
                flowPO.setCreateTime(null);
                flowPO.setUpdateTime(null);
                parentIds.add(flowPO.getId());
            }
            context.setFlows(flows);
            List<NodeFlowModelPO> nodeFlowModelPOS = nodeFlowModelMapper.selectByParentIds(parentIds);
            context.setFlowModels(nodeFlowModelPOS);
            stopWatch.stop();
        }
    }

    public JsonResult<String> dataExport(SourceFlowExportParam exportParam) {
        StopWatch stopWatch = new StopWatch();
        try {
            SourceFlowExportContext context = new SourceFlowExportContext();
            // 1.获取数据
            fetchData(context, exportParam, stopWatch);
            List<NodeFlowPO> flows = context.getFlows();
            if (CollectionUtils.isNotEmpty(flows)) {
                for (NodeFlowPO flow : flows) {
                    JSONArray nodes = JSON.parseArray(flow.getFlowData());
                    if (CollectionUtils.isNotEmpty(nodes)) {
                        JSONObject nodeTags = new JSONObject();
                        for (int i = 0; i < nodes.size(); i++) {
                            String nodeType = nodes.getJSONObject(i).getString("type");
                            // 统计关联了哪些模型topic
                            if ("supmodel".equals(nodeType)) {
                                String nodeId = nodes.getJSONObject(i).getString("id");
                                String result = NodeRedUtils.getTag(nodeId,nodeRedAdapterService.getNodeRedHost(), nodeRedAdapterService.getNodeRedPort());
                                NodeRedTagsDTO tagsResponse = JSON.parseObject(result, NodeRedTagsDTO.class);
                                nodeTags.put(nodeId, tagsResponse);
                            }
                        }
                        flow.setNodeTags(nodeTags);
                    }
                }
            }

            // 2.开始将数据写入json文件
            stopWatch.start("global sourceFlow export write data");
            String path = new SourceFlowExporter().exportData(context);
            stopWatch.stop();
            return new JsonResult<String>().setData(FileUtils.getRelativePath(path));
        } catch (Exception e) {
            log.error("global sourceFlow export error", e);
            String msg = I18nUtils.getMessage("global.sourceFlow.export.error");
            return new JsonResult<>(500, msg);
        } finally {
            log.info("global sourceFlow export time:{}", stopWatch.prettyPrint());
        }
    }
}
