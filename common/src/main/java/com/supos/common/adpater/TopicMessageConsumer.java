package com.supos.common.adpater;

import java.util.Map;

public interface TopicMessageConsumer {

    void onMessageByAlias(String alias, String payload);

    void onMessageByAliasOnUpdate(Map<String, String> aliasVqtMap);
}
