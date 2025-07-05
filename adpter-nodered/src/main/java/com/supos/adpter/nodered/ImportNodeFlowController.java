package com.supos.adpter.nodered;

import com.supos.adpter.nodered.service.ImportNodeRedFlowService;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.Description;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SimpleUnsInstance;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.RemoveTopicsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ImportNodeFlowController {

    @Autowired
    private ImportNodeRedFlowService importNodeRedFlowService;

    /**
     * 从uns批量导入流程
     * @param batchImportRequest
     * @return
     */
    /*@PostMapping("/service-api/supos/uns/import")
    public ResultDTO batchImportFromUNS(@Valid @RequestBody BatchImportRequestVO batchImportRequest) {
        importNodeRedFlowService.importFlowFromUns(batchImportRequest);
        return ResultDTO.success("ok");
    }*/


    /**
     * 批量创建流程
     *
     * @param event
     */
    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(1000)
    @Description("uns.create.task.name.flow")
    void onBatchCreateFlow(BatchCreateTableEvent event) {
        log.info("==>trigger create flow event 开始批量创建流程...");
        BatchImportRequestVO request = new BatchImportRequestVO();
        List<BatchImportRequestVO.UnsVO> unsList = new ArrayList<>();
        StringBuilder firstTopicName = new StringBuilder(128);
        Map<SrcJdbcType, String> firstTopicNameMap = new HashMap<>(event.topics.size());
        for (Map.Entry<SrcJdbcType, CreateTopicDto[]> entry : event.topics.entrySet()) {
            for (CreateTopicDto topic : entry.getValue()) {
                boolean addFlow = topic.getAddFlow();
                // 批量导入的只判断第一个值
                if (!addFlow) {
                    log.info("{} skip create flows, because flag is {}", topic.getTopic(), topic.getFlags());
                    return;
                }
                if (!firstTopicNameMap.containsKey(entry.getKey())) {
                    firstTopicNameMap.put(entry.getKey(), topic.getTopic());
                }
                BatchImportRequestVO.UnsVO uns = new BatchImportRequestVO.UnsVO();
                uns.setUnsTopic(topic.getPath());
                String mockJsonData = genMockData(topic.getFields());
                uns.setProtocol(IOTProtocol.RELATION.getName());
                uns.setJsonExample(mockJsonData);
                uns.setAlias(topic.getAlias());
                unsList.add(uns);
            }
        }

        if (!unsList.isEmpty()) {
            if (event.fromImport) {
                request.setName(event.flowName);
            } else {
                for (String name : firstTopicNameMap.values()) {
                    firstTopicName.append(name).append(";");
                }
                request.setName(firstTopicName.substring(0, firstTopicName.length() - 1));
            }
            request.setUns(unsList);
            importNodeRedFlowService.importFlowFromUns(request);
        }
        log.info("<=== 批量导入成功 {}", unsList.size());
    }

    private List<FieldDefine> filterSystemField(FieldDefine[] fs) {
        List<FieldDefine> newFs = new ArrayList<>();
        for (FieldDefine f : fs) {
            if (!f.isSystemField()) {
                newFs.add(f);
            }
        }
        return newFs;
    }

    /**
     * 批量删除流程
     *
     * @param event
     */
    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(1000)
    void onBatchDeleteFlow(RemoveTopicsEvent event) {
        log.info("==>trigger delete flow event 开始批量删除流程...");
        if (!event.withFlow) {
            log.info("<==skip delete flows, because withFlow is false");
            return;
        }
        if (event.topics == null || event.topics.isEmpty()) {
            log.info("<==skip delete flows, because topics is empty");
            return;
        }
        importNodeRedFlowService.deleteFlows(event.topics.values().stream().map(SimpleUnsInstance::getAlias).collect(Collectors.toSet()));
        log.info("<== 批量删除成功");

    }

    private String genMockData(FieldDefine[] fields) {
        StringBuffer sb = new StringBuffer();
        for (FieldDefine field : fields) {
            if (field.isSystemField()) {
                continue;// 忽略系统自动生成的字段
            }
            FieldType t = field.getType();
            switch (t) {
                case INTEGER:
                case LONG: {
                    sb.append("\n'").append(field.getName()).append("'").append(":generateRandomNumber(),");
                    break;
                }
                case FLOAT:
                case DOUBLE: {
                    sb.append("\n'").append(field.getName()).append("'").append(":generateRandomFloatWithTwoDecimals(),");
                    break;
                }
                case BOOLEAN: {
                    sb.append("\n'").append(field.getName()).append("'").append(":getBool(),");
                    break;
                }
                case STRING: {
                    sb.append("\n'").append(field.getName()).append("'").append(":randomString(20),");
                    break;
                }
                case DATETIME: {
                    sb.append("\n'").append(field.getName()).append("'").append(":formatCurDate(),");
                    break;
                }
                default: {
                    sb.append("\n'").append(field.getName()).append("'").append(":'unknown type',");
                }
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return sb.toString();
    }


}
