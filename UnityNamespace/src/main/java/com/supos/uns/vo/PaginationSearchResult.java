package com.supos.uns.vo;

import com.supos.common.dto.JsonResult;
import com.supos.common.dto.PageResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "分页搜索结果")
@Data
public class PaginationSearchResult<T> extends JsonResult<T> {
    PageResultDTO page;//分页信息
}
