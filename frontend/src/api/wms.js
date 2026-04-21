async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  if (res.status === 204) return null

  let data = null
  const text = await res.text()
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!res.ok) {
    const msg =
      (data && data.message) ||
      (typeof data === 'string' ? data : null) ||
      `Request failed: ${res.status}`
    throw new Error(msg)
  }

  return data
}

export function listItems(q) {
  const qs = q ? `?q=${encodeURIComponent(q)}` : ''
  return request(`/api/items${qs}`)
}

export function createItem(payload) {
  return request('/api/items', { method: 'POST', body: JSON.stringify(payload) })
}

export function updateItem(id, payload) {
  return request(`/api/items/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}

export function deleteItem(id) {
  return request(`/api/items/${id}`, { method: 'DELETE' })
}

export function inbound(id, payload) {
  return request(`/api/items/${id}/inbound`, { method: 'POST', body: JSON.stringify(payload) })
}

export function outbound(id, payload) {
  return request(`/api/items/${id}/outbound`, { method: 'POST', body: JSON.stringify(payload) })
}

export function listTxns(itemId) {
  const qs = itemId ? `?itemId=${encodeURIComponent(itemId)}` : ''
  return request(`/api/txns${qs}`)
}

