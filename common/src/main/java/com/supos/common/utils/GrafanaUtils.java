package com.supos.common.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.grafana.*;
import com.supos.common.enums.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/1 9:55
 * @description
 */
@Slf4j
public class GrafanaUtils {

    public static String getGrafanaUrl() {
        String grafanaUrl;
        if (RuntimeUtil.isLocalProfile()) {
            grafanaUrl = "http://100.100.100.22:33893/grafana/home";
        } else {
            grafanaUrl = "http://grafana:3000";
        }
        return grafanaUrl;
    }

    public static String getDashboardUuidByAlias(String alias){
        return MD5.create().digestHex16(alias);
    }

    public static void deleteDashboard(String uid){
        HttpResponse dbRes = HttpUtil.createRequest(Method.DELETE, getGrafanaUrl() + "/api/dashboards/uid/" + uid).executeAsync();
        log.debug("Delete Dashboard uid:{} 返回结果:{}", uid, dbRes.body());
    }

    public static void deleteDatasource(String uid){
        HttpResponse dsRes = HttpUtil.createRequest(Method.DELETE, getGrafanaUrl() + "/api/datasources/uid/" + uid).executeAsync();
        log.debug("Delete DataSource uid:{} 返回结果:{}", uid, dsRes.body());
    }

    /**
     * 创建数据源
     * @param jdbcType 数据源类型
     * @param dataSource 数据源
     * @param reCreate 是否需要删除再新建
     */
    public static boolean createDatasource(SrcJdbcType jdbcType, DataSourceProperties dataSource,boolean reCreate){
        String title = jdbcType.alias;
        GrafanaDataSourceDto grafanaDataSource = new GrafanaDataSourceDto();
        grafanaDataSource.setUser(dataSource.getUsername());
        grafanaDataSource.setPassword(dataSource.getPassword());
        String uid = MD5.create().digestHex16(title);
        grafanaDataSource.setUid(uid);
        grafanaDataSource.setName(title);
        if (reCreate){
            //先删再新增
            HttpUtil.createRequest(Method.DELETE, getGrafanaUrl() + "/api/datasources/uid/" + grafanaDataSource.getUid()).execute();
        }
        String dsTemplate = "";
        if (SrcJdbcType.Postgresql == jdbcType){
            dsTemplate = ResourceUtil.readUtf8Str("templates/pg-datasource.json");
            grafanaDataSource.setUrl(Constants.PG_JDBC_URL);
        } else if (SrcJdbcType.TimeScaleDB == jdbcType){
            dsTemplate = ResourceUtil.readUtf8Str("templates/pg-datasource.json");
            grafanaDataSource.setUrl(Constants.TSDB_JDBC_URL);
        } else if (SrcJdbcType.TdEngine == jdbcType){
            grafanaDataSource.setUrl(Constants.TD_JDBC_URL);
            grafanaDataSource.createBasicAuth();
            dsTemplate = ResourceUtil.readUtf8Str("templates/td-datasource.json");
        }
        Map<String, Object> dsParams = BeanUtil.beanToMap(grafanaDataSource);
        String dsJson = StrUtil.format(dsTemplate, dsParams);
        log.info(">>>>>>>>>>>>>>>创建 datasource 请求 :{}", dsJson);
        HttpResponse dsResponse = HttpUtil.createPost(getGrafanaUrl() + "/api/datasources").body(dsJson).execute();
        log.info(">>>>>>>>>>>>>>>创建 datasource 返回结果:{}", dsResponse.body());
        return dsResponse.getStatus() == 200 || dsResponse.getStatus() == 409;
    }

    /**
     * @param table 表名
     * @param jdbcType 数据源类型
     * @param schema
     * @param title 用uns alias作为title
     * @param columns 字段列表
     * @return
     */
    public static String createDashboard(String table, SrcJdbcType jdbcType, String schema, String title,String columns, String ct) {
        String template = "";
        String dashboardJson ="";
        String uid = getDashboardUuidByAlias(title);
        Map<String, Object> dbParams = new HashMap<>();
        if (SrcJdbcType.Postgresql == jdbcType){
            template = ResourceUtil.readUtf8Str("templates/pg-dashboard.json");
            PgDashboardParam pgParams = new PgDashboardParam();
            pgParams.setTitle(title);
            pgParams.setUid(uid);
            pgParams.setDataSourceType(jdbcType.dataSrcType);
            pgParams.setDataSourceUid(getDatasourceUuidByJdbc(jdbcType));
            pgParams.setSchema(schema);
            pgParams.setTableName(table);
            pgParams.setColumns("[]");
            dbParams = BeanUtil.beanToMap(pgParams);
        } else if (SrcJdbcType.TdEngine == jdbcType){ //timescale 使用时序模板
            template = ResourceUtil.readUtf8Str("templates/td-dashboard.json");
            TdDashboardParam tdDashboard = new TdDashboardParam();
            tdDashboard.setTitle(title);
            tdDashboard.setUid(uid);
            tdDashboard.setDataSourceType(jdbcType.dataSrcType);
            tdDashboard.setDataSourceUid(getDatasourceUuidByJdbc(jdbcType));
            tdDashboard.setSchema(schema);
            tdDashboard.setTableName(table);
            tdDashboard.setColumns(columns);
            dbParams = BeanUtil.beanToMap(tdDashboard);
        } else if (SrcJdbcType.TimeScaleDB == jdbcType){ //timescale 使用时序模板
            template = ResourceUtil.readUtf8Str("templates/ts-dashboard.json");
            TdDashboardParam tdDashboard = new TdDashboardParam();
            tdDashboard.setTitle(title);
            tdDashboard.setUid(uid);
            tdDashboard.setDataSourceType(jdbcType.dataSrcType);
            tdDashboard.setDataSourceUid(getDatasourceUuidByJdbc(jdbcType));
            tdDashboard.setSchema(schema);
            tdDashboard.setTableName(table);
            tdDashboard.setColumns(columns);
            dbParams = BeanUtil.beanToMap(tdDashboard);
        }
        dbParams.put("sys_field_create_time", ct);
        dashboardJson = StrUtil.format(template, dbParams);
        log.debug(">>>>>>>>>>>>>>>创建 dashboardJson 请求:{}", dashboardJson);
        HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dashboardJson).executeAsync();
        log.debug(">>>>>>>>>>>>>>>创建 dashboardJson 返回结果:{}", dashboardResponse.body());
        return uid;
    }

    public static HttpResponse getDataSourceByName(String name){
        String url = getGrafanaUrl() + "/api/datasources/name/" + name;
        log.debug(">>>>>>>>>>>>>>>查询 datasource 请求 :{}", url);
        HttpResponse response =  HttpUtil.createGet(url).execute();
        log.debug(">>>>>>>>>>>>>>>查询 datasource 返回结果:{}", response.body());
        return response;
    }

    public static String getDatasourceUuidByJdbc(SrcJdbcType jdbcType){
        return MD5.create().digestHex16(jdbcType.alias);
    }

    public static String fields2Columns(SrcJdbcType jdbcType,FieldDefine[] fields){
        //td用`   pg和timescale"  拼接
        String flag = jdbcType.equals(SrcJdbcType.TdEngine) ? "`": "\\\"";
        List<String> fieldNames = Arrays.stream(fields).filter(field ->{
            boolean matchType = field.getType() != FieldType.BLOB && field.getType() != FieldType.LBLOB;//过滤blob
            boolean matchName = !Constants.QOS_FIELD.equals(field.getName()) && !Constants.SYS_SAVE_TIME.equals(field.getName());//过滤qos 和_st
            return matchType && matchName;
        }).map(FieldDefine::getName).collect(Collectors.toList());
        return fieldNames.stream().map(field -> flag + field + flag).collect(Collectors.joining(", "));
    }

    /**
     * 创建时序组合Dashboard
     */
    public static String createTimeSeriesListDashboard(SrcJdbcType srcJdbcType,CreateTopicDto[] topics,String dashboardName){
        long t1 = System.currentTimeMillis();
        log.info(">>>>>>>>>>>>>>>调用 创建时序组合Dashboard dashboardName:{} topics数量：{}",dashboardName,topics.length);
        String panelTemplate = "";
        if (SrcJdbcType.TimeScaleDB.equals(srcJdbcType)){
            panelTemplate = ResourceUtil.readUtf8Str("templates/ts-panel.json");
        } else {
            panelTemplate = ResourceUtil.readUtf8Str("templates/td-panel.json");
        }
        JSONArray panelJsonList = new JSONArray();
        for (int i = 0; i < topics.length; i++) {
            CreateTopicDto dto = topics[i];
            String columns = fields2Columns(srcJdbcType,dto.getFields());
            String title = dto.getTopic();
            String schema = "public";
            String table = dto.getTable();
            int dot = table.indexOf('.');
            if (dot > 0) {
                schema = table.substring(0, dot);
                table = table.substring(dot + 1);
            }
            //panel的x轴
            int gridPosX = i * 8;
            if (gridPosX > 16) {
                gridPosX = (i % 3) * 8;
            }
            TdPanelParam panelParam = new TdPanelParam();
            panelParam.setId(i + 1);
            panelParam.setTitle(title);
            panelParam.setDataSourceUid(getDatasourceUuidByJdbc(srcJdbcType));
            panelParam.setColumns(columns);
            panelParam.setSchema(schema);
            panelParam.setTableName(table);
            panelParam.setGridPosX(gridPosX);
            Map<String, Object> dbParams = BeanUtil.beanToMap(panelParam);
            String json = StrUtil.format(panelTemplate, dbParams);
            panelJsonList.add(JSONObject.parseObject(json));
        }

        String template = ResourceUtil.readUtf8Str("templates/td-dashboard-list.json");
        Map<String, Object> dbParams = new HashMap<>();
        String uuid = IdUtil.fastSimpleUUID();
        dbParams.put("uid", uuid);
        dbParams.put("title",dashboardName);
        dbParams.put("panels",panelJsonList);
        String dashboardJson = StrUtil.format(template, dbParams);
        log.debug(">>>>>>>>>>>>>>>创建时序组合DashboardDashboard 请求:{}", dashboardJson);
        log.info(">>>>>>>>>>>>>>>创建时序组合DashboardDashboard dashboardJson大小：{}", dashboardJson.length());
        HttpResponse dashboardResponse = HttpUtil.createPost(GrafanaUtils.getGrafanaUrl() + "/api/dashboards/db").body(dashboardJson).executeAsync();
        log.info(">>>>>>>>>>>>>>>创建时序组合Dashboard 返回结果:{},耗时：{}", dashboardResponse.body(),System.currentTimeMillis() - t1);
        return uuid;
    }


    public static JSONObject getDashboardByUuid(String uuid){
        String url = getGrafanaUrl() + "/api/dashboards/uid/" + uuid;
        log.debug(">>>>>>>>>>>>>>>查询 dashboards 请求 :{}", url);
        HttpResponse response =  HttpUtil.createGet(url).execute();
        log.debug(">>>>>>>>>>>>>>>查询 dashboards 返回结果:{}", response.body());
        if (response.getStatus() != 200){
            return null;
        }
        return JSONObject.parseObject(response.body());
    }

    public static GrafanaFolderDto createFolder(String uid,String title){
        GrafanaFolderDto folderDto = null;
        HttpResponse response = HttpUtil.createRequest(Method.GET, getGrafanaUrl() + "/api/folders/" + uid).execute();
        if (200 == response.getStatus()){
            folderDto = JSON.parseObject(response.body(), GrafanaFolderDto.class);
            return folderDto;
        }
        JSONObject req = new JSONObject();
        req.put("uid",uid);
        req.put("title",title);
        HttpResponse folderRes = HttpUtil.createRequest(Method.POST, getGrafanaUrl() + "/api/folders").body(req.toJSONString()).execute();
        if (200 == folderRes.getStatus()){
            folderDto = JSON.parseObject(folderRes.body(), GrafanaFolderDto.class);
        }
        return folderDto;
    }

    public static HttpResponse setLanguage(String language){
        String url = getGrafanaUrl() + "/api/org/preferences";
        String json = "{\"language\":\"" + language + "\"}";
        log.info(">>>>>>>>>>>>>>>设置grafana 语言 请求 :{}", json);
        HttpResponse response =  HttpUtil.createRequest(Method.PUT,url).body(json).execute();
        log.info(">>>>>>>>>>>>>>>设置grafana 语言 返回结果:{}", response.body());
        return response;
    }
}
