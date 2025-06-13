package com.supos.uns.service.exportimport.core;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.ApplicationContextUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PathUtil;
import com.supos.uns.bo.CreateModelInstancesArgs;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsAddService;
import com.supos.uns.service.UnsLabelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.UnsTemplateService;
import com.supos.uns.service.exportimport.core.parser.*;
import com.supos.uns.vo.CreateTemplateVo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: DataImporter
 * @date 2025/5/10 11:37
 */
@Slf4j
public abstract class DataImporter {

    @Getter
    private StopWatch stopWatch = new StopWatch();

    @Getter
    private ExcelImportContext context;

    private UnsManagerService unsManagerService;
    private UnsLabelService unsLabelService;
    private UnsTemplateService unsTemplateService;
    private UnsAddService unsAddService;

    private Map<ExcelTypeEnum, ParserAble> parserMap = new HashMap<>();

    public DataImporter(ExcelImportContext context) {
        this.context = context;
        this.unsManagerService = ApplicationContextUtils.getBean(UnsManagerService.class);
        this.unsLabelService = ApplicationContextUtils.getBean(UnsLabelService.class);
        this.unsTemplateService = ApplicationContextUtils.getBean(UnsTemplateService.class);
        this.unsAddService = ApplicationContextUtils.getBean(UnsAddService.class);

        {
            parserMap.put(ExcelTypeEnum.Template, new TemplateParser());
            parserMap.put(ExcelTypeEnum.Label, new LabelParser());
            parserMap.put(ExcelTypeEnum.Folder, new FolderParser());
            parserMap.put(ExcelTypeEnum.FILE_TIMESERIES, new FileTimeseriesParser());
            parserMap.put(ExcelTypeEnum.FILE_RELATION, new FileRelationParser());
            parserMap.put(ExcelTypeEnum.FILE_CALCULATE, new FileCalculateParser());
            parserMap.put(ExcelTypeEnum.FILE_AGGREGATION, new FileAggregationParser());
            parserMap.put(ExcelTypeEnum.FILE_REFERENCE, new FileReferenceParser());
        }
    }

    protected ParserAble getParser(ExcelTypeEnum excelType) {
        return parserMap.get(excelType);
    }

    public abstract void importData(File file);
    public abstract void writeError(File srcfile, File outFile);

    public void doImport(ExcelTypeEnum excelTypeEnum) {
        if (excelTypeEnum == ExcelTypeEnum.Template) {
            importTemplate(context);
        } else if (excelTypeEnum == ExcelTypeEnum.Label) {
            importLabel(context);
        } else if (excelTypeEnum == ExcelTypeEnum.Folder) {
            importFolder(context);
        } else if (excelTypeEnum == ExcelTypeEnum.FILE_TIMESERIES
                || excelTypeEnum == ExcelTypeEnum.FILE_RELATION
                || excelTypeEnum == ExcelTypeEnum.FILE_CALCULATE
                || excelTypeEnum == ExcelTypeEnum.FILE_AGGREGATION
                || excelTypeEnum == ExcelTypeEnum.FILE_REFERENCE) {
            importFile(context, excelTypeEnum);
        }
    }

    /**
     * 导入模板
     * @param context
     */
    public void importTemplate(ExcelImportContext context) {
        // 导入模板
        List<CreateTemplateVo> templateVoList = context.getTemplateVoList();
        if (CollectionUtils.isNotEmpty(templateVoList)) {
            if (log.isInfoEnabled()) {
                log.info("*** Excel[{}] 发起导入模板请求 createTemplate：{}", context.getFile(), JsonUtil.toJsonUseFields(templateVoList));
            }
            stopWatch.start(String.format("import template,size:%d", templateVoList.size()));
            context.addAllError(unsTemplateService.createTemplates(templateVoList));
            stopWatch.stop();
        }
        context.clear();
    }

    /**
     * 导入标签
     * @param context
     */
    public void importLabel(ExcelImportContext context) {
        // 导入标签
        Set<String> labels = context.getLabels();
        if (CollectionUtils.isNotEmpty(labels)) {
            if (log.isInfoEnabled()) {
                log.info("*** Excel[{}] 发起导入标签请求 createLabel：{}", context.getFile(), JsonUtil.toJson(labels));
            }
            stopWatch.start(String.format("import label,size:%d", labels.size()));
            unsLabelService.create(labels);
            stopWatch.stop();
        }
        context.clear();
    }

    /**
     * 导入文件夹
     * @param context
     */
    public void importFolder(ExcelImportContext context) {
        // 导入文件夹
        List<ExcelUnsWrapDto> folderList = context.getUnsList();
        if (CollectionUtils.isNotEmpty(folderList)) {
            // 校验模板是否存在
            checkTemplateExist(context);

            // 处理上下级结构
            checkParent(context, false);
        }

        List<CreateTopicDto> saveFolders = folderList.stream().filter(ExcelUnsWrapDto::isCheckSuccess).map(ExcelUnsWrapDto::getUns).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(saveFolders)) {
            if (log.isInfoEnabled()) {
                log.info("*** Excel[{}] 发起导入文件夹请求 createFolder：{}", context.getFile(), JsonUtil.toJson(saveFolders));
            }
            stopWatch.start(String.format("import folder,size:%d", saveFolders.size()));
            CreateModelInstancesArgs args = new CreateModelInstancesArgs();
            args.setTopics(saveFolders);
            args.setFromImport(false);
            args.setThrowModelExistsErr(false);
            Map<String, String> rs = unsAddService.createModelAndInstancesInner(args);
            context.addAllError(rs);
            stopWatch.stop();
        }
        context.clear();
    }

    /**
     * 导入文件
     * @param context
     */
    public void importFile(ExcelImportContext context, ExcelTypeEnum excelType) {
        // 导入文件
        List<ExcelUnsWrapDto> fileList = context.getUnsList();
        if (CollectionUtils.isNotEmpty(fileList)) {
            stopWatch.start(String.format("import file check,size:%d", fileList.size()));
            // 校验模板是否存在
            checkTemplateExist(context);

            // 校验标签是否存在
            checkLabelExist(context);

            // 处理上下级结构
            checkParent(context, true);

            // 校验文件类型
            checkAliasExist(context);

            // 处理引用
            checkRefer(context);
            stopWatch.stop();

            List<CreateTopicDto> saveFiles = fileList.stream().filter(ExcelUnsWrapDto::isCheckSuccess)
                    .map(w -> {
                        CreateTopicDto uns = w.getUns();
                        if (CollectionUtils.isNotEmpty(w.getLabels())) {
                            uns.setLabelNames(w.getLabels().toArray(new String[w.getLabels().size()]));
                        }
                        return uns;
                    }).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(saveFiles)) {
                if (log.isInfoEnabled()) {
                    log.info("*** Excel[{}] 发起导入文件请求 createFile：{}", context.getFile(), saveFiles.size());
                }
                stopWatch.start(String.format("import file save,size:%d", saveFiles.size()));
                CreateModelInstancesArgs args = new CreateModelInstancesArgs();
                args.setTopics(saveFiles);
                args.setFromImport(false);
                args.setThrowModelExistsErr(false);
                Map<String, String> rs = unsAddService.createModelAndInstancesInner(args);
                context.addAllError(rs);
                stopWatch.stop();
            }
        }

        context.clear();
    }

    /**
     * 校验模板是否存在
     * @param context
     */
    private void checkTemplateExist(ExcelImportContext context) {
        Set<String> checkTemplateAlias = context.getCheckTemplateAlias();
        if (CollectionUtils.isNotEmpty(checkTemplateAlias)) {
            List<UnsPo> templates = unsTemplateService.list(Wrappers.lambdaQuery(UnsPo.class).in(UnsPo::getAlias, checkTemplateAlias));
            Map<String, UnsPo> templateMap = templates.stream().collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2));
            for (ExcelUnsWrapDto wrapDto : context.getUnsList()) {
                if  (wrapDto.isCheckSuccess()) {
                    if (StringUtils.isNotBlank(wrapDto.getTemplateAlias())) {
                        UnsPo template = templateMap.get(wrapDto.getTemplateAlias());
                        if (template == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.template.not.exists"));
                        } else {
                            wrapDto.getUns().setModelId(template.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * 校验标签是否存在
     * @param context
     */
    private void checkLabelExist(ExcelImportContext context) {
        Set<String> checkLabels = context.getCheckLabels();
        if (CollectionUtils.isNotEmpty(checkLabels)) {
            List<UnsLabelPo> labels = unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, checkLabels));
            Map<String, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
            for (ExcelUnsWrapDto wrapDto : context.getUnsList()) {
                if  (wrapDto.isCheckSuccess()) {
                    if (CollectionUtils.isNotEmpty(wrapDto.getLabels())) {
                        for (String label : wrapDto.getLabels()) {
                            if (!labelMap.containsKey(label)) {
                                wrapDto.setCheckSuccess(false);
                                context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.label.not.exists"));
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkParent(ExcelImportContext context, boolean isFile) {
        Map<ExcelUnsWrapDto, String> parentMap = new HashMap<>();
        for (ExcelUnsWrapDto wrapDto : context.getUnsList()) {
            if (wrapDto.isCheckSuccess()) {
                CreateTopicDto uns = wrapDto.getUns();
                String parentPath = PathUtil.subParentPath(uns.getPath());
                if (parentPath != null) {
                    if (!isFile) {
                        ExcelUnsWrapDto parentWrap = context.getUnsMap().get(parentPath);
                        if (parentWrap != null) {
                            if (!parentWrap.isCheckSuccess()) {
                                wrapDto.setCheckSuccess(false);
                                context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.folder.parent.not.found"));
                            } else {
                                uns.setParentAlias(parentWrap.getUns().getAlias());
                            }
                        } else {
                            // 查看数据库是否存在父节点
                            parentMap.put(wrapDto, parentPath);
                        }
                    } else {
                        // 文件只查看数据库是否存在父节点
                        parentMap.put(wrapDto, parentPath);
                    }

                }
            }
        }
        if (MapUtils.isNotEmpty(parentMap)) {
            List<UnsPo> parentList = unsAddService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 0).in(UnsPo::getPath, parentMap.values()));
            Map<String, UnsPo> folderMap = parentList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));
            for(Map.Entry<ExcelUnsWrapDto, String> entry : parentMap.entrySet()) {
                UnsPo parent = folderMap.get(entry.getValue());
                if (parent != null) {
                    entry.getKey().getUns().setParentAlias(parent.getAlias());
                } else {
                    entry.getKey().setCheckSuccess(false);
                    context.addError(entry.getKey().getBatchIndex(), I18nUtils.getMessage("uns.folder.parent.not.found"));
                }
            }
        }
    }

    private void checkAliasExist(ExcelImportContext context) {
        List<ExcelUnsWrapDto> allUnsList = context.getUnsList();
        Set<String> tempAliasFromDb = context.getTempAliasFromDb();
        List<ExcelUnsWrapDto> firstCheckSuccessUnsList = allUnsList.stream().filter(wrapDto -> wrapDto.isCheckSuccess()).collect(Collectors.toList());
        List<ExcelUnsWrapDto> secondCheckSuccessUnsList = new ArrayList<>(firstCheckSuccessUnsList.size());
        for (ExcelUnsWrapDto wrapDto : firstCheckSuccessUnsList) {
            if (tempAliasFromDb.contains(wrapDto.getUns().getAlias())) {
                wrapDto.setCheckSuccess(false);
                context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.alias.has.exist"));
            } else {
                secondCheckSuccessUnsList.add(wrapDto);
            }
        }

        if (CollectionUtils.isNotEmpty(secondCheckSuccessUnsList)) {
            Set<String> aliasSet = secondCheckSuccessUnsList.stream().map(wrapDto -> wrapDto.getUns().getAlias()).collect(Collectors.toSet());
            List<UnsPo> unsPos = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).select(UnsPo::getAlias).in(UnsPo::getAlias, aliasSet));
            if (CollectionUtils.isNotEmpty(unsPos)) {
                Set<String> existAliasSet = unsPos.stream().map(UnsPo::getAlias).collect(Collectors.toSet());
                tempAliasFromDb.addAll(existAliasSet);

                for (ExcelUnsWrapDto wrapDto : secondCheckSuccessUnsList) {
                    if (tempAliasFromDb.contains(wrapDto.getUns().getAlias())) {
                        wrapDto.setCheckSuccess(false);
                        context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.alias.has.exist"));
                    }
                }
            }
        }
    }

    private void checkRefer(ExcelImportContext context) {
        Set<String> checkReferPaths = context.getCheckReferPaths();
        Set<String> checkReferAliass = context.getCheckReferAliass();

        Map<String, UnsPo> referAliasMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(checkReferAliass)) {
            List<UnsPo> referList = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).select(UnsPo::getId, UnsPo::getAlias, UnsPo::getDataType).eq(UnsPo::getPathType, 2).in(UnsPo::getAlias, checkReferAliass));
            referAliasMap.putAll(referList.stream().collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2)));
        }

        Map<String, UnsPo> referPathMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(checkReferPaths)) {
            List<UnsPo> referList = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).select(UnsPo::getId, UnsPo::getAlias, UnsPo::getPath, UnsPo::getDataType).eq(UnsPo::getPathType, 2).in(UnsPo::getPath, checkReferPaths));
            referPathMap.putAll(referList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2)));
        }

        for (ExcelUnsWrapDto wrapDto : context.getUnsList()) {
            if (wrapDto.isCheckSuccess() && wrapDto.getRefers() != null) {
                for (InstanceField refer : wrapDto.getRefers()) {
                    UnsPo existRefer = null;
                    if (StringUtils.isNotBlank(refer.getAlias())) {
                        // alias 存在就直接校验alias
                        existRefer = referAliasMap.get(refer.getAlias());
                        if (existRefer == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.refer.alias.noexist"));
                            continue;
                        }
                    } else if (StringUtils.isNotBlank(refer.getPath())) {
                        // 其次才会校验path
                        existRefer = referPathMap.get(refer.getPath());
                        if (existRefer == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.refer.path.noexist"));
                            continue;
                        }
                    }
                    if (existRefer != null) {
                        if (existRefer.getDataType() == Constants.TIME_SEQUENCE_TYPE || existRefer.getDataType() == Constants.RELATION_TYPE) {
                            refer.setAlias(existRefer.getAlias());
                            refer.setId(existRefer.getId());
                            refer.setPath(null);
                        } else {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getBatchIndex(), I18nUtils.getMessage("uns.refer.datatype.invalid"));
                            continue;
                        }
                    }
                }
                wrapDto.getUns().setRefers(wrapDto.getRefers());
            }
        }
    }
}
