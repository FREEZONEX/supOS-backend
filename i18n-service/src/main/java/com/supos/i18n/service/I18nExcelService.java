package com.supos.i18n.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.Constants;
import com.supos.common.LogWrapperConsumer;
import com.supos.common.RunningStatus;
import com.supos.common.dto.JsonResult;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.FileUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.i18n.dao.mapper.I18nExportRecordMapper;
import com.supos.i18n.dao.po.I18nExportRecordPo;
import com.supos.i18n.dao.po.I18nLanguagePO;
import com.supos.i18n.dao.po.I18nResourceModulePO;
import com.supos.i18n.dao.po.I18nResourcePO;
import com.supos.i18n.dto.I18nExportParam;
import com.supos.i18n.service.excel.ExcelImportContext;
import com.supos.i18n.service.excel.ExcelWriteHandler;
import com.supos.i18n.service.excel.ImportExportHelper;
import com.supos.i18n.service.excel.entity.LanguageData;
import com.supos.i18n.service.excel.entity.ModuleData;
import com.supos.i18n.service.excel.entity.ResourceData;
import com.supos.i18n.dto.I18nExportRecordConfirmReq;
import com.supos.i18n.vo.I18nExportRecordVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 导入导出语言包服务
 * @date 2025/9/3 9:39
 */
@Slf4j
@Service
public class I18nExcelService {

    @Autowired
    private I18nLanguageService i18nLanguageService;

    @Autowired
    private I18nResourceModuleService i18nResourceModuleService;

    @Autowired
    private I18nResourceService i18nResourceService;

    @Autowired
    private I18nManagerService i18nManagerService;

    @Autowired
    private I18nExportRecordMapper i18nExportRecordMapper;

    public static final ExecutorService globalExportMasterEs = new ThreadPoolExecutor(1, 1,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), new ThreadFactory() {
        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "i18n export or import task thread: " + integer.getAndIncrement());
        }
    }, new ThreadPoolExecutor.CallerRunsPolicy());

    public void doDownloadExcelTemplate(HttpServletResponse response) {
        String targetPath = null;
        InputStream targetInputStream = null;
        try {

            // 生成临时文件
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.I18N_EXCEL_OUT_PATH);
            targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);

            ExcelWriter excelWriter = EasyExcel.write(targetPath).registerWriteHandler(new ExcelWriteHandler(new HashMap<>())).build();

            //创建说明页
            ImportExportHelper.writeExplanation(excelWriter, null);

            ImportExportHelper.writeLanguage(excelWriter, null, null);
            ImportExportHelper.writeModule(excelWriter, null, null);
            ImportExportHelper.writeResource(excelWriter, null, null);

            excelWriter.finish();

            targetInputStream = FileUtil.getInputStream(targetPath);

            FileUtils.downloadFile(response, path.split("/")[path.split("/").length - 1], targetInputStream);
        } catch (Exception e) {
            log.error("downloadByPath Exception", e);
        } finally {
            if (targetInputStream != null) {
                IoUtil.close(targetInputStream);
            }
            if (targetPath != null) {
                FileUtil.del(targetPath);
            }
        }
    }

    /********************************************************导入**********************************************************/

    /**
     * 预导入检测
     * @param file
     * @return
     */
    public JsonResult<String> preImport(MultipartFile file) {

        String extName = FileUtil.extName(file.getOriginalFilename());
        if (!"xlsx".equals(extName)) {
            throw new BuzException("i18n.import.not.xlsx");
        }

        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            int sheetCount = reader.getSheetCount();
            if (sheetCount < ImportExportHelper.getSheet().size()) {
                throw new BuzException("i18n.import.template.error");
            }

            // 校验sheetname和表头
            ImportExportHelper.checkExplanationSheet(reader);
            ImportExportHelper.checkLanguageSheet(reader);
            ImportExportHelper.checkModule(reader);
            ImportExportHelper.checkResource(reader);

            Pair<String, File> destFile = destFile(file.getOriginalFilename());
            FileUtil.copyFile(file.getInputStream(), destFile.getRight(), StandardCopyOption.REPLACE_EXISTING);
            return new JsonResult<String>().setData(FileUtils.getRelativePath(destFile.getLeft()));
        } catch (BuzException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入异常", e);
            throw new BuzException("uns.import.error");
        }
    }

    private static final Pair<String, File> destFile(String fileName) {
        String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.EXCEL_ROOT, datePath, fileName);
        File outFile = FileUtil.touch(targetPath);
        return Pair.of(targetPath, outFile);
    }

    public void asyncImport(File file, Consumer<RunningStatus> consumer, boolean isAsync) {
        if (isAsync) {
            String mainLanguage = LocaleContextHolder.getLocale().getLanguage();
            globalExportMasterEs.execute(() -> {
                LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(new Locale(mainLanguage)),  true);
                doImport(file, consumer);
            });
            consumer.accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.start"))
                    .setFinished(false)
                    .setProgress(5.0));
        } else {
            consumer.accept(new RunningStatus()
                    .setTask(I18nUtils.getMessage("uns.create.task.name.start"))
                    .setFinished(false)
                    .setProgress(5.0));
            doImport(file, consumer);

        }
    }

    private void doImport(File file, Consumer<RunningStatus> consumer) {
        if (!file.exists()) {
            String message = I18nUtils.getMessage("uns.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }

        ExcelDataImporter importer = null;
        ExcelImportContext importContext = new ExcelImportContext(consumer);
        try {

            importer = new ExcelDataImporter(importContext, i18nManagerService);
            importer.importData(file);
        } catch (Throwable ex) {
            log.error("i18n importErr:{}", file.getPath(), ex);
            importFinish(importer, importContext, file.getPath(), consumer, file, ex);
            return;
        }
        importFinish(importer, importContext, file.getPath(), consumer, file, null);
    }

    private void importFinish(ExcelDataImporter dataImporter, ExcelImportContext importContext, String excelFilePath, Consumer<RunningStatus> consumer, File file, Throwable ex) {
        try {
            LogWrapperConsumer wrapperConsumer = new LogWrapperConsumer(consumer);
            String finalTask = I18nUtils.getMessage("uns.create.task.name.final");

            if (ex != null) {
                String lastTask = wrapperConsumer.getLastTask();
                Double lastProgress = wrapperConsumer.getLastProgress();
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

            if (!importContext.hasError()) {
                String message = I18nUtils.getMessage("uns.import.rs.ok");
                consumer.accept(new RunningStatus(200, message)
                        .setTask(finalTask)
                        .setProgress(100.0));
                return;
            }

            Pair<String,File> outFile = destFile("err_" + file.getName().replace(' ', '-'));
            log.info("create error file:{}", outFile.toString());
            dataImporter.writeError(file, outFile.getRight());

            String message = I18nUtils.getMessage("uns.import.rs.hasErr");
            consumer.accept(new RunningStatus(206, message, FileUtils.getRelativePath(outFile.getLeft()))
                    .setTask(finalTask)
                    .setProgress(100.0));
        } catch (Throwable e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }

    }

    /********************************************************导出**********************************************************/

    /**
     * 导出数据
     * @param i18nExportParam
     * @return
     */
    public JsonResult<String> dataExport(I18nExportParam i18nExportParam) {
        I18nLanguagePO i18nLanguagePO = i18nLanguageService.getByCode(i18nExportParam.getLanguageCode());
        if (i18nLanguagePO == null) {
            throw new BuzException("i18n.exception.language_has_no_error");
        }

        String mainLanguage = i18nLanguagePO.getLanguageCode();
        globalExportMasterEs.submit(() -> {
            LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(new Locale(mainLanguage)),  true);
            StopWatch stopWatch = new StopWatch();
            try {
                String fileName = String.format("i18n_%s.xlsx", i18nExportParam.getLanguageCode());
                String path = String.format("%s%s/%s", Constants.EXCEL_ROOT, DateTime.now().toString("yyyyMMddHHmmss"), fileName);
                String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);

                FileUtil.touch(targetPath);
                ExcelWriter excelWriter = EasyExcel.write(targetPath).registerWriteHandler(new ExcelWriteHandler()).build();

                // 生成说明页
                ImportExportHelper.writeExplanation(excelWriter, stopWatch);

                // 生成语言页
                LanguageData languageData = new LanguageData();
                languageData.setCode(i18nLanguagePO.getLanguageCode());
                languageData.setName(i18nLanguagePO.getLanguageName());
                ImportExportHelper.writeLanguage(excelWriter, languageData, stopWatch);

                // 生成module页
                List<I18nResourceModulePO> modules = i18nResourceModuleService.getAllModule();
                List<ModuleData> moduleDataList = new ArrayList<>(modules.size());
                for (I18nResourceModulePO module : modules) {
                    ModuleData moduleData = new ModuleData();
                    moduleData.setModuleCode(module.getModuleCode());
                    moduleData.setModuleName(module.getModuleName());
                    moduleDataList.add(moduleData);
                }
                ImportExportHelper.writeModule(excelWriter, moduleDataList, stopWatch);

                // 生成资源页
                List<I18nResourcePO> allResources = i18nResourceService.getAllResource(i18nLanguagePO.getLanguageCode());
                Map<String, List<I18nResourcePO>> moduleResMaps = allResources.stream().collect(Collectors.groupingBy(I18nResourcePO::getModuleCode));
                List<ResourceData> resourceDataList = new ArrayList<>(modules.size());
                for (List<I18nResourcePO> resources : moduleResMaps.values()) {
                    if (CollectionUtil.isNotEmpty(resources)) {
                        for (I18nResourcePO resource : resources) {
                            ResourceData resourceData = new ResourceData();
                            resourceData.setModuleCode(resource.getModuleCode());
                            resourceData.setKey(resource.getI18nKey());
                            resourceData.setValue(resource.getI18nValue());
                            resourceDataList.add(resourceData);
                        }
                    }
                }
                ImportExportHelper.writeResource(excelWriter, resourceDataList, stopWatch);

                excelWriter.finish();

                // 新增用户导出记录
                addExportRecord(i18nExportParam.getUserId(), path);
            } catch (Exception e) {
                log.error("导出异常", e);
            } finally {
                log.info("export time:{}", stopWatch.prettyPrint());
            }

        });
        return new JsonResult<String>().setData("导出成功，稍后在已导出中查看");
    }

    private void addExportRecord(String userId, String path) {
        I18nExportRecordPo i18nExportRecordPo = new I18nExportRecordPo();
        i18nExportRecordPo.setUserId(userId);
        i18nExportRecordPo.setFilePath(path);
        i18nExportRecordPo.setConfirm(false);
        Date now = new Date();
        i18nExportRecordPo.setCreateTime(now);
        i18nExportRecordPo.setUpdateTime(now);
        i18nExportRecordMapper.insert(i18nExportRecordPo);
    }

    public JsonResult<List<I18nExportRecordVO>> getExportRecords(String userId, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<I18nExportRecordPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(I18nExportRecordPo::getUserId, userId);
        queryWrapper.orderByDesc(I18nExportRecordPo::getCreateTime);
        Page<I18nExportRecordPo> page = new Page<>(pageNo, pageSize, true);
        Page<I18nExportRecordPo> i18nExportRecordPoPage = i18nExportRecordMapper.selectPage(page, queryWrapper);
        List<I18nExportRecordPo> i18nExportRecordPos = i18nExportRecordPoPage.getRecords();
        List<I18nExportRecordVO> dataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(i18nExportRecordPos)) {
            for (I18nExportRecordPo i18nExportRecordPo : i18nExportRecordPos) {
                I18nExportRecordVO exportRecordVo = new I18nExportRecordVO();
                exportRecordVo.setId(String.valueOf(i18nExportRecordPo.getId()));
                exportRecordVo.setExportTime(i18nExportRecordPo.getCreateTime().getTime());
                exportRecordVo.setFilePath(i18nExportRecordPo.getFilePath());
                if (StringUtils.isNotBlank(i18nExportRecordPo.getFilePath())) {
                    exportRecordVo.setFileName(i18nExportRecordPo.getFilePath().substring(i18nExportRecordPo.getFilePath().lastIndexOf("/") + 1));
                }
                exportRecordVo.setConfirm(i18nExportRecordPo.getConfirm());
                dataList.add(exportRecordVo);
            }
        }
        JsonResult<List<I18nExportRecordVO>> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        jsonResult.setData(dataList);
        return jsonResult;
    }

    public JsonResult<String> exportRecordConfirm(I18nExportRecordConfirmReq req) {
        if (CollUtil.isNotEmpty(req.getIds())) {
            List<I18nExportRecordPo> i18nExportRecordPos = i18nExportRecordMapper.selectByIds(req.getIds());
            if (CollUtil.isNotEmpty(i18nExportRecordPos)) {
                Date now = new Date();
                for (I18nExportRecordPo i18nExportRecordPo : i18nExportRecordPos) {
                    i18nExportRecordPo.setConfirm(true);
                    i18nExportRecordPo.setUpdateTime(now);
                }
                i18nExportRecordMapper.updateById(i18nExportRecordPos);
            }
        }
        JsonResult<String> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        return jsonResult;
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
            FileUtils.downloadFile(response, fileName, new FileInputStream(file));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BuzException(e.getMessage());
        }
    }
}
