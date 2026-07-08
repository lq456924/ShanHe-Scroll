<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getRegionsByLevel, getRegionsByParent, getAttractions, getAttractionDetail, getStats, search as searchApi, type Region, type Attraction, type SearchResult, type SiteStats } from '@/api/travel'
import { useTheme } from '@/composables/useTheme'
import http, { extractData } from '@/api'

const router = useRouter()
const { theme } = useTheme()

// 地图颜色根据主题切换
const m = computed(() => theme.value === 'dark' ? {
  area: 'rgba(255,255,255,0.06)',
  border: 'rgba(255,255,255,0.12)',
  label: 'rgba(255,255,255,0.6)',
  emphasisLabel: '#fff',
  dimArea: 'rgba(255,255,255,0.03)',
  dimBorder: 'rgba(255,255,255,0.06)',
  dimLabel: 'rgba(255,255,255,0.2)',
  highlight: 'rgba(167,139,250,0.6)',
} : {
  area: 'rgba(0,0,0,0.04)',
  border: 'rgba(0,0,0,0.15)',
  label: 'rgba(0,0,0,0.45)',
  emphasisLabel: '#333',
  dimArea: 'rgba(0,0,0,0.02)',
  dimBorder: 'rgba(0,0,0,0.08)',
  dimLabel: 'rgba(0,0,0,0.15)',
  highlight: 'rgba(102,126,234,0.5)',
})

// ---- 搜索 ----
const searchOpen = ref(false)
const searchKeyword = ref('')
const searchResults = ref<SearchResult[]>([])
const searchFocused = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  const kw = searchKeyword.value.trim()
  if (!kw) {
    searchResults.value = []
    searchOpen.value = false
    return
  }
  searchTimer = window.setTimeout(async () => {
    try {
      searchResults.value = await searchApi(kw)
      searchOpen.value = searchResults.value.length > 0
    } catch { searchResults.value = [] }
  }, 200)
}

function onSearchFocus() {
  searchFocused.value = true
  if (searchKeyword.value.trim() && searchResults.value.length) searchOpen.value = true
}

function onSearchBlur() {
  // 延迟关闭，让点击事件先触发
  setTimeout(() => { searchFocused.value = false; searchOpen.value = false }, 150)
}

async function onSearchSelect(r: SearchResult) {
  searchOpen.value = false
  searchKeyword.value = r.name
  searchResults.value = []

  // 需要定位的省份和城市
  const provinceId = r.parentId    // 省级 ID
  const cityRegionId = r.regionId // 城市 ID
  const attractionId = r.type === 'attraction' ? r.id : null

  // 设置面包屑：先找省份名
  const provinceName = r.parentName || ''
  const cityName = r.regionName || r.name

  // 面包屑
  currentLevel.value = provinceId
  breadcrumb.value = [
    { name: '中国地图', id: 'china' },
    { name: provinceName, id: provinceId },
  ]

  // 渲染省份地图
  await renderProvince(provinceId)

  // 匹配城市并选中
  let cityRegion: Region | undefined
  for (const [, region] of citiesMap) {
    if (region.id === cityRegionId) {
      cityRegion = region
      break
    }
  }
  if (!cityRegion) {
    // 兜底：用名称匹配
    cityRegion = [...citiesMap.values()].find((c) => c.id === cityRegionId)
  }

  if (cityRegion) {
    selectedCity.value = cityRegion
    attractionLoading.value = true
    try {
      attractions.value = await getAttractions(cityRegionId)
    } catch { attractions.value = [] }
    attractionLoading.value = false
  }

  // 如果是景点搜索结果，直接通过详情接口打开（不依赖景点列表）
  if (attractionId) {
    openDetail({ id: attractionId } as Attraction)
  }
}

// ---- 状态 ----
const chartRef = ref<HTMLDivElement>()
const loading = ref(false)
const attractionLoading = ref(false)
const zoomPercent = ref(120) // 初始缩放百分比
const stats = ref<SiteStats | null>(null)

// 层级：'china' | provinceId
const currentLevel = ref<'china' | number>('china')
const breadcrumb = ref<{ name: string; id: number | 'china' }[]>([])
const selectedCity = ref<Region | null>(null)
const attractions = ref<Attraction[]>([])
const categoryFilter = ref('')

const categoryOptions = [
  { value: '', label: '全部' },
  { value: 'attraction', label: '景点' },
  { value: 'food', label: '美食' },
  { value: 'photo_spot', label: '其他' },
]

const categoryLabels: Record<string, string> = {
  attraction: '景点',
  food: '美食',
  photo_spot: '其他',
}

// ---- 景点详情 ----
const showDetail = ref(false)
const detailAttraction = ref<Attraction | null>(null)
const detailLoading = ref(false)

async function openDetail(attr: Attraction) {
  showDetail.value = true
  detailLoading.value = true
  try {
    detailAttraction.value = await getAttractionDetail(attr.id)
  } catch {
    detailAttraction.value = attr // 降级用列表数据
  }
  detailLoading.value = false
}

function goCreateAlbum() {
  if (selectedCity.value) {
    router.push(`/album?cityId=${selectedCity.value.id}`)
  }
}

// ---- 添加景点 ----
const showAddForm = ref(false)
const addLoading = ref(false)
const addError = ref('')
const addSuccess = ref('')
const addForm = ref({
  name: '',
  category: 'attraction',
  description: '',
  address: '',
  image: '',
  recommendMonth: '',
  tips: '',
})

async function submitAttraction() {
  addError.value = ''
  addSuccess.value = ''

  const required: [string, string][] = [
    ['category', '请选择分类'],
    ['name', '请输入名称'],
    ['description', '请输入描述'],
    ['address', '请输入地址'],
    ['recommendMonth', '请输入推荐月份'],
    ['tips', '请输入游玩提示'],
  ]
  for (const [field, msg] of required) {
    if (!(addForm.value as any)[field]?.trim()) {
      addError.value = msg
      return
    }
  }

  addLoading.value = true
  try {
    await http.post('/travel/attractions', {
      regionId: selectedCity.value?.id,
      category: addForm.value.category,
      name: addForm.value.name.trim(),
      description: addForm.value.description.trim(),
      address: addForm.value.address.trim(),
      image: addForm.value.image.trim() || null,
      recommendMonth: addForm.value.recommendMonth.trim() || null,
      tips: addForm.value.tips.trim() || null,
    }).then(extractData)

    addSuccess.value = '提交成功！审核通过后将展示在列表中'
    addForm.value = { name: '', category: 'attraction', description: '', address: '', image: '', recommendMonth: '', tips: '' }
    setTimeout(() => { showAddForm.value = false; addSuccess.value = '' }, 1500)
  } catch (err: any) {
    addError.value = err.message || '提交失败'
  }
  addLoading.value = false
}

// GeoJSON 缓存
const geoJsonCache = new Map<string, any>()

// API 数据缓存
let provincesMap = new Map<string, Region>()
let provinceIdToAdcode = new Map<number, string>()
let citiesMap = new Map<string, Region>() // adcode -> Region

// ---- 工具函数 ----
async function loadGeoJson(adcode: string): Promise<any> {
  if (geoJsonCache.has(adcode)) return geoJsonCache.get(adcode)
  const url = `https://geo.datav.aliyun.com/areas_v3/bound/${adcode}_full.json`
  const res = await fetch(url)
  if (!res.ok) throw new Error('地图数据不可用')
  const json = await res.json()
  geoJsonCache.set(adcode, json)
  return json
}

function getProvinceAdcode(provinceName: string, geo: any): string | null {
  for (const feat of geo.features) {
    if (feat.properties.name === provinceName) {
      return String(feat.properties.adcode)
    }
  }
  return null
}

/** 从中国 GeoJSON 提取单个省份轮廓（无详细 GeoJSON 的兜底方案） */
function extractSingleProvinceGeo(province: Region, _provinceId: number): any | null {
  const chinaGeo = geoJsonCache.get('100000')
  if (!chinaGeo) return null
  const adcode = provinceIdToAdcode.get(province.id)
  if (!adcode) return null
  const feature = chinaGeo.features.find((f: any) => String(f.properties.adcode) === adcode)
  if (!feature) {
    // 用名称兜底匹配
    const n = normalizeName(province.name)
    const f2 = chinaGeo.features.find((f: any) => normalizeName(f.properties.name) === n)
    if (!f2) return null
    return { type: 'FeatureCollection', features: [f2] }
  }
  return { type: 'FeatureCollection', features: [feature] }
}

// 标准化省份名称用于匹配
function normalizeName(name: string): string {
  return name
    .replace(/省$/, '')
    .replace(/市$/, '')
    .replace(/壮族自治区$/, '')
    .replace(/回族自治区$/, '')
    .replace(/维吾尔自治区$/, '')
    .replace(/自治区$/, '')
    .replace(/特别行政区$/, '')
}

// ---- 辅助函数：计算 GeoJSON feature 的中心点 ----
function getFeatureCenter(feature: any): [number, number] {
  const coords = feature.geometry.coordinates
  let xSum = 0, ySum = 0, count = 0
  function walk(arr: any) {
    if (typeof arr[0] === 'number') {
      xSum += arr[0]
      ySum += arr[1]
      count++
    } else {
      arr.forEach(walk)
    }
  }
  walk(coords)
  return [xSum / count, ySum / count]
}

// 查找 GeoJSON 中匹配省份的 feature 并计算中心点
function getProvinceCenterAndAdcode(
  provinceName: string,
  geo: any,
): { center: [number, number]; adcode: string } | null {
  for (const feat of geo.features) {
    if (feat.properties.name === provinceName) {
      return {
        center: getFeatureCenter(feat),
        adcode: String(feat.properties.adcode),
      }
    }
  }
  return null
}
async function initProvinces() {
  const provinces = await getRegionsByLevel(2) // 省级
  for (const p of provinces) {
    provincesMap.set(p.name, p)
  }
}

// ---- 空白区域点击返回 ----
function goBackOnBlank(e: any) {
  // 如果点击的是地图元素（城市多边形），交给 onMapClick 处理
  if (e.target) return
  // 点击真正的空白区域 → 返回中国地图
  if (currentLevel.value !== 'china') {
    goToLevel({ name: '中国地图', id: 'china' })
  }
}

// ---- 主图表渲染 ----
let chartInstance: echarts.ECharts | null = null

// 面积小、名称长的省份：GeoJSON 全称 → 显示短名
const provinceShortName: Record<string, string> = {
  '香港特别行政区': '香港',
  '澳门特别行政区': '澳门',
  '内蒙古自治区': '内蒙古',
  '广西壮族自治区': '广西',
  '西藏自治区': '西藏',
  '宁夏回族自治区': '宁夏',
  '新疆维吾尔自治区': '新疆',
  '黑龙江省': '黑龙江',
}

async function renderChina() {
  if (!chartRef.value) return
  loading.value = true
  breadcrumb.value = [{ name: '中国地图', id: 'china' }]

  let geo
  try {
    geo = await loadGeoJson('100000')
  } catch (e) {
    console.error('加载 GeoJSON 失败，请检查网络是否能访问阿里云 DataV CDN:', e)
    loading.value = false
    return
  }
  const adcodeMap: Record<string, string> = {}

  // 建立 province name → adcode 映射（含标准化匹配）
  for (const feat of geo.features) {
    const geoName = feat.properties.name
    const adcodeStr = String(feat.properties.adcode)
    adcodeMap[geoName] = adcodeStr

    // 精确匹配
    if (provincesMap.has(geoName)) {
      provinceIdToAdcode.set(provincesMap.get(geoName)!.id, adcodeStr)
    } else {
      // 标准化后匹配
      const normalized = normalizeName(geoName)
      for (const [key, val] of provincesMap) {
        if (normalizeName(key) === normalized) {
          provinceIdToAdcode.set(val.id, adcodeStr)
          break
        }
      }
    }
  }

  echarts.registerMap('china', geo)

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    chartInstance.on('click', onMapClick)
    chartInstance.on('georoam', () => {
      const opt = chartInstance!.getOption() as any
      zoomPercent.value = Math.round((opt.series?.[0]?.zoom || 1.2) * 100)
    })
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => provinceShortName[p.name] || p.name,
    },
    series: [
      {
        type: 'map',
        map: 'china',
        roam: true,
        scaleLimit: { min: 1, max: 4 },
        zoom: 1.2,
        center: [104.5, 36],
        label: {
          show: true,
          fontSize: 10,
          color: m.value.label,
          formatter: (p: any) => provinceShortName[p.name] || p.name,
        },
        itemStyle: {
          areaColor: m.value.area,
          borderColor: m.value.border,
          borderWidth: 1,
        },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold', color: m.value.emphasisLabel },
          itemStyle: { areaColor: 'rgba(167,139,250,0.3)' },
        },
        select: {
          label: { show: true, color: '#fff' },
          itemStyle: { areaColor: 'rgba(167,139,250,0.5)' },
        },
        selectedMode: 'single',
        data: [],
      },
    ],
  })

  loading.value = false
  // 移除 zrender 空白区域监听
  chartInstance.getZr().off('click', goBackOnBlank)
  // 存储 adcodeMap 备用
  ;(chartInstance as any)._adcodeMap = adcodeMap
  ;(chartInstance as any)._chinaGeo = geo
}

async function renderProvince(provinceId: number) {
  if (!chartRef.value || !chartInstance) return
  loading.value = true
  selectedCity.value = null
  attractions.value = []

  const province = [...provincesMap.values()].find((p) => p.id === provinceId)
  if (!province) { loading.value = false; return }

  const adcode = provinceIdToAdcode.get(provinceId)
  if (!adcode) { loading.value = false; return }

  let geo: any
  try {
    geo = await loadGeoJson(adcode)
  } catch {
    // 无详细 GeoJSON（台湾/香港/澳门），用中国地图中该省的轮廓兜底
    geo = extractSingleProvinceGeo(province, provinceId)
    if (!geo) {
      loading.value = false
      return
    }
  }
  const mapName = `province_${adcode}`
  echarts.registerMap(mapName, geo)

  // 加载城市数据
  const cities = await getRegionsByParent(provinceId)
  citiesMap.clear()
  for (const c of cities) {
    const apiName = normalizeName(c.name)
    let matched = false
    for (const feat of geo.features) {
      const geoName = normalizeName(feat.properties.name)
      if (geoName === apiName) {
        citiesMap.set(String(feat.properties.adcode), c)
        matched = true
        break
      }
    }
    if (!matched) {
      // 宽松匹配：包含关系
      for (const feat of geo.features) {
        const geoName = feat.properties.name
        if (geoName.includes(c.name) || c.name.includes(geoName)) {
          citiesMap.set(String(feat.properties.adcode), c)
          break
        }
      }
    }
  }

  chartInstance.setOption(
    {
      series: [
        {
          type: 'map',
          map: mapName,
          roam: true,
          zoom: 1.2,
          label: { show: true, fontSize: 11, color: m.value.label },
          itemStyle: {
            areaColor: m.value.area,
            borderColor: m.value.border,
            borderWidth: 1,
          },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold', color: m.value.emphasisLabel },
            itemStyle: { areaColor: 'rgba(167,139,250,0.3)' },
          },
          select: {
            label: { show: true, color: m.value.emphasisLabel },
            itemStyle: { areaColor: 'rgba(167,139,250,0.5)' },
          },
          selectedMode: 'single',
          data: [],
        },
      ],
    },
    true,
  )

  loading.value = false
  ;(chartInstance as any)._provinceGeo = geo
  ;(chartInstance as any)._provinceId = provinceId

  // zrender 层监听：点击空白区域返回中国地图
  const zr = chartInstance.getZr()
  zr.off('click', goBackOnBlank)
  zr.on('click', goBackOnBlank)
}

// ---- 点击事件 ----
async function onMapClick(params: any) {
  if (!params.name || !chartInstance) return

  if (currentLevel.value === 'china') {
    // 点击省份 → 下钻（带缩放动画）
    const provinceName = params.name
    const geo = (chartInstance as any)._chinaGeo

    // 精确匹配 + 标准化后匹配
    let p = provincesMap.get(provinceName)
    if (!p) {
      const normalized = normalizeName(provinceName)
      for (const [key, val] of provincesMap) {
        if (normalizeName(key) === normalized) {
          p = val
          break
        }
      }
    }

    if (!p) {
      const adcode = getProvinceAdcode(provinceName, geo)
      if (adcode) {
        for (const [key, val] of provincesMap) {
          const pAdcode = provinceIdToAdcode.get(val.id)
          if (pAdcode && pAdcode.substring(0, 2) === adcode.substring(0, 2)) {
            p = val
            break
          }
        }
      }
    }

    if (!p) {
      console.warn('未能匹配省份:', provinceName, '可用省份:', [...provincesMap.keys()])
      return
    }

    const info = getProvinceCenterAndAdcode(provinceName, geo)
    const adcode = info?.adcode || provinceIdToAdcode.get(p.id)
    if (adcode) {
      provinceIdToAdcode.set(p.id, adcode)
    }

    // 更新面包屑
    currentLevel.value = p.id
    breadcrumb.value = [
      { name: '中国地图', id: 'china' },
      { name: p.name, id: p.id },
    ]

    // 动画：其他省份褪色，选中省份放大聚焦
    if (info) {
      const dimData = geo.features.map((feat: any) => {
        if (feat.properties.name === provinceName) {
          return { name: feat.properties.name, itemStyle: { areaColor: m.value.highlight, color: m.value.emphasisLabel }, label: { color: m.value.emphasisLabel, fontSize: 16, fontWeight: 'bold' } }
        }
        return { name: feat.properties.name, itemStyle: { areaColor: m.value.dimArea, borderColor: m.value.dimBorder }, label: { color: m.value.dimLabel, fontSize: 8 } }
      })
      chartInstance!.setOption({
        series: [{
          type: 'map', map: 'china',
          center: info.center,
          zoom: 5,
          data: dimData,
        }],
      }, { animationDurationUpdate: 800, animationEasingUpdate: 'cubicInOut' })

      // 等待动画完成后切换地图
      await new Promise((resolve) => setTimeout(resolve, 900))
    }

    await renderProvince(p.id)
  } else {
    // 点击城市 → 查询景区
    const cityName = params.name
    const geo = (chartInstance as any)._provinceGeo

    const normalizedCity = normalizeName(cityName)
    let cityRegion: Region | undefined
    for (const [, region] of citiesMap) {
      if (normalizeName(region.name) === normalizedCity) {
        cityRegion = region
        break
      }
    }
    if (!cityRegion) {
      for (const feat of geo.features) {
        if (feat.properties.name === cityName) {
          cityRegion = citiesMap.get(String(feat.properties.adcode))
          break
        }
      }
    }
    if (!cityRegion) {
      cityRegion = [...citiesMap.values()].find((c) => cityName.includes(c.name) || c.name.includes(cityName))
    }

    if (cityRegion) {
      selectedCity.value = cityRegion
      attractionLoading.value = true
      try {
        attractions.value = await getAttractions(cityRegion.id, undefined, categoryFilter.value || undefined)
      } catch {
        attractions.value = []
      }
      attractionLoading.value = false
    }
  }
}

// 分类切换时重新加载景点
watch(categoryFilter, async () => {
  if (!selectedCity.value) return
  attractionLoading.value = true
  try {
    attractions.value = await getAttractions(selectedCity.value.id, undefined, categoryFilter.value || undefined)
  } catch {
    attractions.value = []
  }
  attractionLoading.value = false
})

// ---- 面包屑返回 ----
async function goToLevel(item: { name: string; id: number | 'china' }) {
  if (item.id === 'china') {
    currentLevel.value = 'china'
    breadcrumb.value = [{ name: '中国地图', id: 'china' }]
    selectedCity.value = null
    attractions.value = []
    await renderChina()
  } else {
    currentLevel.value = item.id
    const idx = breadcrumb.value.findIndex((b) => b.id === item.id)
    breadcrumb.value = breadcrumb.value.slice(0, idx + 1)
    selectedCity.value = null
    attractions.value = []
    await renderProvince(item.id)
  }
}

// ---- 窗口大小调整 ----
function handleResize() {
  chartInstance?.resize()
}

// 景点面板显示/隐藏时，ECharts 容器尺寸变化，需手动 resize
watch(selectedCity, async () => {
  await nextTick()
  chartInstance?.resize()
})

onMounted(async () => {
  try {
    await initProvinces()
  } catch {
    console.warn('获取省份数据失败，后端可能未启动')
  }
  try { stats.value = await getStats() } catch { /* 统计非关键 */ }
  await renderChina()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})

// 主题切换时仅更新地图配色，不重建地图、不重加载数据
function applyMapColors() {
  if (!chartInstance) return
  chartInstance.setOption({
    series: [{
      label: { color: m.value.label },
      itemStyle: {
        areaColor: m.value.area,
        borderColor: m.value.border,
      },
      emphasis: { label: { color: m.value.emphasisLabel } },
      select: { label: { color: m.value.emphasisLabel } },
    }],
  })
}

watch(theme, () => {
  applyMapColors()
})
</script>

<template>
  <div class="home-page">
    <!-- 装饰背景 -->
    <div class="bg-decor">
      <div class="decor decor-1"></div>
      <div class="decor decor-2"></div>
      <div class="decor decor-3"></div>
      <div class="decor decor-4"></div>
    </div>

    <!-- 面包屑 + 统计 + 搜索 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <div v-if="breadcrumb.length" class="breadcrumb-bar">
          <span
            v-for="(item, idx) in breadcrumb"
            :key="item.id"
            class="breadcrumb-item"
            :class="{ clickable: idx < breadcrumb.length - 1 }"
            @click="idx < breadcrumb.length - 1 && goToLevel(item)"
          >
            {{ item.name }}
            <span v-if="idx < breadcrumb.length - 1" class="separator"> &gt; </span>
          </span>
        </div>
      </div>

      <div class="top-bar-right">
        <div v-if="stats" class="stats-bar">
          <span>🏔 {{ stats.provinces }}省</span>
          <span class="stats-dot">·</span>
          <span>🏙 {{ stats.cities }}城</span>
          <span class="stats-dot">·</span>
          <span>📍 {{ stats.attractions.toLocaleString() }}个景点</span>
          <span class="stats-dot">·</span>
          <span>📸 {{ stats.albums.toLocaleString() }}个相册</span>
        </div>

        <div class="search-bar">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索城市或景区..."
            @input="onSearchInput"
            @focus="onSearchFocus"
            @blur="onSearchBlur"
          />
          <span v-if="searchKeyword" class="search-clear" @mousedown.prevent="searchKeyword = ''; searchOpen = false">✕</span>
        </div>
        <div v-if="searchOpen && searchResults.length" class="search-dropdown">
          <div
            v-for="r in searchResults"
            :key="r.type + r.id"
            class="search-item"
            @mousedown.prevent="onSearchSelect(r)"
          >
            <span class="search-type-tag" :class="r.type === 'city' ? 'tag-city' : 'tag-attr'">
              {{ r.type === 'city' ? '城' : categoryLabels[r.category || ''] || '景' }}
            </span>
            <div class="search-item-info">
              <div class="search-item-name">{{ r.name }}</div>
              <div class="search-item-path">
                {{ r.parentName }} · {{ r.type === 'city' ? r.name : r.regionName }}
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
    <div class="home-content" :class="{ 'has-attractions': selectedCity }">
      <!-- 地图区域 -->
      <div class="map-section">
        <div v-if="loading" class="loading-mask">加载中...</div>
        <div ref="chartRef" class="chart-container"></div>
        <div class="zoom-badge">{{ zoomPercent }}%</div>
      </div>

      <!-- 景区列表 -->
      <div v-if="selectedCity" class="attractions-panel">
        <div class="panel-header">
          <h3 class="panel-title">{{ selectedCity.name }} — 打卡点推荐</h3>
          <div class="panel-actions">
            <button class="add-btn" @click="showAddForm = true">+ 添加打卡点</button>
            <button class="album-btn" @click="goCreateAlbum">创建相册</button>
          </div>
        </div>
        <div class="category-tabs">
          <span
            v-for="opt in categoryOptions"
            :key="opt.value"
            class="category-tab"
            :class="{ active: categoryFilter === opt.value }"
            @click="categoryFilter = opt.value"
          >{{ opt.label }}</span>
        </div>
        <div v-if="attractionLoading" class="loading-text">查询中...</div>
        <div v-else-if="attractions.length === 0" class="empty-text">
          该城市暂无景区数据
          <br />
          <span class="add-hint" @click="showAddForm = true">点击添加第一个景点</span>
        </div>
        <div v-else class="attraction-list">
          <div v-for="attr in attractions" :key="attr.id" class="attraction-card" @click="openDetail(attr)">
            <img
              v-if="attr.image"
              :src="attr.image"
              class="attr-img"
              alt=""
              loading="lazy"
              decoding="async"
            />
            <div class="attr-info">
              <div class="attr-name">
                {{ attr.name }}
                <span v-if="attr.category" class="category-tag" :class="'cat-' + attr.category">
                  {{ categoryLabels[attr.category] || attr.category }}
                </span>
              </div>
              <div class="attr-desc">{{ attr.description }}</div>
              <div class="attr-meta">
                <span v-if="attr.rating">⭐ {{ attr.rating }}</span>
                <span v-if="attr.recommendMonth">📅 {{ attr.recommendMonth }}</span>
                <span>❤️ {{ attr.likeCount }}</span>
              </div>
              <div v-if="attr.tips" class="attr-tips">💡 {{ attr.tips }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 景点详情弹窗 -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="detail-card" v-if="detailAttraction">
        <button class="detail-close" @click="showDetail = false">✕</button>

        <!-- 图片轮播 -->
        <div class="detail-images" v-if="detailAttraction.images?.length">
          <img
            v-for="(img, i) in detailAttraction.images"
            :key="img.id"
            :src="img.url"
            :alt="'图片' + (i+1)"
            class="detail-img"
            loading="lazy"
            decoding="async"
          />
        </div>
        <img v-else-if="detailAttraction.image" :src="detailAttraction.image" class="detail-img single" alt="" loading="lazy" decoding="async" />

        <div v-if="detailLoading" class="detail-loading">加载详情...</div>

        <template v-else>
          <h2 class="detail-name">{{ detailAttraction.name }}</h2>

          <div class="detail-meta">
            <span v-if="detailAttraction.rating">⭐ {{ detailAttraction.rating }}</span>
            <span v-if="detailAttraction.recommendMonth">📅 {{ detailAttraction.recommendMonth }}</span>
            <span>❤️ {{ detailAttraction.likeCount }}</span>
          </div>

          <div class="detail-section" v-if="detailAttraction.address">
            <h4>📍 地址</h4>
            <p>{{ detailAttraction.address }}</p>
          </div>

          <div class="detail-section" v-if="detailAttraction.description">
            <h4>📖 简介</h4>
            <p>{{ detailAttraction.description }}</p>
          </div>

          <div class="detail-section" v-if="detailAttraction.tips">
            <h4>💡 游玩提示</h4>
            <p>{{ detailAttraction.tips }}</p>
          </div>
        </template>
      </div>
    </div>

    <!-- 添加景点弹窗 -->
    <div v-if="showAddForm" class="modal-overlay" @click.self="showAddForm = false">
      <div class="modal-card">
        <h3>添加景点 — {{ selectedCity?.name }}</h3>
        <div class="field">
          <label>分类 *</label>
          <select v-model="addForm.category" class="field-select">
            <option v-for="opt in categoryOptions.slice(1)" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>名称 *</label>
          <input v-model="addForm.name" placeholder="如：大雁塔 / 羊肉泡馍" maxlength="100" />
        </div>
        <div class="field">
          <label>描述 *</label>
          <textarea v-model="addForm.description" rows="3" placeholder="简要介绍该景点" maxlength="500"></textarea>
        </div>
        <div class="field">
          <label>地址 *</label>
          <input v-model="addForm.address" placeholder="详细地址" maxlength="200" />
        </div>
        <div class="field">
          <label>图片URL</label>
          <input v-model="addForm.image" placeholder="图片链接（选填）" maxlength="500" />
        </div>
        <div class="field">
          <label>推荐月份 *</label>
          <input v-model="addForm.recommendMonth" placeholder="如：4-5月" maxlength="100" />
        </div>
        <div class="field">
          <label>游玩提示 *</label>
          <textarea v-model="addForm.tips" rows="2" placeholder="小贴士" maxlength="500"></textarea>
        </div>

        <p v-if="addError" class="add-error">{{ addError }}</p>
        <p v-if="addSuccess" class="add-success">{{ addSuccess }}</p>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showAddForm = false">取消</button>
          <button class="btn-submit" :disabled="addLoading" @click="submitAttraction">
            {{ addLoading ? '提交中...' : '提交（需审核）' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 73px);
  overflow: hidden;
  position: relative;
  background: var(--bg-page);
}

/* 装饰背景 */
.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.decor {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.25;
  animation: decorFloat 20s ease-in-out infinite;
}

.decor-1 {
  width: 500px;
  height: 500px;
  background: var(--decor-1);
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

.decor-2 {
  width: 350px;
  height: 350px;
  background: var(--decor-2);
  bottom: -100px;
  left: -100px;
  animation-delay: -7s;
}

.decor-3 {
  width: 280px;
  height: 280px;
  background: var(--decor-3);
  top: 40%;
  left: 30%;
  animation-delay: -14s;
}

.decor-4 {
  width: 200px;
  height: 200px;
  background: var(--decor-4);
  top: 20%;
  right: 30%;
  animation-delay: -10s;
}

@keyframes decorFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -30px) scale(1.15); }
  50% { transform: translate(-20px, 20px) scale(0.9); }
  75% { transform: translate(-30px, -10px) scale(1.05); }
}

/* ---- 搜索栏（在 top-bar 内） ---- */
.search-bar {
  position: relative;
  flex-shrink: 0;
}

.search-input-wrapper {
  position: relative;
  width: 260px;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  pointer-events: none;
  z-index: 1;
}

.search-input {
  width: 100%;
  height: 36px;
  padding: 0 36px 0 38px;
  border: 1px solid var(--border-card);
  border-radius: 18px;
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.search-input::placeholder {
  color: var(--text-dim);
}

.search-input:focus {
  border-color: var(--border-input-focus);
  background: var(--bg-card-hover);
  box-shadow: var(--shadow-focus);
}

.search-clear {
  position: absolute;
  right: 14px;
  font-size: 14px;
  color: var(--text-dim);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s;
}

.search-clear:hover {
  color: var(--text-primary);
  background: var(--bg-card-hover);
}

/* ---- 搜索结果下拉 ---- */
.search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 380px;
  background: var(--modal-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-modal);
  z-index: 100;
  animation: dropIn 0.15s ease;
}

@keyframes dropIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-subtle);
}

.search-item:last-child { border-bottom: none; }

.search-item:hover {
  background: rgba(167, 139, 250, 0.12);
}

.search-type-tag {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.tag-city {
  background: rgba(102, 126, 234, 0.25);
  color: #93a5ff;
}

.tag-attr {
  background: rgba(240, 147, 251, 0.2);
  color: #f5a3fb;
}

.search-item-info {
  flex: 1;
  min-width: 0;
}

.search-item-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-item-path {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.top-bar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  background: var(--bg-card);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-divider);
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.top-bar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 20px;
  flex: 1;
  min-width: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.breadcrumb-bar {
  color: var(--text-body);
  font-size: 14px;
  white-space: nowrap;
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-dim);
  white-space: nowrap;
}

.stats-dot {
  color: var(--text-placeholder);
}

.breadcrumb-item {
  user-select: none;
  transition: color 0.2s;
}

.breadcrumb-item.clickable {
  color: var(--text-accent);
  cursor: pointer;
  font-weight: 500;
}

.breadcrumb-item.clickable:hover {
  color: var(--text-accent2);
}

.separator {
  margin: 0 8px;
  color: var(--text-dim);
}

.home-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.home-content.has-attractions .map-section {
  flex: 0 0 65%;
}

.map-section {
  flex: 1;
  position: relative;
  min-height: 400px;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.zoom-badge {
  position: absolute;
  bottom: 16px;
  right: 20px;
  padding: 4px 12px;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  z-index: 5;
  pointer-events: none;
  user-select: none;
}

.loading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 10;
  font-size: 16px;
  color: var(--text-accent);
  letter-spacing: 1px;
}

/* 景区面板 —— 与登录卡片一致的玻璃材质 */
.attractions-panel {
  width: 420px;
  flex-shrink: 0;
  overflow-y: auto;
  background: var(--bg-panel);
  backdrop-filter: blur(24px);
  border-left: 1px solid var(--border-subtle);
  padding: 24px 20px;
}

.panel-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-accent);
}

.loading-text,
.empty-text {
  text-align: center;
  color: var(--text-placeholder);
  padding: 60px 0;
  font-size: 14px;
}

.attraction-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.attraction-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  overflow: hidden;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.attraction-card:hover {
  transform: translateY(-4px);
  background: var(--bg-card-hover);
  border-color: var(--border-input-focus);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.3);
}

.attr-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.attraction-card:hover .attr-img {
  transform: scale(1.05);
}

.attr-info {
  padding: 16px;
}

.attr-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.cat-attraction {
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
}

.cat-food {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.cat-photo_spot {
  background: rgba(236, 72, 153, 0.15);
  color: #f472b6;
}

.attr-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.attr-meta {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-label);
}

.attr-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.attr-tips {
  margin-top: 10px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(240, 147, 251, 0.12);
  border: 1px solid rgba(240, 147, 251, 0.15);
  border-radius: 8px;
  line-height: 1.5;
}

/* ---- 面板头部 ---- */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.add-btn, .album-btn {
  padding: 6px 16px;
  border: 1px solid rgba(167, 139, 250, 0.4);
  border-radius: 8px;
  background: rgba(167, 139, 250, 0.1);
  color: var(--text-accent);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.add-btn:hover, .album-btn:hover {
  background: rgba(167, 139, 250, 0.25);
  border-color: #a78bfa;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}
.category-tab {
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: var(--bg-card);
  color: var(--text-muted);
  border: 1px solid transparent;
  transition: all 0.2s;
  user-select: none;
}
.category-tab:hover {
  color: var(--text-primary);
  border-color: var(--border-subtle);
}
.category-tab.active {
  background: rgba(167, 139, 250, 0.15);
  color: #a78bfa;
  border-color: rgba(167, 139, 250, 0.4);
}

.album-btn {
  border-color: rgba(240, 147, 251, 0.4);
  color: var(--text-accent2);
  background: rgba(240, 147, 251, 0.1);
}

.album-btn:hover {
  background: rgba(240, 147, 251, 0.25);
  border-color: #f093fb;
}

.add-hint {
  color: var(--text-accent);
  cursor: pointer;
  text-decoration: underline;
}

/* ---- 弹窗 ---- */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: var(--modal-overlay);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  width: 440px;
  max-height: 80vh;
  overflow-y: auto;
  background: var(--modal-bg);
  backdrop-filter: blur(24px);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: var(--shadow-modal);
}

.modal-card h3 {
  margin: 0 0 20px;
  font-size: 18px;
  color: var(--text-primary);
}

.modal-card .field {
  margin-bottom: 14px;
}

.modal-card label {
  display: block;
  font-size: 13px;
  color: var(--text-label);
  margin-bottom: 4px;
}

.modal-card input,
.modal-card textarea,
.modal-card select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-input);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  background: var(--bg-input);
  color: var(--text-primary);
}

.modal-card input::placeholder,
.modal-card textarea::placeholder {
  color: var(--text-placeholder);
}

.modal-card input:focus,
.modal-card textarea:focus,
.modal-card select:focus {
  border-color: var(--border-input-focus);
  background: var(--bg-input-focus);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid var(--border-input);
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-label);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.btn-submit {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: var(--btn-primary);
  color: var(--btn-primary-text);
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-submit:hover { opacity: 0.85; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.add-error { color: var(--color-error); font-size: 13px; margin: 8px 0 0; }
.add-success { color: var(--color-success); font-size: 13px; margin: 8px 0 0; }

/* ---- 景点详情弹窗 ---- */
.detail-card {
  position: relative;
  width: 520px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--modal-bg);
  backdrop-filter: blur(24px);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  box-shadow: var(--shadow-modal);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.detail-close {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 16px;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.detail-close:hover { background: var(--bg-card-hover); }

.detail-images {
  display: flex;
  overflow-x: auto;
  gap: 8px;
  padding: 0;
  border-radius: 16px 16px 0 0;
}

.detail-img {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  flex-shrink: 0;
  border-radius: 16px 16px 0 0;
}

.detail-images .detail-img {
  min-width: 100%;
  border-radius: 0;
}

.detail-images .detail-img:first-child { border-radius: 16px 0 0 0; }
.detail-images .detail-img:last-child { border-radius: 0 16px 0 0; }

.detail-img.single {
  border-radius: 16px 16px 0 0;
}

.detail-loading {
  text-align: center;
  padding: 40px;
  color: var(--text-dim);
}

.detail-name {
  padding: 20px 24px 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.detail-meta {
  display: flex;
  gap: 18px;
  padding: 8px 24px 0;
  font-size: 14px;
  color: var(--text-label);
}

.detail-section {
  padding: 16px 24px 0;
}

.detail-section h4 {
  font-size: 14px;
  color: var(--text-accent);
  margin: 0 0 6px;
}

.detail-section p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.detail-card > :last-child { padding-bottom: 24px; }
</style>
