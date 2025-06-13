package com.supos.common.adpater;

public interface TopicMessageConsumer {
    void onMessageByAlias(String alias, String payload);
}
