package com.supos.uns;


import com.alibaba.fastjson2.JSONObject;
import com.supos.common.dto.*;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
//import com.supos.uns.service.TodoService;
import com.supos.uns.service.TodoService;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TodoController {

    @Resource
    private TodoService todoService;

    @Operation(summary = "分页查询待办信息", tags = "待办中心")
    @PostMapping({"/inter-api/supos/todo/pageList"})
    public PageResultDTO<TodoVo> pageList(@RequestBody TodoQueryDto params) {
        return todoService.pageList(params);
    }

    @Operation(summary = "创建待办", tags = "待办中心")
    @PostMapping({"/inter-api/supos/todo/create", "/open-api/supos/todo/create"})
    public ResultVO createTodo(@Valid @RequestBody CreateTodoVo createTodoVo) {
        return todoService.createTodo(createTodoVo);
    }


    @Hidden
    @Operation(summary = "分页查询待办信息", tags = "待办中心", description = "(OPEN-API)")
    @PostMapping({"/open-api/supos/todo/pageList"})
    public PageResultDTO<TodoVo> pageListByOpen(@Valid @RequestBody TodoOpenQueryDto params) {
        return todoService.pageListByOpen(params);
    }


    @Hidden
    @Operation(summary = "处理待办", tags = "待办中心", description = "(OPEN-API)")
    @PostMapping({"/open-api/supos/todo/handle"})
    public ResultVO handle(@Valid @RequestBody HandleTodoDto handleTodoDto) {
        return todoService.handler(handleTodoDto);
    }

    /**
     * 获取待办的模块列表
     */
    @Hidden
    @Operation(summary = "获取待办的模块列表", tags = "待办中心", description = "(OPEN-API)")
    @GetMapping({"/inter-api/supos/todo/moduleList","/open-api/supos/todo/moduleList"})
    public ResultVO<SysModuleDto> getModuleList() {
        return todoService.getModuleList();
    }

    @GetMapping("/test")
    public void test() {
        System.out.println(I18nUtils.getMessage("aboutus.grafanaDescription"));
        String json = "{\"timeStamp\":1747958828174,\"is_alarm\":true,\"limit_value\":50.0,\"uns\":1922277485476065280,\"_id\":1925705015695069184,\"current_value\":79.0}";
        JSONObject data = JSONObject.parseObject(json);
//        Map<String, Object> data = new HashMap<>();
//        data.put("topic", "/$alarm/ceshi2_c6d71b518e6447ccbc69");
//        data.put("current_value", "100");
//        data.put("limit_value", "50");
//        data.put("_ct", new Date());
//        data.put("_id", "454545");
        TopicMessageEvent topicMessageEvent = new TopicMessageEvent(this,null, 1922566317403439104L, 5, null, "/$alarm/ceshi2_c6d71b518e6447ccbc69", "", data, null, null, null, 1L, null);
//        todoService.alarmEvent(topicMessageEvent);
//        EventBus.publishEvent(topicMessageEvent);
    }
}
