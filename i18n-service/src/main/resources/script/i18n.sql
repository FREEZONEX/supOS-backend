CREATE TABLE IF NOT EXISTS "supos"."supos_i18n_language" (
"id" BIGSERIAL PRIMARY KEY,
"language_code" varchar(256),
"language_name" varchar(256),
"language_type" int2,
"has_used" bool DEFAULT true
);

CREATE TABLE IF NOT EXISTS "supos"."supos_i18n_module" (
"id" BIGSERIAL PRIMARY KEY,
"module_code" varchar(128),
"module_name" varchar(200),
"module_type" int2
);

CREATE TABLE IF NOT EXISTS "supos"."supos_i18n_version" (
"id" BIGSERIAL PRIMARY KEY,
"module_code" varchar(128),
"module_version" varchar(256)
);

CREATE TABLE IF NOT EXISTS "supos"."supos_i18n_resource" (
"id" BIGSERIAL PRIMARY KEY,
"i18n_key" varchar(255),
"i18n_value" varchar(2000),
"language_code" varchar(256),
"module_code" varchar(128),
"module_version" varchar(256),
"modify_flag" varchar(20),
"create_at" timestamptz(6) DEFAULT now(),
"modify_at" timestamptz(6) DEFAULT now()
);

CREATE TABLE if not exists "supos"."supos_i18n_export_record" (
    "id" BIGSERIAL PRIMARY KEY,
	user_id varchar(64) NULL,
	file_path varchar(2000) NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL,
	confirm bool NULL
);