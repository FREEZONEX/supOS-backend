package com.supos.common.dto;


import com.supos.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 分页对象
 *
 * @author xinwangji@supos.com
 * @date 2022/11/24 10:10
 * @description
 */
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationDTO {

  /**
   * 当前页数 默认为第1页
   */
  @Schema(description = "当前页数，默认为1")
  @Min(value = 1L, message = "common.pageno")
  private Long pageNo = Constants.DEFAULT_PAGE_NUM;
  /**
   * 每页记录数 默认为20条记录，最大返回1000条记录
   */
  @Schema(description = "每页记录数，默认为20，最大支持1000")
  @Min(value = 1L, message = "common.pagesize")
  private Long pageSize = Constants.DEFAULT_PAGE_SIZE;

  public Long getPageNo() {
    return pageNo = pageNo == null ? Constants.DEFAULT_PAGE_NUM : pageNo;
  }

//  public Long getPageSize() {
//    return pageSize = pageSize > Constants.MAX_PAGE_SIZE ? Constants.MAX_PAGE_SIZE : pageSize;
//  }


  public Long getPageSize() {
    return pageSize;
  }
}
