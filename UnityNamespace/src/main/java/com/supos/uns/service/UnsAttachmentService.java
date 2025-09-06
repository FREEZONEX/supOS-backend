package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.adpter.minio.MinioAdpterService;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.mqtt.TopicDefinition;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.SuposIdUtil;
import com.supos.uns.bo.UnsAttachmentBo;
import com.supos.uns.dao.mapper.UnsAttachmentMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsAttachmentPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.FileUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.supos.common.Constants.UNS_FLAG_WITH_ATTACHMENT;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 14:55
 */
@Slf4j
@Service
public class UnsAttachmentService extends ServiceImpl<UnsAttachmentMapper, UnsAttachmentPo> {

    @Resource
    private MinioAdpterService minioAdpterService;

    @Resource
    private UnsAttachmentMapper unsAttachmentMapper;

    @Resource
    private SystemConfig systemConfig;
    @Resource
    UnsMapper unsMapper;
    @Resource
    IUnsDefinitionService unsDefinitionService;

    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public ResultVO<List<UnsAttachmentBo>> upload(String alias, MultipartFile[] files) {
        UnsPo unsPo = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getAlias, alias).select(UnsPo::getId, UnsPo::getWithFlags));
        if (unsPo == null) {
            log.warn("upload: 找不到 UNS: {}", alias);
            return ResultVO.fail(I18nUtils.getMessage("uns.folder.or.file.not.found"));
        }
        List<UnsAttachmentBo> boList = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            if (file.getSize() > Constants.ATTACHMENT_MAX_SIZE) {
                throw new BuzException("uns.attachment.max.size");
            }
            String originalFilename = file.getOriginalFilename();
            String extensionName = FileUtil.extName(originalFilename);
            String attachmentName = UUID.randomUUID().toString().replaceAll("-", "");
            if (StringUtils.isNotBlank(extensionName)) {
                attachmentName += "." + extensionName;
            }
            boolean saveLocal = true;
            String attachmentPath = null;
            if (null != systemConfig.getContainerMap().get("minio")) {
                saveLocal = false;
                try {
                    attachmentPath = alias + "/" + attachmentName;
                    minioAdpterService.upload(attachmentPath, file);
                } catch (Exception ex) {
                    log.warn("上传Minio失败：uns={}, file={} | {}", alias, originalFilename, ex.getMessage());
                    saveLocal = true;
                }
            }
            if (saveLocal) {
                File uploadFile = destFile(attachmentName);
                //本地上传
                FileUtil.touch(uploadFile);
                try {
                    file.transferTo(uploadFile);
                } catch (IOException ignored) {
                }
                attachmentPath = FileUtils.getRelativePath(uploadFile.getAbsolutePath());
            }

            UnsAttachmentPo attachmentPo = new UnsAttachmentPo();
            attachmentPo.setId(SuposIdUtil.nextId());
            attachmentPo.setOriginalName(originalFilename);
            attachmentPo.setAttachmentName(attachmentName);
            attachmentPo.setAttachmentPath(attachmentPath);
            attachmentPo.setUnsAlias(alias);
            if (StringUtils.isNotBlank(extensionName)) {
                attachmentPo.setExtensionName(extensionName.toUpperCase());
            }
            unsAttachmentMapper.insert(attachmentPo);
            UnsAttachmentBo bo = BeanUtil.copyProperties(attachmentPo,UnsAttachmentBo.class);
            boList.add(bo);
        }

        Integer flags = unsPo.getWithFlags();
        if (flags == null) {
            flags = 0;
        }
        flags |= UNS_FLAG_WITH_ATTACHMENT;
        unsPo.setWithFlags(flags);
        unsMapper.updateById(unsPo);
        TopicDefinition definition = unsDefinitionService.getTopicDefinitionMap().get(unsPo.getId());
        if (definition != null) {
            definition.getCreateTopicDto().setFlags(flags);
        }

        return ResultVO.successWithData(boList);
    }

    public Pair<String, InputStream> download(String objectName) throws FileNotFoundException {
        List<UnsAttachmentPo> attachmentPos = unsAttachmentMapper.attachmentListByAttachmentPath(objectName);
        if (CollectionUtils.isEmpty(attachmentPos)) {
            throw new BuzException("uns.attachment.not.exit");
        }
        String originalName = attachmentPos.get(0).getOriginalName();

        if (null != systemConfig.getContainerMap().get("minio")) {
            return Pair.of(originalName, minioAdpterService.download(objectName));
        } else {
            objectName = URLDecoder.decode(objectName, StandardCharsets.UTF_8);
            File file = new File(FileUtils.getFileRootPath(), objectName);
            FileInputStream fs = new FileInputStream(file);
            return Pair.of(originalName, fs);
        }
    }

    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public void delete(String objectName) {
        List<UnsAttachmentPo> attachmentPos = unsAttachmentMapper.attachmentListByAttachmentPath(objectName);
        if (CollectionUtils.isNotEmpty(attachmentPos)) {
            if (null != systemConfig.getContainerMap().get("minio")) {
                try {
                    minioAdpterService.delete(objectName);
                } catch (Exception ex) {
                    log.warn("Minio删除失败: {} {}", objectName, ex.getMessage());
                }
            }
            File file = new File(FileUtils.getFileRootPath(), objectName);
            FileUtil.del(file);

            unsAttachmentMapper.deleteById(attachmentPos.get(0).getId());
            String unsAlias = attachmentPos.get(0).getUnsAlias();
            Long countUnsAttachments = unsAttachmentMapper.selectCount(new LambdaQueryWrapper<UnsAttachmentPo>().eq(UnsAttachmentPo::getUnsAlias, unsAlias));
            if (countUnsAttachments == null || countUnsAttachments.intValue() == 0) {
                UnsPo unsPo = unsMapper.selectOne(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getAlias, unsAlias).select(UnsPo::getId, UnsPo::getWithFlags));
                if (unsPo != null) {
                    Integer flags = unsPo.getWithFlags();
                    if (flags == null) {
                        flags = 0;
                    }
                    flags = flags & ~UNS_FLAG_WITH_ATTACHMENT;
                    unsPo.setWithFlags(flags);
                    unsMapper.updateById(unsPo);
                    TopicDefinition definition = unsDefinitionService.getTopicDefinitionMap().get(unsPo.getId());
                    if (definition != null) {
                        definition.getCreateTopicDto().setFlags(flags);
                    }
                } else {
                    log.warn("delete Attachment: 找不到 UNS: {}", unsAlias);
                }
            }
        }
    }

    public void deleteByUns(List<CreateTopicDto> unsPos) {
        List<String> aliases = unsPos.stream().filter(t -> Constants.withAttachment(t.getFlags())).map(CreateTopicDto::getAlias).toList();
        log.info("deleteByUns: size = {}", aliases);
        delete(aliases);
    }

    public void delete(List<String> aliases) {
        if (CollectionUtils.isEmpty(aliases)) {
            return;
        }
        ThreadUtil.execute(() -> {
            for (List<String> seg : Lists.partition(aliases, 1000)) {
                List<UnsAttachmentPo> attachmentPos = unsAttachmentMapper.selectList(Wrappers.lambdaQuery(UnsAttachmentPo.class).in(UnsAttachmentPo::getUnsAlias, seg));
                if (CollectionUtils.isNotEmpty(attachmentPos)) {
                    List<Long> deleteIds = new ArrayList<>(attachmentPos.size());
                    List<String> objectNames = new ArrayList<>(attachmentPos.size());
                    attachmentPos.forEach(attachment -> {
                        deleteIds.add(attachment.getId());
                        objectNames.add(attachment.getAttachmentPath());
                    });
                    minioAdpterService.delete(objectNames);
                    unsAttachmentMapper.deleteBatchIds(deleteIds);
                }
            }
        });
    }

    public List<UnsAttachmentBo> query(String alias) {
        List<UnsAttachmentPo> attachmentPos = unsAttachmentMapper.attachmentListByUnsAlias(alias);
        if (CollectionUtils.isEmpty(attachmentPos)) {
            return new ArrayList<>();
        } else {
            return attachmentPos.stream().map(attachmentPo -> {
                UnsAttachmentBo attachmentBo = new UnsAttachmentBo();
                attachmentBo.setId(attachmentPo.getId());
                attachmentBo.setOriginalName(attachmentPo.getOriginalName());
                attachmentBo.setAttachmentPath(attachmentPo.getAttachmentPath());
                attachmentBo.setExtensionName(attachmentPo.getExtensionName());
                attachmentBo.setCreateAt(attachmentPo.getCreateAt());
                return attachmentBo;
            }).collect(Collectors.toList());
        }
    }

    private static final File destFile(String fileName) {
        String targetPath = String.format("%s%s/%s", FileUtils.getFileRootPath(), Constants.UNS_ROOT, fileName);
        File outFile = FileUtil.touch(targetPath);
        return outFile;
    }
}
