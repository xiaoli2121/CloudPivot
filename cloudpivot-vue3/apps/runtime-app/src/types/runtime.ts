export interface ApiResponse<T> {
  code?: string
  message?: string
  data: T
}

export interface ComponentDetail {
  componentCode: string
  componentType: string
  parentCode: string | null
  sortNo: number
  props: Record<string, unknown>
}

export interface PageDetail {
  pageId: number
  pageCode: string
  pageName: string
  pageType: string
  routePath: string
  statusCode: string
  components: ComponentDetail[]
}

export interface PublishedVersion {
  versionId: number
  versionCode: string
  versionStatus: string
  snapshotSummary: string
  publishedTime: string
}

export interface RuntimeEntryResponse {
  appCode: string
  appName: string
  page: PageDetail
  components: ComponentDetail[]
  publishedVersion: PublishedVersion
}
