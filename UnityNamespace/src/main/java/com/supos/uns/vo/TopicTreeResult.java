package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.PathUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TreeSet;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicTreeResult {
    @Schema(description = "ID")
    String id;
    @Schema(description = "别名")
    String alias;// 别名
    @Schema(description = "目录ID")
    String parentId;// 目录ID
    @Schema(description = "目录别名")
    String parentAlias;// 目录别名
    @Schema(description = "节点值，本期表示消费下来的数据条数")
    Long value; //节点值，本期表示消费下来的数据条数
    @Schema(description = "最新更新时间")
    Long lastUpdateTime;
    @Schema(description = "子节点数(文件)")
    Integer countChildren;//只包含文件夹的数量（子孙）
//    @Schema(description = "路径类型: 0--文件夹，1--模板，2--文件")
//    int type; // 路径类型: 0--文件夹，1--模板，2--文件
    @Schema(description = "路径类型: 0--文件夹，1--模板，2--文件")
    int pathType; // 路径类型: 0--文件夹，1--模板，2--文件
    int type; // 路径类型: 0--文件夹，1--模板，2--文件
    /**
     * 0--保留（模板），1--时序，2--关系，3--计算型, 5--告警 6--聚合 7--引用
     */
    Integer dataType;
    /**
     * 0-3 see com.supos.common.enums.FolderDataType
     */
    Integer parentDataType;
    @Schema(description = "名称")
    String name;//名称
    @Schema(description = "显示名称")
    String displayName;//显示名称
    @Schema(description = "树的路径")
    String path;//树的路径
    @Schema(description = "文件路径名")
    String pathName;//文件路径名
    @Schema(description = "描述")
    String description;
    @Schema(description = "模板别名")
    String templateAlias;
    @Schema(description = "协议类型")
    String protocol; // 协议类型
    @Schema(description = "字段定义")
    FieldDefine[] fields;// 字段定义
    @Schema(description = "子节点")
    Collection<TopicTreeResult> children;// 子节点
    @Schema(description = "扩展字段")
    LinkedHashMap<String, Object> extend;//扩展字段 文件/文件夹：扩展字段  报警类型：workflow表主键ID
    @Schema(description = "当前节点下是否有子的文件夹")
    Boolean hasChildren;//当前节点下是否有子的文件夹 （只查子节点）
    @Schema(description = "创建时间")
    Date createAt;
    @Schema(description = "更新时间")
    Date updateAt;
    @Schema(description = "挂载信息")
    MountDetailVo mount;

    public TopicTreeResult(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public TopicTreeResult(String id, String alias, String parentId, String name, String path) {
        this.id = id;
        this.alias = alias;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
    }

    public void addChild(TopicTreeResult child) {
        if (children == null) {
            children = new TreeSet<>((o1, o2) -> {
                String a = PathUtil.getName(o1.path), b = PathUtil.getName(o2.path);
                if ("".equals(a) && "".equals(b) ) {
                    return 0;
                }
                if (Character.isDigit(a.charAt(a.length() - 1)) && Character.isDigit(b.charAt(b.length() - 1))) {
                    int i = numberStartIndex(a), j = numberStartIndex(b);
                    String n1 = i > 0 ? a.substring(0, i) : "", n2 = j > 0 ? b.substring(0, j) : "";
                    int ns = n1.compareTo(n2);// 先比较字符串前缀
                    if (ns != 0) {
                        return ns;
                    }
                    String aNumStr = a.substring(i), bNumStr = b.substring(j);
                    return StringUtils.compare(aNumStr, bNumStr);// 按后缀数字大小排序
                } else {
                    int r = a.compareTo(b);
                    if (r == 0) {
                        return o1.path.compareTo(o2.path);
                    }
                    return r;
                }
            });//sort by name
        }
        child.parent = this;
        children.add(child);
    }

    private static int numberStartIndex(String s) {
        boolean prevIsNumber = false;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (Character.isDigit(s.charAt(i))) {
                prevIsNumber = true;
            } else if (prevIsNumber) {
                return i + 1;
            }
        }
        return prevIsNumber ? 0 : -1;
    }

    public Integer getCountChildren() {
        if (countChildren == null) {
            if (children != null) {
                int count = 0;
                for (TopicTreeResult child : children) {
                    if (child.pathType == 2) {
                        count++;
                    }
                    count += child.getCountChildren();
                }
                countChildren = count;
            } else {
                countChildren = 0;
            }
        }
        return countChildren;
    }

    public Long getValue() {
        if (value == null) {
            if (children != null) {
                long total = 0;
                for (TopicTreeResult child : children) {
                    total += child.getValue();
                }
                value = total;
            } else {
                value = 0L;
            }
        }
        return value;
    }

    public TopicTreeResult setPathType(int pathType) {
        this.pathType = pathType;
        this.type = pathType;
        return this;
    }

    public TopicTreeResult setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getName() {
        Long value;
        if (lastUpdateTime != null && (value = getValue()) != null && value > 0 && name != null) {
            return name + '(' + value + ')';
        }
        return name;
    }

    public
    @JsonIgnore
    transient TopicTreeResult parent;

    public String toString() {
        return path;
    }
}
