-- ============================================================
-- CloudPivot 社区版 1.0 数据库表结构
-- 兼容 H2(MySQL模式) / MySQL / PostgreSQL
-- ============================================================

-- -----------------------------------------------------------
-- IAM 领域
-- -----------------------------------------------------------

create table iam_org (
    id bigint primary key,
    org_name varchar(128) not null,
    parent_id bigint,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create table iam_user (
    id bigint primary key,
    org_id bigint not null,
    user_name varchar(64) not null,
    login_name varchar(64) not null,
    password_hash varchar(255) not null,
    phone varchar(32),
    email varchar(128),
    user_status varchar(32) not null default 'ENABLED',
    super_admin_flag smallint not null default 0,
    auth_version bigint not null default 0,
    lock_expire_time timestamp,
    last_login_time timestamp,
    access_token varchar(128),
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_iam_user_login_name on iam_user (login_name);

create table iam_role (
    id bigint primary key,
    role_code varchar(64) not null,
    role_name varchar(128) not null,
    data_scope varchar(64) not null default 'SELF',
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_iam_role_role_code on iam_role (role_code);

create table iam_user_role_rel (
    id bigint primary key,
    user_id bigint not null,
    role_id bigint not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_iam_user_role_user on iam_user_role_rel (user_id);
create index idx_iam_user_role_role on iam_user_role_rel (role_id);

create table iam_menu (
    id bigint primary key,
    menu_code varchar(64) not null,
    menu_name varchar(128) not null,
    menu_type varchar(32) not null default 'MENU',
    path varchar(128) not null,
    parent_id bigint,
    icon varchar(128),
    sort_no integer not null,
    visible_flag smallint not null default 1,
    permission_code varchar(128),
    api_path varchar(255),
    component_code varchar(128),
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_iam_menu_code on iam_menu (menu_code);

create table iam_role_permission_rel (
    id bigint primary key,
    role_id bigint not null,
    resource_type varchar(32) not null,
    permission_code varchar(128) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_iam_role_perm_role on iam_role_permission_rel (role_id);
create unique index uk_iam_role_perm on iam_role_permission_rel (role_id, permission_code);

create table iam_role_data_scope_rel (
    id bigint primary key,
    role_id bigint not null,
    org_id bigint not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_iam_role_data_scope on iam_role_data_scope_rel (role_id, org_id);

-- -----------------------------------------------------------
-- System 领域
-- -----------------------------------------------------------

create table sys_dict (
    id bigint primary key,
    dict_code varchar(64) not null,
    dict_name varchar(128) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_sys_dict_dict_code on sys_dict (dict_code);

create table sys_dict_item (
    id bigint primary key,
    dict_id bigint not null,
    item_label varchar(128) not null,
    item_value varchar(64) not null,
    sort_no integer not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_sys_dict_item_dict on sys_dict_item (dict_id);

create table sys_announcement (
    id bigint primary key,
    title varchar(255) not null,
    level_code varchar(32) not null,
    publisher_name varchar(128) not null,
    publish_time varchar(32) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create table sys_login_log (
    id bigint primary key,
    user_id bigint,
    login_name varchar(64) not null,
    action_code varchar(32) not null,
    result_code varchar(32) not null,
    token_jti varchar(64),
    session_id varchar(64),
    login_ip varchar(64),
    user_agent varchar(512),
    trace_id varchar(64),
    event_time timestamp not null
);

create index idx_sys_login_log_user on sys_login_log (user_id);
create index idx_sys_login_log_time on sys_login_log (event_time);

-- -----------------------------------------------------------
-- Metadata 领域（低代码引擎）
-- -----------------------------------------------------------

create table meta_app (
    id bigint primary key,
    app_code varchar(64) not null,
    app_name varchar(128) not null,
    owner_name varchar(128) not null,
    app_status varchar(32) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_meta_app_app_code on meta_app (app_code);

create table meta_object (
    id bigint primary key,
    app_id bigint not null,
    object_code varchar(64) not null,
    object_name varchar(128) not null,
    store_type varchar(32) not null,
    primary_field_code varchar(64) not null,
    status_code varchar(32) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_meta_object_app on meta_object (app_id);

create table meta_object_field (
    id bigint primary key,
    object_id bigint not null,
    field_code varchar(64) not null,
    field_name varchar(128) not null,
    field_type varchar(32) not null,
    required_flag smallint not null default 0,
    sort_no integer not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_meta_field_object on meta_object_field (object_id);

create table meta_page (
    id bigint primary key,
    app_id bigint not null,
    page_code varchar(64) not null,
    page_name varchar(128) not null,
    page_type varchar(32) not null,
    route_path varchar(128) not null,
    status_code varchar(32) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_meta_page_app on meta_page (app_id);

create table meta_component (
    id bigint primary key,
    page_id bigint not null,
    component_code varchar(64) not null,
    component_type varchar(64) not null,
    parent_id bigint,
    sort_no integer not null,
    component_props text,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_meta_component_page on meta_component (page_id);

create table meta_publish_version (
    id bigint primary key,
    app_id bigint not null,
    version_code varchar(64) not null,
    version_status varchar(32) not null,
    snapshot_summary varchar(255) not null,
    snapshot_content text,
    published_time timestamp not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create index idx_meta_publish_app on meta_publish_version (app_id);

-- -----------------------------------------------------------
-- Plugin 领域
-- -----------------------------------------------------------

create table plugin_registry (
    id bigint primary key,
    plugin_code varchar(64) not null,
    plugin_name varchar(128) not null,
    plugin_type varchar(64) not null,
    plugin_version varchar(32) not null,
    status_code varchar(32) not null,
    entry_point varchar(255) not null,
    description varchar(255) not null,
    created_by bigint not null,
    created_time timestamp not null,
    updated_by bigint not null,
    updated_time timestamp not null,
    deleted_flag smallint not null default 0,
    version_no bigint not null default 0
);

create unique index uk_plugin_registry_plugin_code on plugin_registry (plugin_code);
