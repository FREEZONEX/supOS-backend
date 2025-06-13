package com.supos.uns.vo;

import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel(description = "分页搜索结果")
@Data
public class PaginationSearchResult<T> extends JsonResult<T> {
    PageResultDTO page;//分页信息
}
