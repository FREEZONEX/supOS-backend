package com.supos.uns.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.JsonResult;
import com.supos.common.enums.GlobalExportModuleEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.*;
import com.supos.uns.bo.GlobalExportConfig;
import com.supos.uns.bo.GlobalRunningStatus;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.dao.mapper.GlobalExportRecordMapper;
import com.supos.uns.dao.po.GlobalExportRecordPo;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.ExportRecordConfirmReq;
import com.supos.uns.vo.ExportRecordVo;
import com.supos.uns.vo.GlobalExportParam;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月19日 11:04
 */
@Slf4j
@Service
public class GlobalExportService {
    @Autowired
    private SystemConfig systemConfig;
    @Resource
    private UnsExcelService unsExcelService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private EventFlowService eventFlowService;
    @Autowired
    private SourceFlowService sourceFlowService;
    @Autowired
    private GlobalExportRecordMapper globalExportRecordMapper;
    @Autowired
    private GlobalExportRecordService globalExportRecordService;

    public static final ExecutorService asyncForGlobalImport = new ThreadPoolExecutor(4, 8,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), new ThreadFactory() {
        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "asyncForGlobalImport task thread: " + integer.getAndIncrement());
        }
    }, new ThreadPoolExecutor.CallerRunsPolicy());
    public static final ExecutorService globalExportMasterEs = new ThreadPoolExecutor(1, 1,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), new ThreadFactory() {
        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "globalExportMasterEs task thread: " + integer.getAndIncrement());
        }
    }, new ThreadPoolExecutor.CallerRunsPolicy());
    public static final ExecutorService globalExportSlaveEs = new ThreadPoolExecutor(4, 8,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000), new ThreadFactory() {
        private final AtomicInteger integer = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "globalExportSlaveEs task thread: " + integer.getAndIncrement());
        }
    }, new ThreadPoolExecutor.CallerRunsPolicy());

    public JsonResult<String> dataImport(MultipartFile file) {
        String extName = FileUtil.extName(file.getOriginalFilename());
        // 校验文件格式
        if (!"zip".equals(extName)) {
            throw new BuzException("global.import.need.zip");
        }
        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT, datePath, file.getOriginalFilename());
            File destFile = FileUtil.touch(targetPath);
            FileUtil.writeBytes(file.getBytes(), destFile);
            return new JsonResult<String>().setData(FileUtils.getRelativePath(targetPath));
        } catch (BuzException e) {
            throw e;
        } catch (Exception e) {
            log.error("全局导入异常", e);
            throw new BuzException("global.import.error");
        }
    }

    private void appendZipEntry(ZipOutputStream zipOutputStream, File file, String zipEntryName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
            }
            zipOutputStream.closeEntry();
        }
    }

    private String zipFiles(String zipFileName, File exportYml, List<String> filePaths, List<String> zipEntryNameList) {
        try {
            // 创建一个临时文件来存储 ZIP
            Path tempZipFilePath = Files.createTempFile(zipFileName, ".zip");
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFilePath))) {
                appendZipEntry(zipOutputStream, exportYml, "export.yml");
                for (int i = 0; i < filePaths.size(); i++) {
                    String filePath = FileUtils.getFileRootPath() + filePaths.get(i);
                    // 读取对象内容
                    File file = new File(filePath);
                    appendZipEntry(zipOutputStream, file, zipEntryNameList.get(i) + StrPool.SLASH + file.getName());
                    // 删除各个模块的导出文件
                    FileUtil.del(file);
                }
            }
            String destFilePath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_EXPORT, DateUtil.format(new Date(), "yyyyMMddHHmmss"), zipFileName + ".zip");
            // 保存 ZIP 文件
            FileUtil.move(tempZipFilePath.toFile(), new File(destFilePath), true);
            log.error("global export zipFileName :" + zipFileName);
            return destFilePath;
        } catch (Exception e) {
            log.error("global export zipFiles error", e);
            return null;
        }
    }

    private String zipErrorFiles(String zipFileName, List<String> filePaths, List<String> zipEntryNameList) {
        try {
            // 创建一个临时文件来存储 ZIP
            Path tempZipFilePath = Files.createTempFile(zipFileName, ".zip");
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFilePath))) {
                for (int i = 0; i < filePaths.size(); i++) {
                    String filePath = filePaths.get(i);
                    if (filePath.startsWith("/")) {
                        filePath = FileUtils.getFileRootPath() + filePaths.get(i);
                    }
                    // 读取对象内容
                    File file = new File(filePath);
                    appendZipEntry(zipOutputStream, file, zipEntryNameList.get(i) + StrPool.SLASH + file.getName());
                }
            }
            String destFilePath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT_ERROR, DateUtil.format(new Date(), "yyyyMMddHHmmss"), zipFileName + ".zip");
            // 保存 ZIP 文件
            FileUtil.move(tempZipFilePath.toFile(), new File(destFilePath), true);
            log.error("global export error zipFileName :" + zipFileName);
            return FileUtils.getRelativePath(destFilePath);
        } catch (Exception e) {
            log.error("global export error zipFiles error", e);
            return null;
        }
    }

    public JsonResult<String> dataExport(GlobalExportParam globalExportParam) {
        File exportYml;
        try {
            exportYml = MyYamlUtils.writeYamlFile(globalExportParam.getName(), new GlobalExportConfig(globalExportParam.getName(), systemConfig.getVersion(), ""));
        } catch (IOException e) {
            log.error("全局导出生成export.yml失败", e);
            return new JsonResult<>(500, "全局导出生成export.yml失败");
        }
        String sub = UserContext.get().getSub();
        globalExportMasterEs.submit(() -> {
            List<Future<JsonResult<String>>> futures = new ArrayList<>();
            Map<Future<JsonResult<String>>, GlobalExportModuleEnum> moduleEnumMap = new HashMap<>();
            if (globalExportParam.getUnsExportParam() != null) {
                Future<JsonResult<String>> future = globalExportSlaveEs.submit(() -> unsExcelService.dataExport(globalExportParam.getUnsExportParam(), false));
                futures.add(future);
                moduleEnumMap.put(future, GlobalExportModuleEnum.UNS);
            }
            if (globalExportParam.getSourceFlowExportParam() != null) {
                Future<JsonResult<String>> future = globalExportSlaveEs.submit(() -> sourceFlowService.dataExport(globalExportParam.getSourceFlowExportParam()));
                futures.add(future);
                moduleEnumMap.put(future, GlobalExportModuleEnum.SOURCE_FLOW);
            }
            if (globalExportParam.getEventFlowExportParam() != null) {
                Future<JsonResult<String>> future = globalExportSlaveEs.submit(() -> eventFlowService.dataExport(globalExportParam.getEventFlowExportParam()));
                futures.add(future);
                moduleEnumMap.put(future, GlobalExportModuleEnum.EVENT_FLOW);
            }
            if (globalExportParam.getDashboardExportParam() != null) {
                Future<JsonResult<String>> future = globalExportSlaveEs.submit(() -> dashboardService.dataExport(globalExportParam.getDashboardExportParam()));
                futures.add(future);
                moduleEnumMap.put(future, GlobalExportModuleEnum.DASHBOARD);
            }
            List<String> filePaths = new ArrayList<>();
            List<String> zipEntryNameList = new ArrayList<>();
            for (Future<JsonResult<String>> future : futures) {
                try {
                    filePaths.add(future.get().getData());
                    zipEntryNameList.add(moduleEnumMap.get(future).getCode());
                } catch (Exception e) {
                    log.error("global import await error", e);
                }
            }
            // 将各个模块的导出文件压缩成zip
            String zipFileName =globalExportParam.getName() +"_"+ DateUtil.format(new Date(),"yyMMdd");

            String zipFilePath = zipFiles(zipFileName, exportYml, filePaths, zipEntryNameList);
            // 新增用户导出记录
            addExportRecord(sub,FileUtils.getRelativePath(zipFilePath));
        });
        return new JsonResult<String>().setData("导出成功，稍后在已导出中查看");
    }

    private void addExportRecord(String userId,String zipFilePath) {
        GlobalExportRecordPo globalExportRecordPo = new GlobalExportRecordPo();
        globalExportRecordPo.setId(SuposIdUtil.nextId());
        globalExportRecordPo.setFilePath(zipFilePath);
        globalExportRecordPo.setUserId(userId);
        globalExportRecordPo.setConfirm(false);
        Date now = new Date();
        globalExportRecordPo.setCreateTime(now);
        globalExportRecordPo.setUpdateTime(now);
        globalExportRecordService.save(globalExportRecordPo);
    }

    public void asyncImport(WebSocketSession session, File zipFile, Consumer<RunningStatus> consumer, boolean isAsync) {
        if (!zipFile.exists()) {
            String message = I18nUtils.getMessage("global.import.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }
        // 判断版本 根据当前环境版本做匹配，不支持跨版本的导入
        GlobalExportConfig globalExportConfig;
        try {
            ZipFile zip = new ZipFile(zipFile);
            FileHeader fileHeader = zip.getFileHeader(Constants.GLOBAL_EXPORT_YAML);
            try (InputStream io = zip.getInputStream(fileHeader)) {
                globalExportConfig = MyYamlUtils.loadYaml(io, GlobalExportConfig.class);
            }
            assert globalExportConfig != null;
        } catch (Exception ignore) {
            String message = I18nUtils.getMessage("global.import.error.format");
            consumer.accept(new RunningStatus(400, message));
            return;
        }
        if (!Objects.equals(globalExportConfig.getSupOsVersion(), systemConfig.getVersion())) {
            String message = I18nUtils.getMessage("global.import.error.version");
            consumer.accept(new RunningStatus(400, message));
            return;
        }
        // 解压zip
        File fileFolder = ZipUtil.unzip(zipFile);
        File[] files = fileFolder.listFiles();
        List<Future<?>> futures = new ArrayList<>();
        assert files != null;
        GlobalRunningStatus globalRunningStatus = new GlobalRunningStatus(200, I18nUtils.getMessage("sourceFlow.import.rs.ok"));
        for (File file : files) {
            if (GlobalExportModuleEnum.UNS.is(file.getName())) {
                for (File unsFile : Objects.requireNonNull(file.listFiles())) {
                    // 将uns文件移动到uns导入目录
                    String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.EXCEL_ROOT, DateUtil.format(new Date(), "yyyyMMddHHmmss"), unsFile.getName());
                    File unsExcelFile = new File(targetPath);
                    FileUtil.move(unsFile, unsExcelFile, true);
                    // 异步并行处理
                    futures.add(asyncForGlobalImport.submit(() -> {
                        unsExcelService.asyncImport(unsExcelFile, runningStatus -> {
                            runningStatus.setModule(GlobalExportModuleEnum.UNS.getCode());
                            globalRunningStatus.getRunningStatusList().add(runningStatus);
                        }, false, null);
                    }));
                }
            }
            if (GlobalExportModuleEnum.SOURCE_FLOW.is(file.getName())) {
                for (File sourceFlowFile : Objects.requireNonNull(file.listFiles())) {
                    // 将文件移动到导入目录
                    String targetPath = String.format("%s%s%s/%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT, GlobalExportModuleEnum.SOURCE_FLOW.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), sourceFlowFile.getName());
                    File targetFile = new File(targetPath);
                    FileUtil.move(sourceFlowFile, targetFile, true);
                    // 异步并行处理
                    futures.add(asyncForGlobalImport.submit(() -> sourceFlowService.asyncImport(targetFile, runningStatus -> {
                        runningStatus.setModule(GlobalExportModuleEnum.SOURCE_FLOW.getCode());
                        globalRunningStatus.getRunningStatusList().add(runningStatus);
                    })));
                }
            }
            if (GlobalExportModuleEnum.EVENT_FLOW.is(file.getName())) {
                for (File eventFlowFile : Objects.requireNonNull(file.listFiles())) {
                    // 将文件移动到导入目录
                    String targetPath = String.format("%s%s%s/%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT, GlobalExportModuleEnum.EVENT_FLOW.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), eventFlowFile.getName());
                    File targetFile = new File(targetPath);
                    FileUtil.move(eventFlowFile, targetFile, true);
                    // 异步并行处理
                    futures.add(asyncForGlobalImport.submit(() -> eventFlowService.asyncImport(targetFile, runningStatus -> {
                        runningStatus.setModule(GlobalExportModuleEnum.EVENT_FLOW.getCode());
                        globalRunningStatus.getRunningStatusList().add(runningStatus);
                    })));
                }
            }
            if (GlobalExportModuleEnum.DASHBOARD.is(file.getName())) {
                for (File dashboardFile : Objects.requireNonNull(file.listFiles())) {
                    // 将文件移动到导入目录
                    String targetPath = String.format("%s%s%s/%s/%s", FileUtils.getFileRootPath(), Constants.GLOBAL_IMPORT, GlobalExportModuleEnum.DASHBOARD.getCode(), DateUtil.format(new Date(), "yyyyMMddHHmmss"), dashboardFile.getName());
                    File targetFile = new File(targetPath);
                    FileUtil.move(dashboardFile, targetFile, true);
                    // 异步并行处理
                    futures.add(asyncForGlobalImport.submit(() -> dashboardService.asyncImport(targetFile, runningStatus -> {
                        runningStatus.setModule(GlobalExportModuleEnum.DASHBOARD.getCode());
                        globalRunningStatus.getRunningStatusList().add(runningStatus);
                    })));
                }
            }
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("global import await error", e);
            }
        }
        List<String> errFilePathList = new ArrayList<>();
        List<String> zipEntryNameList = new ArrayList<>();
        int totalCount = 0;
        int errorCount = 0;
        for (RunningStatus runningStatus : globalRunningStatus.getRunningStatusList()) {
            if (StringUtils.hasText(runningStatus.getErrTipFile())) {
                errFilePathList.add(runningStatus.getErrTipFile());
                globalRunningStatus.setCode(runningStatus.getCode());
                zipEntryNameList.add(runningStatus.getModule());
            }
            if(Objects.equals(206,runningStatus.getCode())){
                globalRunningStatus.setMsg(runningStatus.getMsg());
            }
            if(Objects.equals(globalRunningStatus.getCode(),200)  && Objects.equals(0,runningStatus.getTotalCount()) && Objects.equals(0,runningStatus.getSuccessCount())){
               runningStatus.setMsg(I18nUtils.getMessage("global.import.rs.notData"));
            }else{
                totalCount += runningStatus.getTotalCount();
                errorCount += runningStatus.getErrorCount();
            }
        }
        if(Objects.equals(totalCount,errorCount)){
            globalRunningStatus.setMsg(I18nUtils.getMessage("global.import.rs.allErr"));
        }else if(Objects.equals(globalRunningStatus.getCode(),206)){
            globalRunningStatus.setMsg(I18nUtils.getMessage("sourceFlow.import.rs.hasErr"));
        }
        if (CollUtil.isNotEmpty(errFilePathList)) {
            // 压缩zip
            String errorFilePath = zipErrorFiles("global-import-error", errFilePathList, zipEntryNameList);
            globalRunningStatus.setErrTipFile(errorFilePath);
        }
        String json = null;
        try {
            json = JsonUtil.toJson(globalRunningStatus);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("global import fail to send uploadStatus: " + json, e);
        }
    }

    public JsonResult<List<ExportRecordVo>> getExportRecords(Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<GlobalExportRecordPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlobalExportRecordPo::getUserId, UserContext.get().getSub());
        queryWrapper.orderByDesc(GlobalExportRecordPo::getCreateTime);
        Page<GlobalExportRecordPo> page = new Page<>(pageNo, pageSize, true);
        Page<GlobalExportRecordPo> globalExportRecordPoPage = globalExportRecordMapper.selectPage(page, queryWrapper);
        List<GlobalExportRecordPo> globalExportRecordPos = globalExportRecordPoPage.getRecords();
        List<ExportRecordVo> dataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(globalExportRecordPos)) {
            for (GlobalExportRecordPo globalExportRecordPo : globalExportRecordPos) {
                ExportRecordVo exportRecordVo = new ExportRecordVo();
                exportRecordVo.setId(String.valueOf(globalExportRecordPo.getId()));
                exportRecordVo.setExportTime(globalExportRecordPo.getCreateTime().getTime());
                exportRecordVo.setFilePath(globalExportRecordPo.getFilePath());
                if (StringUtils.hasText(globalExportRecordPo.getFilePath())) {
                    exportRecordVo.setFileName(globalExportRecordPo.getFilePath().substring(globalExportRecordPo.getFilePath().lastIndexOf("/") + 1));
                }
                exportRecordVo.setConfirm(globalExportRecordPo.getConfirm());
                dataList.add(exportRecordVo);
            }
        }
        JsonResult<List<ExportRecordVo>> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        jsonResult.setData(dataList);
        return jsonResult;
    }

    public JsonResult<String> exportRecordConfirm(ExportRecordConfirmReq req) {
        if (CollUtil.isNotEmpty(req.getIds())) {
            List<GlobalExportRecordPo> globalExportRecordPos = globalExportRecordMapper.selectByIds(req.getIds());
            if (CollUtil.isNotEmpty(globalExportRecordPos)) {
                Date now = new Date();
                for (GlobalExportRecordPo globalExportRecordPo : globalExportRecordPos) {
                    globalExportRecordPo.setConfirm(true);
                    globalExportRecordPo.setUpdateTime(now);
                }
                globalExportRecordMapper.updateById(globalExportRecordPos);
            }
        }
        JsonResult<String> jsonResult = new JsonResult<>();
        jsonResult.setCode(0);
        jsonResult.setMsg("ok");
        return jsonResult;
    }

    public void fileDownload(HttpServletResponse response, String path) {
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(FileUtils.getFileRootPath(), path);
            if (!file.exists()) {
                throw new BuzException("global.file.not.exist");
            }
            String fileName = file.getName();
            FileUtils.downloadFile(response, fileName, new FileInputStream(file));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BuzException(e.getMessage());
        }
    }
}
