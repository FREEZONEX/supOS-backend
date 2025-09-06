package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.config.FieldsTypeHandler;
import com.supos.uns.config.InstanceFieldsHandler;
import com.supos.uns.config.JsonMapLongIntTypeHandler;
import com.supos.uns.config.JsonMapTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@TableName(UnsPo.TABLE_NAME)
public class UnsPo implements Cloneable {
    public static final String TABLE_NAME = "uns_namespace";
    @TableId
    Long id;
    String layRec;// 祖链: 根节点id/p1Id/p2Id/../id
    String name;// 原始名

    /**
     * 显示名
     */
    String displayName;

    @TableField(exist = false)
    String pathName;// 内部决定的路径名,兄弟节点之间唯一,name+重复序号来区分重名兄弟
    String path; // pathName 拼接组成的路径: 根节点pathName/p1Name /p2Name/../pathName

    Long parentId; // 父节点id(所在文件夹的id)
    Long modelId; // 实例对应的模板
    String alias;
    String parentAlias;
    @TableField(exist = false)
    String modelAlias;
    @TableField(exist = false)
    String templateName;//模板名称

    @TableField(exist = false)
    String templateAlias;//模板别名
    /**
     * 0--文件夹，1--模板，2--文件
     */
    Integer pathType;

    Integer dataSrcId;
    /**
     * 0--保留（模板），1--时序，2--关系，3--计算型, 5--告警 6--聚合 7--引用
     */
    Integer dataType;

    String dataPath;
    @TableField(typeHandler = FieldsTypeHandler.class)
    FieldDefine[] fields;

    Integer extendFieldFlags;

    String description;

    String protocol;

    Integer withFlags;// 1--addFlow, 2--addDashBoard, 3--二者都有

    Date createAt;

    Date updateAt;

    String protocolType;

    @TableField(typeHandler = JsonMapLongIntTypeHandler.class)
    Map<Long, Integer> refUns;
    @TableField(typeHandler = InstanceFieldsHandler.class)
    InstanceField[] refers;// 计算实例引用的其他实例字段
    String expression;// 计算表达式
    String tableName;
    Integer numberFields;
    @TableField(typeHandler = JsonMapTypeHandler.class)
    LinkedHashMap<String, Object> extend;//扩展字段 文件/文件夹：扩展字段  报警类型：workflow表主键ID
    @TableField(exist = false)
    int countChildren;
    @TableField(exist = false)
    int countDirectChildren;
    @TableField(exist = false)
    String labels;
    @TableField(typeHandler = JsonMapTypeHandler.class)
    TreeMap<Long, String> labelIds;

    public UnsPo(String path) {
        this.path = path;
    }

    public UnsPo(Long id, String alias, String name, int pathType, Integer dataType, Integer dataSrcId, FieldDefine[] fields, String description) {
        this.id = id;
        this.alias = alias;
        this.name = name;
        this.pathType = pathType;
        this.dataType = dataType;
        this.dataSrcId = dataSrcId;
        this.fields = fields;
        this.description = description;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnsPo po = (UnsPo) o;
        return Objects.equals(id, po.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public UnsPo clone() {
        try {
            return (UnsPo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
