import { getRegionsByLevel, type Region } from '@/api/travel'

// 缓存地域数据
let allProvinces: Region[] = []
let allCities: Region[] = []
let loaded = false

/** 加载地域数据（省份+城市），用于相册分组 */
export async function loadRegionData() {
  if (loaded) return
  const [provinces, cities] = await Promise.all([
    getRegionsByLevel(2),
    getRegionsByLevel(3),
  ])
  allProvinces = provinces
  allCities = cities
  loaded = true
}

/** 获取所有省份列表 */
export function getProvinces(): Region[] {
  return allProvinces
}

/** 获取某省下的所有城市 */
export function getCitiesByProvince(provinceId: number): Region[] {
  return allCities.filter(c => c.parentId === provinceId)
}

/** 根据城市 ID 找到所属省份 */
export function getProvinceByCityId(cityId: number): Region | undefined {
  const city = allCities.find(c => c.id === cityId)
  if (!city) return undefined
  return allProvinces.find(p => p.id === city.parentId)
}

/** 根据城市 ID 找到所属省份名 */
export function getProvinceName(cityId: number): string {
  const city = allCities.find(c => c.id === cityId)
  if (!city) return '其他'
  const province = allProvinces.find(p => p.id === city.parentId)
  return province?.name || '其他'
}

/** 根据城市 ID 找到城市名 */
export function getCityName(cityId: number): string {
  return allCities.find(c => c.id === cityId)?.name || ''
}

/** 按省份分组 */
export function groupByProvince<T extends { regionId: number | null }>(
  items: T[],
): { province: string; items: T[] }[] {
  const map = new Map<string, T[]>()
  for (const item of items) {
    const prov = item.regionId ? getProvinceName(item.regionId) : '其他'
    if (!map.has(prov)) map.set(prov, [])
    map.get(prov)!.push(item)
  }
  // 排序："其他" 放最后
  const result = [...map.entries()]
    .sort(([a], [b]) => {
      if (a === '其他') return 1
      if (b === '其他') return -1
      return a.localeCompare(b)
    })
    .map(([province, items]) => ({ province, items }))
  return result
}
