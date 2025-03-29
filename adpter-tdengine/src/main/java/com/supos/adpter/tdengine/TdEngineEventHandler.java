package com.supos.adpter.tdengine;

import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.adpater.StreamHandler;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.annotation.DateTimeConstraint;
import com.supos.common.annotation.Description;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.StreamWindowType;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SaveDataEvent;
import com.supos.common.utils.ExpressionUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.supos.common.utils.DateTimeUtils.getDateTimeStr;

@Slf4j
public class TdEngineEventHandler implements TimeSequenceDataStorageAdapter {
    @Override
    public String name() {
        return "TdEngineAdapter";
    }

    @Override
    public SrcJdbcType getJdbcType() {
        return SrcJdbcType.TdEngine;
    }

    @Override
    public DataSourceProperties getDataSourceProperties() {
        return dataSourceProperties;
    }

    @Override
    public StreamHandler getStreamHandler() {
        return streamHandler;
    }

    @Override
    public String execSQL(String sql) {
        if (sql.toLowerCase().startsWith("select")) {
            List<Map> rs = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Map.class));
            return JsonUtil.toJson(rs);
        } else {
            jdbcTemplate.execute(sql);
            return "ok";
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final TdStreamHandler streamHandler;
    private final DataSourceProperties dataSourceProperties;
    private final String dbName;

    public TdEngineEventHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        dataSourceProperties = (DataSourceProperties) jdbcTemplate.getDataSource();
        dbName = dataSourceProperties.getSchema();
        streamHandler = new TdStreamHandler(jdbcTemplate);
    }


    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(5)
    @Description("uns.create.task.name.td")
    void onCreateTable(BatchCreateTableEvent event) {
        CreateTopicDto[] topics = event.topics.get(SrcJdbcType.TdEngine);
        if (topics != null && event.getSource() != this) {
            batchCreateTables(topics, true);
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(5)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (SrcJdbcType.TdEngine == event.jdbcType && event.getSource() != this) {
            Map<String, SimpleUnsInstance> tableInstances = new LinkedHashMap<>(event.topics.size());
            for (SimpleUnsInstance ins : event.topics.values()) {
                if (ins.isRemoveTableWhenDeleteInstance()) {
                    tableInstances.put(ins.getTableName(), ins);
                }
            }
            if (!CollectionUtils.isEmpty(tableInstances)) {
                ArrayList<String> dropTableSQLs = new ArrayList<>(tableInstances.size());
                for (Map.Entry<String, SimpleUnsInstance> entry : tableInstances.entrySet()) {
                    String table = entry.getKey();
                    SimpleUnsInstance ins = entry.getValue();
                    String[] dbTab = getDbAndTable(table);
                    String dbName = dbTab[0];
                    int dataType = ins.getDataType();
                    if (dataType == Constants.CALCULATION_HIST_TYPE) {
                        dropTableSQLs.add("DROP STREAM if exists `" + table + "`");
                    }
                    dropTableSQLs.add(String.format("DROP TABLE IF EXISTS %s.`%s`", dbName, table));
                }
                log.debug("TdEngine 删除：{}", dropTableSQLs);
                jdbcTemplate.batchUpdate(dropTableSQLs.toArray(new String[dropTableSQLs.size()]));
            }
            if (!CollectionUtils.isEmpty(event.modelTopics)) {
                ArrayList<String> dropSQLs = new ArrayList<>(event.modelTopics.size());
                for (String topic : event.modelTopics) {
                    dropSQLs.add("drop table if exists `" + dbName + "`.`" + topic + '`');
                }
                jdbcTemplate.batchUpdate(dropSQLs.toArray(new String[0]));
                log.debug("TdEngine 删除模型：{}", dropSQLs);
            }
        }
    }

    static class TopicSaveInfo {
        final SaveDataDto dto;
        final String table;
        final String insertSQL;

        public TopicSaveInfo(SaveDataDto dto, String table, String insertSQL) {
            this.dto = dto;
            this.table = table;
            this.insertSQL = insertSQL;
        }

        public String toString() {
            return insertSQL;
        }
    }

    private static final String SUPER_TABLE_PREV = "sup_";

    @EventListener(classes = SaveDataEvent.class)
    @Order(5)
    void onSaveData(SaveDataEvent event) {
        if (SrcJdbcType.TdEngine == event.jdbcType && event.getSource() != this) {
            ArrayList<TopicSaveInfo> SQLs = new ArrayList<>(event.topicData.length);
            LinkedHashMap<String, SaveDataDto> dataBySuperTable = new LinkedHashMap<>();
            for (SaveDataDto dto : event.topicData) {
                String[] dbTab = getDbAndTable(dto.getTable());
                String tableName = dbTab[1];
                String modelId = dto.getCreateTopicDto().getModelId();
                List<Map<String, Object>> list = dto.getList();
                if (modelId != null) {// 写超级表
                    for (Map<String, Object> map : list) {
                        map.put("tbname", tableName);
                    }
                    dataBySuperTable.compute(SUPER_TABLE_PREV + modelId, (k, v) -> {
                        if (v != null) {
                            v.getList().addAll(list);
                        } else {
                            v = dto.clone();
                            if (v.getTables() == null) {
                                v.setTables(new HashSet<>(16));
                            }
                        }
                        return v;
                    }).getTables().add(tableName);
                } else {// 写普通表
                    dataBySuperTable.compute(tableName, (k, v) -> {
                        if (v != null) {
                            v.getList().addAll(list);
                        } else {
                            v = dto.clone();
                        }
                        return v;
                    });
                }
            }
            for (Map.Entry<String, SaveDataDto> entry : dataBySuperTable.entrySet()) {
                String tableName = entry.getKey();
                SaveDataDto dto = entry.getValue();
                for (List<Map<String, Object>> list : Lists.partition(dto.getList(), Constants.SQL_BATCH_SIZE)) {
                    String insertSQL = getInsertSQL(list, dbName, tableName, dto.getFieldDefines());
                    SQLs.add(new TopicSaveInfo(dto, tableName, insertSQL));
                }
            }
            List<List<TopicSaveInfo>> segments = Lists.partition(SQLs, Constants.SQL_BATCH_SIZE);
            for (List<TopicSaveInfo> sqlPairList : segments) {
                List<String> sqlList = sqlPairList.stream().map(t -> t.insertSQL).collect(Collectors.toList());
                log.debug("TdEngineWrite: \n{}", sqlList);
                String[] insertSQLs = sqlList.toArray(new String[0]);
                try {
                    jdbcTemplate.batchUpdate(insertSQLs);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        Throwable nextCause = cause.getCause();
                        if (nextCause != null) {
                            cause = nextCause;
                        } else {
                            break;
                        }
                    }
                    String errMsg = cause != null ? cause.getMessage() : ex.getMessage();
                    if (errMsg.contains("(0x231d)")) {
                        log.debug("td 不可用!");
                        TopologyLog.log(TopologyLog.Node.DATA_PERSISTENCE, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.db.td"));
                        return;
                    }
                    Map<String, TopicSaveInfo> topics = sqlPairList.stream()
                            .collect(Collectors.toMap(t -> t.dto.getTopic(), t -> t));
                    TopologyLog.log(topics.keySet(), TopologyLog.Node.DATA_PERSISTENCE, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.db.td"));

                    // 表不存在，重新show tables, 尝试建表
                    ArrayList<CreateTopicDto> mayBeShouldCreates = new ArrayList<>(topics.size());
                    for (TopicSaveInfo info : topics.values()) {
                        mayBeShouldCreates.add(info.dto.getCreateTopicDto());
                    }
                    try {
                        procTableConflict(topics.values());
                        batchCreateTables(mayBeShouldCreates.toArray(new CreateTopicDto[0]), false);
                        jdbcTemplate.batchUpdate(insertSQLs);
                        log.debug("TD retry success!");
                        continue;
                    } catch (Exception re) {
                        log.warn("TD ReTryErr:{}", re.getMessage());
                    }
                    log.error("TdEngine 写入re失败: {}, ex={}", sqlList, errMsg);
                }
            }

        }
    }

    void procTableConflict(Collection<TopicSaveInfo> topicSaveInfos) throws Exception {
        StringBuilder sts = new StringBuilder(256);
        sts.append("SELECT table_name,stable_name FROM information_schema.ins_tables WHERE  db_name = '").append(this.dbName)
                .append("' and table_name in(");
        HashMap<String, String> superTableMap = new HashMap<>(topicSaveInfos.size());
        for (TopicSaveInfo info : topicSaveInfos) {
            Set<String> curTables = info.dto.getTables();
            String superTable = info.table;
            if (curTables != null && !curTables.isEmpty()) {
                for (String t : curTables) {
                    superTableMap.put(t, superTable);
                    sts.append('\'').append(t).append("',");
                }
            } else {
                superTableMap.put(superTable, null);
                sts.append('\'').append(superTable).append("',");
            }
        }
        sts.setCharAt(sts.length() - 1, ')');
        String querySql = sts.toString();
        String dropSQL = jdbcTemplate.query(querySql, rs -> {
            sts.delete(0, sts.length());
            while (rs.next()) {
                String table = rs.getString(1);
                String superTable = rs.getString(2);
                String currentSTable = superTableMap.get(table);
                if (!Objects.equals(currentSTable, superTable)) {
                    if (sts.length() > 0) {
                        sts.append(',');
                    } else {
                        sts.append("drop table ");
                    }
                    sts.append(" if exists `").append(dbName).append("`.`").append(table).append('`');
                }
            }
            return sts.toString();
        });
        log.debug("删除冲突表：{}", dropSQL);
        if (dropSQL != null && !dropSQL.isEmpty()) {
            jdbcTemplate.execute(dropSQL);
        }
    }

    void batchCreateTables(CreateTopicDto[] topics, boolean createChildTable) {
        HashSet<String> tables = new HashSet<>(topics.length + 128);
        for (CreateTopicDto dto : topics) {
            String tableName = dto.getTable();
            String modelId = dto.getModelId();
            if (modelId != null) {
                String supperTable = SUPER_TABLE_PREV + modelId;
                tables.add(supperTable);
            } else {
                if (dto.getReferTable() != null) {
                    tables.add(dto.getReferTable());
                }
                String refModel = dto.getReferModelId();
                if (refModel != null) {
                    tables.add(SUPER_TABLE_PREV + refModel);
                }
            }
            tables.add(tableName);
        }
        final String dbName = this.dbName;
        Map<String, TableInfo> tableInfoMap = listTableInfos(jdbcTemplate, dbName, tables);
        ArrayList<String> createTableSQLs = new ArrayList<>(2 * topics.length);
        int size = Math.max(32, topics.length);
        HashMap<String, List<String>> bySuperTables = new HashMap<>(size);
        for (CreateTopicDto dto : topics) {
            String tableName = dto.getTable();

            String modelId = dto.getModelId();
            final boolean isStream = dto.getStreamOptions() != null;
            if (!isStream && modelId != null) {
                String supperTable = SUPER_TABLE_PREV + modelId;
                boolean first = bySuperTables.get(supperTable) == null;
                bySuperTables.computeIfAbsent(supperTable, k -> new ArrayList<>()).add(tableName);
                if (first) {
                    addDDL(dto.getFields(), dbName, supperTable, createTableSQLs, TABLE_TYPE_SUPER, tableInfoMap.get(supperTable));
                }
            } else if (modelId == null) {
                if (isStream) {
                    createTableSQLs.add("DROP STREAM if exists `" + tableName + "`");
                    TableInfo curTable = tableInfoMap.get(tableName);
                    if (curTable != null) {
                        Map<String, String> curFieldTypes = Arrays.stream(dto.getFields()).filter(
                                d -> d.getName().equals(Constants.SYS_FIELD_CREATE_TIME) || !d.getName().startsWith(Constants.SYSTEM_FIELD_PREV)
                        ).collect(Collectors.toMap(FieldDefine::getName, d -> d.getType().getName()));
                        if (!curTable.fieldTypes.equals(curFieldTypes)) {
                            createTableSQLs.add("DROP TABLE IF EXISTS `" + dbName + "`.`" + tableName + "`");
                        }
                    }
                    String referTable = dto.getReferTable();
                    if (dto.getRefFields() != null && referTable != null) {
                        String refModel = dto.getReferModelId();
                        if (refModel != null) {// 流计算引用了子表
                            String refSupperTable = SUPER_TABLE_PREV + refModel;
                            addDDL(dto.getRefFields(), dbName, refSupperTable, createTableSQLs, TABLE_TYPE_SUPER, tableInfoMap.get(refSupperTable));
                            bySuperTables.computeIfAbsent(refSupperTable, k -> new ArrayList<>()).add(referTable);
                            createChildTable = true;
                        } else { // 流计算引用了普通表
                            addDDL(dto.getRefFields(), dbName, referTable, createTableSQLs, TABLE_TYPE_NORMAL, tableInfoMap.get(referTable));
                        }
                    }
                    createTableSQLs.add(getCreateStreamSQL(dbName, tableName, dto));
                } else {
                    addDDL(dto.getFields(), dbName, tableName, createTableSQLs, TABLE_TYPE_NORMAL, tableInfoMap.get(tableName));
                }
            }
        }
        if (createChildTable) {
            for (Map.Entry<String, List<String>> entry : bySuperTables.entrySet()) {
                String supperTable = entry.getKey();
                List<List<String>> segments = Lists.partition(entry.getValue(), Constants.SQL_BATCH_SIZE);
                for (List<String> childTables : segments) {
                    StringBuilder createSQL = new StringBuilder(128).append("CREATE table ");
                    for (String tableName : childTables) {
                        createSQL.append(" if not exists `")
                                .append(dbName).append("`.`").append(tableName).append("` USING `")
                                .append(dbName).append("`.`").append(supperTable).append("` (`tbname`) TAGS('").append(tableName).append("')")
                        ;
                    }
                    createTableSQLs.add(createSQL.toString());
                }
            }
        }
        log.debug("TdCreateTable: {}", createTableSQLs);

        List<List<String>> segments = Lists.partition(createTableSQLs, Constants.SQL_BATCH_SIZE);
        for (List<String> sqlList : segments) {
            try {
                jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
            } catch (Exception ex) {
                log.error("TdCreateTable Error: " + sqlList, ex);
                throw ex;
            }
        }
    }

    private static void addDDL(FieldDefine[] fields, String dbName, String tableName, ArrayList<String> createTableSQLs, String tableType, TableInfo tableInfo) {

        Map<String, String> oldFieldTypes = Collections.emptyMap();
        String actualTableType = null;
        if (tableInfo != null) {
            oldFieldTypes = tableInfo.fieldTypes;
            actualTableType = tableInfo.tableType;
        }
        final boolean hasDrop;
        if (actualTableType != null && !actualTableType.equals(tableType)) {
            hasDrop = true;
            String dropSQL = "drop table IF EXISTS `" + dbName + "`.`" + tableName + '`';
            createTableSQLs.add(dropSQL);
        } else {
            hasDrop = false;
        }
        if (oldFieldTypes.isEmpty() || hasDrop) {
            createTableSQLs.add(getCreateTableSQL(dbName, tableName, fields, TABLE_TYPE_SUPER.equals(tableType)));
        } else {
            Map<String, FieldDefine> curFieldTypes = Arrays.stream(fields).collect(Collectors.toMap(FieldDefine::getName, d -> d));
            boolean hasTypeChanged = false;
            LinkedList<String> delFs = new LinkedList<>();
            for (Map.Entry<String, String> entry : oldFieldTypes.entrySet()) {
                String field = entry.getKey(), oldType = entry.getValue();
                FieldDefine curType = curFieldTypes.remove(field);
                if (curType == null) {
                    delFs.add(field);
                } else if (oldType != null && !curType.getType().name.equals(oldType)) {
                    hasTypeChanged = true;
                    break;
                }
            }
            if (hasTypeChanged) {// td 不支持修改字段类型，有这种情况则删除表
                String dropSQL = "drop table IF EXISTS `" + dbName + "`.`" + tableName + '`';
                createTableSQLs.add(dropSQL);
            } else if (!delFs.isEmpty() || !curFieldTypes.isEmpty()) {
                // td 删除或新增字段
                final String prev = "ALTER TABLE `" + dbName + "`.`" + tableName + '`';
                for (String delF : delFs) {
                    createTableSQLs.add(prev + " DROP COLUMN `" + delF + '`');
                }
                for (Map.Entry<String, FieldDefine> entry : curFieldTypes.entrySet()) {
                    FieldDefine def = entry.getValue();
                    String field = entry.getKey(), type = fieldType2DBTypeMap.get(def.getType().name);
                    type = getStringType(def, type);
                    createTableSQLs.add(prev + " ADD COLUMN `" + field + "` " + type);
                }
            }
        }
    }

    private static String getStringType(FieldDefine def, String type) {
        Integer len = def.getMaxLen();
        if (len != null && def.getType() == FieldType.STRING) {
            type = "NCHAR(" + len + ")";
        }
        return type;
    }

    private String[] getDbAndTable(String tableName) {
        int dot = tableName.indexOf('.');
        String dbName = this.dbName;
        if (dot > 0) {
            dbName = tableName.substring(0, dot);
            tableName = tableName.substring(dot + 1);
        }
        return new String[]{dbName, tableName};
    }

    static final Map<String, String> fieldType2DBTypeMap = new HashMap<>(8);
    static final Map<String, String> dbType2FieldTypeMap = new HashMap<>(8);

    static {
        // {"int", "long", "float", "string", "boolean", "datetime"}
        fieldType2DBTypeMap.put(FieldType.INT.name, "INT");
        fieldType2DBTypeMap.put(FieldType.LONG.name, "BIGINT");
        fieldType2DBTypeMap.put(FieldType.FLOAT.name, "FLOAT");
        fieldType2DBTypeMap.put(FieldType.DOUBLE.name, "DOUBLE");
        fieldType2DBTypeMap.put(FieldType.STRING.name, "NCHAR(255)");
        fieldType2DBTypeMap.put(FieldType.BOOLEAN.name, "BOOL");
        fieldType2DBTypeMap.put(FieldType.DATETIME.name, "TIMESTAMP");

        for (Map.Entry<String, String> entry : fieldType2DBTypeMap.entrySet()) {
            String ft = entry.getKey(), dt = entry.getValue();
            int q = dt.indexOf('(');
            if (q > 0) {
                dt = dt.substring(0, q);
            }
            dbType2FieldTypeMap.put(dt.toUpperCase(), ft);
        }
        dbType2FieldTypeMap.put("VARCHAR", FieldType.STRING.name);
    }


    private static String getCreateTableSQL(String db, String tableName, FieldDefine[] fields, boolean isSupperTable) {
        StringBuilder builder = new StringBuilder(128 + fields.length * 64);
        builder.append("create ").append(isSupperTable ? "STABLE" : "table")
                .append(" IF NOT EXISTS ").append(db).append(".`").append(tableName).append("` (");
        for (FieldDefine def : fields) {
            String type = fieldType2DBTypeMap.get(def.getType().name.toLowerCase());
            String name = def.getName();
            if (!"tbname".equals(name)) {
                builder.append('`').append(name).append("` ");
            } else {
                builder.append(name).append(' ');
            }
            type = getStringType(def, type);
            builder.append(type);
            builder.append(',');
        }
        builder.setCharAt(builder.length() - 1, ')');
        if (isSupperTable) {
            builder.append("TAGS (`tbname`  varchar(192) )");
        }
        return builder.toString();
    }

    static String getCreateStreamSQL(String db, String tableName, CreateTopicDto dto) {
        StringBuilder sql = new StringBuilder(256);
        sql.append("CREATE STREAM IF NOT EXISTS `").append(tableName).append("` ");
        StreamOptions options = dto.getStreamOptions();
        String trigger = options.getTrigger();
        if (StringUtils.hasText(trigger)) {
            sql.append(" TRIGGER ").append(trigger);
        }
        String waterMark = options.getWaterMark();
        if (StringUtils.hasText(waterMark)) {
            sql.append(" WATERMARK ").append(waterMark);
        }
        Boolean ignoreExpired = options.getIgnoreExpired(),
                fillHistory = options.getFillHistory(),
                ignoreUpdate = options.getIgnoreUpdate();
        StreamWindowType windowType = options.getWindow().getStreamWindowType();
        if (ignoreExpired != null && windowType != StreamWindowType.COUNT_WINDOW) {
            sql.append(" IGNORE EXPIRED ").append(ignoreExpired ? 1 : 0);
        }
        String deleteMark = options.getDeleteMark();
        if (StringUtils.hasText(deleteMark)) {
            sql.append(" DELETE_MARK ").append(deleteMark);
        }
        if (fillHistory != null) {
            sql.append(" FILL_HISTORY ").append(fillHistory ? 1 : 0);
        }
        if (ignoreUpdate != null) {
            sql.append(" IGNORE UPDATE ").append(ignoreUpdate ? 1 : 0);
        }
        sql.append(" INTO ").append(db).append(".`").append(tableName)
                .append("` AS SELECT _wstart as ").append(Constants.SYS_FIELD_CREATE_TIME);
        for (FieldDefine f : dto.getFields()) {
            if (!f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)) {
                String func = f.getIndex();
                int qs = func.indexOf('(');
                sql.append(',');
                if (qs > 0) {
                    int ed = func.indexOf(')', qs + 1);
                    String var = func.substring(qs + 1, ed).trim();
                    if (var.charAt(0) != '`') {
                        sql.append(func, 0, qs + 1).append('`').append(var).append("`)");
                    } else {
                        sql.append(func);
                    }
                } else {
                    sql.append(func);
                }
                sql.append(" AS `").append(f.getName()).append('`');
            }
        }
        sql.append(" FROM ").append(db).append(".`").append(dto.getReferTable()).append("` ");
        String where = options.getWhereCondition();
        String st = options.getStartTime(), ed = options.getEndTime();
        boolean hasWhere = StringUtils.hasText(where), hasSt = StringUtils.hasText(st), hasEd = StringUtils.hasText(ed);
        if (hasWhere || hasSt || hasEd) {
            sql.append(" WHERE ");
            if (hasWhere) {
                where = ExpressionUtils.encloseExpressionVars(where, '`');
                sql.append(where);
            }
            if (hasSt) {
                sql.append(hasWhere ? " and " : " ").append(Constants.SYS_FIELD_CREATE_TIME).append(" >= ")
                        .append('\'').append(DateTimeConstraint.parseDate(st)).append('\'');
            }
            if (hasEd) {
                sql.append(hasWhere || hasSt ? " and " : " ").append(Constants.SYS_FIELD_CREATE_TIME).append(" <= ")
                        .append('\'').append(DateTimeConstraint.parseDate(ed)).append('\'');
            }
        }
        sql.append(' ').append(options.getWindow().getOptionBean().toString());
        String having = options.getHavingCondition();
        if (StringUtils.hasText(having)) {
            having = ExpressionUtils.encloseExpressionVars(having, '`');
            sql.append(" HAVING ").append(having);
        }
        return sql.toString();
    }

    static String getInsertSQL(List<Map<String, Object>> list, String db, String table, FieldDefines fieldDefines) {
        /**
         * INSERT INTO test (ts, name, email)
         * VALUES ('2024-01-19 09:01:01.000', 'Alice', 'alice@example.com')
         */
        Map<String, Object> firstRow = list.get(0);
        StringBuilder builder = new StringBuilder(255);
        builder.append("INSERT INTO ").append(db).append(".`").append(table).append("` (");

        Map<String, FieldDefine> fieldsMap = fieldDefines.getFieldsMap();
        LinkedHashSet<String> columns = new LinkedHashSet<>(fieldsMap.size());
        for (Map.Entry<String, FieldDefine> entry : fieldsMap.entrySet()) {
            String col = entry.getKey();
            columns.add(col);
            builder.append("`").append(col).append("`,");
        }
        if (firstRow.containsKey("tbname")) {
            columns.add("tbname");
            builder.append("tbname").append(',');
        }

        String timeCol = "";
        String[] uks = fieldDefines.getUniqueKeys();
        if (uks != null && uks.length > 0 && !columns.contains(uks[0])) {
            columns.add(timeCol = uks[0]);
            builder.append("`").append(timeCol).append("`,");
        }
        builder.setCharAt(builder.length() - 1, ')');
        builder.append(" VALUES ");
        boolean firstErr = true;
        for (Map<String, Object> bean : list) {
            builder.append('(');
            for (String f : columns) {
                Object val = bean.get(f);
                if (val != null) {
                    FieldDefine define = fieldsMap.get(f);
                    if (define != null) {
                        FieldType fieldType = define.getType();
                        if (fieldType == FieldType.DATETIME && val instanceof Long) {
                            val = getDateTimeStr(val);
                        } else if (fieldType == FieldType.STRING) {
                            //TdEngine 的单引号处理方式：斜杠转义
                            val = val.toString().replace("'", "\\'");
                        }
                    }
                    builder.append('\'').append(val).append("',");
                } else {
                    if (firstErr) {
                        firstErr = false;
                        log.debug("Field404: {}.{}, id={}", table, f, timeCol);
                    }
                    if (Constants.SYS_SAVE_TIME.equals(f)) {
                        builder.append("NOW,");
                    } else {
                        builder.append("null,");
                    }
                }
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(',');
        }
        builder.setCharAt(builder.length() - 1, ' ');
        return builder.toString();
    }

    static final String TABLE_TYPE_SUPER = "SUPER_TABLE";
    static final String TABLE_TYPE_NORMAL = "NORMAL_TABLE";
    static final String TABLE_TYPE_CHILD = "CHILD_TABLE";

    static class TableInfo {
        final String tableType;
        final Map<String, String> fieldTypes;

        TableInfo(String tableType) {
            this.tableType = tableType;
            this.fieldTypes = new HashMap<>(16);
        }
    }

    static Map<String, TableInfo> listTableInfos(JdbcTemplate template, String db, Collection<String> tables) {
        StringBuilder sql = new StringBuilder(128 + tables.size() * 64);
        sql.append("SELECT table_name,table_type, col_name, col_type FROM information_schema.ins_columns WHERE table_name IN(");
        for (String tableName : tables) {
            sql.append('\'').append(tableName).append("',");
        }
        sql.setCharAt(sql.length() - 1, ')');
        sql.append(" and db_name = '").append(db).append('\'');

        return template.query(sql.toString(), rs -> {
            Map<String, TableInfo> map = new TreeMap<>();
            while (rs.next()) {
                String tableName = rs.getString(1), tableType = rs.getString(2);
                String col = rs.getString(3), type = rs.getString(4).toUpperCase();
                int qs = type.indexOf('(');
                if (qs > 0) {
                    type = type.substring(0, qs);
                }
                String fieldType = dbType2FieldTypeMap.get(type);
                map.computeIfAbsent(tableName, k -> new TableInfo(tableType)).fieldTypes.put(col, fieldType);
            }
            return map;
        });
    }
}
