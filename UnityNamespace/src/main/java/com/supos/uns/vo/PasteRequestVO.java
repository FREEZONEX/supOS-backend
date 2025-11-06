package com.supos.uns.vo;

import com.supos.common.dto.CreateTopicDto;
import lombok.Data;

/**
 * 粘贴文件夹请求参数
 */
@Data
public class PasteRequestVO {

    private String sourceId;

    private String targetId;

    /**
     * new file or folder
     */
    private CreateTopicDto newF;
}
