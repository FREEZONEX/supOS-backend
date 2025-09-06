package com.supos.uns.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.uns.vo.InstanceFieldVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDetailVo {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "*文件名称，和文件夹显示名一致。支持修改。")
    private String name;

    @Schema(description = "*文件显示名。支持修改和重名。最大长度128字符")
    private String displayName;

    @Schema(description = "文件别名。最大长度63字符，允许字符包括英文、数字、下划线。为空则系统自动生成")
    private String alias;

    @Schema(description = "所属父文件夹别名")
    private String parentAlias;

    @Schema(description = "1-时序，2-关系，3-序实时计算，4-历史计算，6-聚合，7-时序引用。为1/3时supOS系统默认创建\"value\"，\"timeStamp\"，\"status\"三个键。其中value的数据类型要和文件数据类型一致。")
    private Integer dataType;

    @Schema(description = "文件类型 0--文件夹，2--文件")
    private Integer pathType;

    @Schema(description = "全路径")
    private String path;

    @Schema(description = "路径名")
    private String pathName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "字段定义")
    private FieldDefine[] definition;

    @Schema(description = "是否持久化 默认false")
    private Boolean persistence = false;

    @Schema(description = "是否生成仪表盘默认false")
    private Boolean dashBoard = false;

    @Schema(description = "是否模拟数据 默认false")
    private Boolean addFlow = false;

    @Schema(description = "引用对象 当dataType为计算、聚合、引用时必填")
    private InstanceFieldVo[] refers;

    @Schema(description = "当dataType=3时可指定表达式，a1表示refers中第一个，a2表示第二个，以此类推。允许为空或无该字段，表示暂无表达式，此时可按照1-时序的特性来处理。")
    private String expression;

    @Schema(description = "聚合计算频率：当聚合类型时(dataType=6)的计算时间间隔，单位支持：秒:s 分钟:m 小时：h；如三分钟：3m")
    private String frequency;
    @Schema(description = "关联的模板别名")
    private String templateAlias;

    @Schema(description = "扩展属性 最大支持3个")
    @Size(max = 3)
    LinkedHashMap<String, Object> extendProperties;

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒
    @Schema(description = "修改时间--单位：毫秒")
    Long updateTime;

    @Hidden
    @Schema(description = "协议")
    Map<String, Object> protocol;
}
