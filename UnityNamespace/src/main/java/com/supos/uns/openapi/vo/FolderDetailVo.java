package com.supos.uns.openapi.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderDetailVo {

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

    @Schema(description = "扩展属性 最大支持3个")
    @Size(max = 3)
    LinkedHashMap<String, Object> extendProperties;

    @Schema(description = "关联的模板别名")
    private String templateAlias;

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒
    @Schema(description = "修改时间--单位：毫秒")
    Long updateTime;
}
