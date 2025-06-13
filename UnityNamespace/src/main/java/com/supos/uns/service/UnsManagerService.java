package com.supos.uns.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.NodeFlowDTO;
import com.supos.common.dto.ResultDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.UnsPo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnsManagerService extends ServiceImpl<UnsMapper, UnsPo> {

    @Autowired
    UnsAddService unsAddService;
    @Autowired
    UnsCalcService unsCalcService;
    @Autowired
    UnsTemplateService unsTemplateService;

    public Set<Long> listInstances(Collection<Long> instanceIds) {
        return baseMapper.listInstanceIds(instanceIds);
    }

    /**
     * 检测是否引用计算实例和流程
     *
     * @param alias
     * @param fields
     * @return
     */
    public ResultVO detectIfFieldReferenced(String alias, FieldDefine[] fields) {
        UnsPo uns = baseMapper.getByAlias(alias);
//        String[] errors = new String[1];
//        FieldDefine[] autoFilledFields = processFieldDefines(uns.getPath(), uns.getDataType(), uns.getDataSrcId(), fields, errors, true);
//        if (StringUtils.hasText(errors[0])) {
//            throw new BuzException(errors[0]);
//        }
        Map<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("referred", false);

        if (uns.getPathType() == 0) {
            return ResultVO.successWithData(dataMap);
        }
        // 查询模型下的实例, 以便后续修改实例的表结构
        List<UnsPo> instances = baseMapper.listInstancesByModel(uns.getId());
        if (instances.isEmpty()) {
            return ResultVO.successWithData(dataMap);
        }
        List<FieldDefine> oldFields = Arrays.asList(uns.getFields());
        // 筛选出新增和删除的属性集合
//        List<FieldDefine> inputFields = new ArrayList<>(Arrays.asList(autoFilledFields));
        Set<String> inputFields = Arrays.stream(fields).map(FieldDefine::getName).collect(Collectors.toSet());
        List<FieldDefine> delFields = oldFields.stream().filter(e -> !inputFields.contains(e.getName())).collect(Collectors.toList());
        // 检测计算实例是否有引用
        List<UnsPo> calcInsts = unsCalcService.detectReferencedCalcInstance(instances, uns.getPath(), delFields, false);
        // 检测node-red流程是否引用了
        List<String> instanceTopics = instances.stream().map(UnsPo::getPath).collect(Collectors.toList());
        // 筛选出告警实例
        List<String> alarms = calcInsts.stream().filter(i -> i.getDataType() == 5).map(UnsPo::getPath).collect(Collectors.toList());

        boolean bingo = detectReferencedNodeRed(instanceTopics);
        int tipFlow = bingo ? 1 : 0;
        int tipCalc = calcInsts.isEmpty() ? 0 : 2;
        int tipAlarm = alarms.isEmpty() ? 0 : 4;
        String tips = I18nUtils.getMessage("uns.update.field.tips" + (tipFlow + tipCalc + tipAlarm));
        dataMap.put("referred", (tipFlow + tipCalc + tipAlarm) > 0);
        dataMap.put("tips", tips);

        return ResultVO.successWithData(dataMap);
    }

    private boolean detectReferencedNodeRed(List<String> instanceTopics) {
        HttpRequest clientRequest = HttpUtil.createPost("http://localhost:8080/service-api/supos/flow/by/topics");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        clientRequest.addHeaders(headers);
        clientRequest.timeout(3000);
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("topics", instanceTopics);
        clientRequest.body(JSON.toJSONString(bodyMap));
        HttpResponse response = clientRequest.execute();
        if (response.getStatus() == 200) {
            ResultDTO<List<NodeFlowDTO>> resultDTO = JSON.parseObject(response.body(), ResultDTO.class);
            return resultDTO.getData() != null && !resultDTO.getData().isEmpty();
        }
        return false;
    }

    private static int countMatches(CharSequence str, int from, int end, char ch) {
        int count = 0;
        from = Math.max(0, from);
        end = Math.min(str.length(), end);
        for (int i = from; i < end; ++i) {
            if (ch == str.charAt(i)) {
                ++count;
            }
        }

        return count;
    }

}
