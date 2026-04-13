<template>
  <main class="shell">
    <section class="hero">
      <p class="eyebrow">CloudPivot</p>
      <h1>Vue3 Admin Shell</h1>
      <p class="summary">
        Minimal starter workspace for the CloudPivot management console. This page now performs a
        live backend health check against the Spring Boot service.
      </p>
      <section class="health-card">
        <div class="health-header">
          <div>
            <p class="health-label">Backend Health</p>
            <h2>{{ statusTitle }}</h2>
          </div>
          <button class="refresh-btn" type="button" @click="loadHealth" :disabled="loading">
            {{ loading ? 'Checking...' : 'Refresh' }}
          </button>
        </div>
        <p class="health-copy">{{ statusDescription }}</p>
        <dl v-if="health" class="health-grid">
          <div>
            <dt>Application</dt>
            <dd>{{ health.application }}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{{ health.status }}</dd>
          </div>
          <div>
            <dt>API Version</dt>
            <dd>{{ health.apiVersion }}</dd>
          </div>
        </dl>
      </section>
      <div class="panels">
        <article class="panel">
          <h2>Admin Foundation</h2>
          <p>Prepare navigation, auth integration, and system management modules here.</p>
        </article>
        <article class="panel">
          <h2>Frontend-Backend Link</h2>
          <p>Use the Vite proxy in local development so `/api/health` reaches the Spring Boot service on port 8080.</p>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

interface HealthResponse {
  application: string
  status: string
  apiVersion: string
}

const health = ref<HealthResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const statusTitle = computed(() => {
  if (loading.value) {
    return 'Checking backend status'
  }

  if (errorMessage.value) {
    return 'Connection not ready'
  }

  if (!health.value) {
    return 'Waiting for first check'
  }

  return `${health.value.status} · ${health.value.application}`
})

const statusDescription = computed(() => {
  if (loading.value) {
    return 'Requesting /api/health from the Spring Boot service.'
  }

  if (errorMessage.value) {
    return errorMessage.value
  }

  if (!health.value) {
    return 'Trigger the first health check to confirm the frontend-backend link.'
  }

  return `The admin shell can reach the backend successfully. Current API version: ${health.value.apiVersion}.`
})

const loadHealth = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/health')
    if (!response.ok) {
      throw new Error(`Health check failed with status ${response.status}.`)
    }

    health.value = (await response.json()) as HealthResponse
  } catch (error) {
    health.value = null
    errorMessage.value = error instanceof Error
      ? error.message
      : 'Unable to reach the backend health endpoint.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadHealth()
})
</script>
