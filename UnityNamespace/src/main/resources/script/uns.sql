
CREATE TABLE if not exists uns_namespace (
	"id" bigint PRIMARY KEY NOT NULL,
	"lay_rec" text NOT NULL,
	"alias" varchar(128) NOT NULL,
	"parent_alias" varchar(128) NULL,
	"name" varchar(512) NOT NULL,
	"path" text NOT NULL,
	"path_type" int2 NOT NULL,
	"data_type" int2 NULL,
	"fields" json NULL,
	"create_at" timestamptz DEFAULT now() NULL,
	"status" smallint DEFAULT 1 NULL,
	"description" varchar(255),
	"update_at" timestamptz NULL,
	"protocol" varchar(2000) NULL,
	"data_path" varchar(128) NULL,
	 "with_flags" integer NULL default 0,
	 "data_src_id" int2 NULL,
	 "ref_uns" jsonb default '{}',
	 "refers" json NULL,
	 "expression" varchar(255) NULL,
	 "table_name" varchar(190) NULL,
	 "number_fields" int2 default NULL,
	 "parent_id" bigint default NULL,
	 "model_id" bigint default NULL,
	 "protocol_type" varchar(64) NULL,
	 "extend" jsonb DEFAULT '{}'
);
ALTER TABLE uns_namespace ALTER COLUMN fields TYPE json USING fields::json;
ALTER TABLE uns_namespace ALTER COLUMN refers TYPE json USING refers::json;
ALTER TABLE uns_namespace ALTER COLUMN extend TYPE jsonb USING extend::jsonb;
ALTER TABLE uns_namespace ADD IF NOT EXISTS "label_ids" jsonb default NULL;
ALTER TABLE uns_namespace ALTER COLUMN "label_ids" TYPE jsonb USING label_ids::jsonb;
CREATE UNIQUE INDEX if not exists idx_uns_spacex_alias ON uns_namespace (alias);

insert into uns_namespace("id","path_type","lay_rec","alias","name","path","description")values(1,1,'1','__templates__','tmplt','tmplt','模板顶级目录')ON CONFLICT (id) DO NOTHING;

CREATE TABLE if not exists uns_dashboard (
	id varchar(64) PRIMARY KEY NOT NULL,
	"name" varchar(255) NULL,
	description varchar(255) NULL,
    "json_content" text NULL,
	update_time timestamp(6) NULL,
	create_time timestamp(6) NULL
);
CREATE TABLE if not exists "supos_user_menu" (
    "id" int8 PRIMARY KEY NOT NULL,
    "user_id" varchar(64) COLLATE "pg_catalog"."default",
    "menu_name" varchar(255) COLLATE "pg_catalog"."default",
    "picked" bool DEFAULT true,
    "update_time" timestamp(6),
    "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE "supos_user_menu" OWNER TO "postgres";

CREATE INDEX if not exists "idx_user_id" ON "supos_user_menu" USING btree ("user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST);

CREATE TABLE if not exists "uns_alarms_data" (
"_id" BIGSERIAL PRIMARY KEY,
"uns" bigint NOT NULL,
"uns_path" varchar(255) NULL,
"current_value" float4,
"limit_value" float4,
"is_alarm" bool DEFAULT true,
"read_status" bool DEFAULT false,
"_ct" timestamptz(6) DEFAULT now()
);

ALTER TABLE "uns_alarms_data" ALTER COLUMN "read_status" SET DEFAULT false;
ALTER TABLE "uns_alarms_data" ALTER COLUMN "is_alarm" SET DEFAULT true;
ALTER TABLE "uns_alarms_data" ADD IF NOT EXISTS "uns_path" varchar(255) NULL;

CREATE index if not exists uns_alarms_data_uns_idx ON "uns_alarms_data" ("uns");

alter table "uns_dashboard"  add if not exists "type" int2 DEFAULT 1;

CREATE TABLE if not exists "uns_tag" (
"id" int8 NOT NULL,
"topic" varchar(255) COLLATE "pg_catalog"."default",
"tag_name" varchar(255) COLLATE "pg_catalog"."default",
"is_deleted" bool DEFAULT false,
"create_at" timestamptz(6) DEFAULT now(),
CONSTRAINT "uns_tag_pkey" PRIMARY KEY ("id")
);

CREATE TABLE if not exists uns_attachment (
    "id" bigint NOT NULL PRIMARY KEY,
	"uns_alias" varchar(128) NOT NULL,
	"original_name" varchar(255) NOT NULL,
	"attachment_name" varchar(255) NOT NULL,
	"attachment_path" varchar(255) NULL,
	"extension_name" varchar(20) NULL,
	"create_at" timestamptz DEFAULT now() NULL
);

CREATE TABLE if not exists "supos"."uns_label" (
"id" BIGSERIAL PRIMARY KEY,
"label_name" varchar(255) COLLATE "pg_catalog"."default",
"create_at" timestamptz(6) DEFAULT now()
);

CREATE TABLE if not exists "supos"."uns_label_ref" (
"id" BIGSERIAL PRIMARY KEY,
"label_id" int8 NOT NULL,
"uns_id" bigint NOT NULL,
"create_at" timestamptz(6) DEFAULT now()
);

ALTER TABLE uns_label_ref DROP COLUMN id,
DROP CONSTRAINT if exists uns_label_ref_pkey,
ADD PRIMARY KEY (label_id, uns_id);

CREATE TABLE if not exists "supos"."supos_todo" (
"id" BIGSERIAL PRIMARY KEY,
"user_id" varchar(64) NOT NULL,
"username" varchar(64) NOT NULL,
"module_code" varchar(32),
"module_name" varchar(32),
"status" smallint DEFAULT 0 NULL,
"todo_msg" varchar(256) ,
"business_id" bigint,
"link" varchar(512),
"handler_user_id" varchar(64),
"handler_username" varchar(64),
"handler_time" timestamptz(6),
"create_at" timestamptz(6) DEFAULT now()
);

alter table supos_todo add if not exists "handler_time" timestamptz(6);
alter table supos_todo add if not exists "module_name" varchar(32);

COMMENT ON COLUMN "supos"."supos_todo"."user_id" IS '用户ID';
COMMENT ON COLUMN "supos"."supos_todo"."username" IS '用户名';
COMMENT ON COLUMN "supos"."supos_todo"."module_code" IS '模块编码';
COMMENT ON COLUMN "supos"."supos_todo"."status" IS '代办状态：0-未处理 1-已处理';
COMMENT ON COLUMN "supos"."supos_todo"."todo_msg" IS '事项信息';
COMMENT ON COLUMN "supos"."supos_todo"."business_id" IS '业务主键';
COMMENT ON COLUMN "supos"."supos_todo"."link" IS '链接';
COMMENT ON COLUMN "supos"."supos_todo"."handler_user_id" IS '处理人用户ID';
COMMENT ON COLUMN "supos"."supos_todo"."handler_username" IS '处理人用户名';
COMMENT ON COLUMN "supos"."supos_todo"."create_at" IS '创建时间';


CREATE TABLE if not exists "supos"."supos_example" (
"id" BIGSERIAL PRIMARY KEY,
"name" varchar(255) COLLATE "pg_catalog"."default",
"description" varchar(512) COLLATE "pg_catalog"."default",
"package_path" varchar(512) COLLATE "pg_catalog"."default",
"status" int2,
"type" int2,
"dashboard_type" int2,
"dashboard_id" varchar(64) COLLATE "pg_catalog"."default",
"dashboard_name" varchar(512) COLLATE "pg_catalog"."default",
"create_at" timestamptz(6) DEFAULT now());

COMMENT ON COLUMN "supos"."supos_example"."status" IS '安装状态：1-未安装，2-安装中，3已安装';
COMMENT ON COLUMN "supos"."supos_example"."type" IS '类型：1-OT 2-IT';

INSERT INTO "supos"."supos_example" ("id", "name", "description", "package_path", "status", "type", "dashboard_type", "dashboard_id", "dashboard_name", "create_at") VALUES (1, 'ot-demo', 'ot-demo', '/templates/example/ot.zip', 1, 1, NULL, NULL, NULL, '2025-02-25 08:31:06.039+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_example" ("id", "name", "description", "package_path", "status", "type", "dashboard_type", "dashboard_id", "dashboard_name", "create_at") VALUES (2, 'it-demo', 'it-demo', '/templates/example/it.zip', 1, 2, NULL, NULL, NULL, '2025-02-25 08:31:06.039+00') ON CONFLICT (id) DO NOTHING;

CREATE TABLE if not exists "supos"."uns_alarms_handler" (
"id" BIGSERIAL PRIMARY KEY,
"uns_id" bigint,
"user_id" varchar(64),
"username" varchar(256),
"create_at" timestamptz(6) DEFAULT now());


alter table supos_todo add if not exists "process_id" int8 NULL;
alter table supos_todo add if not exists "process_instance_id" varchar(64) NULL;

CREATE DATABASE camunda;

CREATE TABLE if not exists "supos"."supos_workflow_process" (
"id" BIGSERIAL PRIMARY KEY,
"description" varchar(512),
"process_definition_id" varchar(64),
"process_definition_name" varchar(256),
"process_definition_key" varchar(256),
"status" int2 default 0,
"deploy_id" varchar(64),
"deploy_name" varchar(256),
"deploy_time" timestamptz(6),
"bpmn_xml" text,
"create_at" timestamptz(6) DEFAULT now());

alter table uns_namespace add if not exists "display_name" varchar(512) NULL;

CREATE TABLE if not exists "supos_app_key" (
"id" BIGSERIAL PRIMARY KEY,
"app_secret_key" varchar(200) NOT NULL,
"app_secret_value" varchar(200) NOT NULL,
"status" int2 default 1,
"create_time" timestamptz(6) DEFAULT now());

CREATE TABLE if not exists "supos"."global_export_record" (
	id int8 NOT NULL,
	user_id varchar(64) NULL,
	file_path varchar(2000) NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL,
	confirm bool NULL,
	CONSTRAINT global_export_record_pk PRIMARY KEY (id)
);

CREATE TABLE if not exists "uns_history_delete_job" (
    "id" BIGSERIAL PRIMARY KEY,
    "alias" varchar(128) NOT NULL,
    "name" varchar(512) NOT NULL,
    "table_name" varchar(128) NULL,
    "path" text NOT NULL,
    "path_type" int2 NOT NULL,
    "data_type" int2 NULL,
    "fields" json NULL,
    "status" smallint DEFAULT 1 NULL,
    "create_at" timestamptz DEFAULT now() NULL
);
CREATE INDEX if not exists idx_uns_delete_alias ON uns_history_delete_job (alias);

CREATE TABLE if not exists "supos"."uns_person_config" (
  "id" BIGSERIAL PRIMARY KEY,
  "user_id" varchar(64) NOT NULL,
  "main_language" varchar(64) NULL,
  "create_at" timestamptz(6) DEFAULT now(),
  "update_at" timestamptz(6) DEFAULT now()
);

CREATE UNIQUE INDEX if not exists uq_idx_uns_person_config_uid ON supos.uns_person_config (user_id);

CREATE TABLE if not exists "supos_resource" (
"id" BIGSERIAL PRIMARY KEY,
"group_type" int2,
"parent_id" int8,
"type" int2,
"code" varchar(256) COLLATE "pg_catalog"."default",
"url" varchar(512) COLLATE "pg_catalog"."default",
"url_type" int2 DEFAULT '1'::smallint,
"open_type" int2 DEFAULT '1'::smallint,
"icon" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"description" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"sort" int4,
"remark" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"enable" bool DEFAULT true,
"update_at" timestamptz(6) DEFAULT now(),
"create_at" timestamptz(6) DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "supos"."supos_resource" (
"id" BIGSERIAL PRIMARY KEY,
"group_type" int2,
"parent_id" int8,
"type" int2,
"code" varchar(256) COLLATE "pg_catalog"."default",
"url" varchar(512) COLLATE "pg_catalog"."default",
"url_type" int2 DEFAULT '1'::smallint,
"open_type" int2 DEFAULT '1'::smallint,
"icon" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"description" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"sort" int4,
"remark" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
"enable" bool DEFAULT true,
"update_at" timestamptz(6) DEFAULT now(),
"create_at" timestamptz(6) DEFAULT now()
);

INSERT INTO "supos"."supos_resource" VALUES (61, 1, 60, 2, 'RoutingManagement', '/konga/home/', 2, 0, 'RoutingManagement.svg', 'menu.desc.konga', 1, '路由管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (62, 1, 60, 2, 'Authentication', '/keycloak/home/', 2, 0, 'Authentication.svg', 'menu.desc.keycloak', 2, '身份与访问管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (63, 1, 60, 2, 'UserManagement', '/account-management', 1, 0, 'UserManagement.svg', 'menu.desc.account', 4, '用户管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (64, 1, 60, 2, 'Dashboards', '/dashboards', 1, 0, 'Dashboards.svg', 'menu.desc.dashboards', 5, '数据看板', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (65, 1, 60, 2, 'ContainerManagement', '/portainer/home/', 2, 0, 'ContainerManagement.svg', 'menu.desc.dockerMgmt', 6, '容器管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (66, 1, 60, 2, 'AboutUs', '/aboutus', 1, 0, 'AboutUs.svg', 'menu.desc.aboutus', 8, '关于我们', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (67, 1, 60, 2, 'AdvancedUse', '/advanced-use', 1, 0, 'AdvancedUse.svg', 'menu.desc.advanceUse', 9, '高阶使用', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (10, 1, NULL, 2, 'Home', '/home', 1, 0, 'Home.svg', 'menu.desc.home', 1, '首页', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (20, 1, NULL, 2, 'SourceFlow', '/collection-flow', 1, 0, 'SourceFlow.svg', 'menu.desc.nodered.flow', 2, '数据连接', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (30, 1, NULL, 2, 'Namespace', '/uns', 1, 0, 'Namespace.svg', 'menu.desc.dataModeling', 3, '数据建模', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (40, 1, NULL, 2, 'EventFlow', '/EventFlow', 1, 0, 'EventFlow.svg', 'menu.desc.eventflow', 4, '事件流程', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (51, 1, 50, 2, 'CICD', '/gitea/home/user/login?redirect_to=/gitea/home/', 2, 0, 'CICD.svg', 'menu.desc.cicd', 1, 'CICD', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (52, 1, 50, 2, 'DBConnect', '/chat2db/home/connections', 2, 0, 'DBConnect.svg', 'menu.desc.dbconnect', 2, '数据源连接', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (54, 1, 50, 2, 'SQLEditor', '/chat2db/home/workspace', 2, 0, 'SQLEditor.svg', 'menu.desc.sqledit', 4, 'SQL编辑器', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (100, 2, 3, 2, 'ContainerManagement', '/portainer/home/', 2, 0, 'ContainerManagement.svg', 'menu.desc.dockerMgmt', 6, '容器管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (114, 2, 1, 2, 'SourceFlow', '/collection-flow', 1, 0, 'homeSourceFlow.svg', 'menu.desc.nodered.flow', 1, '数据连接', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (115, 2, 1, 2, 'Namespace', '/uns', 1, 0, 'homeNamespace.svg', 'menu.desc.dataModeling', 2, '数据建模', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (116, 2, 1, 2, 'EventFlow', '/EventFlow', 1, 0, 'homeEventFlow.svg', 'menu.desc.eventflow', 3, '事件流程', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (118, 2, 2, 2, 'CICD', '/gitea/home/user/login?redirect_to=/gitea/home/', 2, 0, 'CICD.svg', 'menu.desc.cicd', 1, 'CICD', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (119, 2, 2, 2, 'DBConnect', '/chat2db/home/connections', 2, 0, 'DBConnect.svg', 'menu.desc.dbconnect', 2, '数据源连接', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127485120514, 1, 40, 3, 'EventFlow.add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.401799+00', '2025-07-18 08:29:22.401799+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127589978113, 1, 40, 3, 'EventFlow.edit', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.426814+00', '2025-07-18 08:29:22.426814+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127694835714, 1, 40, 3, 'EventFlow.delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.450541+00', '2025-07-18 08:29:22.450541+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127791304706, 1, 40, 3, 'EventFlow.design', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.472525+00', '2025-07-18 08:29:22.472525+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127875190786, 1, 40, 3, 'EventFlow.save', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.494352+00', '2025-07-18 08:29:22.494352+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125127980048385, 1, 40, 3, 'EventFlow.deploy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.51839+00', '2025-07-18 08:29:22.51839+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125128110071809, 1, 40, 3, 'EventFlow.import', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.549476+00', '2025-07-18 08:29:22.549476+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125128219123714, 1, 40, 3, 'EventFlow.export', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.577649+00', '2025-07-18 08:29:22.577649+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125128428838914, 1, 40, 3, 'EventFlow.process', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.626496+00', '2025-07-18 08:29:22.626496+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125128529502210, 1, 40, 3, 'EventFlow.copy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.650649+00', '2025-07-18 08:29:22.650649+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129364168706, 1, 20, 3, 'SourceFlow.add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.848559+00', '2025-07-18 08:29:22.848559+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129456443393, 1, 20, 3, 'SourceFlow.edit', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.874359+00', '2025-07-18 08:29:22.874359+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129578078209, 1, 20, 3, 'SourceFlow.delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.898821+00', '2025-07-18 08:29:22.898821+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129674547202, 1, 20, 3, 'SourceFlow.design', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.922289+00', '2025-07-18 08:29:22.922289+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129775210497, 1, 20, 3, 'SourceFlow.save', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.946532+00', '2025-07-18 08:29:22.946532+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129867485185, 1, 20, 3, 'SourceFlow.deploy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.969701+00', '2025-07-18 08:29:22.969701+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125129976537090, 1, 20, 3, 'SourceFlow.import', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.994398+00', '2025-07-18 08:29:22.994398+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125130073006082, 1, 20, 3, 'SourceFlow.export', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.016462+00', '2025-07-18 08:29:23.016462+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125130165280770, 1, 20, 3, 'SourceFlow.node_management', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.039479+00', '2025-07-18 08:29:23.039479+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125130257555457, 1, 20, 3, 'SourceFlow.process', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.06356+00', '2025-07-18 08:29:23.06356+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125130362413057, 1, 20, 3, 'SourceFlow.copy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.085591+00', '2025-07-18 08:29:23.085591+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (103, 2, 3, 2, 'AboutUs', '/aboutus', 1, 0, 'AboutUs.svg', 'menu.desc.aboutus', 8, '关于我们', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (104, 2, 3, 2, 'AdvancedUse', '/advanced-use', 1, 0, 'AdvancedUse.svg', 'menu.desc.advanceUse', 9, '高阶使用', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (90, 2, NULL, 1, 'menu.tag.appspace', NULL, 1, 0, 'menu.tag.appspace.svg', NULL, 7, '应用集', 't', '2025-07-17 02:46:14.330044+00', '2025-07-17 02:46:14.330044+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (60, 1, NULL, 1, 'menu.tag.system', NULL, 1, 0, 'menu.tag.system.svg', NULL, 8, '系统管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131041890306, 1, 30, 3, 'Namespace.uns_import', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.248923+00', '2025-07-18 08:29:23.248923+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131134164993, 1, 30, 3, 'Namespace.uns_export', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.270463+00', '2025-07-18 08:29:23.270463+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131226439681, 1, 30, 3, 'Namespace.uns_batch_generation', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.292393+00', '2025-07-18 08:29:23.292393+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1, 2, NULL, 1, 'menu.tag.uns', NULL, 1, 0, 'menu.tag.uns.svg', NULL, 1, '数据管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131318714370, 1, 30, 3, 'Namespace.label_detail', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.314552+00', '2025-07-18 08:29:23.314552+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131415183362, 1, 30, 3, 'Namespace.label_delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.337476+00', '2025-07-18 08:29:23.337476+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131515846657, 1, 30, 3, 'Namespace.label_add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.36048+00', '2025-07-18 08:29:23.36048+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131603927042, 1, 30, 3, 'Namespace.folder_detail', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.383392+00', '2025-07-18 08:29:23.383392+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (2, 2, NULL, 1, 'menu.tag.devtools', NULL, 1, 0, 'menu.tag.devtools.svg', NULL, 2, '工具集', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131700396033, 1, 30, 3, 'Namespace.folder_delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.406334+00', '2025-07-18 08:29:23.406334+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131796865026, 1, 30, 3, 'Namespace.folder_copy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.429593+00', '2025-07-18 08:29:23.429593+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131897528321, 1, 30, 3, 'Namespace.folder_paste', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.452556+00', '2025-07-18 08:29:23.452556+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (3, 2, NULL, 1, 'menu.tag.system', NULL, 1, 0, 'menu.tag.system.svg', NULL, 3, '系统管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (21, 1, 20, 2, 'route.flowEditor', '/flow-editor', 1, 0, NULL, NULL, 1, '数据连接编辑', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (41, 1, 40, 2, 'route.eventFlowEditor', '/Editor', 1, 0, NULL, NULL, NULL, '事件流程编辑', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (73, 1, 60, 2, 'PluginManagement', '/plugin-management', 1, 0, 'PluginManagement.svg', 'menu.desc.PluginManagement', 14, '插件管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (101, 2, 3, 2, 'UserManagement', '/account-management', 1, 0, 'UserManagement.svg', 'menu.desc.account', 4, '用户管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (106, 2, 3, 2, 'PluginManagement', '/plugin-management', 1, 0, 'PluginManagement.svg', 'menu.desc.PluginManagement', 14, '插件管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (102, 2, 3, 2, 'Dashboards', '/dashboards', 1, 0, 'Dashboards.svg', 'menu.desc.dashboards', 5, '数据看板', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125131993997314, 1, 30, 3, 'Namespace.folder_add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.47466+00', '2025-07-18 08:29:23.47466+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132077883393, 1, 30, 3, 'Namespace.file_detail', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.4974+00', '2025-07-18 08:29:23.4974+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132182740993, 1, 30, 3, 'Namespace.file_delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.519358+00', '2025-07-18 08:29:23.519358+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132275015682, 1, 30, 3, 'Namespace.file_copy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.54147+00', '2025-07-18 08:29:23.54147+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132367290369, 1, 30, 3, 'Namespace.file_paste', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.563482+00', '2025-07-18 08:29:23.563482+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132455370754, 1, 30, 3, 'Namespace.file_add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.585485+00', '2025-07-18 08:29:23.585485+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132543451138, 1, 30, 3, 'Namespace.template_detail', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.607302+00', '2025-07-18 08:29:23.607302+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132644114434, 1, 30, 3, 'Namespace.template_delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.631487+00', '2025-07-18 08:29:23.631487+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132740583426, 1, 30, 3, 'Namespace.template_copy', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.654578+00', '2025-07-18 08:29:23.654578+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125132837052418, 1, 30, 3, 'Namespace.template_add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.67947+00', '2025-07-18 08:29:23.67947+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125133046767617, 1, 73, 3, 'PluginManagement.install', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.72547+00', '2025-07-18 08:29:23.72547+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125133134848002, 1, 73, 3, 'PluginManagement.unInstall', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.747555+00', '2025-07-18 08:29:23.747555+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125133227122690, 1, 73, 3, 'PluginManagement.update', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:23.768363+00', '2025-07-18 08:29:23.768363+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125134963564545, 1, 63, 3, 'UserManagement.add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.183467+00', '2025-07-18 08:29:24.183467+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135055839233, 1, 63, 3, 'UserManagement.edit', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.205529+00', '2025-07-18 08:29:24.205529+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135148113921, 1, 63, 3, 'UserManagement.enable', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.229494+00', '2025-07-18 08:29:24.229494+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135252971521, 1, 63, 3, 'UserManagement.disable', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.252455+00', '2025-07-18 08:29:24.252455+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135349440513, 1, 63, 3, 'UserManagement.resetPassword', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.276391+00', '2025-07-18 08:29:24.276391+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135445909505, 1, 63, 3, 'UserManagement.delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.298443+00', '2025-07-18 08:29:24.298443+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125135550767105, 1, 63, 3, 'UserManagement.role_setting', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.323439+00', '2025-07-18 08:29:24.323439+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136301547521, 1, 64, 3, 'Dashboards.add', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.50154+00', '2025-07-18 08:29:24.50154+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136389627906, 1, 64, 3, 'Dashboards.preview', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.523482+00', '2025-07-18 08:29:24.523482+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136486096898, 1, 64, 3, 'Dashboards.design', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.54665+00', '2025-07-18 08:29:24.54665+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136578371586, 1, 64, 3, 'Dashboards.edit', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.569571+00', '2025-07-18 08:29:24.569571+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136679034881, 1, 64, 3, 'Dashboards.delete', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.59253+00', '2025-07-18 08:29:24.59253+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136775503874, 1, 64, 3, 'Dashboards.save', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.615455+00', '2025-07-18 08:29:24.615455+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136871972866, 1, 64, 3, 'Dashboards.export', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.637458+00', '2025-07-18 08:29:24.637458+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125136960053249, 1, 64, 3, 'Dashboards.import', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.66057+00', '2025-07-18 08:29:24.66057+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125137173962753, 1, 10, 3, 'Home.import', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.714657+00', '2025-07-18 08:29:24.714657+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125137283014657, 1, 10, 3, 'Home.export', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:24.736467+00', '2025-07-18 08:29:24.736467+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (1946125128323981313, 1, 40, 3, 'EventFlow.node_management', NULL, 1, 1, NULL, NULL, NULL, NULL, 't', '2025-07-18 08:29:22.603813+00', '2025-07-18 08:29:22.603813+00') ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (120, 2, 2, 2, 'SQLEditor', '/chat2db/home/workspace', 2, 0, 'SQLEditor.svg', 'menu.desc.sqledit', 4, 'SQL编辑器', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (121, 2, 3, 2, 'Authentication', '/keycloak/home/', 2, 0, 'Authentication.svg', 'menu.desc.keycloak', 2, '身份与访问管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (122, 2, 3, 2, 'RoutingManagement', '/konga/home/', 2, 0, 'RoutingManagement.svg', 'menu.desc.konga', 1, '路由管理', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (641, 1, 64, 2, 'route.dashboardsPreview', '/preview', 1, 0, NULL, NULL, NULL, '数据看板预览', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (50, 1, NULL, 1, 'menu.tag.devtools', NULL, 1, 0, 'menu.tag.devtools.svg', NULL, 6, '工具集', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;
INSERT INTO "supos"."supos_resource" VALUES (4, 1, NULL, 1, 'menu.tag.appspace', NULL, 1, 0, 'menu.tag.appspace.svg', NULL, 7, '应用集', 't', NULL, NULL) ON CONFLICT (id) DO NOTHING;

CREATE TABLE if not exists "supos"."uns_export_record" (
	id int8 NOT NULL,
	user_id varchar(64) NULL,
	file_path varchar(2000) NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL,
	confirm bool NULL,
	CONSTRAINT uns_export_record_pk PRIMARY KEY (id)
);

alter table uns_namespace add if not exists "extend_field_flags" integer NULL default 0;