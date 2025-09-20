package com.supos.adpter.pg;

import com.google.common.collect.Lists;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.DbTableNameUtils;
import com.supos.common.utils.PostgresqlTypeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static com.supos.common.utils.DateTimeUtils.getDateTimeStr;
@Slf4j
public class PostgresqlBase {
    @Getter
    final JdbcTemplate jdbcTemplate;
    final String currentSchema;
    final TransactionTemplate transactionTemplate;

    PostgresqlBase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        DataSource dataSource = jdbcTemplate.getDataSource();
        DataSourceProperties dataSourceProperties = (DataSourceProperties) dataSource;
        currentSchema = dataSourceProperties.getSchema();
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DataSourceProperties getDataSourceProperties() {
        return (DataSourceProperties) jdbcTemplate.getDataSource();
    }

    protected void doTx(java.lang.Runnable dbTask) {
        transactionTemplate.executeWithoutResult(transactionStatus -> dbTask.run());
    }

    static class TableInfo {
        TreeSet<String> pkSet = new TreeSet<>(); //主键
        String[] pks;
        final Map<String, String> fieldTypes;

        TableInfo() {
            this.fieldTypes = new HashMap<>(16);
        }
    }

    Map<String, TableInfo> listTableInfos(CreateTopicDto[] topics) {
        if (topics == null || topics.length == 0) {
            return Collections.emptyMap();
        }
        HashMap<String, Set<String>> schemaTables = new HashMap<>(2);
        for (CreateTopicDto dto : topics) {
            String tableName = dto.getTable();
            int dot = tableName.indexOf('.');
            String dbName = this.currentSchema;
            if (dot > 0) {
                dbName = tableName.substring(0, dot);
                tableName = tableName.substring(dot + 1);
            }
            schemaTables.computeIfAbsent(dbName, k -> new LinkedHashSet<>()).add(tableName);
        }
        Map<String, TableInfo> rs = null;
        for (Map.Entry<String, Set<String>> entry : schemaTables.entrySet()) {
            String schema = entry.getKey();
            Set<String> tables = entry.getValue();
            Map<String, TableInfo> tableInfoMap = listTableInfos(jdbcTemplate, schema, tables);
            if (rs != null) {
                rs.putAll(tableInfoMap);
            } else {
                rs = tableInfoMap;
            }
        }
        if (rs == null) {
            rs = Collections.emptyMap();
        }
        return rs;
    }

    static Map<String, TableInfo> listTableInfos(JdbcTemplate template, String schema, Collection<String> tablesSet) {
        Map<String, TableInfo> allMap = new TreeMap<>();
        for (List<String> tables : Lists.partition(new ArrayList<>(tablesSet), 999)) {
            StringBuilder sql = new StringBuilder(128 + tables.size() * 64);
            sql.append("SELECT table_name,column_name, udt_name FROM information_schema.columns WHERE table_name IN(");
            for (String tableName : tables) {
                sql.append('\'').append(tableName).append("',");
            }
            sql.setCharAt(sql.length() - 1, ')');
            sql.append(" and table_schema = '").append(schema).append('\'').append(" order by table_name,column_name");
            template.query(sql.toString(), rs -> {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    String col = rs.getString(2), type = rs.getString(3);
                    String fieldType = type.toLowerCase();//
                    fieldType = PostgresqlTypeUtils.dbType2FieldTypeMap.get(fieldType);
                    allMap.computeIfAbsent(tableName, k -> new TableInfo()).fieldTypes.put(col, fieldType);
                }
                return allMap;
            });
            String pkQuery = " SELECT \n" +
                    "  tc.table_name,\n" +
                    "  kcu.column_name,\n" +
                    "  CASE WHEN tc.constraint_type = 'PRIMARY KEY' THEN true ELSE false END AS is_primary\n" +
                    "FROM  information_schema.table_constraints tc  \n" +
                    " JOIN information_schema.key_column_usage kcu \n" +
                    "  ON tc.constraint_name = kcu.constraint_name\n" +
                    "WHERE tc.table_name in (";
            sql = new StringBuilder(pkQuery.length() + 64 * tablesSet.size());
            sql.append(pkQuery);
            for (String tableName : tables) {
                sql.append('\'').append(tableName).append("',");
            }
            sql.setCharAt(sql.length() - 1, ')');
            template.query(sql.toString(), rs -> {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    String col = rs.getString(2);
                    boolean isPk = rs.getBoolean(3);
                    TableInfo info = allMap.get(tableName);
                    if (info != null && isPk) {
                        info.pkSet.add(col);
                    }
                }
                return Collections.emptyMap();
            });
            for (TableInfo info : allMap.values()) {
                info.pks = !info.pkSet.isEmpty() ? info.pkSet.toArray(new String[0]) : null;
            }
        }
        return allMap;
    }

    public void saveByCsvCopy(BaseConnection connection, CreateTopicDto dto, Collection<Map<String, Object>> list) throws SQLException, IOException {
        CopyManager copyManager = new CopyManager(connection);
        String table = DbTableNameUtils.getFullTableName(dto.getTable());
        String copySQL = getCsvCopySQL(table, dto);
        String csv = getCsv(list, dto);
        long rows, affected = -1;
        try {
            rows = copyManager.copyIn(copySQL, new StringReader(csv));
        } catch (Exception ex) {
            long[] rowsAndUpdates = saveByTempTable(connection, copySQL, copyManager, dto, csv);
            rows = rowsAndUpdates[0];
            affected = rowsAndUpdates[1];
        }
        log.debug("saveByCsvCopy[{}, {}]", rows, affected);
    }

    static String getCsvCopySQL(String table, CreateTopicDto dto) {
        StringBuilder builder = new StringBuilder(255);
        builder.append("COPY ").append(table).append(" (");
        FieldDefine[] columns = dto.getFields();
        for (FieldDefine fieldDefine : columns) {
            builder.append("\"").append(fieldDefine.getName()).append("\",");
        }
        builder.setCharAt(builder.length() - 1, ')');
        builder.append("  FROM STDIN WITH (FORMAT CSV) ");
        return builder.toString();
    }

    static Object getFieldValue(FieldType fieldType, Object val) {
        if (val == null) {
            return val;
        }
        if (fieldType == FieldType.DATETIME && val instanceof Long) {
            val = getDateTimeStr(val);
        } else if (fieldType == FieldType.STRING) {
            // postgresql 的单引号处理方式：特殊语法，双单引号代替单个单引号
            val = val.toString().replace("'", "''");
        }
        return val;
    }

    static String getCsv(Collection<Map<String, Object>> list, CreateTopicDto dto) {
        StringBuilder builder = new StringBuilder(2048);
        FieldDefine[] columns = dto.getFields();
        for (Map<String, Object> bean : list) {
            for (FieldDefine define : columns) {
                String f = define.getName();
                Object val = bean.get(f);
                if (val != null) {
                    val = getFieldValue(define.getType(), val);
                    builder.append(val).append(",");
                } else {
                    builder.append("0,");
                }
            }
            builder.setCharAt(builder.length() - 1, '\n');
        }
        return builder.toString();
    }

    private static long[] saveByTempTable(Connection connection, String copySQL, CopyManager copyManager, CreateTopicDto dto, String csv) {
        long[] rows = new long[2];
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
            copySQL = "COPY " + tmpTable + copySQL.substring(st);
            rows[0] = copyManager.copyIn(copySQL, new StringReader(csv));
            // 执行MERGE操作
            try (Statement stmt = connection.createStatement()) {
                StringBuilder mergeSql = new StringBuilder(256);
                mergeSql.append("INSERT INTO  ").append(origTable).append(" SELECT * FROM ").append(tmpTable).append(' ');
                String[] pks = dto.getPrimaryField();
                if (pks == null) {
                    pks = new String[0];
                }
                if (pks.length > 0) {
                    mergeSql.append("ON CONFLICT(");
                    for (String f : pks) {
                        mergeSql.append('"').append(f).append("\",");
                    }
                    mergeSql.setCharAt(mergeSql.length() - 1, ')');
                    mergeSql.append(" DO UPDATE SET ");
                    for (FieldDefine define : dto.getFields()) {
                        if (!define.isUnique()) {
                            String f = define.getName();
                            mergeSql.append('"').append(f).append("\"=EXCLUDED.\"").append(f).append("\",");
                        }
                    }
                    mergeSql.setCharAt(mergeSql.length() - 1, ' ');
                }
                rows[1] = stmt.executeUpdate(mergeSql.toString());
            }
            connection.commit();
        } catch (SQLException | IOException ex2) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return rows;
    }
}
