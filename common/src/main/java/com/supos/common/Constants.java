package com.supos.common;

import cn.hutool.system.SystemUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final boolean useAliasAsTopic; // 是否使用别名alias作为 mqtt topic,false 则使用文件路径作为 mqtt topic
    public static final String MQTT_PLUGIN;
    public static final String SYSTEM_FIELD_PREV = "_";
    public static final String SYSTEM_SEQ_TAG = "tag_name";
    public static final String SYSTEM_SEQ_VALUE = "value";
    public static final String SYS_FIELD_CREATE_TIME;
    public static final String QOS_FIELD;
    public static final String SYS_SAVE_TIME = Constants.SYSTEM_FIELD_PREV + "st";
    public static final int UNS_ADD_BATCH_SIZE;
    public static final String SYS_FIELD_ID = Constants.SYSTEM_FIELD_PREV + "id";
    public static final Set<String> systemFields;
    public static final String MERGE_FLAG = "#mg#";// 按时间戳合并消息的标志
    public static final String FIRST_MSG_FLAG = "#1#";//启动时首条消息的标志
    static {
        final String k = "SYS_OS_USE_ALIAS_PATH_AS_TOPIC";
        String v = System.getenv(k);
        if (v == null || v.trim().isEmpty()) {
            v = System.getProperty(k);
        }
        useAliasAsTopic = Boolean.parseBoolean(v);

        SYS_FIELD_CREATE_TIME = SystemUtil.get("SYS_OS_TIMESTAMP_NAME", "timeStamp");
        QOS_FIELD = SystemUtil.get("SYS_OS_QUALITY_NAME", "status");
        UNS_ADD_BATCH_SIZE = Integer.parseInt(SystemUtil.get("UNS_ADD_BATCH_SIZE", "1000"));
        MQTT_PLUGIN =  SystemUtil.get("MQTT_PLUGIN", "emqx");
        systemFields = new HashSet<>(Arrays.asList(SYSTEM_SEQ_TAG, SYS_FIELD_ID, SYS_FIELD_CREATE_TIME, Constants.QOS_FIELD, Constants.SYS_SAVE_TIME, "_ct"));

    }

    public static final int PATH_TYPE_DIR = 0;// 目录
    public static final int PATH_TYPE_FILE = 2;// 文件
    public static final int PATH_TYPE_TEMPLATE = 1;// 模板

    public static final String RESULT_TOPIC_PREV = "_rs/";// 处理结果 topic前缀
    public static final String MSG_RAW_DATA_KEY = "_source_"; // 原始数据的 json key
    public static final String MSG_RES_DATA_KEY = "_resource_";// 处理过的 json key

    public static final int UNS_FLAG_WITH_FLOW = 1 << 0;// 是否添加数据采集流程
    public static final int UNS_FLAG_WITH_DASHBOARD = 1 << 1;// 是否添加数据看板

    public static final int UNS_FLAG_WITH_SAVE2DB = 1 << 2;// 是否持久化到数据库
    public static final int UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE = 1 << 3;// 刪除实例时是否保留数据表
    public static final int UNS_FLAG_ALARM_ACCEPT_PERSON = 1 << 4;// 报警规则接收方式 16人员
    public static final int UNS_FLAG_ALARM_ACCEPT_WORKFLOW = 1 << 5;// 报警规则接收方式 32工作流
    public static final int UNS_FLAG_ACCESS_LEVEL_READ_ONLY = 1 << 6; // 北向访问级别:READ_ONLY-只读
    public static final int UNS_FLAG_ACCESS_LEVEL_READ_WRITE = 1 << 7;// 北向访问级别:READ_WRITE-读写

    public static boolean withFlow(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_FLOW) == UNS_FLAG_WITH_FLOW;
    }

    public static boolean withDashBoard(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_DASHBOARD) == UNS_FLAG_WITH_DASHBOARD;
    }

    public static boolean withSave2db(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_SAVE2DB) == UNS_FLAG_WITH_SAVE2DB;
    }

    public static boolean withRetainTableWhenDeleteInstance(int unsFlag) {
        return (unsFlag & UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE) == UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE;
    }

    public static String withReadOnly(int unsFlag) {
        if ((unsFlag & UNS_FLAG_ACCESS_LEVEL_READ_ONLY) == UNS_FLAG_ACCESS_LEVEL_READ_ONLY) {
            return ACCESS_LEVEL_READ_ONLY;
        }
        if ((unsFlag & UNS_FLAG_ACCESS_LEVEL_READ_WRITE) == UNS_FLAG_ACCESS_LEVEL_READ_WRITE) {
            return ACCESS_LEVEL_READ_WRITE;
        }
        return null;
    }

    /**
     * 时序类型
     */
    public static final int TIME_SEQUENCE_TYPE = 1;
    /**
     * 关系类型
     */
    public static final int RELATION_TYPE = 2;


    public static final int CALCULATION_REAL_TYPE = 3;// 实时计算
    public static final int CALCULATION_HIST_TYPE = 4;// 历史值计算

    /**
     * 报警规则类型
     */
    public static final int ALARM_RULE_TYPE = 5;

    /**
     * 聚合类型
     */
    public static final int MERGE_TYPE = 6;

    /**
     * 引用类型，不持久化，只读, 不能引用引用类型的文件
     */
    public static final int CITING_TYPE = 7;

    public static boolean isValidDataType(int type) {
        return type >= TIME_SEQUENCE_TYPE && type <= CITING_TYPE;
    }

    /**
     * 分页条数
     */
    public final static long DEFAULT_PAGE_SIZE = 20;
    /**
     * 分页起始页
     */
    public final static long DEFAULT_PAGE_NUM = 1;

    /**
     * 最大分页条数
     */
    public final static int MAX_PAGE_SIZE = 1000;

    public static final int SQL_BATCH_SIZE = 200;

    public static final String ROOT_PATH = "/data";

    public static final String PLUGIN_PATH = ROOT_PATH + "/plugins";
    public static final String PLUGIN_TEMP_PATH = PLUGIN_PATH + "/temp";
    public static final String PLUGIN_INSTALLED_PATH = PLUGIN_PATH + "/installed";
    public static final String PLUGIN_FRONTEND_PATH = ROOT_PATH + "/plugins-frontend";

    public static final String LOG_PATH = "/logs";

    public static final String EXCEL_ROOT = "/excel/";

    public static final String EXAMPLE_ROOT = "/example/";

    public static final String UNS_ROOT = "/uns/";

    public static final String SYSTEM_ROOT = "/system/";

    public final static String EXCEL_TEMPLATE_PATH = "/templates/all-namespace.xlsx";
    public final static String JSON_TEMPLATE_PATH = "/templates/all-namespace.json";
    public final static String EXCEL_OUT_PATH = "/export/all-namespace.xlsx";
    public final static String JSON_OUT_PATH = "/export/all-namespace.json";
    public final static String EXCEL_OUT_TEMP_PATH = "/export/temp-namespace.xlsx";

    public static final String BLOB_PATH = "/data/uns";

    public static final String ACCESS_TOKEN_KEY = "supos_community_token";
    public static final int TOKEN_MAX_AGE = 60 * 60 * 6;//token失效时间（秒）

    public static final String ALIAS_REG = "[a-zA-Z_\\$][a-zA-Z0-9_\\$]*$";
    public static final String TOPIC_REG = "^[\\u4e00-\\u9fa5a-zA-Z0-9/_-]+$";

    public static final String VAR_PREV = "a";


    public static final String DEFAULT_ROLE_ID = "d12d7ca2-34e1-4f26-9a03-6b4f7f411567";

    public static final long ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024;

    public static final String TD_JDBC_URL = "tdengine:6041";
    public static final String PG_JDBC_URL = "postgresql:5432";
    public static final String TSDB_JDBC_URL = "tsdb:5432";

    public static final String AUTH_CHECK_KONG_PLUGIN_ID = "1845ee75-d704-40e1-a8b0-aa2baaf9d71b";

    public static final String EMQX_API_KEY = "b441dbabd9bd5c26";
    public static final String EMQX_SECRET_KEY = "59CdRlRvDaygamiil6789A2JvbXfO9ADRcLEcgxB9CYVv5Y";

    public static final String UNKNOWN_USER = "Unknown User";

    public static final String EXAMPLE_FUXA_FILE = "fuxa-";
    public static final String EXAMPLE_GRAFANA = "grafana-";
    public static final String EXAMPLE_METADATA = "metadata.json";
    public static final String EXAMPLE_PROTOCOL = "protocol.json";

    public static final String FUXA_API_URL = "http://fuxa:1881";
    /**
     * 实时数据订阅
     */
    public static final Integer CMD_SUB = 1;
    /**
     * 订阅响应
     */
    public static final Integer CMD_SUB_RES = 2;
    /**
     * 实时值推送
     */
    public static final Integer CMD_VAL_PUSH = 3;

    /**
     * @安冬冬：按照最多500 位号值的方式进行聚合返回
     */
    public static final int VALUE_PUSH_BATCH_SIZE = 500;

    /**
     * pride 模板名称前缀
     */
    public static final String PRIDE_TEMPLATE_PREFIX = "system.";

    /**
     * pride采集器文件前缀
     */
    public static final String PRIDE_COLLECTOR_FILE_PREFIX = "C";


    // 北向访问级别:READ_ONLY-只读
    public static final String ACCESS_LEVEL_READ_ONLY = "READ_ONLY";

    // 北向访问级别:READ_WRITE-读写
    public static final String ACCESS_LEVEL_READ_WRITE = "READ_WRITE";

    /**
     * 通知方式配置文件
     */
    public static final String NOTIFY_CONFIG_FILE = "notify_config.yml";
}
