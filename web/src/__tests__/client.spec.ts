import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, getToken, http, onUnauthorized, setToken } from '@/api/client'

function respond(status: number, body?: unknown) {
  return vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('api client', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.unstubAllGlobals())

  it('sends the stored token', async () => {
    const fetchMock = respond(200, { ok: true })
    vi.stubGlobal('fetch', fetchMock)
    setToken('abc')

    await http.get('/groups')

    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toBe('/api/groups')
    expect(init.headers.Authorization).toBe('Bearer abc')
  })

  it('builds query strings and skips empty values', async () => {
    const fetchMock = respond(200, [])
    vi.stubGlobal('fetch', fetchMock)

    await http.get('/groups/1/rides', { from: '2026-03-02', to: undefined, page: 0 })

    expect(fetchMock.mock.calls[0]![0]).toBe('/api/groups/1/rides?from=2026-03-02&page=0')
  })

  it('sends json bodies', async () => {
    const fetchMock = respond(201, { id: 1 })
    vi.stubGlobal('fetch', fetchMock)

    await http.post('/groups', { name: 'Office' })

    const init = fetchMock.mock.calls[0]![1]
    expect(init.method).toBe('POST')
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe('{"name":"Office"}')
  })

  it('turns error bodies into ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      respond(400, { message: 'Validation failed', fields: { name: 'required' } }),
    )

    const error = (await http.post('/groups', {}).catch((e) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.fields).toEqual({ name: 'required' })
  })

  it('returns undefined for 204', async () => {
    vi.stubGlobal('fetch', respond(204))

    await expect(http.delete('/trips/1')).resolves.toBeUndefined()
  })

  it('reports a rejected token', async () => {
    vi.stubGlobal('fetch', respond(401, { message: 'Authentication required' }))
    const handler = vi.fn()
    onUnauthorized(handler)
    setToken('expired')

    await expect(http.get('/me')).rejects.toBeInstanceOf(ApiError)
    expect(handler).toHaveBeenCalledOnce()
  })

  it('explains network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = (await http.get('/groups').catch((e) => e)) as ApiError

    expect(error.status).toBe(0)
    expect(error.message).toMatch(/Cannot reach the server/)
  })

  it('stores and clears the token', () => {
    setToken('t')
    expect(getToken()).toBe('t')
    setToken(null)
    expect(getToken()).toBeNull()
  })
})
