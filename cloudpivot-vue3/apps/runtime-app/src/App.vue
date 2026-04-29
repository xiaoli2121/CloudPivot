<template>
  <main class="runtime-app">
    <section class="runtime-shell" v-if="entry">
      <article class="hero">
        <p class="eyebrow">Published Runtime</p>
        <h1>{{ entry.appName }}</h1>
        <p>{{ entry.page.pageName }} · {{ entry.page.routePath }}</p>
        <span class="version-chip">{{ entry.publishedVersion.versionCode }}</span>
      </article>

      <section class="component-grid">
        <article v-for="component in entry.components" :key="component.componentCode" class="component-card">
          <h3>{{ component.componentType }}</h3>
          <p>{{ component.componentCode }}</p>
          <p v-if="component.componentType === 'PAGE_CONTAINER'">
            {{ String(component.props.title ?? 'Untitled Page') }}
          </p>
          <ul v-else-if="component.componentType === 'TABLE'">
            <li v-for="column in tableColumns(component)" :key="column">{{ column }}</li>
          </ul>
          <p v-else>{{ JSON.stringify(component.props) }}</p>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getJson } from './lib/http'
import type { ComponentDetail, RuntimeEntryResponse } from './types/runtime'

const entry = ref<RuntimeEntryResponse | null>(null)

const runtimeAppCode = () => {
  const search = globalThis.location?.search ?? ''
  const params = new URLSearchParams(search)
  return params.get('app') ?? 'crm-leads'
}

const tableColumns = (component: ComponentDetail) => {
  const columns = component.props.columns
  return Array.isArray(columns) ? columns.map((column) => String(column)) : []
}

onMounted(async () => {
  entry.value = await getJson<RuntimeEntryResponse>(`/api/runtime/apps/${runtimeAppCode()}/entry`)
})
</script>
