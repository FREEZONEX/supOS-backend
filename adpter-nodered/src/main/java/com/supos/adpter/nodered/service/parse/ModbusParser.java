package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.protocol.MappingDTO;
import com.supos.common.dto.protocol.ModbusConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("modbusParser")
public class ModbusParser extends ParserApi {

    private String tplFile = "/modbus.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String modbusReadId = IDGenerator.generate();
        String modbusClientId = IDGenerator.generate();
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_modbus_read", modbusReadId)
                .replaceAll("\\$id_modbus_client", modbusClientId);
        // 替换模型topic
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());
        // 将modbus的配置由key-value转成对象，方便读取
        ModbusConfigDTO modbusConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), ModbusConfigDTO.class, Feature.TrimStringFieldValue);

        // 替换unit id
        jsonFlowStr = jsonFlowStr.replaceAll("\\$modbus_unit_id", modbusConfig.getUnitId());
        // 替换modbus功能码
        jsonFlowStr = jsonFlowStr.replace("$modbus_func_code", modbusConfig.getFc());
        // 替换modbus数组起始位置以及数量
        jsonFlowStr = jsonFlowStr.replace("$modbus_adr", modbusConfig.getAddress());
        jsonFlowStr = jsonFlowStr.replace("$modbus_quantity", modbusConfig.getQuantity());
        // 替换modbus采集频率
        jsonFlowStr = jsonFlowStr.replace("$Modbus_rate_unit", modbusConfig.getPollRate().getUnit());
        jsonFlowStr = jsonFlowStr.replace("$modbus_rate", modbusConfig.getPollRate().getValue() + "");
        // 替换modbus服务端地址和端口
        jsonFlowStr = jsonFlowStr.replace("$modbus_host", modbusConfig.getServer().getHost().trim());
        jsonFlowStr = jsonFlowStr.replace("$modbus_port", modbusConfig.getServer().getPort().trim());
        // 给modbus服务端起一个名字
        jsonFlowStr = jsonFlowStr.replace("$modbus_client_name", modbusConfig.getServerName());

        Map<String, List<MappingDTO>> ms = buildMapping(uns.getFields(), uns.getUnsTopic());

        String serverConfigJson = JSON.toJSONString(modbusConfig.getServer());
        super.createServer(modbusConfig.getServerName(), IOTProtocol.MODBUS.getName(), serverConfigJson);

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 设置节点高度
        for (int i = 0; i < jsonArr.size(); i++) {
            Integer highSpace = jsonArr.getJSONObject(i).getInteger("iy");
            int y = maxHeight + intervalHeight;
            if (highSpace != null) {
                y += highSpace;
            }
            jsonArr.getJSONObject(i).put("y", y);
            String nodeType = jsonArr.getJSONObject(i).getString("type");
            if ("supmodel".equals(nodeType)) {
                jsonArr.getJSONObject(i).put("mappings", ms);
                jsonArr.getJSONObject(i).put("model", uns.getModel());
            }
        }
        fullNodes.addAll(jsonArr);
    }

}
