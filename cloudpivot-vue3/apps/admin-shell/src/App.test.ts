import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'

const jsonResponse = (payload: unknown) =>
  Promise.resolve({
    ok: true,
    json: async () => payload
  })

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the admin shell login and then loads dashboard data after sign in', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)

      if (url === '/api/auth/login' && init?.method === 'POST') {
        return jsonResponse({
          code: 'SUCCESS',
          message: 'Request processed successfully.',
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

      if (url === '/api/auth/current-user') {
        return jsonResponse({
          code: 'SUCCESS',
          data: {
            userId: 1,
            userName: 'Platform Admin',
            orgName: 'CloudPivot Product Center',
            roles: ['PLATFORM_ADMIN'],
            permissions: ['system:user:view']
          }
        })
      }

      if (url === '/api/iam/menu-tree') {
        return jsonResponse({
          code: 'SUCCESS',
          data: [
            { code: 'dashboard', name: 'Dashboard', path: 'dashboard', children: [] },
            {
              code: 'system',
              name: 'Platform Management',
              path: 'system',
              children: [
                { code: 'users', name: 'User Management', path: 'users', children: [] },
                { code: 'roles', name: 'Role Management', path: 'roles', children: [] },
                { code: 'apps', name: 'App Center', path: 'apps', children: [] }
              ]
            }
          ]
        })
      }

      if (url === '/api/iam/users') {
        return jsonResponse({
          code: 'SUCCESS',
          data: {
            total: 3,
            records: [
              {
                userId: 1,
                userName: 'Platform Admin',
                loginName: 'admin',
                orgName: 'CloudPivot Product Center',
                status: 'ENABLED',
                roles: ['PLATFORM_ADMIN']
              }
            ]
          }
        })
      }

      if (url === '/api/iam/roles') {
        return jsonResponse({
          code: 'SUCCESS',
          data: [
            { roleId: 1, roleCode: 'PLATFORM_ADMIN', roleName: 'Platform Admin', dataScope: 'ALL' },
            { roleId: 2, roleCode: 'IMPLEMENTATION_CONSULTANT', roleName: 'Implementation Consultant', dataScope: 'ORG_AND_CHILDREN' }
          ]
        })
      }

      if (url === '/api/system/announcements') {
        return jsonResponse({
          code: 'SUCCESS',
          data: [
            { announcementId: 1, title: 'Community 1.0 Kickoff', level: 'INFO', publisher: 'Product Committee', publishedAt: '2026-04-13 09:00' }
          ]
        })
      }

      if (url === '/api/metadata/apps') {
        return jsonResponse({
          code: 'SUCCESS',
          data: [
            { appId: 1, appCode: 'crm-leads', appName: 'Lead CRM', owner: 'Delivery Team', status: 'ACTIVE' },
            { appId: 2, appCode: 'ops-workbench', appName: 'Ops Workbench', owner: 'Platform Team', status: 'PLANNING' }
          ]
        })
      }

      if (url === '/api/plugins/registry') {
        return jsonResponse({
          code: 'SUCCESS',
          data: [
            {
              pluginCode: 'core-table',
              pluginName: 'Core Table Renderer',
              pluginType: 'COMPONENT',
              version: '1.0.0',
              status: 'ACTIVE',
              entryPoint: 'plugin://core/table',
              description: 'Built-in table renderer.'
            }
          ]
        })
      }

      if (url === '/api/health') {
        return jsonResponse({
          application: 'CloudPivot',
          status: 'UP',
          apiVersion: 'v1'
        })
      }

      return Promise.reject(new Error(`Unhandled request: ${url}`))
    })

    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(App)
    expect(wrapper.text()).toContain('CloudPivot Admin Console')
    expect(wrapper.text()).toContain('Sign In')

    await wrapper.get('[data-testid="username"]').setValue('admin')
    await wrapper.get('[data-testid="password"]').setValue('admin123')
    await wrapper.get('[data-testid="login-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Community 1.0 Kickoff')
    expect(wrapper.text()).toContain('Platform Admin')
    expect(wrapper.text()).toContain('CloudPivot Product Center')
    expect(wrapper.text()).toContain('Total Users')
    expect(wrapper.text()).toContain('3')

    await wrapper.get('[data-testid="nav-users"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('User Management')
    expect(wrapper.text()).toContain('admin')

    await wrapper.get('[data-testid="nav-roles"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Role Management')
    expect(wrapper.text()).toContain('Implementation Consultant')

    await wrapper.get('[data-testid="nav-apps"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('App Center')
    expect(wrapper.text()).toContain('crm-leads')

    await wrapper.get('[data-testid="nav-plugins"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Plugin Registry')
    expect(wrapper.text()).toContain('Core Table Renderer')

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock.mock.calls[0]?.[1]).toEqual(expect.objectContaining({
      body: JSON.stringify({
        loginName: 'admin',
        password: 'admin123'
      })
    }))
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/current-user', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer mock-access-token'
      })
    }))
    expect(fetchMock).toHaveBeenCalledWith('/api/iam/users', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer mock-access-token'
      })
    }))
    expect(fetchMock).toHaveBeenCalledWith('/api/plugins/registry', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer mock-access-token'
      })
    }))
  })
})
