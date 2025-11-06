package com.supos.uns;


import com.alibaba.fastjson2.JSONObject;
import com.supos.common.dto.*;
import com.supos.common.event.*;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
//import com.supos.uns.service.TodoService;
import com.supos.uns.service.TodoService;
import com.supos.uns.service.UnsDefinitionService;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class TodoController {

    @Resource
    private TodoService todoService;
    @Autowired
    private UnsDefinitionService unsDefinitionService;

    @Operation(summary = "分页查询待办信息", tags = "待办中心")
    @PostMapping({"/inter-api/supos/todo/pageList"})
    public PageResultDTO<TodoVo> pageList(@RequestBody TodoQueryDto params) {
        return todoService.pageList(params);
    }


    /**
     * 获取待办的模块列表
     */
    @Operation(summary = "获取待办的模块列表", tags = "待办中心", description = "(OPEN-API)")
    @GetMapping({"/inter-api/supos/todo/moduleList"})
    public ResultVO<SysModuleDto> getModuleList() {
        return todoService.getModuleList();
    }

    @GetMapping("/test")
    public void test() {
//        System.out.println(I18nUtils.getMessage("aboutus.grafanaDescription"));
//        String json = "{\"timeStamp\":1747958828174,\"is_alarm\":true,\"limit_value\":50.0,\"uns\":1922277485476065280,\"_id\":1925705015695069184,\"current_value\":79.0}";
//        JSONObject data = JSONObject.parseObject(json);
//        Map<String, Object> data = new HashMap<>();
//        data.put("topic", "/$alarm/ceshi2_c6d71b518e6447ccbc69");
//        data.put("current_value", "100");
//        data.put("limit_value", "50");
//        data.put("_ct", new Date());
//        data.put("_id", "454545");
//        TopicMessageEvent topicMessageEvent = new TopicMessageEvent(this,null, 1922566317403439104L, 5, null, "/$alarm/ceshi2_c6d71b518e6447ccbc69", "", data, null, null, null, 1L, null);
//        todoService.alarmEvent(topicMessageEvent);
//        EventBus.publishEvent(topicMessageEvent);

        CreateTopicDto topic = unsDefinitionService.getDefinitionByAlias("_vqt_892fe425c19441c78566");


//        List<FileBlobDataQueryDto.EQCondition> eqConditions = new ArrayList<>();
//        FileBlobDataQueryDto.EQCondition condition = new FileBlobDataQueryDto.EQCondition();
//        condition.setFieldName("tag");
//        condition.setValue(topic.getId() + "");
//        eqConditions.add(condition);
//
//        QueryDataEvent event = new QueryDataEvent(this, topic, eqConditions);
//
//        EventBus.publishEvent(event);
//        List<Map<String, Object>> values = event.getValues();
//        System.out.println();

        String tableName = "supos_timeserial_integer";

        List<Long> ids = List.of(1981201029961617408L, 1983341044908953600L);
        BatchQueryLastMsgVqtEvent lastMsgEvent = new BatchQueryLastMsgVqtEvent(this,tableName, ids);
        EventBus.publishEvent(lastMsgEvent);
        List<Map<String, Object>> values = lastMsgEvent.getValues();
        Map<Long, Map<String, Object>> tags = values.stream().collect(Collectors.toMap(stringObjectMap -> (Long)stringObjectMap.get("tag"), Function.identity()));

        System.out.println();


    }
}
