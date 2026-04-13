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
