package com.supos.adpater.grafana;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.annotation.Description;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.CreateDashboardEvent;
import com.supos.common.event.EventBus;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.utils.GrafanaUtils;
import com.supos.common.utils.RuntimeUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GrafanaEventHandler {

    @Resource
    private SystemConfig systemConfig;

    @Order(300)
    @EventListener(classes = BatchCreateTableEvent.class)
    @Description("uns.create.task.name.dashboard")
    public void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        if (null == systemConfig.getContainerMap().get("grafana")) {
            log.debug(">>>>>>>>>当前系统未启用grafana服务，不执行批量创建grafana dashboard");
            return;
        }
        log.info(">>>>>> GrafanaEventHandler 批量创建事件,topic数量：{},flowName:{}", event.topics.size(), event.flowName);
        ThreadUtil.execute(() -> {
            for (Map.Entry<SrcJdbcType, CreateTopicDto[]> entry : event.topics.entrySet()) {
                try {
                    create(entry.getKey(), entry.getValue(), event.flowName, event.fromImport);
                } catch (Exception e) {
                    log.error("批量创建dashboard异常", e);
                }
            }
        });
        log.info(">>>>>> GrafanaEventHandler 批量创建事件,已完成,flowName:{}", event.flowName);
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    public void onRemoveTopics(RemoveTopicsEvent event) {
        if (null == systemConfig.getContainerMap().get("grafana")) {
            log.debug(">>>>>>>>>当前系统未启用grafana服务，不执行批量删除grafana dashboard");
            return;
        }
        if (CollectionUtil.isEmpty(event.topics)) {
            return;
        }
        ThreadUtil.execute(() -> {
            try {
                //table = alias
                event.topics.values().stream().filter(even -> ObjectUtil.isNotNull(event.jdbcType)).map(in -> in.getTableName()).forEach(table -> {
                    String uid = GrafanaUtils.getDashboardUuidByAlias(table);
                    if (event.withDashboard) {
                        GrafanaUtils.deleteDashboard(uid);
                    }
                });
            } catch (Exception e) {
                log.error("删除grafana dashboard 异常", e);
            }
        });
    }

    private final Map<SrcJdbcType, DataStorageAdapter> dataStorageAdapterMap = new HashMap<>();

    public void create(final SrcJdbcType jdbcType, final CreateTopicDto[] topics, String flowName, boolean fromImport) {
        DataStorageAdapter storageAdapter = dataStorageAdapterMap.get(jdbcType);
        DataSourceProperties ds = storageAdapter.getDataSourceProperties();
        long t1 = System.currentTimeMillis();
        log.info("开始循环创建dashboard 预计数量：{},flowName:{},数据源类型:{}", topics.length, flowName, jdbcType.alias);
        String tempName = null;
        int count = 0;
        for (CreateTopicDto dto : topics) {
            if (!dto.getAddDashBoard()) {
                continue;
            }
            if (tempName == null) {
                tempName = dto.getAlias();
            }
            try {
                String columns = GrafanaUtils.fields2Columns(jdbcType, dto.getFields());
                String title = dto.getAlias();
                String schema = ds.getSchema();
                String table = dto.getTable();
                String tbValue = dto.getTbFieldName();
                String tagNameCondition = "";
                if (StringUtils.isNotBlank(tbValue)){
                    tagNameCondition = " and tag_name='" + table + "' ";
                }
                log.debug(">>>>>> create grafana dashboard columns:{},title:{},schema:{},table:{},tagNameCondition:{}", columns, title, schema, table,tagNameCondition);
                int dot = table.indexOf('.');
                if (dot > 0) {
                    schema = table.substring(0, dot);
                    table = table.substring(dot + 1);
                }
                String uuid = GrafanaUtils.createDashboard(table,tagNameCondition, jdbcType, schema, title, columns, dto.getTimestampField());
                //当创建类型为 非导入（手动创建），发送事件，创建数据看板
                if (!fromImport) {
                    EventBus.publishEvent(new CreateDashboardEvent(this, uuid, title, title));
                }
                count++;
            } catch (Exception e) {
                log.error("GrafanaUtils.createDashboard 异常", e);
            }
        }
        log.info("结束循环创建dashboard 实际创建：{}, 耗时：{},flowName:{}", count, System.currentTimeMillis() - t1, flowName);
        //组合dashboard
        if (fromImport) {
            if (jdbcType == SrcJdbcType.TdEngine || jdbcType == SrcJdbcType.TimeScaleDB) {
                if (tempName == null) {
                    return;
                }
                String dashboardName = DateUtil.format(new Date(), "yyyyMMddHHmmss");
                if (StrUtil.isNotBlank(flowName)) {
                    dashboardName = String.format("%s-%s", flowName, dashboardName);
                } else {
                    dashboardName = String.format("%s-%s", tempName, dashboardName);
                }
                String uuid = GrafanaUtils.createTimeSeriesListDashboard(jdbcType, topics, dashboardName);
                EventBus.publishEvent(new CreateDashboardEvent(this, uuid, dashboardName, "组合dashboard"));
            }
        }
    }

    public void onUpdateTopics() {
        String alias = "";
        String uuid = GrafanaUtils.getDashboardUuidByAlias(alias);
        String sql = "";
        JSONObject dbJson = GrafanaUtils.getDashboardByUuid(uuid);
        if (null == dbJson) {
            log.warn("uns alias : {} 查询不到对应的dashboard，更新终止", alias);
            return;
        }

        // 定位到 sql 字段
        JSONObject dashboard = dbJson.getJSONObject("dashboard");
        JSONArray panels = dashboard.getJSONArray("panels");
        if (panels == null || panels.size() == 0) {
            return;
        }

        JSONObject firstPanel = panels.getJSONObject(0);
        JSONArray targets = firstPanel.getJSONArray("targets");
        if (targets == null || targets.size() == 0) {
            return;
        }

        JSONObject firstTarget = targets.getJSONObject(0);
        firstTarget.put("sql", sql);
        JSONObject target = dbJson.getJSONObject("dashboard").getJSONArray("panels").getJSONObject(0).getJSONArray("targets").getJSONObject(0);
        target.put("sql", "demo");
        log.debug("onUpdateTopics body:{}", dbJson);
        HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dbJson.toJSONString()).execute();
        log.debug("onUpdateTopics response:{}", dashboardResponse);
    }

    /**
     * 启动后进行datasource的创建
     */
    @EventListener(classes = ContextRefreshedEvent.class)
    void onStartup(ContextRefreshedEvent event) {
        SystemConfig systemConfig = event.getApplicationContext().getBean(SystemConfig.class);
        if (null == systemConfig.getContainerMap().get("grafana")) {
            log.info(">>>>>>>>>当前系统未启用grafana服务，grafana初始化不进行");
            return;
        }
        Map<String, DataStorageAdapter> adapterMap = event.getApplicationContext().getBeansOfType(DataStorageAdapter.class);
        if (adapterMap != null && adapterMap.size() > 0) {
            for (DataStorageAdapter adapter : adapterMap.values()) {
                dataStorageAdapterMap.put(adapter.getJdbcType(), adapter);
            }
        }
        if (RuntimeUtil.isLocalProfile()) {
            log.info(">>>>>>>>>Grafana 默认datasource 取消创建：非正式环境");
            return;
        }
        log.info(">>>>>>>>>Grafana 默认datasource 开始检查数据源.");
        ThreadUtil.execute(() -> {
            boolean tdExist = false;
            boolean pgExist = false;
            boolean tsdbExist = false;
            while (!tdExist || !pgExist || !tsdbExist) {
                try {
                    HttpResponse pgRes = GrafanaUtils.getDataSourceByName(SrcJdbcType.Postgresql.dataSrcType);
                    if (200 == pgRes.getStatus()) {
                        pgExist = true;
                    } else {
                        DataStorageAdapter dataStorageAdapter = dataStorageAdapterMap.get(SrcJdbcType.Postgresql);
                        if (null == dataStorageAdapter) {
                            pgExist = true;
                        } else {
                            pgExist = GrafanaUtils.createDatasource(SrcJdbcType.Postgresql, dataStorageAdapterMap.get(SrcJdbcType.Postgresql).getDataSourceProperties(), false);
                        }
                    }

                    HttpResponse tsdbRes = GrafanaUtils.getDataSourceByName(SrcJdbcType.TimeScaleDB.dataSrcType);
                    if (200 == tsdbRes.getStatus()) {
                        tsdbExist = true;
                    } else {
                        DataStorageAdapter dataStorageAdapter = dataStorageAdapterMap.get(SrcJdbcType.TimeScaleDB);
                        if (null == dataStorageAdapter) {
                            tsdbExist = true;
                        } else {
                            tsdbExist = GrafanaUtils.createDatasource(SrcJdbcType.TimeScaleDB, dataStorageAdapterMap.get(SrcJdbcType.TimeScaleDB).getDataSourceProperties(), false);
                        }
                    }

                    HttpResponse tdRes = GrafanaUtils.getDataSourceByName(SrcJdbcType.TdEngine.dataSrcType);
                    if (200 == tdRes.getStatus()) {
                        tdExist = true;
                    } else {
                        DataStorageAdapter dataStorageAdapter = dataStorageAdapterMap.get(SrcJdbcType.TdEngine);
                        if (null == dataStorageAdapter) {
                            tdExist = true;
                        } else {
                            tdExist = GrafanaUtils.createDatasource(SrcJdbcType.TdEngine, dataStorageAdapterMap.get(SrcJdbcType.TdEngine).getDataSourceProperties(), false);
                        }
                    }
                } catch (Exception e) {
                    log.error("Grafana 数据源检查发生异常", e);
                }
                ThreadUtil.sleep(15000);
            }
            if ("zh-CN".equals(systemConfig.getLang())) {
                GrafanaUtils.setLanguage("zh-Hans");
            }
            log.info(">>>>>>>>>Grafana 默认datasource 完成创建.");
        });
    }
}
