package com.supos.adpter.kong;

import com.supos.adpter.kong.dto.ResourceQuery;
import com.supos.adpter.kong.service.ResourceService;
import com.supos.adpter.kong.vo.ResourceVo;
import com.supos.adpter.kong.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ResourceController {

    @Autowired
    private ResourceService resourceService;


    @GetMapping("/inter-api/supos/resource")
    public ResultVO<List<ResourceVo>> getResourceList(ResourceQuery query){
        return resourceService.getResourceList(query);
    }


    @GetMapping("/inter-api/supos/resource/setButton")
    public void setButton(){
        resourceService.setButton();
    }

}