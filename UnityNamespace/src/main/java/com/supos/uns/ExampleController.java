package com.supos.uns;


import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.PaginationDTO;
import com.supos.common.dto.mock.MockDemoDTO;
import com.supos.common.dto.mock.MockOrderDTO;
import com.supos.common.dto.mock.MockWeatherDTO;
import com.supos.common.exception.vo.ResultVO;
import com.supos.uns.dao.po.ExamplePo;
import com.supos.uns.service.ExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/inter-api/supos/example")
public class ExampleController {

    @Resource
    private ExampleService exampleService;

    /**
     * 获取
     * @param params
     * @return
     */
    @PostMapping("/pageList")
    public PageResultDTO<ExamplePo> pageList(@RequestBody PaginationDTO params){
       return exampleService.pageList(params);
    }

    /**
     * 安装接口
     * @param id
     * @return
     */
    @PostMapping("/install")
    public ResultVO install(@RequestParam Long id){
        return exampleService.install(id);
    }

    /**
     * 卸载
     * @param id
     * @return
     */
    @DeleteMapping("/uninstall")
    public ResultVO uninstall(@RequestParam Long id){
        return exampleService.uninstall(id);
    }

    /**
     * 模拟restApi数据
     * @return
     */
    @RequestMapping("/mock/restapi")
    public ResultVO<List<MockWeatherDTO>> mockRestapi(){
        return exampleService.mockRestApiData();
    }

    /**
     * 模拟电器厂IT数据
     * @return
     */
    @RequestMapping("/mock/restapi/demo")
    public ResultVO<List<MockDemoDTO>> mockRestapiDemo(){
        return ResultVO.successWithData(Collections.singletonList(exampleService.mockDemoData()));
    }

    /**
     * 模拟订单数据
     * @return
     */
    @GetMapping("/mock/restapi/order")
    public List<MockOrderDTO> mockOrderData(){
        return Collections.singletonList(new MockOrderDTO());
    }

    /**
     * 初始化发电机数据
     * @return
     */
    @RequestMapping("/mock/demo/init")
    public ResultVO demoInit(){
        return exampleService.initDemoItData();
    }

}
