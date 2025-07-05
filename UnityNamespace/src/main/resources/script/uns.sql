
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
