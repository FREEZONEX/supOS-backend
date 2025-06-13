package com.supos.common.adpater;

import com.supos.common.SrcJdbcType;
import org.springframework.jdbc.core.JdbcTemplate;

public interface DataStorageAdapter extends Adapter {

    SrcJdbcType getJdbcType();

    default JdbcTemplate getJdbcTemplate() {return null;}

    DataSourceProperties getDataSourceProperties();

}
