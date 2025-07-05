package com.supos.uns.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supos.common.Constants;
import com.supos.common.dto.CreateFileDto;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.JsonResult;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.ExternalTopicCacheDto;
import com.supos.uns.vo.OuterStructureVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsExternalTopicService {

    @Autowired
    UnsMapper unsMapper;
    @Autowired
    UnsQueryService unsQueryService;
    @Autowired
    UnsAddService unsAddService;
    @Autowired
    UnsLabelService unsLabelService;

    public ResultVO<List<OuterStructureVo>> parserTopicPayload(String topic) {
//        String json = "{\"protocol\":\"smtps\",\"host\":\"smtpdm.aliyun.com\",\"port\":25,\"ssl\":false,\"username\":\"test@service.supos.com\",\"password\":\"HelloCloud123\",\"maxFileSize\":0,\"timeout\":0}";
//        UnsQueryService.EXTERNAL_TOPIC_CACHE.put(topic,new ExternalTopicCacheDto(json,new Date()));

        ExternalTopicCacheDto cache = UnsQueryService.EXTERNAL_TOPIC_CACHE.get(topic);
        if (cache == null) {
            return ResultVO.successWithData(Collections.emptyList());
        }
        String payload = cache.getPayload();
        if (StringUtils.isBlank(payload)) {
            return ResultVO.successWithData(Collections.emptyList());
        }

        JsonResult<List<OuterStructureVo>> result = unsQueryService.parseJson2uns(payload);
        if (result.getCode() != 0) {
            return ResultVO.successWithData(Collections.emptyList());
        }

        return ResultVO.successWithData(result.getData());
    }

    public ResultVO topic2Uns(CreateFileDto createFileDto) {
        List<CreateTopicDto> topicDtoList = new ArrayList<>();
        String path = createFileDto.getPath();
        if (StringUtils.isBlank(path)) {
            return ResultVO.fail("path不可为空");
        }
        if (path.contains("/")) {
            if (path.indexOf("/") == 0) {
                path = path.replaceFirst("/", "");
            }
            String folderPath = PathUtil.removeLastLevel(path);
            String folderAlias = unsMapper.selectAliasByPath(folderPath);
            //目录已存在  直接建文件
            if (StringUtils.isNotBlank(folderAlias)) {
                String name = PathUtil.getLastLevel(path);
                createFileDto.setName(name);
                createFileDto.setParentAlias(folderAlias);
                return saveFile(createFileDto);
            }

            List<String> folderPathList = splitFolderPath(path);
            List<UnsPo> unsList = unsMapper.selectList(new LambdaQueryWrapper<UnsPo>().in(UnsPo::getPath, folderPathList));
            Map<String, UnsPo> folderMap = unsList.stream().collect(Collectors.toMap(UnsPo::getPath, v -> v));

            String[] names = path.split("/");
            String parentAlias = null;
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                if (i > 0) {
                    currentPath.append("/");
                }
                currentPath.append(name);

                UnsPo uns = folderMap.get(currentPath.toString());

                String alias;
                //如果文件夹已存在 使用原文件夹别名
                if (uns != null) {
                    alias = uns.getAlias();
                } else {
                    //不存在，则新建
                    alias = PathUtil.generateAlias(name, Constants.PATH_TYPE_DIR);
                    CreateTopicDto topicDto = new CreateTopicDto();
                    topicDto.setAlias(alias);
                    topicDto.setName(name);
                    topicDto.setParentAlias(parentAlias);
                    topicDto.setPathType(Constants.PATH_TYPE_DIR);
                    //最后一级 是文件  单独创建
                    if (i + 1 == names.length) {
                        createFileDto.setAlias(alias);
                        createFileDto.setName(name);
                        createFileDto.setParentAlias(parentAlias);
                    } else {
                        topicDtoList.add(topicDto);
                    }
                }
                parentAlias = alias;
            }
        } else {
            createFileDto.setAlias(PathUtil.generateAlias(path, Constants.PATH_TYPE_DIR));
            createFileDto.setName(path);
        }
        //创建文件夹
        if (CollectionUtils.isNotEmpty(topicDtoList)){
            unsAddService.createModelAndInstance(topicDtoList, false);
        }
        return saveFile(createFileDto);
    }


    private ResultVO<String> saveFile(CreateFileDto createFileDto) {
        if (createFileDto.getName().length() > 63) {
            return ResultVO.fail(I18nUtils.getMessage("uns.external.name.length.limit.exceed"));
        }
        createFileDto.setAlias(PathUtil.generateAlias(createFileDto.getName(), Constants.PATH_TYPE_DIR));
        JsonResult<String> result = unsAddService.createModelInstance(createFileDto);
        if (result.getCode() == 0) {
            String id = result.getData();
            if (CollectionUtils.isNotEmpty(createFileDto.getLabelList())) {
                unsLabelService.makeLabel(Long.parseLong(id), createFileDto.getLabelList());
            }
            UnsQueryService.EXTERNAL_TOPIC_CACHE.remove(createFileDto.getPath());
            return ResultVO.successWithData(id);
        } else {
            return ResultVO.fail(result.getMsg());
        }
    }


    /**
     * 构建路径层级列表（不包含最后一级）
     *
     * @param path 原始路径，例如 "a/b/c/d/e"
     * @return 每一层路径组成的列表，不含最后一级
     */
    public static List<String> splitFolderPath(String path) {
        List<String> result = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            return result;
        }

        String[] parts = path.split("/");

        // 如果路径少于2层，直接返回空列表
        if (parts.length <= 1) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) { // 注意这里减1
            if (i > 0) {
                current.append("/");
            }
            current.append(parts[i]);
            result.add(current.toString());
        }

        return result;
    }

}
