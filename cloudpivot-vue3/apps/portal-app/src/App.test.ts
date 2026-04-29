import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'

describe('PortalApp', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads portal applications and renders runtime entry links', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: async () => ({
          code: 'SUCCESS',
          data: [
            {
              appCode: 'crm-leads',
              appName: 'Lead CRM',
              owner: 'Delivery Team',
              entryRoute: '/crm/leads',
              versionCode: 'v1.0.0'
            }
          ]
        })
      })
    ))

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('Lead CRM')
    expect(wrapper.text()).toContain('/crm/leads')
    expect(wrapper.html()).toContain('?app=crm-leads')
  })
})
