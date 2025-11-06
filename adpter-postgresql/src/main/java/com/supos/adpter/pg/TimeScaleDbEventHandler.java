package com.supos.adpter.pg;

import cn.hutool.core.lang.Pair;
import cn.hutool.system.SystemUtil;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.StreamHandler;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.adpater.historyquery.*;
import com.supos.common.annotation.Description;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SaveDataDto;
import com.supos.common.dto.SimpleUnsInfo;
import com.supos.common.event.*;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.DbTableNameUtils;
import com.supos.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.core.BaseConnection;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.supos.adpter.pg.PostgresqlEventHandler.*;

@Slf4j
public class TimeScaleDbEventHandler extends PostgresqlBase implements TimeSequenceDataStorageAdapter {
    DefaultHistoryQueryService defaultHistoryQueryService;

    public TimeScaleDbEventHandler(JdbcTemplate jdbcTemplate, IUnsDefinitionService unsDefinitionService) {
        super(jdbcTemplate);
        log.info("sinkVersion: {}", sinkVersion);
        defaultHistoryQueryService = new DefaultHistoryQueryService(jdbcTemplate, unsDefinitionService, name()) {
            @Override
            protected String escape(String name) {
                return '"' + name + '"';
            }

            @Override
            protected String buildHistoryQuerySQL(String ct, String qos, List<Select> selects, String whereSql, HistoryQueryParams params) {
                return TimeScaleDbEventHandler.this.buildHistoryQuerySQL(unsDefinitionService, ct, qos, selects, whereSql, params);
            }

            @Override
            protected String buildGetNearestSQL(String alias, String ctField, boolean lessThanDate, String date) {
                CreateTopicDto def = unsDefinitionService.getDefinitionByAlias(alias);
                String table = def.getTable();
                String dbTable = currentSchema + "." + escape(table), CT = escape(ctField);
                String select, op;
                if (lessThanDate) {
                    select = " = (SELECT MAX(";
                    op = " < '";
                } else {
                    select = " = (SELECT MIN(";
                    op = " > '";
                }
                StringBuilder s = new StringBuilder(256).append("select * from ").append(dbTable);
                s.append(" where ");
                String tbf = def.getTbFieldName();
                String extFilter = "";
                if (tbf != null) {
                    extFilter = " \"" + tbf + "\" = " + def.getId() + " and ";
                    s.append(extFilter);
                }
                s.append(CT).append(select).append(CT).append(") FROM ").append(dbTable)
                        .append(" WHERE ").append(extFilter).append(CT).append(op).append(date).append("')");
                return s.toString();
            }
        };
    }

    @Override
    public String name() {
        return "TimeScaleDBAdapter";
    }

    @Override
    public SrcJdbcType getJdbcType() {
        return SrcJdbcType.TimeScaleDB;
    }

    @Override
    public StreamHandler getStreamHandler() {
        return new TimeScaleStreamHandler();
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public String execSQL(String sql) {
        if (sql.toLowerCase().startsWith("select")) {
            List rs = jdbcTemplate.query(sql, new ColumnMapRowMapper());
            return JsonUtil.toJson(rs);
        } else {
            jdbcTemplate.execute(sql);
            return "ok";
        }
    }

    /**
     * SELECT
     * time_bucket('5m', _ct, '10s') AS ts,
     * first(t0,_ct) FILTER(WHERE t0 IS NOT NULL)  ,
     * last(t0,_ct)  ,
     * AVG(t0),sum(t0),min(t0),max(t0)
     * FROM public.dddd_00906721e779632aa8f51f7e4e50f1f7
     * group by ts order by ts;
     *
     * @param params 查询参数
     * @return
     */
    @Override
    public HistoryQueryResult queryHistory(HistoryQueryParams params) {
        return defaultHistoryQueryService.queryHistory(params);
    }

    protected String buildHistoryQuerySQL(IUnsDefinitionService unsDefinitionService,
                                          final String ct, final String qos, List<Select> selects, String whereSql, HistoryQueryParams params) {
        StringBuilder sql = new StringBuilder(256);
        sql.append("select ");
        boolean aggregation = selects.get(0).getFunction() != null;
        if (aggregation) {
            //time_bucket('5m', _ct, '10s') AS "#ts"
            IntervalWindow window = params.getIntervalWindow();
            String interval = window.getInterval(), offset = window.getOffset();
            sql.append(" time_bucket ('").append(interval).append("', \"").append(ct).append("\"");
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(offset)) {
                sql.append(",'").append(offset).append('\'').append("::INTERVAL");
            }
            sql.append(") AS \"#ts\", ");
        } else {
            sql.append('"').append(ct).append("\",");
        }
        for (Select select : selects) {
            SelectFunction function = select.getFunction();
            if (function != null) {
                sql.append(function.name()).append('(');
            }
            sql.append('"').append(select.getColumn()).append('"');
            if (function != null) {
                if (function == SelectFunction.First || function == SelectFunction.Last) {
                    sql.append(',').append('"').append(ct).append('"');
                }
                sql.append(')');
            }
            sql.append(" AS \"").append(select.selectName()).append('"').append(',');
        }
        if (!aggregation && qos != null) {
            sql.append('"').append(qos).append('"');
        } else {
            sql.setCharAt(sql.length() - 1, ' ');
        }
        String alias = selects.get(0).getTable();
        CreateTopicDto def = unsDefinitionService.getDefinitionByAlias(alias);
        String tbf = def.getTbFieldName();
        String tableName = alias, extFilter = null;
        if (tbf != null) {
            extFilter = " \"" + tbf + "\" = " + def.getId() + " ";
            tableName = def.getTable();
        }
        sql.append(" FROM ").append('"').append(tableName).append('"').append(' ');
        if (whereSql != null) {
            sql.append(whereSql);
            if (extFilter != null) {
                sql.append(" AND ").append(extFilter);
            }
        } else if (extFilter != null) {
            sql.append(" WHERE ").append(extFilter);
        }
        if (aggregation) {
            sql.append(" GROUP BY \"#ts\" ");
        }
        sql.append(" order by ").append(aggregation ? "\"#ts\"" : '"' + ct + '"')
                .append(params.isAscOrder() ? " ASC" : " DESC")
                .append(" LIMIT ").append(params.getLimit()).append(" OFFSET ").append(params.getOffset());
        return sql.toString();
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(9)
    @Description("uns.create.task.name.tmsc")
    public void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        CreateTopicDto[] topics;
        if (event.getSource() != this && ArrayUtils.isNotEmpty(topics = event.topics.get(SrcJdbcType.TimeScaleDB))) {
            Map<String, TableInfo> tableInfoMap = listTableInfos(topics);
            super.doTx(() -> batchCreateTables(topics, tableInfoMap));
        }
    }

    @EventListener(classes = RemoveTimeScaleTopicsEvent.class)
    @Order(9)
    void onRemoveTopicsEvent(RemoveTimeScaleTopicsEvent event) {
        List<String> sqls = new ArrayList<>();
        if (!CollectionUtils.isEmpty(event.getStandard())) {
            for (SimpleUnsInfo simpleUns : event.getStandard()) {
                StringBuilder sql = new StringBuilder("delete from ");
                sql.append(simpleUns.getTableName()).append(" where ").append(Constants.SYSTEM_SEQ_TAG).append("=").append(simpleUns.getId());
                sqls.add(sql.toString());
            }
        }
        if (!CollectionUtils.isEmpty(event.getNonStandard())) {
            for (SimpleUnsInfo simpleUns : event.getNonStandard()) {
//                String table = DbTableNameUtils.getFullTableName(simpleUns.getTableName());
                String sql = String.format("drop table if exists %s.\"%s\"", currentSchema, simpleUns.getAlias());
                sqls.add(sql);
            }
        }
        if (!sqls.isEmpty()) {
            log.debug("TimeScaleDB 删除：{}", sqls);
            jdbcTemplate.batchUpdate(sqls.toArray(new String[0]));
        }
    }

    private static final int sinkVersion = SystemUtil.getInt("sink_version", 1);

    @EventListener(classes = SaveDataEvent.class)
    @Order(9)
    void onSaveData(SaveDataEvent event) {
        if (SrcJdbcType.TimeScaleDB == event.jdbcType && ArrayUtils.isNotEmpty(event.topicData) && event.getSource() != this) {
            switch (sinkVersion) {
                case 2:
                    saveBatchV2(event);
                    break;
                case 1:
                    saveBatchV1(event);
                    break;
            }
        }
    }

    private void saveBatchV2(SaveDataEvent event) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            BaseConnection pgConn = (BaseConnection) connection.unwrap(Connection.class);
            for (SaveDataDto dto : event.topicData) {
                //受限于SQL 1M 的限制, 1万一批；实际可以10万~50万一批写入csv文件
                List<List<Map<String, Object>>> listList = Lists.partition(dto.getList(), 10000);
                for (List<Map<String, Object>> list : listList) {
                    try {
                        saveByCsvCopy(pgConn, dto.getCreateTopicDto(), list);
                    } catch (Exception e) {
                        log.warn("saveBatchV2 Fail: {} {}", dto.getTable(), e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveBatchV1(SaveDataEvent event) {
        ArrayList<Pair<SaveDataDto, String>> SQLs = new ArrayList<>(2 * event.topicData.length);
        try {
            for (SaveDataDto dto : event.topicData) {
                String table = DbTableNameUtils.getFullTableName(dto.getTable());

                List<List<Map<String, Object>>> listList = Lists.partition(dto.getList(), 1000);
                for (List<Map<String, Object>> list : listList) {
                    String saveOrUpdateSQL = getInsertSQL(list, table, dto, event.duplicateIgnore);
                    SQLs.add(Pair.of(dto, saveOrUpdateSQL));
                }
            }

        } catch (Throwable ex) {
            log.error("tsdb onSaveData SQLErr", ex);
        }

        List<List<Pair<SaveDataDto, String>>> segments = Lists.partition(SQLs, Constants.SQL_BATCH_SIZE);
        for (List<Pair<SaveDataDto, String>> sqlPairList : segments) {
            String[] sqlArray = new String[sqlPairList.size()];
            for (int i = 0; i < sqlPairList.size(); i++) {
                sqlArray[i] = sqlPairList.get(i).getValue();
            }
            try {
                jdbcTemplate.batchUpdate(sqlArray);
            } catch (DuplicateKeyException e1) {
                // 使用 on conflict update重试
                log.warn("PgTimeScale 写入失败, 主键冲突， 使用on conflict update重试: {}", e1.getMessage());
                try {
                    List<String> retrySqlList = buildRetrySqlArray(sqlPairList);
                    jdbcTemplate.batchUpdate(retrySqlList.toArray(new String[0]));
                    log.info("PgTimeScale retry success!");
                } catch (Exception rex) {
                    log.error("PgTimeScale写入 重试失败:", rex);
                }

            } catch (Exception ex) {
                log.error("PgTimeScale 写入失败", ex);
            }
        }
    }

    private List<String> buildRetrySqlArray(List<Pair<SaveDataDto, String>> sqlPairList) {
        List<String> sqlList = new ArrayList<>();
        for (Pair<SaveDataDto, String> pair : sqlPairList) {
            String table = DbTableNameUtils.getFullTableName(pair.getKey().getCreateTopicDto().getTable());
            String[] pks = pair.getKey().getCreateTopicDto().getPrimaryField();
            StringBuilder builder = new StringBuilder(pair.getValue());
            FieldDefine[] columns = pair.getKey().getCreateTopicDto().getFields();
            if (pks != null && pks.length > 0) {
                builder.append("ON CONFLICT(");
                for (String f : pks) {
                    builder.append('"').append(f).append("\",");
                }
                builder.setCharAt(builder.length() - 1, ')');
                builder.append(" do update set ");
                for (FieldDefine define : columns) {
                    if (!define.isUnique()) {
                        String f = define.getName();
                        builder.append('"').append(f).append("\"=COALESCE(EXCLUDED.\"").append(f).append("\",")
                                .append(table).append('.').append('"').append(f).append("\"),");
                    }
                }
                builder.setCharAt(builder.length() - 1, ' ');
            }
            sqlList.add(builder.toString());
        }
        return sqlList;
    }

    /*static List<String> getSaveOrUpdateSQLs(List<Map<String, Object>> list, String table, SaveDataDto saveDataDto) {

        Set<String> pks = saveDataDto.getFieldDefines().getUniqueKeys();
        if (pks == null || pks.isEmpty()) {
            return List.of(getInsertSQL(list, table, saveDataDto, "true"));
        }
        Map<String, Object>[] array = list.toArray(new Map[0]);
        final boolean onUpdate = array[0].containsKey(Constants.FIRST_MSG_FLAG);
        FieldDefine[] columns = saveDataDto.getCreateTopicDto().getFields();
        ArrayList<String> sqls = new ArrayList<>(array.length);
        list = new ArrayList<>(array.length);
        ArrayList<Map<String, Object>> saveOrUpdates = null;
        for (Map<String, Object> bean : array) {
            Object mergeFlag = bean.get(Constants.MERGE_FLAG);
            if (mergeFlag == null) {
                list.add(bean);
                continue;
            }
            if (mergeFlag instanceof Number number && number.intValue() != 1) {
                if (!onUpdate) {
                    if (saveOrUpdates == null) {
                        saveOrUpdates = new ArrayList<>(128);
                    }
                    saveOrUpdates.add(bean);
                } else {
                    list.add(bean);
                }
                continue;
            }
            StringBuilder whereExpression = new StringBuilder(255);
            StringBuilder builder = new StringBuilder(255);
            builder.append("UPDATE ").append(table).append(" SET ");
            boolean hasField = false;
            for (FieldDefine define : columns) {
                String f = define.getName();
                Object val = bean.get(f);
                val = getFieldValue(define.getType(), val);
                if (pks.contains(f)) {
                    whereExpression.append('"').append(f).append("\"='").append(val).append("' AND ");
                } else if (val != null) {
                    hasField = true;
                    builder.append('"').append(f).append('"').append('=').append('\'').append(val).append("',");
                }
            }
            if (hasField) {
                builder.setCharAt(builder.length() - 1, ' ');
                builder.append("WHERE ").append(whereExpression, 0, whereExpression.length() - 5);

                sqls.add(builder.toString());
            } else {
                list.add(bean);
            }
        }
        if (!list.isEmpty()) {
            sqls.add(0, getInsertSQL(list, table, saveDataDto, !onUpdate));
        }
        if (saveOrUpdates != null) {
            sqls.add(0, getInsertSQL(saveOrUpdates, table, saveDataDto, false));
        }
        return sqls;
    }*/

    @EventListener(classes = UpdateInstanceEvent.class)
    @Order(9)
    void onUpdateInstanceEvent(UpdateInstanceEvent event) {
        List<CreateTopicDto> topicList = event.topics;
        if (!CollectionUtils.isEmpty(topicList)) {
            CreateTopicDto[] topics = topicList.stream().filter(t -> Boolean.TRUE.equals(t.getFieldsChanged()) && t.getDataSrcId() == SrcJdbcType.TimeScaleDB).toArray(CreateTopicDto[]::new);
            if (event.getSource() != this && ArrayUtils.isNotEmpty(topics)) {
                Map<String, TableInfo> tableInfoMap = listTableInfos(topics);
                super.doTx(() -> batchCreateTables(topics, tableInfoMap));
            }
        }
    }

    void batchCreateTables(CreateTopicDto[] topics, Map<String, TableInfo> tableInfoMap) {
        // 批量执行 TimeScaleDB 建表
        ArrayList<String> createTableSQLs = new ArrayList<>(topics.length);
        HashSet<String> tables = new HashSet<>(tableInfoMap.size());
        for (CreateTopicDto dto : topics) {
            String tableName = dto.getTable();
            String quotationTableName = DbTableNameUtils.getFullTableName(tableName);
            String createTableSQL = getCreateTableSQL(dto, quotationTableName, dto.getFields());

            int dot = tableName.indexOf('.');
            String dbName = this.currentSchema;
            if (dot > 0) {
                dbName = tableName.substring(0, dot);
                tableName = tableName.substring(dot + 1);
            }
            if (!tables.add(tableName)) {//表名去重
                log.debug("表名已处理：{}, uns={}", tableName, dto.getAlias());
                continue;
            }
            TableInfo tableInfo = tableInfoMap.get(tableName);
            final int ch = checkTableModify(dto, dbName, tableName, createTableSQLs, tableInfo);

            // CREATE INDEX idx_ct_BRIN_demoseq ON public.demoseq  USING BRIN ("_ct");
//            if (ch == MDF_TYPE_CHANGED && dto.getTbFieldName() == null) {
//                createTableSQLs.add("DROP INDEX if exists idx_ct_BRIN_" + tableName);
//            }
            if (ch == MDF_NEW_TABLE || ch == MDF_TYPE_CHANGED) {

                createTableSQLs.add(createTableSQL);
                String ct = dto.getTimestampField();
                createTableSQLs.add("SELECT create_hypertable('" + dbName + ".\"" + tableName + "\"', '" + ct + "',chunk_time_interval => INTERVAL '7 day')");
//                createTableSQLs.add("CREATE INDEX idx_ct_BRIN_" + tableName +
//                        " ON " + dbName + "." + quotationTableName + " USING BRIN (\"" + ct + "\")");
            }
        }
        log.debug("PgTimeScale CreateTable: {} {}", createTableSQLs.size(), createTableSQLs);
        List<List<String>> segments = Lists.partition(createTableSQLs, Constants.SQL_BATCH_SIZE);
        for (List<String> sqlList : segments) {
            try {
                jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
            } catch (Exception ex) {
                log.error("PgCreateTable Error: " + sqlList, ex);
                throw ex;
            }
        }
    }


    @EventListener(classes = QueryDataEvent.class)
    @Order(9)
    void onQueryData(QueryDataEvent event) {
        CreateTopicDto topic = event.getTopicDto();
        if (topic != null && SrcJdbcType.TimeScaleDB == topic.getDataSrcId() && event.getSource() != this) {
            StringBuilder sts = new StringBuilder(256);

            String tableName = topic.getTable();
            String quotationTableName = DbTableNameUtils.getFullTableName(tableName);
            sts.append("select *").append(" from ").append(quotationTableName);
            if (!CollectionUtils.isEmpty(event.getEqConditions())) {
                sts.append(" where ");
                String tbValue = topic.getTbFieldName();
                if (tbValue != null) {
                    sts.append('"').append(tbValue).append("\" = ").append(topic.getId()).append(" AND ");
                }
                for (int i = 0; i < event.getEqConditions().size(); i++) {
                    sts.append(event.getEqConditions().get(i).getFieldName()).append(" = ").append(event.getEqConditions().get(i).getValue()).append("");
                    if (i < event.getEqConditions().size() - 1) {
                        sts.append(" and ");
                    }
                }

            }

            List<Map<String, Object>> values = jdbcTemplate.queryForList(sts.toString());
            event.setValues(values);
        }
    }

    @EventListener(classes = QueryLastMsgEvent.class)
    void onQueryLastMsgEvent(QueryLastMsgEvent event) {
        CreateTopicDto topic = event.uns;
        if (getJdbcType() == topic.getDataSrcId() && event.getSource() != this) {
            StringBuilder sql = new StringBuilder(256);

            String tableName = topic.getTable();
            final String ct = topic.getTimestampField();
            sql.append("select *").append(" from ").append(currentSchema).append(".\"").append(tableName).append("\"");
            String tbValue = topic.getTbFieldName();
            if (tbValue != null) {
                sql.append(" where \"").append(tbValue).append("\" = ").append(topic.getId()).append(" ");
            }
            sql.append(" ORDER BY \"")
                    .append(ct)
                    .append("\" DESC LIMIT 1");
            List<Map<String, Object>> values;
            try {
                values = jdbcTemplate.queryForList(sql.toString());
            } catch (Exception ex) {
                log.warn("查询最新数据失败,尝试建表: " + tableName, ex);
                CreateTopicDto[] topics = new CreateTopicDto[]{topic};
                Map<String, TableInfo> tableInfoMap = listTableInfos(topics);
                batchCreateTables(topics, tableInfoMap);
                return;
            }
            if (!values.isEmpty()) {
                Map<String, Object> msg = values.get(0);
                Object tm = msg.get(ct);
                if (tm instanceof Timestamp timestamp) {
                    event.setMsgCreateTime(timestamp.getTime());
                    event.setLastMessage(msg);
                }
            }
        }
    }

    @EventListener(classes = BatchQueryLastMsgVqtEvent.class)
    void onBatchQueryLastMsgVqtEvent(BatchQueryLastMsgVqtEvent event) {
        List<Long> ids = event.unsIds;
        StringBuilder sql = new StringBuilder(256);
        final String ct = Constants.SYS_FIELD_CREATE_TIME;
        sql.append("SELECT DISTINCT ON (tag) tag ,")
                .append(" \"").append(ct).append("\",")
                .append(" \"").append("quality").append("\",")
                .append(" \"").append("value").append("\"")
                .append(" FROM ").append("public").append(".\"").append(event.tableName).append("\"")
                .append(" WHERE tag in (").append(StringUtils.join(ids,",")).append(")")
                .append(" ORDER BY tag, \"").append(ct).append("\"")
                .append(" DESC");

        List<Map<String, Object>> values = jdbcTemplate.queryForList(sql.toString());
        event.setValues(values);
    }
}

