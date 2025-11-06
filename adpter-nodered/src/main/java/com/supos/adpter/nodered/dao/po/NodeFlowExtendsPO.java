package com.supos.adpter.nodered.dao.po;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class NodeFlowExtendsPO extends NodeFlowPO {

    /**
     * 是否置顶
     */
    private Integer mark;
}
