package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.service.ImportNodeRedFlowService;
import com.supos.adpter.nodered.service.ProtocolServerService;
import com.supos.adpter.nodered.vo.AddServerRequestVO;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.MappingDTO;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class ParserApi {

    /**
     * 缓存模版内容
     */
    Map<String, String> cacheTplJson = new HashMap<>();

    protected int intervalHeight = 80;

    @Autowired
    private ProtocolServerService protocolServerService;

    protected String readFromTpl(String filePath) {
        String cachedJson = cacheTplJson.get(filePath);
        if (cachedJson != null) {
            return cachedJson;
        }
        try (InputStream inputStream = ImportNodeRedFlowService.class.getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder builder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            cacheTplJson.put(filePath, builder.toString());
            return builder.toString();
        } catch (IOException e) {
            throw new NodeRedException(400, "nodered.template.read.error");
        }
    }

    protected String createServer(String serverName, String protocolName, String serverConfig) {
        AddServerRequestVO addServerRequest = buildParam(serverName, protocolName, serverConfig);
        return protocolServerService.addServer(addServerRequest, false);
    }

    private AddServerRequestVO buildParam(String serverName, String protocolName, String serverConfig) {
        AddServerRequestVO param = new AddServerRequestVO();
//        String configJson = JSON.toJSONString(serverConfig);
        Map config = JSON.parseObject(serverConfig, Map.class);
        param.setServer(config);
        param.setProtocolName(protocolName);
        param.setServerName(serverName);
        return param;
    }

    public abstract String readTplFromCache(BatchImportRequestVO.UnsVO uns);

    public abstract void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes);

    public Map<String, List<MappingDTO>> buildMapping(List<FieldDefine> fields, String alias) {
        // key=index  value=[{"alias": "", "name": ""}]
        Map<String, List<MappingDTO>> mappings = new HashMap<>();
        for (FieldDefine f : fields) {
            List<MappingDTO> m = mappings.get(f.getIndex());
            if (m == null) {
                m = new ArrayList<>();
            }
            m.add(new MappingDTO(alias, f.getName()));
            mappings.put(f.getIndex(), m);
        }
        return mappings;
    }

    protected int getMaxHeight(JSONArray fullNodes) {
        int maxHeight = 0;
        for (int i = 0; i < fullNodes.size(); i++) {
            Integer height = fullNodes.getJSONObject(i).getInteger("y");
            if (height != null && height.intValue() > maxHeight) {
                maxHeight = height.intValue();
            }
        }
        return maxHeight;
    }

}
