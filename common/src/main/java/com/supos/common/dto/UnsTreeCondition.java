package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnsTreeCondition extends PaginationDTO{

    @Schema(description = "查询类型：1-UNS（名称+别名） 2-含标签 3-含模板", defaultValue = "1")
    private int searchType = 1;

    @Schema(description = "关键字：路径或别名")
    private String keyword;

    @Schema(description = "父级ID  可为空，传0查询顶级节点，空值时查询所有")
    private Long parentId;

    @Schema(description = "数据类型：1--时序，2--关系，3--计算型, 5--告警 6--聚合 7--引用")
    Integer dataType;

    @Schema(description = "路径类型: 0--文件夹，2--文件")
    Integer pathType;
}
