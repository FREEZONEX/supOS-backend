package com.supos.adpter.nodered.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowModelMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.service.parse.ParserApi;
import com.supos.adpter.nodered.service.parse.RelationalParser;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.adpter.nodered.vo.NodeFlowVO;
import com.supos.common.dto.ResultDTO;
import com.supos.common.event.FlowInstallEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImportNodeRedFlowService {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private NodeFlowMapper nodeFlowMapper;
    @Autowired
    private NodeFlowModelMapper nodeFlowModelMapper;
    private ParserApi parserApi;
    @Autowired
    private RelationalParser relationalParser;
    @Autowired
    private ObjectCachePool objectCachePool;

    /**
     * @param requestVO
     */
    public void importFlowFromUns(BatchImportRequestVO requestVO) {
        NodeFlowPO nf = nodeFlowMapper.getByName(requestVO.getName());
        while (nf != null) {
            String newName = requestVO.getName() + "(1)";
            log.error("流程({})已存在, 重名为{}", requestVO.getName(), newName);
            requestVO.setName(newName);
            nf = nodeFlowMapper.getByName(newName);
        }
        int topicSize = requestVO.getUns().size();
        JSONArray fullNodes = new JSONArray();
        log.info("node-red接收到协议：{}", requestVO.getUns().get(0).getProtocol());
        for (int i = 0; i < topicSize; i++) {
            BatchImportRequestVO.UnsVO unsVO = requestVO.getUns().get(i);
            // 解析时序还是关系模型的相关实现类
            ParserApi parserService = getParserImpl(unsVO.getProtocol(), topicSize);
            // 读取node-red的相关模版内容
            String tplJson = parserService.readTplFromCache(unsVO);
            // 替换模版中变量
            parserService.parse(tplJson, unsVO, fullNodes);

        }
        // 调用node-red接口， 部署新流程
        long id = nodeRedAdapterService.createFlow(requestVO.getName(), "", "node-red");
        List<String> aliases = requestVO.getUns().stream().map(BatchImportRequestVO.UnsVO::getAlias).collect(Collectors.toList());
        nodeRedAdapterService.proxyDeploy(id, fullNodes, aliases);
    }

    public NodeFlowVO createMockFlow(String path, String alias) {
        NodeFlowVO byAlias = nodeRedAdapterService.getByAlias(alias);
        if (byAlias != null) {
            return byAlias;
        }
        BatchImportRequestVO bir = new BatchImportRequestVO();
        bir.setName(path);
        BatchImportRequestVO.UnsVO uns = new BatchImportRequestVO.UnsVO();
        uns.setUnsTopic(path);
        uns.setAlias(alias);
        uns.setProtocol("mock");
        uns.setTplFile("/mock.json.tpl");
        uns.setJsonExample("");
        bir.setUns(List.of(uns));
        importFlowFromUns(bir);

        return nodeRedAdapterService.getByAlias(alias);

    }

    public ParserApi getParserImpl(String protocol, int topicSize) {
        return relationalParser;
        /*IOTProtocol protocolEnum = IOTProtocol.getByName(protocol);
        switch (protocolEnum) {
            case MODBUS: {
                if (topicSize == 1) {
                    return modbusParser;
                }
                return modbusBatchParser;
            }
            case OPC_UA: {
                // 判断是否启用批量模版
                if (topicSize == 1) {
                    return opcUAParser;
                }
                return opcUABatchParser;
            }
            case OPC_DA:
                return opcdaParse;
            case REST:
                return restApiParser;
            case RELATION:
                return relationalParser;
            case MQTT:
                return mqttParser;
            case ICMP:
                return icmpParser;
            default:
                IOTProtocolPO iotProtocol = objectCachePool.getProtocolByName(protocol);
                if (iotProtocol == null) {
                    throw new NodeRedException(400, "nodered.protocol.not.exist");
                }
                return customProtocolParser;
        }*/
    }

    /**
     * 根据别名批量删除关联流程
     *
     * @param aliases
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void deleteFlows(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return;
        }
        List<List<String>> listList = Lists.partition(aliases, 500);
        for (List<String> sublist : listList) {
            List<Long> parentIds = nodeFlowModelMapper.selectByAliases(sublist);
            if (parentIds.isEmpty()) {
                log.info("skip delete flows, because flow-model refs is empty");
                return;
            }
            for (Long id : parentIds) {
                nodeRedAdapterService.deleteFlow(id, false);
            }
        }

    }

    @EventListener(classes = FlowInstallEvent.class)
    public void onInstallFlow(FlowInstallEvent event) {
        if (FlowInstallEvent.INSTALL.equals(event.getOperation())) {
            importDemo(event.getFlowName());
        } else if (FlowInstallEvent.UNINSTALL.equals(event.getOperation())) {
            deleteDemo(event.getFlowName());
        }
    }

    /**
     * 导入demo流程
     *
     * @param demoFileName demo-it-flows / demo-ot-flows
     * @return
     */
    public ResultDTO importDemo(String demoFileName) {
        String flowContent = "";
        String filePath = "/demo/" + demoFileName + ".json";
        try {
            flowContent = readDemoFlowJson(filePath);
        } catch (IOException e) {
            return ResultDTO.fail(String.format("文件%s不存在", filePath));
        }
        JSONArray nodes = JSON.parseArray(flowContent);
        long id = nodeRedAdapterService.createFlow(demoFileName, "", "node-red");
        nodeRedAdapterService.proxyDeploy(id, nodes, null);
        return ResultDTO.success("");
    }

    /**
     * 删除demo流程
     *
     * @param demoFileName demo-it-flows / demo-ot-flows
     */
    public void deleteDemo(String demoFileName) {
        NodeFlowPO demoFlow = nodeFlowMapper.getByName(demoFileName);
        if (demoFlow == null) {
            return;
        }
        nodeRedAdapterService.deleteFlow(demoFlow.getId(), false);
    }

    private String readDemoFlowJson(String filepath) throws IOException {
        try (InputStream inputStream = ImportNodeRedFlowService.class.getResourceAsStream(filepath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder builder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }
}
