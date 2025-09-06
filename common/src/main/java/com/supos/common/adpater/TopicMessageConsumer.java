package com.supos.common.adpater;

import java.util.Map;

public interface TopicMessageConsumer {

    void onMessageByAlias(String alias, String payload);

    void onBatchMessage(Map<String, Map<String, Object>> payloads);

    void onMessageByAliasOnUpdate(Map<String, String> aliasVqtMap);
}
