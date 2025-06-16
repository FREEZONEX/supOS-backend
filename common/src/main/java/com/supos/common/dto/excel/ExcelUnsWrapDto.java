package com.supos.common.dto.excel;

import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.InstanceField;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelUnsWrapDto
 * @date 2025/4/23 21:06
 */
@Data
public class ExcelUnsWrapDto {

    private String batchIndex;

    private boolean checkSuccess = true;

    private CreateTopicDto uns;

    /**
     * 关联的模板别名
     */
    private String templateAlias;

    /**
     * 关联的标签
     */
    private Set<String> labels;

    /**
     * 关联的引用
     */
    private InstanceField[] refers;

    public ExcelUnsWrapDto(String batchIndex, CreateTopicDto uns) {
        this.batchIndex = batchIndex;
        this.uns = uns;
    }

    public void addLabel(String label) {
        if (labels == null) {
            labels = new HashSet<>();
        }
        labels.add(label);
    }
}
