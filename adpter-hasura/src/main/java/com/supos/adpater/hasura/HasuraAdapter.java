package com.supos.adpater.hasura;

import cn.hutool.http.HttpGlobalConfig;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpater.hasura.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.annotation.Description;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SimpleUnsInstance;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.RemoveTopicsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HasuraAdapter {
    @Value("${hasura.url::http://hasura:8080/v1/metadata}")
    private String hasuraNotifyUrl;
    static final String TRACK = "pg_track_table";
    static final String UnTRACK = "pg_untrack_table";

    private static final int timeout = Integer.parseInt(System.getProperty("hasura.timeout", "7000"));

    static {
        HttpGlobalConfig.setTimeout(Integer.parseInt(System.getProperty("http.timeout", "15000")));
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(200)
    @Description("uns.create.task.name.hasura")
    void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        CreateTopicDto[] topics = event.topics.get(SrcJdbcType.Postgresql);
        if (topics != null) {
            List<CreateTopicDto> topicDtos = Arrays.stream(topics).filter(HasuraAdapter::shouldTack).collect(Collectors.toList());
            if (!topicDtos.isEmpty()) {
                notifyHasura(hasuraNotifyUrl, pgSchema, TRACK, topicDtos.stream().map(CreateTopicDto::getTable).collect(Collectors.toList()));
                createRestApi(hasuraNotifyUrl, topicDtos);
            }
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(200)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (event.jdbcType == SrcJdbcType.Postgresql) {
            Collection<String> tables = event.topics.values().stream().filter(t -> t.getDataType() != null && com.supos.common.Constants.RELATION_TYPE == t.getDataType()).map(SimpleUnsInstance::getTableName).collect(Collectors.toSet());
            if (!tables.isEmpty()) {
                notifyHasura(hasuraNotifyUrl, pgSchema, UnTRACK, tables);
            }
        }
    }

    private static boolean shouldTack(CreateTopicDto dto) {
        return (dto.getDataType() != null && dto.getFlags() != null && com.supos.common.Constants.RELATION_TYPE == dto.getDataType() && com.supos.common.Constants.withSave2db(dto.getFlags()));
    }

    static void notifyHasura(String url, String pgSchema, final String type, final Collection<String> tables) {
        final String json = getPostBody(pgSchema, type, tables);
        notifyHasuraServer(url, type, json);
    }

    static String getPostBody(String pgSchema, String type, Collection<String> tables) {
        final String seg0 = "{\"type\": \"";
        final String seg1 = "\",\"args\": {\"table\": {\"name\": \"";
        final String seg2 = "\",\"schema\": \"";
        final String seg22 = "\"},\"configuration\": {\"custom_name\": \"";
        final String seg3 = "\"},\"source\": \"default\"}";
        final int BLOCK_LEN = seg0.length() + type.length() + seg1.length() + seg2.length() + seg22.length() + seg3.length() + 32;
        int LEN_TOTAL = 0;
        for (String t : tables) {
            LEN_TOTAL += t.length();
        }
        final int LEN = tables.size();
        StringBuilder jsonBd = new StringBuilder(64 + (BLOCK_LEN + (int) Math.ceil(LEN_TOTAL * 1.0 / LEN)) * LEN);
        jsonBd.append("{\"type\": \"bulk\",\"source\": \"default\",\"args\": [");
        for (String t : tables) {
            jsonBd.append(seg0).append(type).append(seg1).append(t).append(seg2).append(pgSchema).append(seg22)
                    .append(t.replace('/', '_')).append(seg3);
            if (UnTRACK.equals(type)) {
                jsonBd.setCharAt(jsonBd.length() - 1, ',');
                jsonBd.append("\"cascade\": true}");// 级联删除
            }
            jsonBd.append('}').append(',');
        }
        jsonBd.setCharAt(jsonBd.length() - 1, ']');
        jsonBd.append('}');
        final String json = jsonBd.toString();
        return json;
    }

    static void notifyHasuraServer(String url, String type, String paramJson) {
        try {
            HttpUtil.createPost(url).timeout(timeout).body(paramJson).then(httpResponse -> {
                log.debug("notifyHasura[{}]: request: {}", url, paramJson);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();

                if (status != 200) {
                    log.warn("{} HasuraResp[{}]: body[{}], head[{}], params={}", type, status, respBody, httpResponse.headers(), paramJson);
                    JSONObject params = JSON.parseObject(paramJson);
                    JSONArray array = params.getJSONArray("args");
                    if (!array.isEmpty() && !"reload_metadata".equals(array.getJSONObject(0).getString("type"))) {
                        params.put("type", "bulk_keep_going");
                        JSONArray head2 = new JSONArray(2);
                        {
                            JSONObject reload = new JSONObject();
                            head2.add(reload);
                            reload.put("type", "reload_metadata");
                            JSONObject reloadArgs = new JSONObject();
                            reload.put("args", reloadArgs);
                            reloadArgs.put("reload_remote_schemas", true);
                            reloadArgs.put("reload_sources", true);
                            reloadArgs.put("recreate_event_triggers", true);
                        }
                        {
                            JSONObject dropInconsistentMetadata = new JSONObject();
                            head2.add(dropInconsistentMetadata);
                            dropInconsistentMetadata.put("type", "drop_inconsistent_metadata");
                            dropInconsistentMetadata.put("args", new JSONObject());
                        }
                        array.addAll(0, head2);
                        // hasura 出现数据不一致问题，reload 元数据,清理不一致后 重试。
                        notifyHasuraServer(url, "Retry-" + type, params.toJSONString());
                    }
                } else {
                    log.info("{} HasuraResp[{}]: body[{}], head[{}]", type, status, respBody, httpResponse.headers());
                }
            });
        } catch (Exception ex) {
            log.error("notifyHasura[{}]: request: {}", url, paramJson, ex);
        }
    }

    private String pgSchema = "public";

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order
    void init(ContextRefreshedEvent event) {
        Map<String, DataStorageAdapter> adapterMap = event.getApplicationContext().getBeansOfType(DataStorageAdapter.class);
        if (adapterMap != null && adapterMap.size() > 0) {
            for (DataStorageAdapter adapter : adapterMap.values()) {
                if (adapter.getJdbcType() == SrcJdbcType.Postgresql) {
                    pgSchema = adapter.getDataSourceProperties().getSchema();
                    log.info("find pgSchema: {}", pgSchema);
                    break;
                }
            }
        }
    }

    /**
     * 创建GraphQL对应的的restApi
     */
    public static void createRestApi(String url, Collection<CreateTopicDto> topics) {
//        ThreadUtil.execute(() -> {
        if (!collectionExist(url)) {
            createCollection(url);
        }

        StringBuilder args = new StringBuilder();
        for (CreateTopicDto topic : topics) {
            StringBuilder fieldQuery = new StringBuilder();
            for (FieldDefine field : topic.getFields()) {
                fieldQuery.append(field.getName()).append("\\r\\n");
            }
            String addQuery = String.format(Constants.CREATE_REST_ARGS_ADD_QUERY_FORMATE, topic.getTable(), topic.getTable(), topic.getTable(), fieldQuery);
            String createRest = String.format(Constants.CREATE_REST_ARGS_CREATE_REST_FORMATE, topic.getTable(), topic.getTable(), topic.getTable());
            args.append(addQuery).append(",").append(createRest).append(',');
        }
        if (args.length() == 0) {
            return;
        }
        String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, args.substring(0, args.length() - 1));

        try {
            HttpUtil.createPost(url).body(restQuery).then(httpResponse -> {
                log.debug("notifyHasura createRestApi[{}]: request: {}", url, restQuery);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("createRestApiResp[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("createRestApiResp error!");
                }
            });
        } catch (Exception ex) {
            log.error("createRestApi[{}]: request: {}", url, restQuery, ex);
        }
//        });
    }

    public static boolean collectionExist(String url) {
        AtomicBoolean exist = new AtomicBoolean(false);
        try {
            HttpUtil.createPost(url).body(Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE).then(httpResponse -> {
                log.debug("notifyHasura queryCollection[{}]: request: {}", url, Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("queryCollection[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("queryCollection error!");
                    exist.set(false);
                    return;
                }

                if (respBody.contains("query_collections") && respBody.contains(Constants.COLLECTION)) {
                    exist.set(true);
                }
            });
        } catch (Exception ex) {
            log.error("queryCollection[{}]: request: {}", url, Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE, ex);
        }
        return exist.get();
    }

    public static void createCollection(String url) {
        String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, Constants.CREATE_REST_ARGS_ADD_COLLECTION_FORMATE);
        try {
            HttpUtil.createPost(url).body(restQuery).then(httpResponse -> {
                log.debug("notifyHasura createCollection[{}]: request: {}", url, restQuery);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("createCollection[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("createCollection error!");
                }
            });
        } catch (Exception ex) {
            log.error("createCollection[{}]: request: {}", url, restQuery, ex);
        }
    }

    public static void dropRestApi(String url, final Collection<String> tables) {
        StringBuilder args = new StringBuilder();
        boolean first = true;
        for (String table : tables) {
            if (!first) {
                args.append(',');
            } else {
                first = false;
            }
            String dropRest = String.format(Constants.CREATE_REST_ARGS_DROP_REST_FORMATE, table);
            String dropQuery = String.format(Constants.CREATE_REST_ARGS_DROP_QUERY_FORMATE, table);

            args.append(dropRest).append(",").append(dropQuery);
        }
        if (args.length() == 0) {
            return;
        }
        String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, args);

        try {
            HttpUtil.createPost(url).body(restQuery).then(httpResponse -> {
                log.debug("notifyHasura dropRestApi[{}]: request: {}", url, restQuery);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("dropRestApiResp[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("dropRestApiResp error!");
                }
            });
        } catch (Exception ex) {
            log.error("dropRestApi[{}]: request: {}", url, restQuery, ex);
        }
    }

    /**
     curl --location --request POST 'http://hasura:8080/v1/metadata' \
     --header 'Content-Type: application/json' \
     --data '{
     "type": "bulk",
     "source": "default",
     "args": [
     {
     "type": "pg_track_table",
     "args": {
     "table": {
     "name": "/company/fx/client/c2",
     "schema": "public"
     },
     "configuration": {
     "custom_name": "_company_fx_client_c2"
     },
     "source": "default"
     }
     },
     {
     "type": "pg_track_table",
     "args": {
     "table": {
     "name": "/company/fx/client/c3",
     "schema": "public"
     },
     "configuration": {
     "custom_name": "_company_fx_client_c3"
     },
     "source": "default"
     }
     }
     ]
     }'
     */
}
