package com.supos.common.sdk;

import com.supos.common.event.WebsocketNotifyEvent;

public interface WebsocketSender {

    void sendLatestMsg(WebsocketNotifyEvent event);
}
