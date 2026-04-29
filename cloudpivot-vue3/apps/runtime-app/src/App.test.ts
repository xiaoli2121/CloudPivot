import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'

describe('RuntimeApp', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads published runtime entry and renders components', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: async () => ({
          code: 'SUCCESS',
          data: {
            appCode: 'crm-leads',
            appName: 'Lead CRM',
            page: {
              pageId: 1,
              pageCode: 'lead-list',
              pageName: 'Lead List',
              pageType: 'LIST',
              routePath: '/crm/leads',
              statusCode: 'PUBLISHED',
              components: []
            },
            components: [
              { componentCode: 'page-root', componentType: 'PAGE_CONTAINER', parentCode: null, sortNo: 1, props: { title: 'Lead List' } },
              { componentCode: 'lead-table', componentType: 'TABLE', parentCode: 'page-root', sortNo: 2, props: { columns: ['lead_name', 'customer_name'] } }
            ],
            publishedVersion: {
              versionId: 1,
              versionCode: 'v1.0.0',
              versionStatus: 'PUBLISHED',
              snapshotSummary: 'Lead CRM initial published snapshot',
              publishedTime: '2026-04-14T08:00:00'
            }
          }
        })
      })
    ))

    vi.stubGlobal('location', { search: '?app=crm-leads' })

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('Lead CRM')
    expect(wrapper.text()).toContain('Lead List')
    expect(wrapper.text()).toContain('lead_name')
    expect(wrapper.text()).toContain('v1.0.0')
  })
})
