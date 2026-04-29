import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'

const ok = (payload: unknown) =>
  Promise.resolve({
    ok: true,
    json: async () => payload
  })

describe('DesignerStudioApp', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads designer schema, saves draft, and publishes a version', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)

      if (url === '/api/auth/login') {
        return ok({
          code: 'SUCCESS',
          data: {
            accessToken: 'mock-access-token',
            refreshToken: 'mock-refresh-token',
            expiresIn: 7200,
            userId: 1,
            userName: 'Platform Admin',
            roles: ['PLATFORM_ADMIN']
          }
        })
      }

      if (url === '/api/metadata/apps') {
        return ok({
          code: 'SUCCESS',
          data: [
            { appId: 1, appCode: 'crm-leads', appName: 'Lead CRM', owner: 'Delivery Team', status: 'ACTIVE' }
          ]
        })
      }

      if (url === '/api/metadata/apps/crm-leads/designer' && init?.method === 'PUT') {
        return ok({
          code: 'SUCCESS',
          data: {
            appCode: 'crm-leads',
            pageName: 'Lead Pipeline',
            routePath: '/crm/pipeline',
            componentCount: 2
          }
        })
      }

      if (url === '/api/metadata/apps/crm-leads/designer') {
        return ok({
          code: 'SUCCESS',
          data: {
            app: { appId: 1, appCode: 'crm-leads', appName: 'Lead CRM', owner: 'Delivery Team', status: 'ACTIVE' },
            object: {
              objectId: 1,
              objectCode: 'lead',
              objectName: 'Lead',
              storeType: 'RELATIONAL',
              primaryFieldCode: 'lead_name',
              statusCode: 'PUBLISHED',
              fields: [
                { fieldId: 1, fieldCode: 'lead_name', fieldName: 'Lead Name', fieldType: 'TEXT', required: true, sortNo: 1 }
              ]
            },
            page: {
              pageId: 1,
              pageCode: 'lead-list',
              pageName: 'Lead List',
              pageType: 'LIST',
              routePath: '/crm/leads',
              statusCode: 'PUBLISHED',
              components: [
                { componentCode: 'page-root', componentType: 'PAGE_CONTAINER', parentCode: null, sortNo: 1, props: { title: 'Lead List' } }
              ]
            },
            latestPublishedVersion: {
              versionId: 1,
              versionCode: 'v1.0.0',
              versionStatus: 'PUBLISHED',
              snapshotSummary: 'Lead CRM initial published snapshot',
              publishedTime: '2026-04-14T08:00:00'
            }
          }
        })
      }

      if (url === '/api/metadata/apps/crm-leads/publish' && init?.method === 'POST') {
        return ok({
          code: 'SUCCESS',
          data: {
            appCode: 'crm-leads',
            versionCode: 'v1.0.1',
            versionStatus: 'PUBLISHED',
            snapshotSummary: 'Lead pipeline release'
          }
        })
      }

      return Promise.reject(new Error(`Unhandled request: ${url}`))
    })

    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(App)

    await wrapper.get('[data-testid="login-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Lead CRM')
    expect(wrapper.get('[data-testid="page-name"]').element.value).toBe('Lead List')

    await wrapper.get('[data-testid="page-name"]').setValue('Lead Pipeline')
    await wrapper.get('[data-testid="route-path"]').setValue('/crm/pipeline')
    await wrapper.get('[data-testid="add-component"]').trigger('click')
    await wrapper.get('[data-testid="save-draft"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Draft saved')
    expect(wrapper.text()).toContain('2 components')

    await wrapper.get('[data-testid="publish-app"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('v1.0.1')
    expect(fetchMock.mock.calls[0]?.[1]).toEqual(expect.objectContaining({
      body: JSON.stringify({
        loginName: 'admin',
        password: 'admin123'
      })
    }))
  })
})
