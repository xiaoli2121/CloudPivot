<template>
  <main class="designer-app">
    <section v-if="!session.accessToken" class="auth-shell">
      <div class="auth-card">
        <p class="eyebrow">Community 1.0</p>
        <h1>Designer Studio</h1>
        <p class="lead">
          登录后即可查看元数据、调整页面草稿，并将当前设计一键发布为运行版本。
        </p>
        <label class="field">
          <span>账号</span>
          <input v-model="loginForm.loginName" type="text">
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="loginForm.password" type="password">
        </label>
        <button data-testid="login-button" class="primary-btn" type="button" @click="handleLogin">
          进入设计器
        </button>
        <p v-if="session.message" class="message">{{ session.message }}</p>
      </div>
    </section>

    <section v-else class="studio-shell">
      <article class="hero-card">
        <p class="eyebrow">Low-Code Closed Loop</p>
        <h1>{{ session.schema?.app.appName }}</h1>
        <div class="hero-meta">
          <span>应用编码 <strong>{{ session.schema?.app.appCode }}</strong></span>
          <span>对象模型 <strong>{{ session.schema?.object.objectName }}</strong></span>
          <span>最近发布 <strong>{{ session.publishedVersion }}</strong></span>
        </div>
      </article>

      <div class="studio-grid">
        <section class="panel">
          <h2>页面草稿</h2>
          <div class="toolbar-grid">
            <label class="toolbar-field">
              <span>页面名称</span>
              <input data-testid="page-name" v-model="pageForm.pageName" type="text">
            </label>
            <label class="toolbar-field">
              <span>路由地址</span>
              <input data-testid="route-path" v-model="pageForm.routePath" type="text">
            </label>
            <label class="toolbar-field">
              <span>页面类型</span>
              <select v-model="pageForm.pageType">
                <option value="LIST">LIST</option>
                <option value="FORM">FORM</option>
                <option value="DETAIL">DETAIL</option>
              </select>
            </label>
            <label class="toolbar-field">
              <span>页面状态</span>
              <select v-model="pageForm.statusCode">
                <option value="DRAFT">DRAFT</option>
                <option value="PUBLISHED">PUBLISHED</option>
              </select>
            </label>
          </div>

          <div class="toolbar-actions">
            <button data-testid="add-component" class="ghost-btn" type="button" @click="addComponent">
              添加组件
            </button>
            <button data-testid="save-draft" class="secondary-btn" type="button" @click="handleSaveDraft">
              保存草稿
            </button>
            <button data-testid="publish-app" class="primary-btn compact" type="button" @click="handlePublish">
              发布应用
            </button>
          </div>

          <p v-if="session.message" class="message">{{ session.message }}</p>

          <div class="component-list">
            <article v-for="component in components" :key="component.componentCode" class="component-card">
              <code>{{ component.componentCode }}</code>
              <h4>{{ component.componentType }}</h4>
              <p>父组件：{{ component.parentCode ?? 'ROOT' }}</p>
              <p>属性：{{ JSON.stringify(component.props) }}</p>
            </article>
          </div>
        </section>

        <aside class="panel">
          <h2>对象字段</h2>
          <div class="field-list">
            <article
              v-for="field in session.schema?.object.fields ?? []"
              :key="field.fieldCode"
              class="field-card"
            >
              <h4>{{ field.fieldName }}</h4>
              <p>{{ field.fieldCode }} · {{ field.fieldType }}</p>
            </article>
          </div>
        </aside>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'

import { getJson, postJson, putJson, setAccessToken } from './lib/http'
import type {
  AppSummary,
  ComponentDetail,
  DesignerSaveResponse,
  DesignerSchema,
  LoginResponse,
  PublishVersionResponse
} from './types/designer'

const loginForm = reactive({
  loginName: 'admin',
  password: 'admin123'
})

const session = reactive({
  accessToken: '',
  message: '',
  apps: [] as AppSummary[],
  schema: null as DesignerSchema | null
})

const pageForm = reactive({
  pageName: '',
  pageType: 'LIST',
  routePath: '',
  statusCode: 'DRAFT'
})

const components = reactive<ComponentDetail[]>([])

const publishedVersion = computed(() => session.schema?.latestPublishedVersion?.versionCode ?? 'Unpublished')

const syncFromSchema = (schema: DesignerSchema) => {
  pageForm.pageName = schema.page.pageName
  pageForm.pageType = schema.page.pageType
  pageForm.routePath = schema.page.routePath
  pageForm.statusCode = schema.page.statusCode

  components.splice(0, components.length, ...schema.page.components.map((component) => ({ ...component })))
}

const loadWorkspace = async () => {
  session.apps = await getJson<AppSummary[]>('/api/metadata/apps')
  const selectedApp = session.apps[0]
  if (!selectedApp) {
    session.message = 'No application metadata available.'
    return
  }

  session.schema = await getJson<DesignerSchema>(`/api/metadata/apps/${selectedApp.appCode}/designer`)
  syncFromSchema(session.schema)
}

const handleLogin = async () => {
  const response = await postJson<LoginResponse>('/api/auth/login', loginForm)
  session.accessToken = response.accessToken
  setAccessToken(response.accessToken)
  await loadWorkspace()
}

const addComponent = () => {
  components.push({
    componentCode: `component-${components.length + 1}`,
    componentType: 'TEXT_BLOCK',
    parentCode: components[0]?.componentCode ?? null,
    sortNo: components.length + 1,
    props: {
      text: `Block ${components.length + 1}`
    }
  })
}

const handleSaveDraft = async () => {
  if (!session.schema) {
    return
  }

  const result = await putJson<DesignerSaveResponse>(`/api/metadata/apps/${session.schema.app.appCode}/designer`, {
    pageName: pageForm.pageName,
    pageType: pageForm.pageType,
    routePath: pageForm.routePath,
    statusCode: pageForm.statusCode,
    components
  })

  session.message = `Draft saved: ${result.componentCount} components`
}

const handlePublish = async () => {
  if (!session.schema) {
    return
  }

  const published = await postJson<PublishVersionResponse>(`/api/metadata/apps/${session.schema.app.appCode}/publish`, {
    versionNote: 'Lead pipeline release'
  })
  session.message = `Published ${published.versionCode}`
  if (session.schema?.latestPublishedVersion) {
    session.schema.latestPublishedVersion.versionCode = published.versionCode
    session.schema.latestPublishedVersion.versionStatus = published.versionStatus
    session.schema.latestPublishedVersion.snapshotSummary = published.snapshotSummary
  }
}
</script>
