package com.supos.adpter.nodered.vo;

import com.supos.common.dto.protocol.MappingDTO;
import com.supos.common.dto.protocol.SimpleModelDTO;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.dto.FieldDefine;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Valid
public class BatchImportRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // 流程名称,可以是文件名不带后缀
    @NotEmpty(message = "name can't be empty")
    private String name;

    @NotNull(message = "model list can't be null")
    private List<UnsVO> uns;

    @Data
    public static class UnsVO implements Serializable {

        private static final long serialVersionUID = 1l;

        private String unsTopic;

        private String alias;

        private String tplFile;

        private List<FieldDefine> fields;

        /**
         * 模型实例化数据-json格式
         */
        private String jsonExample;

        private boolean mockData;

        /**
         * 需要根据具体协议自动生成对应客户端
         */
        private String protocol;

        /**
         * 协议对应的配置
         */
        private Map<String, Object> config;

        /**
         * 协议配置对象: RestConfigDTO, ModbusConfigDTO, OpcUAConfigDTO
         */
        private Object protocolBean;

        private SimpleModelDTO model;

    }

}


