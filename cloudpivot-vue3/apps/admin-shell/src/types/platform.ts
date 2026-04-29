export interface ApiResponse<T> {
  code?: string
  message?: string
  data: T
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  userName: string
  roles: string[]
}

export interface CurrentUser {
  userId: number
  userName: string
  orgName: string
  roles: string[]
  permissions: string[]
}

export interface MenuNode {
  code: string
  name: string
  path: string
  children: MenuNode[]
}

export interface UserSummary {
  userId: number
  userName: string
  loginName: string
  orgName: string
  status: string
  roles: string[]
}

export interface RoleSummary {
  roleId: number
  roleCode: string
  roleName: string
  dataScope: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
}

export interface AnnouncementSummary {
  announcementId: number
  title: string
  level: string
  publisher: string
  publishedAt: string
}

export interface AppSummary {
  appId: number
  appCode: string
  appName: string
  owner: string
  status: string
}

export interface PluginSummary {
  pluginCode: string
  pluginName: string
  pluginType: string
  version: string
  status: string
  entryPoint: string
  description: string
}

export interface HealthResponse {
  application: string
  status: string
  apiVersion: string
}
