package com.supos.uns.util;

import com.supos.common.Constants;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.dto.WebhookDataDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WebhookUtils {

    public static List<WebhookDataDTO> transfer(Collection<UnsPo> unsList) {
        List<WebhookDataDTO> wdds = new ArrayList<>();
        for (UnsPo uns : unsList) {
            // 只推送实例数据
            if (uns.getPathType() == 2 && uns.getDataType() != Constants.CITING_TYPE) {
                WebhookDataDTO wdd = new WebhookDataDTO();
                wdd.setFields(JsonUtil.jackToJson(uns.getFields()));
                wdd.setConfig(uns.getProtocol());
                wdd.setTopic(Constants.useAliasAsTopic ? uns.getAlias() :uns.getPath());
                wdd.setType(uns.getDataType());
                wdd.setAlias(uns.getAlias());
                wdd.setId(uns.getId().toString());
                wdds.add(wdd);
            }
        }
        return wdds;
    }

    public static List<WebhookDataDTO> transfer(List<UnsPo> unsList, String fields) {
        List<WebhookDataDTO> wdds = new ArrayList<>();
        for (UnsPo uns : unsList) {
            // 只推送实例数据
            if (uns.getPathType() == 2 && uns.getDataType() != Constants.CITING_TYPE) {
                WebhookDataDTO wdd = new WebhookDataDTO();
                wdd.setFields(fields);
                wdd.setConfig(uns.getProtocol());
                wdd.setTopic(Constants.useAliasAsTopic ? uns.getAlias() :uns.getPath());
                wdd.setType(uns.getDataType());
                wdd.setAlias(uns.getAlias());
                wdd.setId(uns.getId().toString());
                wdds.add(wdd);
            }
        }
        return wdds;
    }
}
