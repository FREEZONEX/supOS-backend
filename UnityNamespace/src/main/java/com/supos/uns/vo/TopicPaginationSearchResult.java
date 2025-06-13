package com.supos.uns.vo;

import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

import java.util.List;

@Data
public class TopicPaginationSearchResult extends JsonResult<List<Object>> {
    PageResultDTO page;//分页信息

    //  topic列表
    @Override
    public List<Object> getData() {
        return super.getData();
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
