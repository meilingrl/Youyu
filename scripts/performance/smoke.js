import http from 'k6/http'
import { check, sleep } from 'k6'

const baseUrl = (__ENV.BASE_URL || 'http://localhost:18080').replace(/\/+$/, '')
const vus = Number.parseInt(__ENV.VUS || '2', 10)
const duration = __ENV.DURATION || '10s'
const sleepSeconds = Number.parseFloat(__ENV.SLEEP_SECONDS || '0.2')

export const options = {
  vus,
  duration,
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
  },
}

const endpoints = [
  { name: 'health', path: '/api/health' },
  { name: 'products', path: '/api/products?page=1&pageSize=12' },
  { name: 'hot-search', path: '/api/search/hot' },
  { name: 'home-recommendations', path: '/api/recommend/home?limit=8' },
]

function isSuccessfulApiResponse(response) {
  if (response.status !== 200) {
    return false
  }

  try {
    return response.json('success') === true
  } catch {
    return false
  }
}

export default function () {
  const responses = http.batch(
    endpoints.map(({ name, path }) => ({
      method: 'GET',
      url: `${baseUrl}${path}`,
      params: { tags: { endpoint: name } },
    })),
  )

  endpoints.forEach(({ name }, index) => {
    check(responses[index], {
      [`${name} returns a successful API response`]: isSuccessfulApiResponse,
    })
  })

  sleep(sleepSeconds)
}
