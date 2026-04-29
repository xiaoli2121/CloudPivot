# CloudPivot Spring Boot Modules

The backend uses a modular monolith layout so platform capabilities remain clearly bounded while staying easy to bootstrap and evolve.

- `cloudpivot-boot`: application bootstrap and module assembly
- `cloudpivot-common`: shared backend foundations
- `cloudpivot-auth`: authentication, token, and session services
- `cloudpivot-iam`: identity, org, role, and permission capabilities
- `cloudpivot-system`: dictionaries, parameters, files, logs, notifications, and jobs
- `cloudpivot-metadata`: metadata center
- `cloudpivot-generator`: code generator services
- `cloudpivot-designer`: designer backend services
- `cloudpivot-workflow`: workflow services
- `cloudpivot-rule`: rule engine services
- `cloudpivot-integration`: integration and connector services
- `cloudpivot-plugin`: plugin registry and lifecycle services
- `cloudpivot-tenant`: tenant and operations services

## Current Foundation APIs

The current foundation slice exposes demo-ready Community 1.0 APIs for:

- `/api/health`
- `/api/auth/login`
- `/api/auth/current-user`
- `/api/iam/menu-tree`
- `/api/iam/users`
- `/api/iam/roles`
- `/api/system/dictionaries`
- `/api/system/announcements`
- `/api/metadata/apps`

## Current Security Baseline

The current backend slice now uses Shiro bearer authentication.

- `/api/health` and `/api/auth/login` allow anonymous access
- all other `/api/**` endpoints require `Authorization: Bearer <access-token>`
- the current demo login returns `mock-access-token`, which can be used for local integration and runtime verification

## Current Persistence Baseline

The current backend slice now boots with a real relational persistence layer.

- MyBatis-Plus is used as the current persistence access foundation
- local runtime and tests default to H2 in MySQL compatibility mode
- schema and seed data are initialized automatically from `schema.sql` and `data.sql`
- current `auth`, `iam`, `system`, and `metadata` foundation APIs now read from database-backed tables instead of in-memory demo lists
- low-code metadata main tables are initialized for `meta_app`, `meta_object`, `meta_object_field`, `meta_page`, `meta_component`, and `meta_publish_version`

## Local Startup

Windows development can start the backend with:

```bat
bat\start-backend.bat
```
