package com.supos.uns.service.exportimport.core;

import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.ExportImportUtil;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExportNode
 * @date 2025/5/10 17:58
 */
@Data
public class ExportNode {
    private UnsPo unsPo;
    private ExportImportUtil.RowWrapper rowWrapper;

    public ExportNode(UnsPo unsPo) {
        this.unsPo = unsPo;
    }
}
