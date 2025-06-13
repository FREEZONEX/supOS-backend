CREATE TABLE if not exists supos_node_flows (
    id bigint NOT NULL PRIMARY KEY,
    flow_id varchar NULL,
	flow_name varchar Not NULL,
	flow_status varchar Not NULL,
	flow_data TEXT NULL,
	description varchar NULL,
	template varchar null DEFAULT 'node-red',
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL
);

CREATE TABLE if not exists supos_node_flow_models (
    parent_id bigint NULL,
	topic varchar NULL,
	alias varchar NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX if not exists  idx_flow_alias ON supos_node_flow_models (alias);

CREATE TABLE if not exists supos_node_server (
    id varchar NOT NULL PRIMARY KEY,
    server_name varchar NOT NULL,
    protocol_name varchar NOT NULL,
	config_json varchar(2000) NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL
);

CREATE TABLE if not exists supos_node_protocol (
    name varchar NOT NULL PRIMARY KEY,
    description varchar NULL,
    server_conn varchar NULL,
	client_config_json varchar(2000) NULL,
	server_config_json varchar(2000) NULL,
	custom int2 default 1,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL
);

alter table supos_node_flow_models add if not exists "alias" varchar(128) NULL;
