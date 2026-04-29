export interface ApiResponse<T> {
  code?: string
  message?: string
  data: T
}

export interface LoginResponse {
  accessToken: string
}

export interface AppSummary {
  appId: number
  appCode: string
  appName: string
  owner: string
  status: string
}

export interface FieldDetail {
  fieldId: number
  fieldCode: string
  fieldName: string
  fieldType: string
  required: boolean
  sortNo: number
}

export interface ObjectDetail {
  objectId: number
  objectCode: string
  objectName: string
  storeType: string
  primaryFieldCode: string
  statusCode: string
  fields: FieldDetail[]
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

export interface DesignerSchema {
  app: AppSummary
  object: ObjectDetail
  page: PageDetail
  latestPublishedVersion: PublishedVersion | null
}

export interface DesignerSaveResponse {
  appCode: string
  pageName: string
  routePath: string
  componentCount: number
}

export interface PublishVersionResponse {
  appCode: string
  versionCode: string
  versionStatus: string
  snapshotSummary: string
}
