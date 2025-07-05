package com.supos.uns.service.exportimport.json.data;

import com.supos.uns.dao.po.DashboardPo;
import lombok.Data;

import java.util.List;

/**
 * @author wangshuzheng
 * @description:
 * @date 2025年06月20日 16:15
 */
@Data
public class DashboardJsonWrapper {
    private List<DashboardPo> data;
}
