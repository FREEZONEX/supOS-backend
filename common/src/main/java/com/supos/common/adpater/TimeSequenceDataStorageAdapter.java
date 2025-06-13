package com.supos.common.adpater;

import com.supos.common.adpater.historyquery.HistoryQueryParams;
import com.supos.common.adpater.historyquery.HistoryQueryResult;

public interface TimeSequenceDataStorageAdapter extends DataStorageAdapter {
    default StreamHandler getStreamHandler() {
        return null;
    }

    /**
     * 执行原生SQL
     *
     * @param sql SQL语句
     * @return 执行结果
     */
    String execSQL(String sql);

    /**
     * 历史数据查询
     *
     * @param params 查询参数
     * @return 查询结果
     */
    HistoryQueryResult queryHistory(HistoryQueryParams params);
}
