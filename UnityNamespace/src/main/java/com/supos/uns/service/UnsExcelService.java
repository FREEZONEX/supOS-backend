package com.supos.uns.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.SuposIdUtil;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.dao.mapper.UnsExportRecordMapper;
import com.supos.uns.dao.po.UnsExportRecordPo;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.cjson.ComplexJsonDataExporter;
import com.supos.uns.service.exportimport.cjson.ComplexJsonDataImporter;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.ExportImportHelper;
import com.supos.uns.service.exportimport.excel.ExcelDataExporter;
import com.supos.uns.service.exportimport.excel.ExcelDataImporter;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.ExportParam;
import com.supos.uns.vo.UnsExportRecordConfirmReq;
import com.supos.uns.vo.UnsExportRecordVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UnsExcelService {

    public static final ExecutorService globalExportMasterEs = new ThreadPoolExecutor(1, 1,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), new ThreadFactory() {
        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "uns export or import task thread: " + integer.getAndIncrement());
        }
    }, new ThreadPoolExecutor.CallerRunsPolicy());

    @Resource
    private UnsManagerService unsManagerService;

    @Resource
    private UnsLabelService unsLabelService;

    @Resource
    private UnsLabelRefService unsLabelRefService;

    @Autowired
    UnsTemplateService unsTemplateService;
    @Autowired
    private UnsAddService unsAddService;

    @Autowired
    private UnsExportRecordMapper unsExportRecordMapper;

    public void downloadTemplate(String fileType, HttpServletResponse response) {
        if (StringUtils.equals(fileType, "excel")) {
            doDownloadExcelTemplate(response);
        } else if (StringUtils.equals(fileType, "json")) {
            doDownloadJsonTemplate(response);
        }
    }

    public void doDownloadExcelTemplate(HttpServletResponse response) {
        ClassPathResource classPathResource = new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH);
        InputStream inputStream = null;
        String targetPath = null;
        InputStream targetInputStream = null;
        try {
            inputStream = classPathResource.getInputStream();

            // 生成临时文件
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.EXCEL_OUT_PATH);
            targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);

            String templatePath = Constants.EXCEL_TEMPLATE_PATH;
            Locale locale = LocaleContextHolder.getLocale();
            if (locale != null) {
                if (StringUtils.containsIgnoreCase(locale.getLanguage(), "zh")) {
                    templatePath = Constants.EXCEL_TEMPLATE_ZH_PATH;
                }
            }
            ExcelWriter excelWriter = EasyExcel.write(targetPath).withTemplate(new ClassPathResource(templatePath).getInputStream()).build();
            writeExplanationRow(excelWriter);
            excelWriter.finish();

            targetInputStream = FileUtil.getInputStream(targetPath);

            FileUtils.downloadFile(response, path.split("/")[path.split("/").length - 1], targetInputStream);
        } catch (IOException e) {
            log.error("downloadByPath Exception", e);
        } finally {
            if (inputStream != null) {
                IoUtil.close(inputStream);
            }
            if (targetInputStream != null) {
                IoUtil.close(targetInputStream);
            }
            if (targetPath != null) {
                FileUtil.del(targetPath);
            }
        }
    }

    public void doDownloadJsonTemplate(HttpServletResponse response) {
        ClassPathResource classPathResource = new ClassPathResource(Constants.JSON_TEMPLATE_PATH);
        InputStream inputStream = null;
        try {
            inputStream = classPathResource.getInputStream();
            String path = Constants.JSON_TEMPLATE_PATH;
            FileUtils.downloadFile(response, path.split("/")[path.split("/").length - 1], inputStream);
        } catch (IOException e) {
            log.error("downloadByPath Exception", e);
        } finally {
            if (inputStream != null) {
                IoUtil.close(inputStream);
            }
        }
    }

    public void excelDownload(HttpServletResponse response, String path) {
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(FileUtils.getFileRootPath(), path);
            if (!file.exists()) {
                throw new BuzException("uns.file.not.exist");
            }

            String extName = FileUtil.extName(file.getName());
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String fileName = file.getName();
/*            if ("xlsx".equals(extName)) {
                fileName = String.format("%s%s", datePath, ".xlsx");
            } else if ("json".equals(extName)) {
                fileName = String.format("%s%s", datePath, ".json");
            }*/
            FileUtils.downloadFile(response, fileName, new FileInputStream(file));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    public static class LogWrapperConsumer implements Consumer<RunningStatus> {
        final Consumer<RunningStatus> target;
        Boolean finished;
        String lastTask;
        Double lastProgress;

        public LogWrapperConsumer(Consumer<RunningStatus> target) {
            this.target = target;
        }

        @Override
        public void accept(RunningStatus runningStatus) {
            log.info("** status: {}", JSON.toJSONString(runningStatus));
            finished = runningStatus.getFinished();
            String task = runningStatus.getTask();
            if (task != null) {
                lastTask = task;
            }
            Double progress = runningStatus.getProgress();
            if (progress != null) {
                lastProgress = progress;
            }
            target.accept(runningStatus);
        }
    }

    public void asyncImport(File file, Consumer<RunningStatus> consumer, boolean isAsync, String language) {
        if (isAsync) {
            ThreadUtil.newThread(() -> {
                doImport(file, consumer, language);
            }, "asyncImport-Thread").start();
            consumer.accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.start"))
                    .setFinished(false)
                    .setProgress(5.0));
        } else {
            consumer.accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.start"))
                    .setFinished(false)
                    .setProgress(5.0));
            doImport(file, consumer, language);

        }
    }

    private void doImport(File file, Consumer<RunningStatus> consumer, String language) {
        if (!file.exists()) {
            String message = I18nUtils.getMessage("uns.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }

        String extName = FileUtil.extName(file.getName());
        ExcelImportContext context = new ExcelImportContext(file.toString(), extName, consumer, language);
        DataImporter dataImporter = null;
        try {
            if ("xlsx".equals(extName)) {
                dataImporter = new ExcelDataImporter(context, unsManagerService, unsLabelService, unsTemplateService, unsAddService);
            } else if ("json".equals(extName)) {
                dataImporter = new ComplexJsonDataImporter(context, unsManagerService, unsLabelService, unsTemplateService, unsAddService);
            }
            dataImporter.importData(file);
        } catch (Throwable ex) {
            log.error("UnsImportErr:{}", file.getPath(), ex);
            importFinish(dataImporter, extName, file.getPath(), consumer, file, context, ex);
            return;
        }
        importFinish(dataImporter, extName, file.getPath(), consumer, file, context, null);
    }


    private void importFinish(DataImporter dataImporter, String extName, String excelFilePath, Consumer<RunningStatus> consumer, File file, ExcelImportContext context, Throwable ex) {
        try {
            if (context.dataEmpty()) {
                String message = I18nUtils.getMessage("uns.import.excel.empty");
                consumer.accept(new RunningStatus(400, message));
                return;
            } else {
                LogWrapperConsumer wrapperConsumer = new LogWrapperConsumer(consumer);
                String finalTask = I18nUtils.getMessage("uns.create.task.name.final");

                if (ex != null) {
                    String lastTask = wrapperConsumer.lastTask;
                    Double lastProgress = wrapperConsumer.lastProgress;
                    log.error("UnsImportErr:{} lastTask={}", excelFilePath, lastTask, ex);
                    Throwable cause = ex.getCause();
                    String errMsg;
                    if (cause != null) {
                        errMsg = cause.getMessage();
                    } else {
                        errMsg = ex.getMessage();
                    }
                    if (errMsg == null) {
                        errMsg = I18nUtils.getMessage("uns.create.status.error");
                    }
                    consumer.accept(new RunningStatus(500, errMsg)
                            .setTask(lastTask != null ? lastTask : finalTask)
                            .setProgress(lastProgress != null ? lastProgress : 0.0)
                    );
                    return;
                }

                if (context.getExcelCheckErrorMap().isEmpty()) {
                    String message = I18nUtils.getMessage("uns.import.rs.ok");
                    consumer.accept(new RunningStatus(200, message)
                            .setTask(finalTask)
                            .setProgress(100.0));
                    return;
                }

                if (context.getFileType().equals("xlsx")) {
                    Map<Integer, Map<Integer, String>> error = new HashMap<>();
                    for (Map.Entry<String, String> entry : context.getExcelCheckErrorMap().entrySet()) {
                        String[] keyArr = entry.getKey().split("-");
                        Map<Integer, String> subError = error.computeIfAbsent(Integer.valueOf(keyArr[0]), k -> new HashMap<>());
                        subError.put(Integer.valueOf(keyArr[1]), entry.getValue());
                    }
                    context.getExcelCheckErrorMap().clear();
                    context.getError().putAll(error);
                }

                File outFile = destFile("err_" + file.getName().replace(' ', '-'));
                log.info("create error file:{}", outFile.toString());
                dataImporter.writeError(file, outFile);

                String message = I18nUtils.getMessage("uns.import.rs.hasErr");
                consumer.accept(new RunningStatus(206, message, FileUtils.getRelativePath(outFile.getAbsolutePath()))
                        .setTask(finalTask)
                        .setProgress(100.0));
            }
        } catch (Throwable e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }

    }


    public JsonResult<String> templateImport(MultipartFile file) {
        String extName = FileUtil.extName(file.getOriginalFilename());
        if (!"xlsx".equals(extName) && !"json".equals(extName)) {
            throw new BuzException("uns.import.not.xlsx");
        }
        try {
            if ("xlsx".equals(extName)) {
                int sheetIndex = 0;
                ExcelReader reader = ExcelUtil.getReader(file.getInputStream(), sheetIndex);
                int sheetCount = reader.getSheetCount();
                boolean hasData = false;
                do {
                    String sheetName = reader.getSheet().getSheetName();
                    ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
                    if (ExcelTypeEnum.ERROR.equals(excelType)) {
                        throw new BuzException("uns.import.template.error");
                    }
                    List<Object> heads = reader.readRow(0);
                    if (CollectionUtils.isEmpty(heads) || !ExportImportHelper.checkHead(excelType, heads)) {
                        throw new BuzException("uns.import.head.error", sheetName);
                    }

/*                    List<Map<String, Object>> dataList = reader.read(0, 4, 7);
                    if (!CollectionUtils.isEmpty(dataList)) {
                        hasData = true;
                    }*/
                    sheetIndex++;
                    if (sheetIndex < sheetCount) {
                        reader.setSheet(sheetIndex);
                    }
                } while (sheetIndex < sheetCount);

/*                if (!hasData) {
                    String msg = I18nUtils.getMessage("uns.import.excel.empty");
                    return new JsonResult<>(500, msg);
                }*/
            }

            File destFile = destFile(file.getOriginalFilename());
            file.transferTo(destFile);
            return new JsonResult<String>().setData(FileUtils.getRelativePath(destFile.getAbsolutePath()));
        } catch (BuzException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入异常", e);
            throw new BuzException("uns.import.error");
        }
    }

    private static final File destFile(String fileName) {
        String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.EXCEL_ROOT, datePath, fileName);
        File outFile = FileUtil.touch(targetPath);
        return outFile;
    }

    public static String getString(Map<String, Object> dataMap, String key, String defaultValue) {
        String finalValue = defaultValue;
        Object value = dataMap.get(key);
        if (value != null) {
            if (value instanceof String) {
                String tempValue = (String) value;
                if (StringUtils.isNotBlank(tempValue)) {
                    finalValue = tempValue;
                }
            }
        }

        return finalValue;
    }

    /**
     * 导出数据
     * @param exportParam
     * @return
     */
    public JsonResult<String> dataExport(ExportParam exportParam, boolean isAsync) {
        AtomicReference<String> language = new AtomicReference<>();
        Locale locale = LocaleContextHolder.getLocale();
        if (locale != null) {
            language.set(locale.getLanguage());
        }
        if (isAsync) {
            globalExportMasterEs.submit(() -> {
                StopWatch stopWatch = new StopWatch();
                try {
                    String path = doExport(language.get(), exportParam, stopWatch);
                    // 新增用户导出记录
                    addExportRecord(exportParam.getUserId(), path);
                } catch (Exception e) {
                    log.error("导出异常", e);
                } finally {
                    log.info("export time:{}", stopWatch.prettyPrint());
                }

            });
            return new JsonResult<String>().setData("导出成功，稍后在已导出中查看");
        } else {
            StopWatch stopWatch = new StopWatch();
            try {
                String path = doExport(language.get(), exportParam, stopWatch);
                return new JsonResult<String>().setData(path);
            } catch (Exception e) {
                log.error("导出异常", e);
            } finally {
                log.info("export time:{}", stopWatch.prettyPrint());
            }
        }

        return null;
    }

    private String doExport(String language, ExportParam exportParam, StopWatch stopWatch) {
        ExcelExportContext context = new ExcelExportContext(language);

        // 1.获取基础数据
        context = fetchDatas(context, exportParam, stopWatch);

        // 2.封装文件(引用文件需二次查询)
        context = fetchReferDatas(context, stopWatch);

        // 2.开始将数据写入excel
        stopWatch.start("write data");
        AtomicReference<String> path = new AtomicReference<>();
        if (StringUtils.equals(exportParam.getFileType(), "excel")) {
            path.set(new ExcelDataExporter(unsManagerService, this).exportData(context));
        } else if (StringUtils.equals(exportParam.getFileType(), "json")) {
            path.set(new ComplexJsonDataExporter().exportData(context));
        }
        stopWatch.stop();
        return path.get();
    }

    private ExcelExportContext fetchDatas(ExcelExportContext context, ExportParam exportParam, StopWatch stopWatch) {
        // 1.获取要导出的数据
        final Set<Long> exportTemplateIds = new HashSet<>();
        Set<Long> fileIds = new HashSet<>();
        stopWatch.start("load folder and file");
        if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
            List<UnsPo> folders = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    .eq(UnsPo::getPathType, 0)
                    //.eq(UnsPo::getStatus, 1)
                    .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType))
                    .orderByAsc(UnsPo::getLayRec));
            folders.forEach(folder -> {
                context.addExportFolder(folder);
            });

            List<UnsPo> files = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                    //.eq(UnsPo::getStatus, 1)
                    .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE, Constants.CALCULATION_REAL_TYPE, Constants.MERGE_TYPE, Constants.CITING_TYPE))
                    .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).orderByAsc(UnsPo::getLayRec));
            files.forEach(file -> {
                context.addExportFile(file);
                fileIds.add(file.getId());
            });
        } else if (CollectionUtils.isNotEmpty(exportParam.getModels()) || CollectionUtils.isNotEmpty(exportParam.getInstances())) {
            // 需要导出的文件夹,将文件夹和下属文件都查出来
            if (CollectionUtils.isNotEmpty(exportParam.getModels())) {
                Set<String> folderIds = exportParam.getModels().stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(folderIds)) {
                    Set<Long> queryFolderIds = new HashSet<>();
                    LambdaQueryWrapper<UnsPo> query = Wrappers.lambdaQuery(UnsPo.class)
                            //.eq(UnsPo::getStatus, 1)
                            .in(UnsPo::getPathType, Lists.newArrayList(0, 2))
                            .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType));
                    query.and(i -> {
                        for (String folderId : folderIds) {
                            i.or().like(UnsPo::getLayRec, folderId);
                        }
                    });
                    query.orderByAsc(UnsPo::getPathType, UnsPo::getLayRec);

                    List<UnsPo> folderAndFiles = unsManagerService.list(query);

                    folderAndFiles.forEach(folderOrFile -> {
                        if (folderOrFile.getPathType() == 0) {
                            context.addExportFolder(folderOrFile);
                            if (folderOrFile.getParentAlias() != null) {
                                String[] pathArr = folderOrFile.getLayRec().split("/");
                                for (int i = 0; i < pathArr.length - 1; i++) {
                                    Long folderId = Long.parseLong(pathArr[i]);
                                    if (!context.containExportFolder(folderId)) {
                                        queryFolderIds.add(folderId);
                                    }
                                }
                            }
                        } else if (folderOrFile.getPathType() == 2) {
                            context.addExportFile(folderOrFile);
                            context.addCheckRefer(folderOrFile.getRefers());
                            fileIds.add(folderOrFile.getId());
                        }
                        if (folderOrFile.getModelId() != null) {
                            exportTemplateIds.add(folderOrFile.getModelId());
                        }
                    });

                    if (CollectionUtils.isNotEmpty(queryFolderIds)) {
                        LambdaQueryWrapper<UnsPo> folderQuery = Wrappers.lambdaQuery(UnsPo.class)
                                //.eq(UnsPo::getStatus, 1)
                                .eq(UnsPo::getPathType, 0)
                                .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType))
                                .in(UnsPo::getId, queryFolderIds);
                        folderQuery.orderByAsc(UnsPo::getLayRec);
                        List<UnsPo> folders = unsManagerService.list(folderQuery);

                        folders.forEach(folder -> {
                            context.addExportFolder(folder);
                            if (folder.getModelId() != null) {
                                exportTemplateIds.add(folder.getModelId());
                            }
                        });
                    }
                }
            }

            // 需要导出的文件
            if (CollectionUtils.isNotEmpty(exportParam.getInstances())) {
                Set<Long> queryFolderIds = new HashSet<>();
                // 1.获取文件
                List<UnsPo> files = new ArrayList<>();
                if (StringUtils.equals(exportParam.getFileFlag(), "alias")) {
                    Set<String> queryFileAliass = exportParam.getInstances().stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
                    files.addAll(unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                            //.eq(UnsPo::getStatus, 1)
                            .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE, Constants.CALCULATION_REAL_TYPE, Constants.MERGE_TYPE, Constants.CITING_TYPE))
                            .in(UnsPo::getAlias, queryFileAliass)
                            .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE)));
                } else if (StringUtils.equals(exportParam.getFileFlag(), "path")) {
                    Set<String> queryFilePaths = exportParam.getInstances().stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
                    files.addAll(unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                            //.eq(UnsPo::getStatus, 1)
                            .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE, Constants.CALCULATION_REAL_TYPE, Constants.MERGE_TYPE, Constants.CITING_TYPE))
                            .in(UnsPo::getPath, queryFilePaths)
                            .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE)));
                } else {
                    Set<Long> queryFileIds = exportParam.getInstances().stream().filter(StringUtils::isNotBlank).map(Long::parseLong).collect(Collectors.toSet());
                    files.addAll(unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                            //.eq(UnsPo::getStatus, 1)
                            .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE, Constants.CALCULATION_REAL_TYPE, Constants.MERGE_TYPE, Constants.CITING_TYPE))
                            .in(UnsPo::getId, queryFileIds)
                            .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE)));
                }

                if (CollectionUtils.isNotEmpty(files)) {
                    files.forEach(file -> {
                        context.addExportFile(file);
                        context.addCheckRefer(file.getRefers());
                        fileIds.add(file.getId());
                        if (file.getModelId() != null) {
                            exportTemplateIds.add(file.getModelId());
                        }

                        // 解析文件夹路径
                        if (file.getParentAlias() != null) {
                            String[] pathArr = file.getLayRec().split("/");
                            for (int i = 0; i < pathArr.length - 1; i++) {
                                queryFolderIds.add(Long.parseLong(pathArr[i]));
                            }
                        }
                    });

                    // 2.获取对应的文件夹
                    if (CollectionUtils.isNotEmpty(queryFolderIds)) {
                        LambdaQueryWrapper<UnsPo> folderQuery = Wrappers.lambdaQuery(UnsPo.class)
                                //.eq(UnsPo::getStatus, 1)
                                .eq(UnsPo::getPathType, 0)
                                .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType))
                                .in(UnsPo::getId, queryFolderIds);
                        folderQuery.orderByAsc(UnsPo::getLayRec);
                        List<UnsPo> folders = unsManagerService.list(folderQuery);

                        folders.forEach(folder -> {
                            context.addExportFolder(folder);
                            if (folder.getModelId() != null) {
                                exportTemplateIds.add(folder.getModelId());
                            }
                        });
                    }
                }
            }
        }
        stopWatch.stop();

        // 查询模板
        stopWatch.start("load template");
        if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
            // 导出所有模板
            List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    //.eq(UnsPo::getStatus, 1)
                    .eq(UnsPo::getPathType, 1).ne(UnsPo::getDataType, 5));
            if (CollectionUtils.isNotEmpty(templates)) {
                context.putAllTemplate(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
            }
        } else if (CollectionUtils.isNotEmpty(exportTemplateIds)) {
            List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                    //.eq(UnsPo::getStatus, 1)
                    .eq(UnsPo::getPathType, 1).ne(UnsPo::getDataType, 5).in(UnsPo::getId, exportTemplateIds));
            if (CollectionUtils.isNotEmpty(templates)) {
                context.putAllTemplate(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
            }
        }
        stopWatch.stop();

        // 查询标签
        stopWatch.start("load label");
        if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
            // 导出所有标签
            List<UnsLabelPo> labels = unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class)/*.eq(UnsLabelPo::getDelFlag, false)*/);
            Map<Long, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getId, Function.identity(), (k1, k2) -> k2));
            context.putAllLabels(labelMap);

            List<UnsLabelRefPo> labelRefPos = unsLabelRefService.list(Wrappers.lambdaQuery(UnsLabelRefPo.class));
            if (CollectionUtils.isNotEmpty(labelRefPos)) {
                Map<Long, List<UnsLabelRefPo>> unsLabelMap = labelRefPos.stream().collect(Collectors.groupingBy(UnsLabelRefPo::getUnsId));
                for (Map.Entry<Long, List<UnsLabelRefPo>> e : unsLabelMap.entrySet()) {
                    for (UnsLabelRefPo labelRefPo : e.getValue()) {
                        UnsLabelPo unsLabelPo = labelMap.get(labelRefPo.getLabelId());
                        if (unsLabelPo != null) {
                            context.computeIfAbsentLabel(e.getKey(), unsLabelPo.getLabelName());
                        }
                    }
                }
            }
        } else if (CollectionUtils.isNotEmpty(fileIds)) {
            List<UnsLabelRefPo> labelRefPos = unsLabelRefService.list(Wrappers.lambdaQuery(UnsLabelRefPo.class).in(UnsLabelRefPo::getUnsId, fileIds));
            if (CollectionUtils.isNotEmpty(labelRefPos)) {
                Set<Long> labelIds = labelRefPos.stream().map(UnsLabelRefPo::getLabelId).collect(Collectors.toSet());
                List<UnsLabelPo> labels = unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getId, labelIds)/*.eq(UnsLabelPo::getDelFlag, false)*/);
                Map<Long, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getId, Function.identity(), (k1, k2) -> k2));
                context.putAllLabels(labelMap);

                Map<Long, List<UnsLabelRefPo>> unsLabelMap = labelRefPos.stream().collect(Collectors.groupingBy(UnsLabelRefPo::getUnsId));
                for (Map.Entry<Long, List<UnsLabelRefPo>> e : unsLabelMap.entrySet()) {
                    for (UnsLabelRefPo labelRefPo : e.getValue()) {
                        UnsLabelPo unsLabelPo = labelMap.get(labelRefPo.getLabelId());
                        if (unsLabelPo != null) {
                            context.computeIfAbsentLabel(e.getKey(), unsLabelPo.getLabelName());
                        }
                    }
                }
            }
        }
        stopWatch.stop();

        return context;
    }

    private ExcelExportContext fetchReferDatas(ExcelExportContext context, StopWatch stopWatch) {

        if (CollectionUtils.isNotEmpty(context.getCheckReferIds())) {
            List<String> checkReferIds = context.getCheckReferIds().stream().filter(id -> !context.getFileIdToAliasMap().containsKey(id)).map(id -> String.valueOf(id)).collect(Collectors.toList());
            ExportParam exportParam = new ExportParam();
            exportParam.setInstances(checkReferIds);
            exportParam.setFileFlag("id");
            fetchDatas(context, exportParam, stopWatch);
        }

        if (CollectionUtils.isNotEmpty(context.getCheckReferAliass())) {
            List<String> checkReferAliass = context.getCheckReferAliass().stream().filter(alias -> !context.getExportFileMap().containsKey(alias)).collect(Collectors.toList());
            ExportParam exportParam = new ExportParam();
            exportParam.setInstances(checkReferAliass);
            exportParam.setFileFlag("alias");
            fetchDatas(context, exportParam, stopWatch);
        }

        if (CollectionUtils.isNotEmpty(context.getCheckReferPaths())) {
            List<String> checkReferPaths = context.getCheckReferPaths().stream().filter(path -> !context.getFilePathToAliasMap().containsKey(path)).collect(Collectors.toList());
            ExportParam exportParam = new ExportParam();
            exportParam.setInstances(checkReferPaths);
            exportParam.setFileFlag("path");
            fetchDatas(context, exportParam, stopWatch);
        }

        return context;
    }

    public void writeExplanationRow(ExcelWriter excelWriter) {
        WriteSheet writeSheet = EasyExcel.writerSheet().relativeHeadRowIndex(0).sheetNo(ExcelTypeEnum.Explanation.getIndex()).build();

        List<Map<Integer, Object>> dataList = Lists.newArrayList();
        for (String explanation : ExportImportHelper.EXPLANATION) {
            Map<Integer, Object> dataMap = new HashMap<>(1);
            dataMap.put(0, I18nUtils.getMessage(explanation));
            dataList.add(dataMap);
        }

        excelWriter.write(dataList, writeSheet);
    }

    private void addExportRecord(String userId, String path) {
        UnsExportRecordPo unsExportRecordPo = new UnsExportRecordPo();
        unsExportRecordPo.setId(SuposIdUtil.nextId());
        unsExportRecordPo.setUserId(userId);
        unsExportRecordPo.setFilePath(path);
        unsExportRecordPo.setConfirm(false);
        Date now = new Date();
        unsExportRecordPo.setCreateTime(now);
        unsExportRecordPo.setUpdateTime(now);
        unsExportRecordMapper.insert(unsExportRecordPo);
    }

    public JsonResult<List<UnsExportRecordVo>> getExportRecords(String userId, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<UnsExportRecordPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UnsExportRecordPo::getUserId, userId);
        queryWrapper.orderByDesc(UnsExportRecordPo::getCreateTime);
        Page<UnsExportRecordPo> page = new Page<>(pageNo, pageSize, true);
        Page<UnsExportRecordPo> unsExportRecordPoPage = unsExportRecordMapper.selectPage(page, queryWrapper);
        List<UnsExportRecordPo> unsExportRecordPos = unsExportRecordPoPage.getRecords();
        List<UnsExportRecordVo> dataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(unsExportRecordPos)) {
            for (UnsExportRecordPo unsExportRecordPo : unsExportRecordPos) {
                UnsExportRecordVo exportRecordVo = new UnsExportRecordVo();
                exportRecordVo.setId(String.valueOf(unsExportRecordPo.getId()));
                exportRecordVo.setExportTime(unsExportRecordPo.getCreateTime().getTime());
                exportRecordVo.setFilePath(unsExportRecordPo.getFilePath());
                if (StringUtils.isNotBlank(unsExportRecordPo.getFilePath())) {
                    exportRecordVo.setFileName(unsExportRecordPo.getFilePath().substring(unsExportRecordPo.getFilePath().lastIndexOf("/") + 1));
                }
                exportRecordVo.setConfirm(unsExportRecordPo.getConfirm());
                dataList.add(exportRecordVo);
            }
        }
        JsonResult<List<UnsExportRecordVo>> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        jsonResult.setData(dataList);
        return jsonResult;
    }

    public JsonResult<String> exportRecordConfirm(UnsExportRecordConfirmReq req) {
        if (CollUtil.isNotEmpty(req.getIds())) {
            List<UnsExportRecordPo> unsExportRecordPos = unsExportRecordMapper.selectByIds(req.getIds());
            if (CollUtil.isNotEmpty(unsExportRecordPos)) {
                Date now = new Date();
                for (UnsExportRecordPo unsExportRecordPo : unsExportRecordPos) {
                    unsExportRecordPo.setConfirm(true);
                    unsExportRecordPo.setUpdateTime(now);
                }
                unsExportRecordMapper.updateById(unsExportRecordPos);
            }
        }
        JsonResult<String> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        return jsonResult;
    }
}
