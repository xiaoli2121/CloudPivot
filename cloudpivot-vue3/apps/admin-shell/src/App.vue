<template>
  <main class="workspace">
    <section v-if="!session.accessToken" class="login-shell">
      <div class="login-panel">
        <p class="eyebrow">Community 1.0</p>
        <h1>CloudPivot Admin Console</h1>
        <p class="lead">
          Unified operations console for IAM, system configuration, low-code applications, and plugin registration.
        </p>
        <label class="field">
          <span>Username</span>
          <input
            data-testid="username"
            v-model="loginForm.loginName"
            type="text"
            placeholder="Enter username"
          >
        </label>
        <label class="field">
          <span>Password</span>
          <input
            data-testid="password"
            v-model="loginForm.password"
            type="password"
            placeholder="Enter password"
          >
        </label>
        <button
          data-testid="login-button"
          class="primary-btn"
          type="button"
          :disabled="session.loading"
          @click="handleLogin"
        >
          {{ session.loading ? 'Signing In...' : 'Sign In' }}
        </button>
        <p v-if="session.errorMessage" class="error-text">{{ session.errorMessage }}</p>
        <p class="hint-text">Demo account: `admin / admin123`</p>
      </div>
    </section>

    <section v-else class="app-shell">
      <aside class="sidebar">
        <div class="brand-block">
          <p class="eyebrow">CloudPivot</p>
          <h2>Community Console</h2>
          <p>{{ session.currentUser?.orgName }}</p>
        </div>
        <nav class="nav-list">
          <button
            data-testid="nav-dashboard"
            class="nav-item"
            :class="{ active: session.activeView === 'dashboard' }"
            type="button"
            @click="session.activeView = 'dashboard'"
          >
            Dashboard
          </button>
          <button
            data-testid="nav-users"
            class="nav-item"
            :class="{ active: session.activeView === 'users' }"
            type="button"
            @click="session.activeView = 'users'"
          >
            Users
          </button>
          <button
            data-testid="nav-roles"
            class="nav-item"
            :class="{ active: session.activeView === 'roles' }"
            type="button"
            @click="session.activeView = 'roles'"
          >
            Roles
          </button>
          <button
            data-testid="nav-apps"
            class="nav-item"
            :class="{ active: session.activeView === 'apps' }"
            type="button"
            @click="session.activeView = 'apps'"
          >
            Apps
          </button>
          <button
            data-testid="nav-plugins"
            class="nav-item"
            :class="{ active: session.activeView === 'plugins' }"
            type="button"
            @click="session.activeView = 'plugins'"
          >
            Plugins
          </button>
        </nav>
      </aside>

      <section class="content">
        <header class="topbar">
          <div>
            <p class="eyebrow">Platform Snapshot</p>
            <h1>{{ viewTitle }}</h1>
          </div>
          <div class="topbar-meta">
            <div class="status-chip">
              <span>Backend</span>
              <strong>{{ session.health?.status ?? 'UNKNOWN' }}</strong>
            </div>
            <div class="user-chip">
              <span>{{ session.currentUser?.userName }}</span>
              <strong>{{ session.currentUser?.roles[0] }}</strong>
            </div>
          </div>
        </header>

        <section v-if="session.activeView === 'dashboard'" class="view-grid">
          <article class="metric-card">
            <p>Total Users</p>
            <strong>{{ session.users.total }}</strong>
          </article>
          <article class="metric-card">
            <p>Roles</p>
            <strong>{{ session.roles.length }}</strong>
          </article>
          <article class="metric-card">
            <p>Apps</p>
            <strong>{{ session.apps.length }}</strong>
          </article>
          <article class="metric-card">
            <p>Plugins</p>
            <strong>{{ session.plugins.length }}</strong>
          </article>
          <article class="panel wide">
            <h2>Announcements</h2>
            <ul class="announcement-list">
              <li v-for="announcement in session.announcements" :key="announcement.announcementId">
                <strong>{{ announcement.title }}</strong>
                <span>{{ announcement.publisher }} · {{ announcement.publishedAt }}</span>
              </li>
            </ul>
          </article>
          <article class="panel wide">
            <h2>Platform Navigation</h2>
            <div class="tag-list">
              <span v-for="item in flattenedMenus" :key="item.code" class="tag-pill">
                {{ item.name }}
              </span>
            </div>
          </article>
        </section>

        <section v-else-if="session.activeView === 'users'" class="panel">
          <h2>User Management</h2>
          <table class="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Login</th>
                <th>Organization</th>
                <th>Status</th>
                <th>Roles</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in session.users.records" :key="user.userId">
                <td>{{ user.userName }}</td>
                <td>{{ user.loginName }}</td>
                <td>{{ user.orgName }}</td>
                <td>{{ user.status }}</td>
                <td>{{ user.roles.join(', ') }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-else-if="session.activeView === 'roles'" class="panel">
          <h2>Role Management</h2>
          <div class="role-grid">
            <article v-for="role in session.roles" :key="role.roleId" class="role-card">
              <p>{{ role.roleCode }}</p>
              <h3>{{ role.roleName }}</h3>
              <span>{{ role.dataScope }}</span>
            </article>
          </div>
        </section>

        <section v-else-if="session.activeView === 'apps'" class="panel">
          <h2>App Center</h2>
          <div class="app-grid">
            <article v-for="app in session.apps" :key="app.appId" class="app-card">
              <p>{{ app.appCode }}</p>
              <h3>{{ app.appName }}</h3>
              <span>Owner: {{ app.owner }}</span>
              <strong>{{ app.status }}</strong>
            </article>
          </div>
        </section>

        <section v-else class="panel">
          <h2>Plugin Registry</h2>
          <div class="role-grid">
            <article v-for="plugin in session.plugins" :key="plugin.pluginCode" class="role-card">
              <p>{{ plugin.pluginCode }}</p>
              <h3>{{ plugin.pluginName }}</h3>
              <span>{{ plugin.pluginType }} · {{ plugin.version }} · {{ plugin.status }}</span>
            </article>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'

import { clearAccessToken, getJson, postJson, setAccessToken } from './lib/http'
import type {
  AnnouncementSummary,
  AppSummary,
  CurrentUser,
  HealthResponse,
  LoginResponse,
  MenuNode,
  PageResponse,
  PluginSummary,
  RoleSummary,
  UserSummary
} from './types/platform'

type ActiveView = 'dashboard' | 'users' | 'roles' | 'apps' | 'plugins'

const loginForm = reactive({
  loginName: 'admin',
  password: 'admin123'
})

const session = reactive({
  accessToken: '',
  loading: false,
  errorMessage: '',
  activeView: 'dashboard' as ActiveView,
  currentUser: null as CurrentUser | null,
  menus: [] as MenuNode[],
  users: { records: [], total: 0 } as PageResponse<UserSummary>,
  roles: [] as RoleSummary[],
  announcements: [] as AnnouncementSummary[],
  apps: [] as AppSummary[],
  plugins: [] as PluginSummary[],
  health: null as HealthResponse | null
})

const flattenedMenus = computed(() =>
  session.menus.flatMap((item) => item.children.length > 0 ? item.children : [item]))

const viewTitle = computed(() => {
  switch (session.activeView) {
    case 'users':
      return 'User Management'
    case 'roles':
      return 'Role Management'
    case 'apps':
      return 'App Center'
    case 'plugins':
      return 'Plugin Registry'
    default:
      return 'Dashboard'
  }
})

const loadWorkspace = async () => {
  const [currentUser, menus, users, roles, announcements, apps, plugins, health] = await Promise.all([
    getJson<CurrentUser>('/api/auth/current-user'),
    getJson<MenuNode[]>('/api/iam/menu-tree'),
    getJson<PageResponse<UserSummary>>('/api/iam/users'),
    getJson<RoleSummary[]>('/api/iam/roles'),
    getJson<AnnouncementSummary[]>('/api/system/announcements'),
    getJson<AppSummary[]>('/api/metadata/apps'),
    getJson<PluginSummary[]>('/api/plugins/registry'),
    getJson<HealthResponse>('/api/health')
  ])

  session.currentUser = currentUser
  session.menus = menus
  session.users = users
  session.roles = roles
  session.announcements = announcements
  session.apps = apps
  session.plugins = plugins
  session.health = health
}

const handleLogin = async () => {
  session.loading = true
  session.errorMessage = ''

  try {
    const result = await postJson<LoginResponse>('/api/auth/login', loginForm)
    session.accessToken = result.accessToken
    setAccessToken(result.accessToken)
    await loadWorkspace()
  } catch (error) {
    session.accessToken = ''
    clearAccessToken()
    session.errorMessage = error instanceof Error
      ? error.message
      : 'Login failed, please try again later.'
  } finally {
    session.loading = false
  }
}
</script>
