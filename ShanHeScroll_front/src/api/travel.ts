import http, { extractData } from './index'

export interface Region {
  id: number
  parentId: number
  name: string
  level: number
  description: string | null
  image: string | null
  sortOrder: number
}

export interface AttractionImage {
  id: number
  attractionId: number
  url: string
  sortOrder: number
}

export interface Attraction {
  id: number
  regionId: number
  name: string
  category: string
  description: string
  image: string | null
  address: string | null
  rating: number | null
  recommendMonth: string | null
  tips: string | null
  likeCount: number
  images: AttractionImage[]
}

// 查询地区
export function getRegionsByLevel(level: number) {
  return http.get<Region[]>('/public/regions', { params: { level } }).then(extractData)
}

export function getRegionsByParent(parentId: number) {
  return http.get<Region[]>('/public/regions', { params: { parentId } }).then(extractData)
}

// 查询景点列表（category 为空时查全部分类）
export function getAttractions(regionId: number, sortBy?: string, category?: string) {
  return http.get<Attraction[]>('/public/attractions', { params: { regionId, sortBy, category } }).then(extractData)
}

// 景点详情
export function getAttractionDetail(id: number) {
  return http.get<Attraction>(`/public/attractions/${id}`).then(extractData)
}

// 搜索结果
export interface SearchResult {
  type: 'city' | 'attraction'
  id: number
  name: string
  regionId: number
  regionName: string
  parentId: number
  parentName: string
  category?: string
}

export function search(keyword: string, category?: string) {
  return http.get<SearchResult[]>('/public/search', { params: { keyword, category } }).then(extractData)
}

// 站点统计
export interface SiteStats {
  provinces: number
  cities: number
  attractions: number
  albums: number
}

export function getStats() {
  return http.get<SiteStats>('/public/stats').then(extractData)
}
