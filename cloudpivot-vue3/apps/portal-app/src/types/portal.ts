export interface ApiResponse<T> {
  code?: string
  message?: string
  data: T
}

export interface PortalAppSummary {
  appCode: string
  appName: string
  owner: string
  entryRoute: string
  versionCode: string
}
