import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders backend health data after a successful request', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        application: 'CloudPivot',
        status: 'UP',
        apiVersion: 'v1'
      })
    }))

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('UP · CloudPivot')
    expect(wrapper.text()).toContain('Current API version: v1.')
    expect(wrapper.text()).toContain('Application')
  })
})
