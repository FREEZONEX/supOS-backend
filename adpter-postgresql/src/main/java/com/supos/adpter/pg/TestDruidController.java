package com.supos.adpter.pg;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;

@RestController
public class TestDruidController {

    @Autowired
    private PostgresqlEventHandler pgHandler;

    @GetMapping("/inter-api/supos/close/druid")
    public void closeDruidConnection() throws SQLException {
        DruidDataSource dataSource = (DruidDataSource)pgHandler.getJdbcTemplate().getDataSource();
        dataSource.restart();
    }
}
