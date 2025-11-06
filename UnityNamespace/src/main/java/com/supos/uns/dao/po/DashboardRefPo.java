package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(DashboardRefPo.TABLE_NAME)
public class DashboardRefPo {

    public static final String TABLE_NAME = "uns_dashboard_ref";

    private String dashboardId;

    private String unsAlias;

    private Date createAt;

    public DashboardRefPo(String dashboardId, String unsAlias) {
        this.dashboardId = dashboardId;
        this.unsAlias = unsAlias;
    }
}
