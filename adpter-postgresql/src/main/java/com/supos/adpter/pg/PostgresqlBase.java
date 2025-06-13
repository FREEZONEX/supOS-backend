package com.supos.adpter.pg;

import com.google.common.collect.Lists;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.utils.PostgresqlTypeUtils;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.*;

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
}
