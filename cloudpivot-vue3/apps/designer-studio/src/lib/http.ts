import type { ApiResponse } from '../types/designer'

let accessToken = ''

export const setAccessToken = (token: string) => {
  accessToken = token
}

const request = async <T>(input: string, init?: RequestInit): Promise<T> => {
  const response = await fetch(input, {
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(init?.headers ?? {})
    },
    ...init
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}.`)
  }

  const payload = (await response.json()) as ApiResponse<T> | T
  if (typeof payload === 'object' && payload !== null && 'data' in payload) {
    return (payload as ApiResponse<T>).data
  }

  return payload as T
}

export const getJson = <T>(input: string) => request<T>(input)

export const postJson = <T>(input: string, body: unknown) =>
  request<T>(input, {
    method: 'POST',
    body: JSON.stringify(body)
  })

export const putJson = <T>(input: string, body: unknown) =>
  request<T>(input, {
    method: 'PUT',
    body: JSON.stringify(body)
  })
