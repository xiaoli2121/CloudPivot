-- ============================================================
-- CloudPivot 社区版 1.0 种子数据
-- ============================================================

-- -----------------------------------------------------------
-- 组织
-- -----------------------------------------------------------

insert into iam_org (id, org_name, parent_id, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'CloudPivot Product Center', null, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'East Delivery Center', 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 'Solution Center', 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 用户
-- BCrypt hashes:
--   admin123       -> $2a$10$/t8wedrcCi9dK9gvQCGRTuwCDPBJ6GjzYKGXim47qQIueba0dsOL2
--   consultant123  -> $2a$10$yaFd0KjbcyJrxG42cpX3FuP9xrWa.aWOwBmHSjeI.C0YxFyTEQs.6
--   analyst123     -> $2a$10$6uEZoRGiywNPzoxcbkYa6OVccczLGrkRWrfcBr94y7bqMmSFtQBY6
-- access_token 列保留兼容旧数据，但不再作为鉴权依据
-- -----------------------------------------------------------

insert into iam_user (id, org_id, user_name, login_name, password_hash, phone, email, user_status, super_admin_flag, auth_version, lock_expire_time, last_login_time, access_token, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'Platform Admin', 'admin', '$2a$10$/t8wedrcCi9dK9gvQCGRTuwCDPBJ6GjzYKGXim47qQIueba0dsOL2', null, null, 'ENABLED', 1, 0, null, null, 'mock-access-token', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 2, 'Implementation Consultant', 'consultant', '$2a$10$yaFd0KjbcyJrxG42cpX3FuP9xrWa.aWOwBmHSjeI.C0YxFyTEQs.6', null, null, 'ENABLED', 0, 0, null, null, 'consultant-access-token', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 3, 'Business Analyst', 'analyst', '$2a$10$6uEZoRGiywNPzoxcbkYa6OVccczLGrkRWrfcBr94y7bqMmSFtQBY6', null, null, 'DISABLED', 0, 0, null, null, 'analyst-access-token', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 角色
-- -----------------------------------------------------------

insert into iam_role (id, role_code, role_name, data_scope, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'PLATFORM_ADMIN', 'Platform Admin', 'ALL', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'IMPLEMENTATION_CONSULTANT', 'Implementation Consultant', 'ORG_AND_CHILDREN', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 'BUSINESS_ANALYST', 'Business Analyst', 'SELF', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 用户-角色关联
-- -----------------------------------------------------------

insert into iam_user_role_rel (id, user_id, role_id, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 2, 2, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 3, 3, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 菜单与按钮资源（含 menu_type: DIR / MENU / BUTTON）
-- -----------------------------------------------------------

insert into iam_menu (id, menu_code, menu_name, menu_type, path, parent_id, icon, sort_no, visible_flag, permission_code, api_path, component_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
-- 顶级目录
(1,  'dashboard',     'Dashboard',           'DIR',   '/dashboard',     null, 'Odometer',     1, 1, null,                     null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2,  'system',        'Platform Management', 'DIR',   '/system',        null, 'Setting',      2, 1, null,                     null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3,  'lowcode',       'Low-Code',            'DIR',   '/lowcode',       null, 'Monitor',      3, 1, null,                     null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(4,  'config',        'System Config',       'DIR',   '/config',        null, 'Tools',        4, 1, null,                     null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
-- 系统管理菜单
(10, 'user-mgmt',    'User Management',     'MENU',  '/system/users',   2,   'User',         1, 1, 'menu:user-management',    null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(11, 'user-add',     'Add User',            'BUTTON', '',               10,  null,            1, 1, 'btn:user:add',            '/api/iam/users:POST', null,           1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(12, 'user-edit',    'Edit User',           'BUTTON', '',               10,  null,            2, 1, 'btn:user:edit',           '/api/iam/users:PUT', null,            1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(13, 'user-delete',  'Delete User',         'BUTTON', '',               10,  null,            3, 1, 'btn:user:delete',         '/api/iam/users:DELETE', null,         1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(20, 'role-mgmt',    'Role Management',     'MENU',  '/system/roles',   2,   'Lock',         2, 1, 'menu:role-management',    null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(21, 'role-add',     'Add Role',            'BUTTON', '',               20,  null,            1, 1, 'btn:role:add',            '/api/iam/roles:POST', null,           1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(22, 'role-edit',    'Edit Role',           'BUTTON', '',               20,  null,            2, 1, 'btn:role:edit',           '/api/iam/roles:PUT', null,            1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(23, 'role-delete',  'Delete Role',         'BUTTON', '',               20,  null,            3, 1, 'btn:role:delete',         '/api/iam/roles:DELETE', null,         1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(30, 'org-mgmt',     'Organization Management', 'MENU', '/system/orgs', 2, 'OfficeBuilding', 3, 1, 'menu:org-management',    null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
-- 系统配置菜单
(40, 'dict-mgmt',    'Dictionary Management','MENU', '/config/dicts',   4,   'Document',     1, 1, 'menu:dict-management',    null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(50, 'announcement-mgmt', 'Announcement Management', 'MENU', '/config/announcements', 4, 'Bell', 2, 1, 'menu:announcement-management', null, null, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(60, 'login-log-mgmt', 'Login Log',         'MENU',  '/config/login-logs', 4, 'List',      3, 1, 'menu:login-log-management', null, null,               1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
-- 低代码菜单
(70, 'app-mgmt',     'App Management',      'MENU',  '/lowcode/apps',   3,   'Grid',         1, 1, 'menu:app-management',     null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(80, 'designer',     'Designer',            'MENU',  '/lowcode/designer', 3, 'Edit',         2, 1, 'menu:designer',           null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(90, 'publish-mgmt', 'Publish Management',  'MENU',  '/lowcode/publish', 3,  'Upload',       3, 1, 'menu:publish-management', null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(100,'plugin-mgmt',  'Plugin Management',   'MENU',  '/lowcode/plugins', 3, 'Connection',   4, 1, 'menu:plugin-management',  null, null,                   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 角色-权限关联（iam_role_permission_rel）
-- -----------------------------------------------------------

-- PLATFORM_ADMIN: 全部权限
insert into iam_role_permission_rel (id, role_id, resource_type, permission_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
-- 菜单权限
(1,  1, 'MENU', 'menu:user-management',         1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2,  1, 'MENU', 'menu:role-management',         1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3,  1, 'MENU', 'menu:org-management',          1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(4,  1, 'MENU', 'menu:dict-management',         1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(5,  1, 'MENU', 'menu:announcement-management', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(6,  1, 'MENU', 'menu:login-log-management',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(7,  1, 'MENU', 'menu:app-management',          1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(8,  1, 'MENU', 'menu:designer',                1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(9,  1, 'MENU', 'menu:publish-management',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(10, 1, 'MENU', 'menu:plugin-management',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
-- 按钮权限
(11, 1, 'BUTTON', 'btn:user:add',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(12, 1, 'BUTTON', 'btn:user:edit',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(13, 1, 'BUTTON', 'btn:user:delete',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(14, 1, 'BUTTON', 'btn:role:add',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(15, 1, 'BUTTON', 'btn:role:edit',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(16, 1, 'BUTTON', 'btn:role:delete',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
-- API 权限
(20, 1, 'API', 'api:iam/users:get',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(21, 1, 'API', 'api:iam/users:post',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(22, 1, 'API', 'api:iam/users:put',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(23, 1, 'API', 'api:iam/users:delete',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(24, 1, 'API', 'api:iam/roles:get',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(25, 1, 'API', 'api:iam/roles:post',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(26, 1, 'API', 'api:iam/roles:put',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(27, 1, 'API', 'api:iam/roles:delete',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(28, 1, 'API', 'api:iam/orgs:get',        1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(29, 1, 'API', 'api:iam/orgs:post',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(30, 1, 'API', 'api:iam/orgs:put',        1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(31, 1, 'API', 'api:iam/orgs:delete',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(32, 1, 'API', 'api:iam/menus:get',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(33, 1, 'API', 'api:iam/menus:post',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(34, 1, 'API', 'api:iam/menus:put',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(35, 1, 'API', 'api:iam/menus:delete',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(36, 1, 'API', 'api:system/dicts:get',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(37, 1, 'API', 'api:system/dicts:post',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(38, 1, 'API', 'api:system/dicts:put',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(39, 1, 'API', 'api:system/dicts:delete', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(40, 1, 'API', 'api:system/announcements:get',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(41, 1, 'API', 'api:system/announcements:post',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(42, 1, 'API', 'api:system/announcements:put',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(43, 1, 'API', 'api:system/announcements:delete', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(44, 1, 'API', 'api:system/login-logs:get',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(45, 1, 'API', 'api:metadata/apps:get',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(46, 1, 'API', 'api:metadata/apps:post',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(47, 1, 'API', 'api:metadata/apps:put',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(48, 1, 'API', 'api:metadata/apps:delete',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(49, 1, 'API', 'api:metadata/objects:get',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(50, 1, 'API', 'api:metadata/objects:post',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(51, 1, 'API', 'api:metadata/objects:put',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(52, 1, 'API', 'api:metadata/objects:delete', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(53, 1, 'API', 'api:metadata/designer:get',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(54, 1, 'API', 'api:metadata/designer:put',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(55, 1, 'API', 'api:metadata/publish:post',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(56, 1, 'API', 'api:metadata/publish:get',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(57, 1, 'API', 'api:plugins:get',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(58, 1, 'API', 'api:plugins:post',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(59, 1, 'API', 'api:plugins:put',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(60, 1, 'API', 'api:plugins:delete',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(61, 1, 'API', 'api:metadata/fields:post',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(62, 1, 'API', 'api:metadata/fields:put',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(63, 1, 'API', 'api:metadata/fields:delete', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- IMPLEMENTATION_CONSULTANT: 低代码 + 部分平台管理
insert into iam_role_permission_rel (id, role_id, resource_type, permission_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(100, 2, 'MENU', 'menu:user-management',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(101, 2, 'MENU', 'menu:app-management',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(102, 2, 'MENU', 'menu:designer',             1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(103, 2, 'MENU', 'menu:publish-management',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(104, 2, 'MENU', 'menu:plugin-management',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(105, 2, 'BUTTON', 'btn:user:edit',           1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(120, 2, 'API', 'api:iam/users:get',          1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(121, 2, 'API', 'api:iam/users:put',          1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(130, 2, 'API', 'api:metadata/apps:get',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(131, 2, 'API', 'api:metadata/apps:post',     1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(132, 2, 'API', 'api:metadata/apps:put',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(133, 2, 'API', 'api:metadata/objects:get',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(134, 2, 'API', 'api:metadata/objects:post',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(135, 2, 'API', 'api:metadata/objects:put',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(136, 2, 'API', 'api:metadata/designer:get',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(137, 2, 'API', 'api:metadata/designer:put',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(138, 2, 'API', 'api:metadata/publish:post',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(139, 2, 'API', 'api:metadata/publish:get',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(140, 2, 'API', 'api:plugins:get',            1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(141, 2, 'API', 'api:plugins:put',            1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(142, 2, 'API', 'api:metadata/fields:post',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(143, 2, 'API', 'api:metadata/fields:put',    1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(144, 2, 'API', 'api:metadata/fields:delete', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- BUSINESS_ANALYST: 应用查询 + 设计器读取 + 有限发布查看
insert into iam_role_permission_rel (id, role_id, resource_type, permission_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(200, 3, 'MENU', 'menu:app-management',       1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(201, 3, 'MENU', 'menu:designer',             1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(202, 3, 'MENU', 'menu:publish-management',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(210, 3, 'API', 'api:metadata/apps:get',      1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(211, 3, 'API', 'api:metadata/designer:get',  1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(212, 3, 'API', 'api:metadata/publish:get',   1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 角色-自定义数据范围关联（iam_role_data_scope_rel）
-- IMPLEMENTATION_CONSULTANT 使用 ORG_AND_CHILDREN，自定义范围覆盖组织 1 及子组织
-- -----------------------------------------------------------

insert into iam_role_data_scope_rel (id, role_id, org_id, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 2, 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 2, 2, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 2, 3, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 系统字典
-- -----------------------------------------------------------

insert into sys_dict (id, dict_code, dict_name, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'USER_STATUS', 'User Status', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'APP_STATUS', 'App Status', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

insert into sys_dict_item (id, dict_id, item_label, item_value, sort_no, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'Enabled', 'ENABLED', 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 1, 'Disabled', 'DISABLED', 2, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 2, 'Active', 'ACTIVE', 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(4, 2, 'Planning', 'PLANNING', 2, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 系统公告
-- -----------------------------------------------------------

insert into sys_announcement (id, title, level_code, publisher_name, publish_time, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'Community 1.0 Kickoff', 'INFO', 'Product Committee', '2026-04-13 09:00', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'Permission Freeze Window', 'WARN', 'Platform Architecture Group', '2026-04-15 18:00', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 'Metadata Tables Landed', 'INFO', 'Low Code Engine Team', '2026-04-16 10:00', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 元数据 - 应用
-- -----------------------------------------------------------

insert into meta_app (id, app_code, app_name, owner_name, app_status, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'crm-leads', 'Lead CRM', 'Delivery Team', 'ACTIVE', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'ops-workbench', 'Ops Workbench', 'Platform Team', 'PLANNING', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 'customer-service', 'Customer Service Hub', 'Solution Center', 'ACTIVE', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 元数据 - 业务对象与字段
-- -----------------------------------------------------------

insert into meta_object (id, app_id, object_code, object_name, store_type, primary_field_code, status_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'lead', 'Lead', 'RELATIONAL', 'lead_name', 'PUBLISHED', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

insert into meta_object_field (id, object_id, field_code, field_name, field_type, required_flag, sort_no, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'lead_name', 'Lead Name', 'TEXT', 1, 1, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 1, 'customer_name', 'Customer Name', 'TEXT', 1, 2, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 1, 'follow_status', 'Follow Status', 'SELECT', 0, 3, 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 元数据 - 页面与组件
-- -----------------------------------------------------------

insert into meta_page (id, app_id, page_code, page_name, page_type, route_path, status_code, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'lead-list', 'Lead List', 'LIST', '/crm/leads', 'PUBLISHED', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

insert into meta_component (id, page_id, component_code, component_type, parent_id, sort_no, component_props, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'page-root', 'PAGE_CONTAINER', null, 1, '{"title":"Lead List"}', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 1, 'search-form', 'FORM', 1, 2, '{"mode":"inline"}', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 1, 'lead-table', 'TABLE', 1, 3, '{"columns":["lead_name","customer_name","follow_status"]}', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);

-- -----------------------------------------------------------
-- 元数据 - 发布版本
-- -----------------------------------------------------------

insert into meta_publish_version (id, app_id, version_code, version_status, snapshot_summary, published_time, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 1, 'v1.0.0', 'PUBLISHED', 'Lead CRM initial published snapshot', timestamp '2026-04-14 08:00:00', 1, timestamp '2026-04-14 08:00:00', 1, timestamp '2026-04-14 08:00:00', 0, 0);

-- -----------------------------------------------------------
-- 插件注册
-- -----------------------------------------------------------

insert into plugin_registry (id, plugin_code, plugin_name, plugin_type, plugin_version, status_code, entry_point, description, created_by, created_time, updated_by, updated_time, deleted_flag, version_no) values
(1, 'core-table', 'Core Table Renderer', 'COMPONENT', '1.0.0', 'ACTIVE', 'plugin://core/table', 'Provides the built-in table renderer used by runtime pages.', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(2, 'core-form', 'Core Form Renderer', 'COMPONENT', '1.0.0', 'ACTIVE', 'plugin://core/form', 'Provides the built-in form renderer used by runtime pages.', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0),
(3, 'crm-template', 'CRM Template Pack', 'TEMPLATE', '1.0.0', 'BETA', 'plugin://template/crm', 'Provides starter low-code templates for CRM scenarios.', 1, timestamp '2026-04-14 00:00:00', 1, timestamp '2026-04-14 00:00:00', 0, 0);
