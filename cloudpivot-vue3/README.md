# CloudPivot Vue3 Workspace

The frontend workspace is organized around application entrypoints and shared packages.

- `apps/admin-shell/`: primary management console shell
- `apps/designer-studio/`: low-code designer workbench
- `apps/runtime-app/`: low-code runtime application container
- `apps/portal-app/`: reserved portal and external app entrypoint
- `packages/`: shared core, UI, SDK, runtime, designer, and utility packages
- `plugins/`: frontend-side official and example plugin resources

## Quick Start

```bash
npm install
npm run dev
```

## Current Admin Shell Scope

The current `admin-shell` now provides a first Community 1.0 management-console slice:

- demo login flow
- dashboard summary cards
- user management view
- role management view
- metadata app center view
- backend health and platform API integration through the Vite `/api` proxy
