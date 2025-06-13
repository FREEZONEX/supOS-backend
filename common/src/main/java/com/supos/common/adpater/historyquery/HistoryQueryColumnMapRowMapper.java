package com.supos.common.adpater.historyquery;

import com.supos.common.utils.DateTimeUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class HistoryQueryColumnMapRowMapper extends ColumnMapRowMapper {

    protected Map<String, Object> createColumnMap(int columnCount) {
        return new LinkedHashMap<>(columnCount);//时序库建表时都是大小写敏感
    }

    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        Object value = JdbcUtils.getResultSetValue(rs, index);
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) value;
            value = DateTimeUtils.dateTimeUTC(timestamp.getTime());
        }
        return value;
    }
}
