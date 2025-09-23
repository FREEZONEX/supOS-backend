package com.supos.uns.openapi;

import com.supos.common.dto.HandleTodoDto;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.SysModuleDto;
import com.supos.common.dto.TodoOpenQueryDto;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.service.TodoService;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TodoOpenApi {

    @Autowired
    private TodoService todoService;

    @Operation(summary = "分页查询待办信息", tags = "待办中心", description = "(OPEN-API)")
    @GetMapping({"/open-api/todo"})
    public PageResultDTO<TodoVo> pageListByOpen(@Valid @ParameterObject TodoOpenQueryDto params) {
        return todoService.pageListByOpen(params);
    }

    @Operation(summary = "获取待办的模块列表", tags = "待办中心", description = "(OPEN-API)")
    @GetMapping({"/open-api/todo/moduleList"})
    public ResultVO<SysModuleDto> getModuleList() {
        return todoService.getModuleList();
    }

    @Operation(summary = "创建待办", tags = "待办中心")
    @PostMapping({"/open-api/todo"})
    public ResultVO createTodo(@Valid @RequestBody CreateTodoVo createTodoVo) {
        return todoService.createTodo(createTodoVo);
    }

    @Operation(summary = "处理待办", tags = "待办中心", description = "(OPEN-API)")
    @PostMapping({"/open-api/todo/handle"})
    public ResultVO handle(@Valid @RequestBody HandleTodoDto handleTodoDto) {
        return todoService.handler(handleTodoDto);
    }

}
