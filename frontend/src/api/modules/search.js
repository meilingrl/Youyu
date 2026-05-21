import service from '@/api/client'

export function getHotSearchList() {
  return service.get('/search/hot')
}

export function getSearchSuggestions(params) {
  return service.get('/search/suggest', { params })
}
