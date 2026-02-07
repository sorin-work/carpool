export class ApiError extends Error {
  readonly status: number
  readonly fields: Record<string, string>

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

const TOKEN_KEY = 'carpool.token'

let unauthorizedHandler: (() => void) | null = null

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** Called when the API rejects the stored token, so the app can send the user to sign in. */
export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler
}

type Params = Record<string, string | number | undefined | null>

function buildUrl(path: string, params?: Params): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  }
  const qs = query.toString()
  return `/api${path}${qs ? `?${qs}` : ''}`
}

export async function request<T>(
  method: string,
  path: string,
  options: { body?: unknown; params?: Params } = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(buildUrl(path, options.params), {
      method,
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.')
  }

  if (response.status === 204) {
    return undefined as T
  }
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && token) {
      unauthorizedHandler?.()
    }
    throw new ApiError(
      response.status,
      data?.message ?? `Request failed (${response.status})`,
      data?.fields ?? {},
    )
  }
  return data as T
}

export const http = {
  get: <T>(path: string, params?: Params) => request<T>('GET', path, { params }),
  post: <T>(path: string, body?: unknown, params?: Params) =>
    request<T>('POST', path, { body, params }),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, { body }),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, { body }),
  delete: <T>(path: string) => request<T>('DELETE', path),
}
