package com.supos.uns;

import com.supos.adpter.kong.dto.ResourceQuery;
import com.supos.uns.service.ResourceService;
import com.supos.adpter.kong.vo.ResourceVo;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.common.dto.resource.BatchUpdateResourceDto;
import com.supos.common.dto.resource.SaveResourceDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/inter-api/supos/resource")
    public ResultVO saveResourceAndChildren(@RequestBody @Valid SaveResourceDto dto){
        return resourceService.saveResourceAndChildren(dto);
    }

    @PutMapping("/inter-api/supos/resource/batch")
    public ResultVO batchUpdate(@RequestBody @Valid List<BatchUpdateResourceDto> dtos){
        return resourceService.batchUpdate(dtos);
    }

    @DeleteMapping("/inter-api/supos/resource/{id}")
    public ResultVO delete(@PathVariable Long id){
        return resourceService.deleteById(id);
    }

    @DeleteMapping("/inter-api/supos/resource/batch")
    public ResultVO delete(@RequestBody List<Long> ids){
        return resourceService.batchDelete(ids);
    }


//    @GetMapping("/inter-api/supos/resource/setButton")
//    public void setButton(){
//        resourceService.setButton();
//    }

}