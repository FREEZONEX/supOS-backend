package com.supos.common.adpater.historyquery;

import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import com.supos.common.exception.BuzException;
import com.supos.common.service.IUnsDefinitionService;
import com.supos.common.utils.DateTimeUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
public abstract class DefaultHistoryQueryService {
    final JdbcTemplate jdbcTemplate;
    final IUnsDefinitionService unsDefinitionService;
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    private final String sourceName;

    public DefaultHistoryQueryService(JdbcTemplate jdbcTemplate, IUnsDefinitionService unsDefinitionService, String srcName) {
        this.jdbcTemplate = jdbcTemplate;
        this.unsDefinitionService = unsDefinitionService;
        this.sourceName = srcName;
    }

    static class SelectInfo {
        final CreateTopicDto uns;
        final String whereSql;
        List<Select> normalSelect = new LinkedList<>();
        List<Select> funcSelect = new LinkedList<>();

        SelectInfo(CreateTopicDto uns, String whereSql) {
            this.uns = uns;
            this.whereSql = whereSql;
        }
    }

    public HistoryQueryResult queryHistory(HistoryQueryParams params) {
        Set<ConstraintViolation<HistoryQueryParams>> checkResult = validator.validate(params);
        if (!checkResult.isEmpty()) {
            throw new BuzException(checkResult.toString());
        }
        Select[] paramsSelect = params.getSelect();
        if (paramsSelect == null || paramsSelect.length == 0) {
            return null;
        }
        HashSet<String> nonExists = new HashSet<>();
        LinkedHashMap<String, SelectInfo> tables = new LinkedHashMap<>(paramsSelect.length);
        for (Select select : paramsSelect) {
            String table = select.table;
            CreateTopicDto uns = unsDefinitionService.getDefinitionByAlias(table);
            if (uns != null) {
                String whereSql = whereToString(params.where, uns);
                SelectInfo selectTable = tables.computeIfAbsent(table, k -> new SelectInfo(uns, whereSql));
                if (select.getFunction() != null) {
                    selectTable.funcSelect.add(select);
                } else {
                    selectTable.normalSelect.add(select);
                }
            } else {
                nonExists.add(table);
            }
        }
        List<FieldsAndData> dataResults = new ArrayList<>(paramsSelect.length);
        for (SelectInfo selectInfo : tables.values()) {
            boolean hasTable = query(selectInfo.uns, selectInfo.normalSelect, selectInfo.whereSql, params, nonExists, dataResults);
            if (hasTable) {
                query(selectInfo.uns, selectInfo.funcSelect, selectInfo.whereSql, params, nonExists, dataResults);
            }
        }
        HistoryQueryResult result = new HistoryQueryResult();
        result.setCode(200);
        result.setMessage("ok");
        result.setResults(dataResults);
        result.setNotExists(nonExists);
        return result;
    }

    private String whereToString(Where where, CreateTopicDto uns) {
        final boolean hasAnd = !CollectionUtils.isEmpty(where.and), hasOr = !CollectionUtils.isEmpty(where.or);
        if (hasAnd || hasOr) {
            StringBuilder s = new StringBuilder(256);
            if (hasAnd) {
                addToSql(uns, s, where.and, "AND");
            }
            if (hasAnd && hasOr) {
                s.append(" AND ");
            }
            if (hasOr) {
                addToSql(uns, s, where.or, "OR");
            }
            return s.toString();
        }
        return "";
    }

    private void addToSql(CreateTopicDto uns, StringBuilder s, List<WhereCondition> list, String op) {
        LinkedList<String> cond = new LinkedList<>();
        for (WhereCondition condition : list) {
            String cs = conditionString(uns, condition);
            if (cs != null) {
                cond.add(cs);
            }
        }
        if (!cond.isEmpty()) {
            if (s.isEmpty()) {
                s.append(" where ");
            }
            Iterator<String> itr = cond.listIterator();
            s.append('(');
            s.append(itr.next());
            while (itr.hasNext()) {
                s.append(' ').append(op).append(' ').append(itr.next());
            }
            s.append(')');
        }
    }


    private String conditionString(CreateTopicDto uns, WhereCondition condition) {
        String name = condition.name, value = condition.value;
        if (name.equals(Constants.SYS_FIELD_CREATE_TIME) || "_ct".equals(name)) {// 时间戳字段
            name = uns.getTimestampField();
        } else if (!uns.getFieldDefines().getFieldsMap().containsKey(name)) {// 其他查询字段
            int dot = name.indexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                String table = name.substring(0, dot).trim();
                String field = name.substring(dot + 1).trim();
                FieldDefine fv;
                if (table.equals(uns.getAlias()) && (fv = uns.getFieldDefines().getFieldsMap().get(field)) != null) {
                    if (fv.getType() == FieldType.DATETIME) {
                        Instant utcDate = DateTimeUtils.parseDate(value);
                        if (utcDate != null) {
                            condition.setTime(utcDate);
                            value = utcDate.toString();
                        } else {
                            return null;
                        }
                    }
                    return String.format("%s %s '%s'", escape(field), condition.op.op, value);
                }
            }
            return null;
        }
        return String.format("%s %s '%s'", escape(name), condition.op.op, value);
    }

    private boolean query(CreateTopicDto uns, List<Select> selects, String whereSql, HistoryQueryParams params, HashSet<String> nonExists, List<FieldsAndData> dataResults) {
        if (selects.isEmpty()) {
            return true;
        }
        final String table = selects.get(0).getTable();
        Set<String> fields = uns.getFieldDefines().getFieldsMap().keySet();
        Iterator<Select> itr = selects.iterator();
        while (itr.hasNext()) {
            Select select = itr.next();
            String column = select.getColumn();
            if (!fields.contains(column)) {
                itr.remove();
                nonExists.add(table + "." + column);
            }
        }
        if (selects.isEmpty()) {
            return true;
        }
        String ct = uns.getTimestampField(), qos = uns.getQualityField();
        List<FieldsAndData> data = list(ct, qos, selects, whereSql, params, nonExists);
        if (data == null) {
            nonExists.add(table);
            return false;
        } else {
            dataResults.addAll(data);
            return true;
        }
    }

    protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
        return new HistoryQueryColumnMapRowMapper();
    }

    protected List<FieldsAndData> list(final String ct, final String qos,
                                       List<Select> selects, String whereSql,
                                       HistoryQueryParams params, HashSet<String> nonExists) {
        String sql = buildHistoryQuerySQL(ct, qos, selects, whereSql, params);
        List<Map<String, Object>> values = Collections.emptyList();
        log.info("{} 历史查询：{}", sourceName, sql);
        RowMapper<Map<String, Object>> rowMapper = getColumnMapRowMapper();
        try {
            values = jdbcTemplate.query(sql, rowMapper);
        } catch (Exception ex) {
            log.warn("历史查询错误:" + sql, ex);
            // TODO 细分异常：表不存在--返回null; 字段不存在--查表结构有哪些字段，做比对，不存在的 add 到 nonExists,都不存在则返回
            int origSize = selects.size();
            handleQueryException(ex, selects, nonExists);
            if (selects.isEmpty()) {
                return null;
            }
            if (selects.size() != origSize) {
                sql = buildHistoryQuerySQL(ct, qos, selects, whereSql, params);
                values = jdbcTemplate.query(sql, rowMapper);
            }
        }
        final int LIMIT = params.getLimit();
        final boolean hasNext = values.size() == LIMIT;
        List<FieldsAndData> list = new ArrayList<>(selects.size());
        for (Select select : selects) {
            List<String> fields = new ArrayList<>(3);
            fields.add(Constants.SYS_FIELD_CREATE_TIME);
            fields.add(select.getColumn());
            fields.add(Constants.QOS_FIELD);
            FillStrategy fillStrategy = params.fillStrategy;
            List<Object[]> datas = Collections.emptyList();
            if (!CollectionUtils.isEmpty(values)) {
                final boolean sample = select.function != null && params.intervalWindow != null
                        && fillStrategy != null && fillStrategy != FillStrategy.None;// 参考 supos oodm
                if (sample) {
                    datas = getInsertDatas(params, select, ct, qos, values, LIMIT, new AtomicReference<>());
                } else {
                    datas = getNormalDatas(select.selectName(), qos, values, LIMIT);
                }
            }
            FieldsAndData data = new FieldsAndData();
            data.setTable(select.getTable());
            data.setFunction(select.getFunction());
            data.setHasNext(hasNext);
            data.setFields(fields);
            data.setDatas(datas);
            list.add(data);
        }
        return list;
    }

    private static final long QOS_NO_DATA = 0x20000000000000L;//supos 质量码：无数据（查询的指定时间点或统计查询时无数据）

    private List<Object[]> getNormalDatas(final String selectName, final String qos,
                                          final List<Map<String, Object>> values, final int LIMIT) {
        List<Object[]> datas = new ArrayList<>(LIMIT);
        boolean hasQos = true;
        for (Map<String, Object> row : values) {
            String ts = row.values().iterator().next().toString();// 首个字段 固定为时间戳
            Object v = row.get(selectName);
            Object q = 0;
            if (hasQos) {
                q = row.get(qos);
                if (q == null) {
                    q = 0;
                    hasQos = false;
                }
            }
            datas.add(new Object[]{ts, v, q});
        }
        return datas;
    }

    private List<Object[]> getInsertDatas(HistoryQueryParams params, final Select select,
                                          final String ctField, final String qosField,
                                          final List<Map<String, Object>> values,
                                          final int LIMIT,
                                          AtomicReference<Map<String, Object>> insertData) {
        final boolean asc = params.ascOrder;
        final long START_TIME = asc ? params.minTime : params.maxTime;
        long endTime = asc ? params.maxTime : params.minTime;
        final long END_TIME = endTime == 0 ? (asc ? Long.MAX_VALUE : Long.MIN_VALUE) : endTime;
        final long WINDOW = params.intervalWindow != null ? params.intervalWindow.intervalMills : 0;
        FillStrategy fillStrategy = params.fillStrategy;
        final boolean prevInsert = fillStrategy == FillStrategy.Previous || fillStrategy == FillStrategy.Linear;
        List<Object[]> datas = new ArrayList<>(LIMIT);
        int prevIndex = -1;
        Object prevData = null;
        final double B = asc ? 1.0 : -1.0;
        final int BL = asc ? 1 : -1;
        boolean hasQos = true;
        Object lastQos = null;
        loopRows:
        for (Map<String, Object> row : values) {
            final String ts = row.values().iterator().next().toString();// 首个字段 固定为时间戳
            Object q = 0;
            if (hasQos) {
                q = row.get(qosField);
                if (q == null) {
                    q = 0;
                    hasQos = false;
                }
            }
            Object v = row.get(select.selectName());
            lastQos = q;
            long curTime = DateTimeUtils.parseDate(ts).toEpochMilli();
            double Index = (B * (curTime - START_TIME) / WINDOW);
            int index = (int) Math.floor(Index);
            Object insertQos = q;
            if (prevIndex == -1 && index > 0) {
                // 说明起始时间所在窗口内 没有数据，如果是前向插值，则需要往前查找最近的一条数据
                String table = params.select[0].table;
                if (prevInsert) {
                    if (asc) {
                        Map<String, Object> insertRow = queryInsertRow(insertData,
                                () -> buildGetNearestSQL(table, ctField, true, ts));
                        if (insertRow == null) {
                            insertQos = QOS_NO_DATA;//找不到时用 Bad 质量码
                        } else {
                            prevData = insertRow.get(select.column);
                            insertQos = insertRow.get(qosField);
                        }
                    } else {
                        prevData = v;
                    }
                } else {
                    if (!asc) {
                        Map<String, Object> insertRow = queryInsertRow(insertData,
                                () -> buildGetNearestSQL(table, ctField, false, ts));
                        if (insertRow == null) {
                            insertQos = QOS_NO_DATA;
                        } else {
                            prevData = insertRow.get(select.column);
                            insertQos = insertRow.get(qosField);
                        }
                    } else {
                        prevData = v;
                    }
                }
            }
            final int insertSize = Math.min((prevIndex >= 0 ? index - prevIndex - 1 : index), LIMIT - datas.size());
            for (int i = 0; i < insertSize; i++) {
                long time = START_TIME + BL * (prevIndex + i + 1) * WINDOW;
                if ((asc && time > END_TIME) || (!asc && time < END_TIME)) {
                    break loopRows;
                }
                String dataStr = DateTimeUtils.dateTimeUTC(time);
                datas.add(new Object[]{dataStr, prevData, insertQos});
            }
            if (datas.size() < LIMIT) {
                prevData = v;
                prevIndex = index;
                long time = START_TIME + BL * index * WINDOW;
                datas.add(new Object[]{DateTimeUtils.dateTimeUTC(time), v, q});
                if (datas.size() == LIMIT) {
                    break;
                }
            } else {
                break;
            }
        }
        for (int i = 0, LEFT = LIMIT - datas.size(); i < LEFT; i++) {
            long time = START_TIME + BL * (prevIndex + i + 1) * WINDOW;
            if ((asc && time > END_TIME) || (!asc && time < END_TIME)) {
                break;
            }
            String dataStr = DateTimeUtils.dateTimeUTC(time);
            datas.add(new Object[]{dataStr, prevData, lastQos});
        }
        return datas;
    }

    private Map<String, Object> queryInsertRow(AtomicReference<Map<String, Object>> insertData, Supplier<String> sqlSupplier) {
        Map<String, Object> insertRow = insertData.get();
        if (insertRow == null) {
            String sql = sqlSupplier.get();
            RowMapper<Map<String, Object>> rowMapper = getColumnMapRowMapper();
            try {
                List<Map<String, Object>> insertValues = jdbcTemplate.query(sql, rowMapper);
                if (!CollectionUtils.isEmpty(insertValues)) {
                    insertData.set(insertRow = insertValues.get(0));
                }
                log.info("查询插值：sql={}, insertRow={}", sql, insertRow);
            } catch (Exception ex) {
                log.warn("查询插值失败:" + sql, ex);
            }
        }
        return insertRow;
    }

    protected void handleQueryException(Exception exception, List<Select> selects, HashSet<String> nonExists) {
    }

    protected abstract String escape(String name);

    protected abstract String buildHistoryQuerySQL(final String ctField, final String qosField,
                                                   List<Select> selects, String whereSql,
                                                   HistoryQueryParams params);

    protected abstract String buildGetNearestSQL(String alias, String ctField, boolean lessThanDate, String date);
}
