package com.supos.common.sdk;

import com.supos.common.event.RefreshLatestMsgEvent;

public interface UnsQueryApi {

    void refreshLatestMsg(RefreshLatestMsgEvent event);
}
