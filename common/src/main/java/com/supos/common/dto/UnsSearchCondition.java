package com.supos.common.dto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnsSearchCondition extends PaginationDTO{

    /**
     * 查询类型：1-UNS（名称+别名） 2-含标签 3-含模板
     */
    private int searchType = 1;

    /**
     * 查询深度。-1为默认值，表示全深度；1表示1层，以此类推
     */
    @Schema(description = "查询深度。-1为默认值，表示全深度；1表示1层，以此类推")
    private Integer deep;

    /**
     * 父级ID
     */
    @Hidden
    private Long parentId;

    /**
     * 路径类型 0--文件夹，2--文件
     */
    @Schema(description = "路径类型 0--文件夹，2--文件")
    private Integer pathType;

    /**
     * 关键字：路径或别名
     */
    @Hidden
    private String keyword;

    /**
     * 别名
     */
    @Hidden
    private String alias;

    /**
     * 父级别名
     */
    @Schema(description = "父级别名")
    private String parentAlias;

    @Schema(description = "父级别名列表")
    private List<String> parentAliasList;

    /**
     * 别名集合
     */
    @Schema(description = "别名集合")
    private List<String> aliasList;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    @Schema(description = "显示名")
    private String displayName;

    /**
     * 路径
     */
    @Hidden
    private String path;

    @Hidden
    private String layRec;

    /**
     * 路径集合
     */
    @Schema(description = "路径集合")
    private List<String> pathList;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板别名")
    private String templateAlias;

    @Schema(description = "模板Id")
    private Long templateId;

    /**
     * 0--保留（模板），1--时序，2--关系，3--计算型, 5--告警
     */
    @Hidden
    Integer dataType;

    /**
     * 标签名称
     */
    @Hidden
    private String labelName;

    /**
     * 更新开始时间
     */
    @Schema(description = "更新开始时间")
    private Date updateStartTime;

    /**
     * 更新结束时间
     */
    @Schema(description = "更新结束时间")
    private Date updateEndTime;

    /**
     * 创建开始时间
     */
    @Schema(description = "创建开始时间")
    private Date createStartTime;

    /**
     * 创建结束时间
     */
    @Schema(description = "创建结束时间")
    private Date createEndTime;

    @Schema(description = "扩展字段")
    Map<String,Object> extend;//扩展字段

    @Schema(description = "是否包含文件的当前值")
    private Boolean withValues;

    @Schema(description = "返回数据是否包含parentId/parentAlias中指定的文件夹信息。ture-返回（默认值），false-不返回")
    private Boolean returnParentInfo = true;

    /**
     * 是否显示记录数
     */
    @Hidden
    private Boolean showRec = false;
    @Hidden
    private Boolean filterFolder;
    public UnsSearchCondition(String keyword){
        this.keyword = keyword;
    }

}
