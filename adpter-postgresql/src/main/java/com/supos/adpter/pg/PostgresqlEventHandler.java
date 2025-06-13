package com.supos.adpter.pg;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.annotation.Description;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SaveDataDto;
import com.supos.common.dto.TopologyLog;
import com.supos.common.enums.FieldType;
import com.supos.common.event.*;
import com.supos.common.utils.DbTableNameUtils;
import com.supos.common.utils.I18nUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.supos.common.utils.DateTimeUtils.getDateTimeStr;

@Slf4j
public class PostgresqlEventHandler extends PostgresqlBase implements DataStorageAdapter {

    public PostgresqlEventHandler(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public String name() {
        return "PostgresqlAdapter";
    }

    @Override
    public SrcJdbcType getJdbcType() {
        return SrcJdbcType.Postgresql;
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(7)
    @Description("uns.create.task.name.pg")
    void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        CreateTopicDto[] topics = event.topics.get(SrcJdbcType.Postgresql);
        if (ArrayUtils.isNotEmpty(topics) && event.getSource() != this) {
            CreateTopicDto[] filterTopics = Arrays.stream(topics)
                    .filter(t -> t.getDataType() != Constants.ALARM_RULE_TYPE).toArray(CreateTopicDto[]::new);
            if (filterTopics.length > 0) {
                Map<String, TableInfo> tableInfoMap = listTableInfos(filterTopics);
                super.doTx(() -> batchCreateTables(filterTopics, tableInfoMap));
            }
        }
    }

    @EventListener(classes = UpdateInstanceEvent.class)
    @Order(7)
    void onUpdateInstanceEvent(UpdateInstanceEvent event) {
        CreateTopicDto[] topics = event.topics.stream().filter(t -> Boolean.TRUE.equals(t.getFieldsChanged()) && t.getDataSrcId() == SrcJdbcType.Postgresql).toArray(CreateTopicDto[]::new);
        if (event.getSource() != this && ArrayUtils.isNotEmpty(topics)) {
            Map<String, TableInfo> tableInfoMap = listTableInfos(topics);
            super.doTx(() -> batchCreateTables(topics, tableInfoMap));
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(7)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (event.jdbcType != SrcJdbcType.Postgresql) {
            return;
        }
        List<String> sqls = Collections.EMPTY_LIST;
        Collection<String> tables = event.topics.values().stream().filter(ins -> ins.isRemoveTableWhenDeleteInstance())
                .map(in -> in.getTableName()).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(tables)) {
            sqls = new ArrayList<>(tables.size());
            for (String table : tables) {
                sqls.add("drop table if exists " + DbTableNameUtils.getFullTableName(table));
            }
        }
        if (sqls.size() > 0) {
            log.debug("PostGreSQL 删除：{}", sqls);
            jdbcTemplate.batchUpdate(sqls.toArray(new String[0]));
        }
    }

    private String[] getDbAndTable(String tableName) {
        int dot = tableName.indexOf('.');
        String dbName = this.currentSchema;
        if (dot > 0) {
            dbName = tableName.substring(0, dot);
            tableName = tableName.substring(dot + 1);
        }
        return new String[]{dbName, tableName};
    }

    @EventListener(classes = SaveDataEvent.class)
    @Order(7)
    void onSaveData(SaveDataEvent event) {
        if (SrcJdbcType.Postgresql == event.jdbcType && event.getSource() != this) {
            ArrayList<Pair<CreateTopicDto, String>> SQLs = new ArrayList<>(2 * event.topicData.length);
            for (SaveDataDto dto : event.topicData) {
                String table = DbTableNameUtils.getFullTableName(dto.getTable());
                for (List<Map<String, Object>> list : Lists.partition(dto.getList(), Constants.SQL_BATCH_SIZE)) {
                    String insertSQL = getInsertSQL(list, table, dto, false);
                    SQLs.add(Pair.of(dto.getCreateTopicDto(), insertSQL));
                }
                dto.getList().clear();
                dto.setList(null);
            }
            List<List<Pair<CreateTopicDto, String>>> segments = Lists.partition(SQLs, Constants.SQL_BATCH_SIZE);
            for (List<Pair<CreateTopicDto, String>> sqlPairList : segments) {
                List<String> sqlList = sqlPairList.stream().map(Pair::getValue).collect(Collectors.toList());
                log.debug("PgWrite: \n{}", sqlList);
                String[] sqlArray = sqlList.toArray(new String[0]);
                try {
                    jdbcTemplate.batchUpdate(sqlArray);
                } catch (Exception ex) {
                    Set<Long> unsIds = sqlPairList.stream().map(s -> s.getKey().getId()).collect(Collectors.toSet());
                    TopologyLog.log(unsIds, TopologyLog.Node.DATA_PERSISTENCE, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.db.pg"));
                    CreateTopicDto[] dtos = sqlPairList.stream().map(Pair::getKey)
                            .toArray(CreateTopicDto[]::new);
                    log.error("PG写入失败: {}, {}", ex.getMessage(), sqlList);
                    try {
                        Map<String, TableInfo> tableInfoMap = listTableInfos(dtos);
                        batchCreateTables(dtos, tableInfoMap);
                        jdbcTemplate.batchUpdate(sqlArray);
                        log.debug("retry success!");
                    } catch (Exception rex) {
                        log.error("PG写入 re失败:" + unsIds, rex);
                    }
                }
            }
        }
    }

    void batchCreateTables(CreateTopicDto[] topics, Map<String, TableInfo> tableInfoMap) {
        if (topics == null || topics.length == 0) {
            return;
        }
        // 批量执行 postgresql 建表
        List<String> createTableSQLs = new ArrayList<>(topics.length);
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
                continue;
            }
            TableInfo tableInfo = tableInfoMap.get(tableName);
            int ch = checkTableModify(dto, dbName, tableName, createTableSQLs, tableInfo);
            if (ch == MDF_NEW_TABLE || ch == MDF_TYPE_CHANGED) {
                createTableSQLs.add(createTableSQL);
            }
        }
        log.debug("PgCreateTable: {} {}", createTableSQLs.size(), createTableSQLs);
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

    static final int MDF_NEW_TABLE = 0;
    static final int MDF_TYPE_CHANGED = 1;
    static final int MDF_ADD_OR_DEL = 2;
    static final int NO_CHANGE = 3;

    static int checkTableModify(CreateTopicDto dto,
                                String dbName,
                                String tableName,
                                List<String> alterTableSQLs,
                                TableInfo tableInfo) {
        if (tableInfo == null || CollectionUtils.isEmpty(tableInfo.fieldTypes)) {
            return MDF_NEW_TABLE;
        }
        Map<String, FieldDefine> curFieldTypes = Arrays.stream(dto.getFields()).collect(Collectors.toMap(FieldDefine::getName, d -> d));
        boolean hasTypeChanged = false;
        LinkedList<String> delFs = new LinkedList<>();
        for (Map.Entry<String, String> entry : tableInfo.fieldTypes.entrySet()) {
            String field = entry.getKey(), oldType = entry.getValue();
            FieldDefine curType = curFieldTypes.remove(field);
            String newType;
            if (curType == null) {
                delFs.add(field);
            } else if (oldType != null && !oldType.equals(curType.getType().getName())) {
                hasTypeChanged = true;
                log.debug("typeChange {}: {}->{}", field, oldType, getTypeDefineWithoutLen(curType));
                break;
            }
        }
        if (hasTypeChanged) {// 修改字段类型的情况则删除表
            String dropSQL = "drop table IF EXISTS \"" + dbName + "\".\"" + tableName + '"';
            alterTableSQLs.add(dropSQL);
            return MDF_TYPE_CHANGED;
        } else if (!delFs.isEmpty() || !curFieldTypes.isEmpty()) {
            // pg 删除或新增字段
            final StringBuilder alterSQL = new StringBuilder(128)
                    .append("ALTER TABLE \"")
                    .append(dbName).append("\".\"").append(tableName).append('"');

            Map<String, Set<String>> typeIds = null;
            for (Map.Entry<String, FieldDefine> entry : curFieldTypes.entrySet()) {
                FieldDefine def = entry.getValue();
                if (def.isUnique()) {
                    if (typeIds == null) {
                        typeIds = new HashMap<>(4);
                    }
                    typeIds.computeIfAbsent(def.getType().name, k -> new LinkedHashSet<>()).add(def.getName());
                }
            }
            for (String delF : delFs) {
                String rename = null;
                if (typeIds != null) {
                    String oldType = tableInfo.fieldTypes.get(delF);
                    Set<String> idNames = typeIds.get(oldType);
                    if (idNames != null && !idNames.isEmpty()) {
                        Iterator<String> itr = idNames.iterator();
                        rename = itr.next();
                        itr.remove();
                        curFieldTypes.remove(rename);
                    }
                }
                if (rename == null) {
                    alterSQL.append(" DROP IF EXISTS \"").append(delF).append("\",");
                } else {
                    String rmSql = "ALTER TABLE \"" +
                            dbName + "\".\"" + tableName + '"' +
                            " RENAME COLUMN \"" + delF + "\" TO \"" + rename + '"';
                    alterTableSQLs.add(rmSql);
                }
            }
            for (Map.Entry<String, FieldDefine> entry : curFieldTypes.entrySet()) {
                FieldDefine def = entry.getValue();
                String field = entry.getKey();
                String type = getTypeDefine(def);
                alterSQL.append(" ADD IF NOT EXISTS \"").append(field).append("\" ").append(type).append(",");
            }
            alterTableSQLs.add(alterSQL.substring(0, alterSQL.length() - 1));
            return MDF_ADD_OR_DEL;
        }
        return NO_CHANGE;
    }

    static String getTypeDefineWithoutLen(FieldDefine def) {
        String type = getTypeDefine(def, false).toLowerCase();
        int q = type.indexOf('(');
        if (q > 0) {
            type = type.substring(0, q);
        }
        return type;
    }

    static String getTypeDefine(FieldDefine def) {
        return getTypeDefine(def, true);
    }

    static String getTypeDefine(FieldDefine def, boolean procSerial) {
        String type = fieldType2DBTypeMap.get(def.getType().name);
        Integer len = def.getMaxLen();
        switch (def.getType()) {
            case STRING:
                if (len != null) {
                    type = "varchar(" + len + ")";
                } else {
                    String nameC = def.getName().toLowerCase();
                    if (nameC.contains("json")) {
                        type = "jsonb";
                    }
                }
                break;
            case DATETIME:
                type = "timestamptz(3)";
                break;
            case INT:
                if (procSerial && def.isUnique()) {
                    type = "serial";
                }
            case FLOAT:
            case LONG:
            case DOUBLE:
                if (procSerial && def.isUnique()) {
                    type = "bigserial";
                }
                break;
        }

        return type;
    }


    static final Map<String, String> fieldType2DBTypeMap;

    static {
        Map<String, String> _fieldType2DBTypeMap = new HashMap<>(8);
        // {"int", "long", "float", "string", "boolean", "datetime"}
        _fieldType2DBTypeMap.put(FieldType.INT.name, "int4");
        _fieldType2DBTypeMap.put(FieldType.LONG.name, "int8");
        _fieldType2DBTypeMap.put(FieldType.FLOAT.name, "float4");
        _fieldType2DBTypeMap.put(FieldType.DOUBLE.name, "float8");
        _fieldType2DBTypeMap.put(FieldType.STRING.name, "text");
        _fieldType2DBTypeMap.put(FieldType.BOOLEAN.name, "boolean");
        _fieldType2DBTypeMap.put(FieldType.DATETIME.name, "timestamptz");
        _fieldType2DBTypeMap.put(FieldType.BLOB.name, "varchar(512)");
        _fieldType2DBTypeMap.put(FieldType.LBLOB.name, "varchar(512)");
        fieldType2DBTypeMap = _fieldType2DBTypeMap;
    }


    static String getInsertSQL(Collection<Map<String, Object>> list, String table, SaveDataDto saveDataDto, boolean ignore) {
        /**
         * INSERT INTO test (id, name, email)
         * VALUES (1, 'Alice', 'alice@example.com')
         * ON CONFLICT (id) DO UPDATE
         * SET name = EXCLUDED.name,
         *     email = EXCLUDED.email;
         */
        StringBuilder builder = new StringBuilder(255);
        builder.append("INSERT INTO ").append(table).append(" (");

        FieldDefine[] columns = saveDataDto.getCreateTopicDto().getFields();
        for (FieldDefine fieldDefine : columns) {
            builder.append("\"").append(fieldDefine.getName()).append("\",");
        }
        builder.setCharAt(builder.length() - 1, ')');
        builder.append(" VALUES ");

        String[] pks = saveDataDto.getCreateTopicDto().getPrimaryField();
        if (pks == null) {
            pks = new String[0];
        }
        LinkedHashMap<String, Map<String, Object>> ids;
        if (pks.length > 0 && list.size() > 1) {
            ids = new LinkedHashMap<>(list.size());
            for (Map<String, Object> bean : list) {
                String idStr = "";
                if (pks.length == 1) {
                    Object id = bean.get(pks[0]);
                    if (id == null) {
                        // id 为空，当做不重复处理
                        ids.put(String.valueOf(System.identityHashCode(bean)), bean);
                        continue;
                    }
                    idStr = String.valueOf(id);
                } else {
                    StringBuilder idBd = new StringBuilder();
                    for (String k : pks) {
                        idBd.append(bean.get(k)).append('`');
                    }
                    idStr = idBd.toString();
                }
                if (!"null".equals(idStr) && !"".equals(idStr)) {
                    ids.put(idStr, bean);
                }
            }
            list = ids.values();
        }
        for (Map<String, Object> bean : list) {
            builder.append('(');
            for (FieldDefine define : columns) {
                String f = define.getName();
                Object val = bean.get(f);
                if (val != null) {
                    val = getFieldValue(define.getType(), val);
                    builder.append('\'').append(val).append("',");
                } else {
                    builder.append("DEFAULT,");
                }
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(',');
        }
        builder.setCharAt(builder.length() - 1, ' ');
        if (pks != null && pks.length > 0) {
            builder.append("ON CONFLICT(");
            for (String f : pks) {
                builder.append('"').append(f).append("\",");
            }
            builder.setCharAt(builder.length() - 1, ')');
            if (ignore) {
                builder.append(" do nothing");
            } else {
                builder.append(" do update set ");
                for (FieldDefine define : columns) {
                    if (!define.isUnique()) {
                        String f = define.getName();
                        builder.append('"').append(f).append("\"=EXCLUDED.\"").append(f).append("\",");
                    }
                }
                builder.setCharAt(builder.length() - 1, ' ');
            }
        }
        String insertSQL = builder.toString();
        return insertSQL;
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

    static String getCreateTableSQL(CreateTopicDto dto, String tableName, FieldDefine[] fields) {
        StringBuilder builder = new StringBuilder(128);
        builder.append("create table IF NOT EXISTS ").append(tableName).append(" (");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        final boolean isShareTable = dto.getTbFieldName() != null;
        for (FieldDefine def : fields) {
            String name = def.getName();
            String type = (def.getType() == FieldType.STRING && isShareTable) ? "text" : getTypeDefine(def);
            if (def.isUnique()) {
                ids.add(name);
            }
            builder.append("\"").append(name).append("\" ").append(type);
            if (def.isUnique()) {
                builder.append(" NOT NULL ");
            } else if (type.startsWith("timestamp") && name.equals(Constants.SYS_SAVE_TIME)) {
                builder.append(" DEFAULT now() ");
            }
            builder.append(',');
        }
        if (!ids.isEmpty()) {
            String table = DbTableNameUtils.getCleanTableName(tableName);
            int x = table.lastIndexOf('/');
            if (x > 0) {
                table = table.substring(x + 1);
            }
            builder.append("CONSTRAINT \"pk_").append(table).append("_").append(IdUtil.getSnowflake().nextIdStr()).append("\" PRIMARY KEY (");
            for (String pk : ids) {
                builder.append("\"").append(pk).append("\",");
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(' ');
        }
        builder.setCharAt(builder.length() - 1, ')');
        String sql = builder.toString();
        return sql;
    }

    @EventListener(classes = QueryDataEvent.class)
    @Order(9)
    void onQueryData(QueryDataEvent event) {
        CreateTopicDto topic = event.getTopicDto();
        if (topic != null && SrcJdbcType.Postgresql == topic.getDataSrcId() && event.getSource() != this) {
            StringBuilder sts = new StringBuilder(256);

            String tableName = topic.getTable();
            String quotationTableName = DbTableNameUtils.getFullTableName(tableName);
            sts.append("select *").append(" from ").append(quotationTableName);
            if (!CollectionUtils.isEmpty(event.getEqConditions())) {
                sts.append(" where ");
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

            String[] dbTab = getDbAndTable(topic.getTable());
            String tableName = dbTab[1];
            final String ct = topic.getTimestampField();
            sql.append("select *").append(" from ").append(dbTab[0]).append(".\"").append(tableName).append("\" ORDER BY \"")
                    .append(ct)
                    .append("\" DESC LIMIT 1");

            List<Map<String, Object>> values = jdbcTemplate.queryForList(sql.toString());
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

}
