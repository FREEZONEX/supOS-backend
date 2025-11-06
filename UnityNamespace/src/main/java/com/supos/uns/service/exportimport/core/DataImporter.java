package com.supos.uns.service.exportimport.core;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.excel.ExcelUnsWrapDto;
import com.supos.common.enums.ExcelTypeEnum;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    public DataImporter(ExcelImportContext context, UnsManagerService unsManagerService, UnsLabelService unsLabelService,
                        UnsTemplateService unsTemplateService, UnsAddService unsAddService) {
        this.context = context;
        this.unsManagerService = unsManagerService;
        this.unsLabelService = unsLabelService;
        this.unsTemplateService = unsTemplateService;
        this.unsAddService = unsAddService;

        {
            parserMap.put(ExcelTypeEnum.Template, new TemplateParser());
            parserMap.put(ExcelTypeEnum.Label, new LabelParser());
            parserMap.put(ExcelTypeEnum.Folder, new FolderParser());
            parserMap.put(ExcelTypeEnum.FILE_TIMESERIES, new FileTimeseriesParser());
            parserMap.put(ExcelTypeEnum.FILE_RELATION, new FileRelationParser());
            parserMap.put(ExcelTypeEnum.FILE_CALCULATE, new FileCalculateParser());
            parserMap.put(ExcelTypeEnum.FILE_AGGREGATION, new FileAggregationParser());
            parserMap.put(ExcelTypeEnum.FILE_REFERENCE, new FileReferenceParser());
            parserMap.put(ExcelTypeEnum.FILE_JSONB, new FileJsonbParser());

            parserMap.put(ExcelTypeEnum.UNS, new UnsParser());
            parserMap.put(ExcelTypeEnum.File, new FileParser());
        }
    }

    protected ParserAble getParser(ExcelTypeEnum excelType) {
        return parserMap.get(excelType);
    }

    public abstract void importData(File file);
    public abstract void writeError(File srcfile, File outFile);



    /**
     * 导入模板
     * @param context
     */
    public void importTemplate(ExcelImportContext context) {
        // 导入模板
        if (context.templateSize() > 0) {
            Collection<CreateTemplateVo> templates = context.getTemplateMap().values();
            if (log.isInfoEnabled()) {
                log.info("*** Excel[{}] 发起导入模板请求 createTemplate：{}", context.getFile(), JsonUtil.toJsonUseFields(templates));
            }
            stopWatch.start(String.format("import template,size:%d", templates.size()));
            context.addAllError(unsTemplateService.createTemplates(templates));
            stopWatch.stop();
        }
        context.clearAfterTemplate();
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
        context.clearAfterLabel();
    }

    /**
     * 导入文件夹
     * @param context
     */
    public void importFolder(ExcelImportContext context) {
        // 导入文件夹
        Collection<ExcelUnsWrapDto> folderList = context.getFolderMap().values();
        if (CollectionUtils.isNotEmpty(folderList)) {
            // 校验模板是否存在
            checkTemplateExist(folderList, context);

            // 校验path
            checkPath(folderList, context);

            // 处理上下级结构
            checkParentPre(folderList, context, false);
            createAutoFolder(context);
            checkParentPost(folderList, context, false);

            // 校验别名
            checkAliasExist(folderList, context);
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
        context.clearAfterFolder();
    }

    /**
     * 剥离引用源文件
     * @param fileMap
     */
    protected void separationRefer(Map<String, ExcelUnsWrapDto> fileMap) {
        Set<String> removePaths = new HashSet<>();
        for (Map.Entry<String, ExcelUnsWrapDto> e : fileMap.entrySet()) {
            ExcelUnsWrapDto unsWrapDto = e.getValue();
            if (context.aliasIsReferSource(unsWrapDto.getAlias())) {
                context.addUnsToReferSource(unsWrapDto);
                removePaths.add(e.getKey());
            } else if (context.pathIsReferSource(unsWrapDto.getPath())) {
                context.addUnsToReferSource(unsWrapDto);
                removePaths.add(e.getKey());
            }
        }

        for (String removePath : removePaths) {
            fileMap.remove(removePath);
        }
    }

    /**
     * 导入文件
     * @param context
     */
    public void importFile(ExcelImportContext context, int dataType) {
        // 导入文件
        Collection<ExcelUnsWrapDto> fileList = null;
        if (dataType == ExcelImportContext.REFER_DATATYPE) {
            fileList = context.getFileSourceMap().values();
        } else if (dataType == Constants.TIME_SEQUENCE_TYPE) {
            fileList = context.getFileTimeseriesMap().values();
        } else if (dataType == Constants.RELATION_TYPE) {
            fileList = context.getFileRelationMap().values();
        }else if (dataType == Constants.CALCULATION_REAL_TYPE) {
            fileList = context.getFileCalculateMap().values();
        }else if (dataType == Constants.MERGE_TYPE) {
            fileList = context.getFileAggregationMap().values();
        }else if (dataType == Constants.CITING_TYPE) {
            fileList = context.getFileReferenceMap().values();
        }else if (dataType == Constants.JSONB_TYPE) {
            fileList = context.getFileJsonbMap().values();
        }

        if (CollectionUtils.isNotEmpty(fileList)) {
            List<List<ExcelUnsWrapDto>> subFileLists = CollectionUtil.split(fileList, 2000);
            for (List<ExcelUnsWrapDto> subFileList : subFileLists) {
                stopWatch.start(String.format("import file check,size:%d", subFileList.size()));
                // 校验模板是否存在
                checkTemplateExist(subFileList, context);

                // 校验标签是否存在
                checkLabelExist(subFileList, context);

                // 校验path
                checkPath(subFileList, context);

                // 处理上下级结构
                checkParentPre(subFileList, context, true);
                createAutoFolder(context);
                checkParentPost(subFileList, context, true);

                // 校验文件类型
                checkAliasExist(subFileList, context);

                // 处理引用
                checkRefer(subFileList, context);
                stopWatch.stop();

                List<CreateTopicDto> saveFiles = subFileList.stream().filter(ExcelUnsWrapDto::isCheckSuccess)
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
        }

        context.clear(ExcelTypeEnum.File, dataType);
    }

    /**
     * 校验模板是否存在
     * @param context
     */
    private void checkTemplateExist(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context) {
        Set<String> checkTemplateAlias = context.getCheckTemplateAlias();
        if (CollectionUtils.isNotEmpty(checkTemplateAlias)) {
            List<UnsPo> templates = unsTemplateService.list(Wrappers.lambdaQuery(UnsPo.class)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getAlias, checkTemplateAlias));
            Map<String, UnsPo> templateMap = templates.stream().collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2));
            for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
                if  (wrapDto.isCheckSuccess()) {
                    if (StringUtils.isNotBlank(wrapDto.getTemplateAlias())) {
                        UnsPo template = templateMap.get(wrapDto.getTemplateAlias());
                        if (template == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.template.not.exists"));
                        } else {
                            wrapDto.getUns().setModelId(template.getId());
                            wrapDto.getUns().setFields(template.getFields());
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
    private void checkLabelExist(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context) {
        Set<String> checkLabels = context.getCheckLabels();
        if (CollectionUtils.isNotEmpty(checkLabels)) {
            List<UnsLabelPo> labels = unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class)
                            //.eq(UnsLabelPo::getDelFlag, false)
                    .in(UnsLabelPo::getLabelName, checkLabels));
            Map<String, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
            for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
                if  (wrapDto.isCheckSuccess()) {
                    if (CollectionUtils.isNotEmpty(wrapDto.getLabels())) {
                        for (String label : wrapDto.getLabels()) {
                            if (!labelMap.containsKey(label)) {
                                wrapDto.setCheckSuccess(false);
                                context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.label.not.exists"));
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 校验path是否重复
     */
    private void checkPath(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context) {
        Map<ExcelUnsWrapDto, String> pathMap = new HashMap<>();
        for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
            if (wrapDto.isCheckSuccess()) {
                pathMap.put(wrapDto, wrapDto.getPath());
            }
        }

        if (MapUtils.isNotEmpty(pathMap)) {
            List<UnsPo> parentList = unsAddService.list(Wrappers.lambdaQuery(UnsPo.class)
                    //.eq(UnsPo::getPathType, isFile ? Constants.PATH_TYPE_FILE : Constants.PATH_TYPE_DIR)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getPath, pathMap.values()));
            Map<String, UnsPo> unsMap = parentList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));
            for(Map.Entry<ExcelUnsWrapDto, String> entry : pathMap.entrySet()) {
                UnsPo uns = unsMap.get(entry.getValue());
                if (uns != null) {
                    entry.getKey().setCheckSuccess(false);
                    context.addError(entry.getKey().getFlagNo(), I18nUtils.getMessage("uns.import.exist", "namespace", uns.getPath()));
                }
            }
        }
    }

    /**
     * 校验父级文件夹是否存在是否存在，不存在就将路径收集起来便于后续自动创建
     */
    private void checkParentPre(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context, boolean isFile) {
        Map<ExcelUnsWrapDto, String> parentMap = new HashMap<>();
        for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
            if (wrapDto.isCheckSuccess()) {
                CreateTopicDto uns = wrapDto.getUns();
                if (uns.getParentAlias() == null) {
                    String parentPath = PathUtil.subParentPath(uns.getPath());
                    if (parentPath != null) {
                        if (!isFile) {
                            ExcelUnsWrapDto parentWrap = context.getFolderMap().get(parentPath);
                            if (parentWrap != null) {
                                if (!parentWrap.isCheckSuccess()) {
                                    // 父级文件夹未通过, 再看数据库里有没有
                                    parentMap.put(wrapDto, parentPath);
                                    //wrapDto.setCheckSuccess(false);
                                    //context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.folder.parent.not.found"));
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
        }
        if (MapUtils.isNotEmpty(parentMap)) {
            List<UnsPo> parentList = unsAddService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .eq(UnsPo::getPathType, 0)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getPath, parentMap.values()));
            Map<String, UnsPo> folderMap = parentList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));
            for(Map.Entry<ExcelUnsWrapDto, String> entry : parentMap.entrySet()) {
                UnsPo parent = folderMap.get(entry.getValue());
                if (parent != null) {
                    // 所属文件夹存在
                    entry.getKey().getUns().setParentAlias(parent.getAlias());
                } else {
                    // 所属文件夹不存在，尝试创建
                    context.addAutoFolder(entry.getKey(), entry.getValue());
                    //entry.getKey().setCheckSuccess(false);
                    //context.addError(entry.getKey().getFlagNo(), I18nUtils.getMessage("uns.folder.parent.not.found"));
                }
            }
        }
    }

    /**
     * 创建自动的父级文件夹
     * @param context
     */
    private void createAutoFolder(ExcelImportContext context) {
        if (MapUtils.isNotEmpty(context.getAutoFolderMap())) {
            Set<String> allParentPathSet = new HashSet<>();
            // uns节点对应父节点路径：uns节点：[父节点路径1，父节点路径2，...]，由长到短
            Map<ExcelUnsWrapDto, List<String>> parentPathMap = new HashMap<>();
            for (Map.Entry<ExcelUnsWrapDto, String> entry : context.getAutoFolderMap().entrySet()) {
                List<String> parentPaths = parentPathMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                String parentPath = entry.getValue();
                while (parentPath != null) {
                    parentPaths.add(parentPath);
                    allParentPathSet.add(parentPath);
                    parentPath = PathUtil.subParentPath(parentPath);
                }
            }

            // 查询已存在的部分父文件夹
            List<UnsPo> parentFolderList = unsAddService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .eq(UnsPo::getPathType, 0)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getPath, allParentPathSet));
            Map<String, UnsPo> parentFolderMap = parentFolderList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));

            // 查看哪些父文件夹需要创建
            Stack<String> needCreatePaths = new Stack<>();
            for (Map.Entry<ExcelUnsWrapDto, List<String>> entry : parentPathMap.entrySet()) {
                for (String parentPath : entry.getValue()) {
                    UnsPo parent = parentFolderMap.get(parentPath);
                    if (parent != null) {
                        // 所属文件夹存在
                        break;
                    } else {
                        // 所属文件夹不存在，尝试创建
                        needCreatePaths.push(parentPath);
                    }
                }
            }

            // 创建父文件夹
            AtomicInteger flagNo = new AtomicInteger(0);
            Map<String, CreateTopicDto> newFolderMap = new HashMap<>();
            while (!needCreatePaths.isEmpty()) {
                String path = needCreatePaths.pop();
                if (newFolderMap.containsKey(path)) {
                    continue;
                }
                String parentPath = PathUtil.subParentPath(path);
                String parentAlias = null;
                if (parentPath != null) {
                    CreateTopicDto parentFolder = newFolderMap.get(parentPath);
                    if (parentFolder != null) {
                        parentAlias = parentFolder.getAlias();
                    } else {
                        UnsPo parentUns = parentFolderMap.get(parentPath);
                        if (parentUns != null) {
                            parentAlias = parentUns.getAlias();
                        }
                    }
                }
                CreateTopicDto topicDto = new CreateTopicDto();
                topicDto.setFlagNo(String.valueOf(flagNo.getAndIncrement()));
                topicDto.setPath(path);
                topicDto.setName(PathUtil.getName(path));
                topicDto.setAlias(PathUtil.generateAlias(path,0));
                topicDto.setParentAlias(parentAlias);
                topicDto.setPathType(0);
                newFolderMap.put(path, topicDto);
            }
            if (MapUtils.isNotEmpty(newFolderMap)) {
                List<CreateTopicDto> saveFolders = newFolderMap.values().stream().collect(Collectors.toList());
                if (log.isInfoEnabled()) {
                    log.info("*** Excel[{}] 发起导入文件夹请求 createAutoFolder：{}", context.getFile(), JsonUtil.toJson(saveFolders));
                }

                CreateModelInstancesArgs args = new CreateModelInstancesArgs();
                args.setTopics(saveFolders);
                args.setFromImport(false);
                args.setThrowModelExistsErr(false);
                Map<String, String> rs = unsAddService.createModelAndInstancesInner(args);
                context.addAllError(rs);

            }
        }
    }

    private void checkParentPost(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context, boolean isFile) {
        Map<ExcelUnsWrapDto, String> parentMap = new HashMap<>();
        for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
            if (wrapDto.isCheckSuccess()) {
                CreateTopicDto uns = wrapDto.getUns();
                if (uns.getParentAlias() == null) {
                    String parentPath = PathUtil.subParentPath(uns.getPath());
                    if (parentPath != null) {
                        if (!isFile) {
                            ExcelUnsWrapDto parentWrap = context.getFolderMap().get(parentPath);
                            if (parentWrap != null) {
                                if (!parentWrap.isCheckSuccess()) {
                                    wrapDto.setCheckSuccess(false);
                                    context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.folder.parent.not.found"));
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
        }
        if (MapUtils.isNotEmpty(parentMap)) {
            List<UnsPo> parentList = unsAddService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .eq(UnsPo::getPathType, 0)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getPath, parentMap.values()));
            Map<String, UnsPo> folderMap = parentList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));
            for(Map.Entry<ExcelUnsWrapDto, String> entry : parentMap.entrySet()) {
                UnsPo parent = folderMap.get(entry.getValue());
                if (parent != null) {
                    // 所属文件夹存在
                    entry.getKey().getUns().setParentAlias(parent.getAlias());
                } else {
                    // 所属文件夹不存在
                    entry.getKey().setCheckSuccess(false);
                    context.addError(entry.getKey().getFlagNo(), I18nUtils.getMessage("uns.folder.parent.not.found"));
                }
            }
        }
    }

    private void checkAliasExist(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context) {
        Set<String> tempAliasFromDb = context.getTempAliasFromDb();
        List<ExcelUnsWrapDto> firstCheckSuccessUnsList = wrapDtoList.stream().filter(wrapDto -> wrapDto.isCheckSuccess()).collect(Collectors.toList());
        List<ExcelUnsWrapDto> secondCheckSuccessUnsList = new ArrayList<>(firstCheckSuccessUnsList.size());
        for (ExcelUnsWrapDto wrapDto : firstCheckSuccessUnsList) {
            if (tempAliasFromDb.contains(wrapDto.getUns().getAlias())) {
                wrapDto.setCheckSuccess(false);
                context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.alias.has.exist"));
            } else {
                secondCheckSuccessUnsList.add(wrapDto);
            }
        }

        if (CollectionUtils.isNotEmpty(secondCheckSuccessUnsList)) {
            Set<String> aliasSet = secondCheckSuccessUnsList.stream().map(wrapDto -> wrapDto.getUns().getAlias()).collect(Collectors.toSet());
            List<UnsPo> unsPos = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .select(UnsPo::getAlias)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getAlias, aliasSet));
            if (CollectionUtils.isNotEmpty(unsPos)) {
                Set<String> existAliasSet = unsPos.stream().map(UnsPo::getAlias).collect(Collectors.toSet());
                tempAliasFromDb.addAll(existAliasSet);

                for (ExcelUnsWrapDto wrapDto : secondCheckSuccessUnsList) {
                    if (tempAliasFromDb.contains(wrapDto.getUns().getAlias())) {
                        wrapDto.setCheckSuccess(false);
                        context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.alias.has.exist"));
                    }
                }
            }
        }
    }

    private void checkRefer(Collection<ExcelUnsWrapDto> wrapDtoList, ExcelImportContext context) {
        Set<String> checkReferPaths = context.getCheckReferPaths();
        Set<String> checkReferAliass = context.getCheckReferAliass();

        Map<String, UnsPo> referAliasMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(checkReferAliass)) {
            List<UnsPo> referList = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .select(UnsPo::getId, UnsPo::getAlias, UnsPo::getDataType)
                    .eq(UnsPo::getPathType, 2)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getAlias, checkReferAliass));
            referAliasMap.putAll(referList.stream().collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2)));
        }

        Map<String, UnsPo> referPathMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(checkReferPaths)) {
            List<UnsPo> referList = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .select(UnsPo::getId, UnsPo::getAlias, UnsPo::getPath, UnsPo::getDataType)
                    .eq(UnsPo::getPathType, 2)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getPath, checkReferPaths));
            referPathMap.putAll(referList.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2)));
        }

        for (ExcelUnsWrapDto wrapDto : wrapDtoList) {
            if (wrapDto.isCheckSuccess() && wrapDto.getRefers() != null) {
                for (InstanceField refer : wrapDto.getRefers()) {
                    UnsPo existRefer = null;
                    if (StringUtils.isNotBlank(refer.getAlias())) {
                        // alias 存在就直接校验alias
                        existRefer = referAliasMap.get(refer.getAlias());
                        if (existRefer == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.refer.alias.noexist"));
                            continue;
                        }
                    } else if (StringUtils.isNotBlank(refer.getPath())) {
                        // 其次才会校验path
                        existRefer = referPathMap.get(refer.getPath());
                        if (existRefer == null) {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.refer.path.noexist"));
                            continue;
                        }
                    }
                    if (existRefer != null) {
                        //if (existRefer.getDataType() == Constants.TIME_SEQUENCE_TYPE || existRefer.getDataType() == Constants.RELATION_TYPE) {
                            refer.setAlias(existRefer.getAlias());
                            refer.setId(existRefer.getId());
                            refer.setPath(null);
                        /*} else {
                            wrapDto.setCheckSuccess(false);
                            context.addError(wrapDto.getFlagNo(), I18nUtils.getMessage("uns.refer.datatype.invalid"));
                            continue;
                        }*/
                    }
                }
                wrapDto.getUns().setRefers(wrapDto.getRefers());
            }
        }
    }
}
