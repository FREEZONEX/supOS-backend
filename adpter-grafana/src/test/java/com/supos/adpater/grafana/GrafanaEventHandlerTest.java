package com.supos.adpater.grafana;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.grafana.GrafanaDataSourceDto;
import com.supos.common.dto.grafana.PgDashboardParam;
import com.supos.common.enums.FieldType;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.utils.GrafanaUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.*;

@Slf4j
public class GrafanaEventHandlerTest {

    @Test
    public void onCreateTable() {
        String grafanaUrl;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            grafanaUrl = "http://100.100.100.20:31012";
        } else {
            grafanaUrl = "http://127.0.0.1:3000";
        }
        //先删再新增
        HttpUtil.createRequest(Method.DELETE,grafanaUrl + "/api/datasources/uid/pg").execute();

        String title = "order-" + DateUtil.format(new Date(),"yyyyMMddHHmmss");
        //create grafana datasource
        GrafanaDataSourceDto grafanaDataSource = new GrafanaDataSourceDto();
        grafanaDataSource.setUid("pg");
        grafanaDataSource.setName(title);
        grafanaDataSource.setUser("postgres");
        grafanaDataSource.setUrl("172.21.2.151:5432");
        grafanaDataSource.setPassword("root");

        String dsTemplate = ResourceUtil.readUtf8Str("templates/pg-datasource.json");
        Map<String, Object> dsParams = BeanUtil.beanToMap(grafanaDataSource);
        String dsJson = StrUtil.format(dsTemplate, dsParams);

        HttpResponse dsResponse = HttpUtil.createPost(grafanaUrl + "/api/datasources").body(dsJson).execute();
        log.info(dsResponse.body());

        //create grafana dashboard
        String template = ResourceUtil.readUtf8Str("templates/pg-dashboard.json");
        PgDashboardParam pgParams = new PgDashboardParam();
        pgParams.setUid("pg");
        pgParams.setTitle(title);
        pgParams.setDataSourceType("postgresql");
        pgParams.setDataSourceUid("pg");
        pgParams.setSchema("public");
        pgParams.setTableName("grafana");
        pgParams.setColumns("[]");
        Map<String, Object> dbParams = BeanUtil.beanToMap(pgParams);
        String dashboardJson = StrUtil.format(template, dbParams);
        log.info(dashboardJson);
        HttpResponse dashboardResponse = HttpUtil.createPost(grafanaUrl + "/api/dashboards/db").body(dashboardJson).execute();
        log.info(dashboardResponse.body());
    }

    @Test
    public void testCreateDashboard(){
        GrafanaUtils.createDashboard("authentication_execution",SrcJdbcType.Postgresql,"public","demo1","id,authenticator", Constants.SYS_FIELD_CREATE_TIME);
    }

    @Test
    public void testCreateListDashboard(){
        FieldDefine[] fields = new FieldDefine[]{new FieldDefine("a", FieldType.STRING,false,"1", null, null)};

        CreateTopicDto d1 = new CreateTopicDto();
        d1.setPath("t1");
        d1.setFields(fields);

        CreateTopicDto d2 = new CreateTopicDto();
        d2.setPath("t2");
        d2.setFields(fields);

        CreateTopicDto d3 = new CreateTopicDto();
        d3.setPath("t3");
        d3.setFields(fields);
        CreateTopicDto[] topics = new CreateTopicDto[]{d1,d2,d3};
//        GrafanaUtils.createTdListDashboard(topics,"test");
    }

    @Test
    public void testBatchCreate(){
        Map<SrcJdbcType, CreateTopicDto[]> topics = new HashMap<>();

        List<CreateTopicDto> topicDtos = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            CreateTopicDto ct = new CreateTopicDto("/xwjtest-" + i,i + "");
            ct.setFields(new FieldDefine[]{new FieldDefine("test",FieldType.INT)});
            topicDtos.add(ct);
        }

        topics.put(SrcJdbcType.TdEngine,topicDtos.toArray(new CreateTopicDto[0]));

        GrafanaEventHandler grafanaEventHandler = new GrafanaEventHandler();
        BatchCreateTableEvent event = new BatchCreateTableEvent(this.getClass(),true,topics);
        grafanaEventHandler.onBatchCreateTableEvent(event);
    }



}