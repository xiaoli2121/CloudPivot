<template>
  <main class="portal-app">
    <section class="portal-shell">
      <article class="hero">
        <p class="eyebrow">Community Portal</p>
        <h1>CloudPivot App Directory</h1>
        <p>从门户进入已发布的业务应用运行页，验证社区版 1.0 的设计到运行闭环。</p>
      </article>

      <section class="app-grid">
        <article v-for="app in apps" :key="app.appCode" class="app-card">
          <p>{{ app.appCode }}</p>
          <h2>{{ app.appName }}</h2>
          <p>Owner: {{ app.owner }}</p>
          <p>Route: {{ app.entryRoute }}</p>
          <p>Version: {{ app.versionCode }}</p>
          <a class="runtime-link" :href="runtimeHref(app.appCode)">Open Runtime</a>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getJson } from './lib/http'
import type { PortalAppSummary } from './types/portal'

const apps = ref<PortalAppSummary[]>([])

const runtimeHref = (appCode: string) => `../runtime-app/?app=${appCode}`

onMounted(async () => {
  apps.value = await getJson<PortalAppSummary[]>('/api/portal/apps')
})
</script>
