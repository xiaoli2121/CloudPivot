import type { ApiResponse } from '../types/portal'

const request = async <T>(input: string): Promise<T> => {
  const response = await fetch(input, {
    headers: {
      'Content-Type': 'application/json'
    }
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
