package com.supos.adpter.pg;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CopyManagerTest {

    @Test
    public void testCopy() throws SQLException, IOException {
        JdbcTemplate template = jdbcTemplate();
        try (Connection connection = template.getDataSource().getConnection()) {
            CopyManager copyManager = new CopyManager((BaseConnection) connection.unwrap(Connection.class));
       /*
            StringBuilder sb = new StringBuilder(128).append("1,Lucy,11\n")
                    .append("2,HanMeimie,22\n")
                    .append("5,老虎,55\n");
            copyManager.copyIn("COPY target_data(id,name,value) FROM STDIN WITH (FORMAT CSV)", new StringReader(sb.toString()));
            */

            boolean useTemptableDirect = false;

            long t0 = System.currentTimeMillis();
            String csv = new StringBuilder(128)
                    .append("2025-09-18T04:59:41.469Z,11,15000,0\n")
                    .append("2025-09-18T05:22:41.469Z,22,17000,0\n")
                    .append("2025-09-18T06:37:37.037Z,33,18000,0\n").toString();
            String copySQL = "COPY supos_timeserial_integer(\"timeStamp\",value,tag,status) FROM STDIN WITH (FORMAT CSV)";
            long rows = 0;
            if (!useTemptableDirect) {
                try {
                    rows = copyManager.copyIn(copySQL, new StringReader(csv));
                } catch (Exception ex) {
                    System.err.println("ERROR: " + ex.getMessage());
                    rows = saveByTempTable(connection, copySQL, copyManager, csv);
                }
            } else {
                rows = saveByTempTable(connection, copySQL, copyManager, csv);
            }

            System.out.printf("copy[%d] 耗时：%d ms\n", rows, System.currentTimeMillis() - t0);
        }
    }

    private static long saveByTempTable(Connection connection, String copySQL, CopyManager copyManager, String csv) throws SQLException {
        long rows = 0;
        try {
            connection.setAutoCommit(false);
            // 创建临时表(事务级)
            int bk = copySQL.indexOf(' '), st = copySQL.indexOf('(', bk + 1);
            String origTable = copySQL.substring(bk + 1, st).trim();
            String tmpTable = "\"#tmp_" + origTable + "\"";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TEMP TABLE  if not exists " + tmpTable +
                        " (LIKE " + origTable + ") ON COMMIT DELETE ROWS");
            }
            copySQL = "COPY " + tmpTable +  copySQL.substring(st);
            rows = copyManager.copyIn(copySQL, new StringReader(csv));
            // 执行MERGE操作
            try (Statement stmt = connection.createStatement()) {
                String mergeSql = "INSERT INTO  " + origTable +
                        " SELECT * FROM " + tmpTable +
                        "ON CONFLICT (\"timeStamp\",tag) DO UPDATE SET " +
                        "value = EXCLUDED.value,status = EXCLUDED.status ";
                int rowsAffected = stmt.executeUpdate(mergeSql);
                System.out.println("Merged " + rowsAffected + " rows");
            }
            connection.commit();
        } catch (Exception ex2) {
            ex2.printStackTrace();
            connection.rollback();
        }
        return rows;
    }

    @Test
    public void testPgConnectionUnWrap() throws SQLException {
        JdbcTemplate template = jdbcTemplate();
        Connection connection = template.getDataSource().getConnection();
        System.out.println(connection.getClass().getName());//com.zaxxer.hikari.pool.HikariProxyConnection
        connection = connection.unwrap(Connection.class);
        System.out.println(connection.getClass().getName());//org.postgresql.jdbc.PgConnection
    }

    private static JdbcTemplate jdbcTemplate() {
        HikariConfig config = new HikariConfig();
        config.setUsername("postgres");
        config.setPassword("postgres");
        config.setSchema("public");
        // connection pool configurations
        config.setConnectionTestQuery("SELECT 1"); // validation query
        config.setJdbcUrl("jdbc:postgresql://100.100.100.20:31014/postgres");
        config.setAutoCommit(true);
        HikariDataSource dataSource = new HikariDataSource(config); // create datasource
        return new JdbcTemplate(dataSource);
    }
}
