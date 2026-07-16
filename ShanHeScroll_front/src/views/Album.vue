<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getMyAlbums, createAlbum, getAlbumDetail, addPhoto, updatePhotoDesc, deletePhoto, deleteAlbum, type Album } from '@/api/album'
import { useTheme } from '@/composables/useTheme'

const { theme: albumTheme } = useTheme()
const am = computed(() => albumTheme.value === 'dark' ? {
  area: 'rgba(255,255,255,0.04)',
  border: 'rgba(255,255,255,0.08)',
  label: 'rgba(255,255,255,0.5)',
  emphasisLabel: '#fff',
  noAlbumBorder: 'rgba(255,255,255,0.06)',
} : {
  area: 'rgba(0,0,0,0.04)',
  border: 'rgba(0,0,0,0.12)',
  label: 'rgba(0,0,0,0.45)',
  emphasisLabel: '#333',
  noAlbumBorder: 'rgba(0,0,0,0.08)',
})
import { loadRegionData, groupByProvince, getCityName, getProvinces, getCitiesByProvince, getProvinceByCityId } from '@/utils/regions'
import { thumbUrl } from '@/utils/thumb'
import http, { extractData } from '@/api'

const route = useRoute()

const albums = ref<Album[]>([])
const mapMode = ref(true)
const mapChartRef = ref<HTMLDivElement>()
let mapChart: echarts.ECharts | null = null
const mapLoading = ref(false)
const zoomPercent = ref(158) // 初始缩放（Album 默认 1.58）

// 地理数据缓存
const geoJsonCache = new Map<string, any>()
let provincesMap = new Map<string, any>()  // name → province region
let provinceIdToAdcode = new Map<number, string>()
let citiesMap = new Map<string, any>() // adcode → city region
const mapLevel = ref<'china' | number>('china')
const mapBreadcrumb = ref<{ name: string; id: number | 'china' }[]>([])
const mapFilterProvince = ref<string | null>(null) // 省份名筛选
const mapSelectedCity = ref<string | null>(null) // 城市名筛选

function getCitiesByProvinceName(provinceName: string) {
  const prov = getProvinces().find(p => p.name === provinceName || normalizeName(p.name) === normalizeName(provinceName))
  return prov ? getCitiesByProvince(prov.id) : []
}

// 计算有相册的城市 ID 集合
const albumCityIds = computed(() => {
  const ids = new Set<number>()
  for (const a of albums.value) {
    if (a.regionId) ids.add(a.regionId)
  }
  return ids
})

// 筛选后的相册（地图模式按城市筛选）
const filteredAlbums = computed(() => {
  if (mapSelectedCity.value) {
    return albums.value.filter(a => getCityName(a.regionId || 0) === mapSelectedCity.value || getCityName(a.regionId || 0) === mapSelectedCity.value!.replace(/市$/, ''))
  }
  if (mapFilterProvince.value) {
    const provCities = getCitiesByProvinceName(mapFilterProvince.value)
    const cityIds = new Set(provCities.map(c => c.id))
    return albums.value.filter(a => a.regionId && cityIds.has(a.regionId))
  }
  return albums.value
})

const grouped = ref<{ province: string; items: Album[] }[]>([])
const loading = ref(true)

// 创建相册
const showCreate = ref(false)
const createLoading = ref(false)
const createError = ref('')
const createForm = ref({
  title: '',
  description: '',
  coverImage: '',
  regionId: null as number | null,
  isShared: false,
  inviteeIds: [''] as string[],
})
const uploadingCover = ref(false)

// 相册详情
const showDetail = ref(false)
const detailAlbum = ref<Album | null>(null)
const visiblePhotos = ref<Album['photos']>([])
const photoPage = ref(0)
const PHOTO_PAGE_SIZE = 8
const hasMorePhotos = ref(false)
const uploadingPhoto = ref(false)
const fileInput = ref<HTMLInputElement>()

// 上传大小限制（与后端一致）
const MAX_FILE_SIZE = 20 * 1024 * 1024   // 单文件 20MB
const MAX_TOTAL_SIZE = 200 * 1024 * 1024 // 总请求 200MB

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 照片预览（支持翻页）
const previewIndex = ref(-1)
const previewPhoto = computed(() => {
  if (previewIndex.value < 0 || previewIndex.value >= visiblePhotos.value.length) return null
  return visiblePhotos.value[previewIndex.value]
})
const canPrev = computed(() => previewIndex.value > 0)
const canNext = computed(() => previewIndex.value < visiblePhotos.value.length - 1)

function openPreview(photo: { url: string; description: string | null }) {
  previewIndex.value = visiblePhotos.value.findIndex(p => p.id === (photo as any).id)
}

function prevPhoto() {
  if (canPrev.value) previewIndex.value--
}

function nextPhoto() {
  if (canNext.value) previewIndex.value++
}

// 编辑描述
const editingDesc = ref(false)
const editDescText = ref('')

function startEditDesc() {
  if (!previewPhoto.value) return
  editDescText.value = previewPhoto.value.description || ''
  editingDesc.value = true
}

async function saveDesc() {
  if (!previewPhoto.value || !detailAlbum.value) return
  const photo = previewPhoto.value as any
  try {
    await updatePhotoDesc(detailAlbum.value.id, photo.id, editDescText.value.trim())
    // 更新本地数据
    photo.description = editDescText.value.trim() || null
    visiblePhotos.value[previewIndex.value] = { ...visiblePhotos.value[previewIndex.value], description: photo.description }
    editingDesc.value = false
  } catch { /* ignore */ }
}

function cancelEditDesc() {
  editingDesc.value = false
}

// 删除相册
const deletingAlbum = ref(false)
async function handleDeleteAlbum(album: Album, e: Event) {
  e.stopPropagation()
  if (!confirm(`确定删除相册「${album.title}」吗？相册内所有照片将被删除。`)) return
  deletingAlbum.value = true
  try {
    await deleteAlbum(album.id)
    await loadAlbums()
  } catch { /* ignore */ }
  deletingAlbum.value = false
}

// 批量删除照片
const selectMode = ref(false)
const selectedPhotoIds = ref<Set<number>>(new Set())
const deletingPhotos = ref(false)

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  selectedPhotoIds.value = new Set()
}

function togglePhotoSelect(photoId: number) {
  const s = new Set(selectedPhotoIds.value)
  if (s.has(photoId)) s.delete(photoId)
  else s.add(photoId)
  selectedPhotoIds.value = s
}

async function handleDeletePhoto(photoId: number) {
  if (!detailAlbum.value || !confirm('确定删除这张照片吗？')) return
  try {
    await deletePhoto(detailAlbum.value.id, photoId)
    detailAlbum.value = await getAlbumDetail(detailAlbum.value.id)
    photoPage.value = 0
    visiblePhotos.value = []
    loadMorePhotos()
  } catch { /* ignore */ }
}

async function handleBatchDelete() {
  if (!detailAlbum.value || selectedPhotoIds.value.size === 0) return
  if (!confirm(`确定删除选中的 ${selectedPhotoIds.value.size} 张照片吗？`)) return
  deletingPhotos.value = true
  try {
    for (const pid of selectedPhotoIds.value) {
      await deletePhoto(detailAlbum.value.id, pid)
    }
    detailAlbum.value = await getAlbumDetail(detailAlbum.value.id)
    photoPage.value = 0
    visiblePhotos.value = []
    loadMorePhotos()
    selectedPhotoIds.value = new Set()
    selectMode.value = false
  } catch { /* ignore */ }
  deletingPhotos.value = false
}

// 添加照片 — 支持多选，每张可单独写描述
interface PendingPhoto {
  file: File
  name: string
  desc: string
}
const pendingPhotos = ref<PendingPhoto[]>([])
const addingPhoto = ref(false)

// 分批并发上传
const BATCH_SIZE = 4
const uploadProgress = ref(0)

// 计算属性：已选文件总大小
const pendingTotalSize = computed(() =>
  pendingPhotos.value.reduce((sum, p) => sum + p.file.size, 0)
)
const sizeExceeded = computed(() => pendingTotalSize.value > MAX_TOTAL_SIZE)

function triggerPhotoUpload() {
  addingPhoto.value = true
  pendingPhotos.value = []
  fileInput.value?.click()
}

function onFileSelected(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) {
    addingPhoto.value = false
    return
  }
  const fileList = Array.from(files)
  // 过滤单文件超限的
  const oversized = fileList.filter(f => f.size > MAX_FILE_SIZE)
  if (oversized.length) {
    alert(`以下文件超过单文件上限(20MB)，已跳过:\n${oversized.map(f => `${f.name}（${formatSize(f.size)}）`).join('\n')}`)
  }
  const valid = fileList.filter(f => f.size <= MAX_FILE_SIZE)
  if (!valid.length) {
    addingPhoto.value = false
    return
  }
  pendingPhotos.value = valid.map(f => ({
    file: f,
    name: f.name,
    desc: '',
  }))
}

function removePending(index: number) {
  pendingPhotos.value.splice(index, 1)
  if (pendingPhotos.value.length === 0) {
    addingPhoto.value = false
  }
}

function cancelAddPhoto() {
  addingPhoto.value = false
  pendingPhotos.value = []
  if (fileInput.value) fileInput.value.value = ''
}

async function confirmUpload() {
  if (!pendingPhotos.value.length || !detailAlbum.value) return
  if (sizeExceeded.value) return

  uploadingPhoto.value = true
  uploadProgress.value = 0

  try {
    // 拆分成多个批次（每批 BATCH_SIZE 个）
    const batches: PendingPhoto[][] = []
    for (let i = 0; i < pendingPhotos.value.length; i += BATCH_SIZE) {
      batches.push(pendingPhotos.value.slice(i, i + BATCH_SIZE))
    }

    // 总字节数，用于计算整体百分比
    const totalBytes = pendingPhotos.value.reduce((s, p) => s + p.file.size, 0)
    const perBatchLoaded = new Array(batches.length).fill(0)

    // 多批并发上传
    const batchPromise = batches.map((batch, idx) => {
      const form = new FormData()
      for (const p of batch) {
        form.append('files', p.file)
      }
      return http.post<string[]>('/file/upload/batch', form, {
        timeout: 300000,
        onUploadProgress: (pe: { loaded?: number }) => {
          if (pe.loaded) {
            perBatchLoaded[idx] = pe.loaded
            const allLoaded = perBatchLoaded.reduce((s, v) => s + v, 0)
            uploadProgress.value = Math.min(Math.round((allLoaded / totalBytes) * 100), 99)
          }
        },
      }).then(extractData)
    })

    const results = await Promise.all(batchPromise)
    uploadProgress.value = 100

    // 按原始顺序展平所有 URL
    const urls: string[] = results.flat()

    // 逐个添加照片（带描述）
    for (let i = 0; i < urls.length; i++) {
      await addPhoto(detailAlbum.value.id, urls[i], pendingPhotos.value[i].desc.trim() || undefined)
    }

    detailAlbum.value = await getAlbumDetail(detailAlbum.value.id)
    photoPage.value = 0
    visiblePhotos.value = []
    loadMorePhotos()
    cancelAddPhoto()
  } catch (err: any) {
    alert('上传失败: ' + (err?.message || '未知错误'))
  }
  uploadingPhoto.value = false
  uploadProgress.value = 0
}

async function openDetail(album: Album) {
  showDetail.value = true
  try {
    detailAlbum.value = await getAlbumDetail(album.id)
  } catch {
    detailAlbum.value = album
  }
  photoPage.value = 0
  visiblePhotos.value = []
  loadMorePhotos()
}

function loadMorePhotos() {
  if (!detailAlbum.value) return
  const start = photoPage.value * PHOTO_PAGE_SIZE
  const batch = detailAlbum.value.photos.slice(start, start + PHOTO_PAGE_SIZE)
  visiblePhotos.value.push(...batch)
  photoPage.value++
  hasMorePhotos.value = start + PHOTO_PAGE_SIZE < detailAlbum.value.photos.length
}

// 地点选择（省→市级联）
const selProvinceId = ref<number | ''>('')
const selCityId = ref<number | ''>('')
const availableCities = ref<ReturnType<typeof getCitiesByProvince>>([])

function onProvinceChange(pid: number | '') {
  selCityId.value = ''
  if (pid === '') {
    availableCities.value = []
    createForm.value.regionId = null
  } else {
    availableCities.value = getCitiesByProvince(pid)
  }
}

function onCityChange(cid: number | '') {
  createForm.value.regionId = cid === '' ? null : cid
}

function openCreate() {
  selProvinceId.value = ''
  selCityId.value = ''
  availableCities.value = []
  createForm.value = { title: '', description: '', coverImage: '', regionId: null, isShared: false, inviteeIds: [''] }
  showCreate.value = true
}

onMounted(async () => {
  await loadRegionData()
  await loadAlbums()

  // 如果从首页带了 cityId 参数，自动打开创建窗并预选地点
  const cityId = route.query.cityId
  if (cityId) {
    const cid = Number(cityId)
    const province = getProvinceByCityId(cid)
    if (province) {
      selProvinceId.value = province.id
      availableCities.value = getCitiesByProvince(province.id)
      selCityId.value = cid
      createForm.value.regionId = cid
    }
    createForm.value.title = getCityName(cid) + '之旅'
    showCreate.value = true
  }

  // 默认地图模式，初始化地图
  if (mapMode.value) {
    await nextTick()
    await new Promise(r => setTimeout(r, 300))
    try {
      if (mapChartRef.value && mapChartRef.value.offsetHeight > 0) {
        await renderAlbumChina()
      }
    } catch (e) {
      console.error('地图渲染失败:', e)
    }
    mapLoading.value = false
  }
})

// ---- 地图模式 ----
function normalizeName(name: string): string {
  return name.replace(/省$/, '').replace(/市$/, '').replace(/壮族自治区$/, '').replace(/回族自治区$/, '').replace(/维吾尔自治区$/, '').replace(/自治区$/, '').replace(/特别行政区$/, '')
}

async function loadGeoJson(adcode: string): Promise<any> {
  if (geoJsonCache.has(adcode)) return geoJsonCache.get(adcode)
  const url = `https://geo.datav.aliyun.com/areas_v3/bound/${adcode}_full.json`
  const res = await fetch(url)
  if (!res.ok) throw new Error('地图数据不可用')
  const json = await res.json()
  geoJsonCache.set(adcode, json)
  return json
}

function getFeatureCenter(feature: any): [number, number] {
  const coords = feature.geometry.coordinates
  let xSum = 0, ySum = 0, count = 0
  function walk(arr: any) {
    if (typeof arr[0] === 'number') { xSum += arr[0]; ySum += arr[1]; count++ }
    else arr.forEach(walk)
  }
  walk(coords)
  return [xSum / count, ySum / count]
}

async function initMapProvinces() {
  const regions = getProvinces()
  for (const p of regions) provincesMap.set(p.name, p)
}

async function renderAlbumChina() {
  if (!mapChartRef.value) return
  mapLoading.value = true
  mapLevel.value = 'china'
  mapBreadcrumb.value = []

  await initMapProvinces()
  const geo = await loadGeoJson('100000')
  echarts.registerMap('china', geo)

  // 建立 adcode 映射
  for (const feat of geo.features) {
    const geoName = feat.properties.name
    const adcode = String(feat.properties.adcode)
    if (provincesMap.has(geoName)) {
      provinceIdToAdcode.set(provincesMap.get(geoName).id, adcode)
    } else {
      const n = normalizeName(geoName)
      for (const [key, val] of provincesMap) {
        if (normalizeName(key) === n) { provinceIdToAdcode.set(val.id, adcode); break }
      }
    }
  }

  if (!mapChart) {
    mapChart = echarts.init(mapChartRef.value)
    mapChart.on('click', onAlbumMapClick)
    mapChart.on('georoam', () => {
      const opt = mapChart!.getOption() as any
      zoomPercent.value = Math.round((opt.series?.[0]?.zoom || 1.58) * 100)
    })
  }
  mapChart.resize()

  // 高亮有相册的省份
  const albumProvinceIds = new Set<number>()
  for (const cid of albumCityIds.value) {
    const city = getCitiesByProvince(0).find(c => c.id === cid) || [...getProvinces().flatMap(p => getCitiesByProvince(p.id))].find(c => c.id === cid)
    if (city) albumProvinceIds.add(city.parentId)
  }

  const mapData = geo.features.map((feat: any) => {
    const name = feat.properties.name
    let pid: number | undefined
    if (provincesMap.has(name)) pid = provincesMap.get(name).id
    else {
      for (const [key, val] of provincesMap) {
        if (normalizeName(key) === normalizeName(name)) { pid = val.id; break }
      }
    }
    const hasAlbum = pid && albumProvinceIds.has(pid)
    return {
      name,
      itemStyle: hasAlbum ? { areaColor: 'rgba(167,139,250,0.5)', borderColor: 'rgba(167,139,250,0.6)' } : { areaColor: am.value.area, borderColor: am.value.noAlbumBorder },
      label: hasAlbum ? { color: '#fff', fontWeight: 'bold' } : {},
    }
  })

  mapChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}' },
    series: [{ type: 'map', map: 'china', roam: true, scaleLimit: { min: 1, max: 4 }, zoom: 1.58, center: [104.5, 36],
      label: { show: true, fontSize: 10, color: am.value.label },
      itemStyle: { areaColor: am.value.area, borderColor: am.value.border, borderWidth: 1 },
      emphasis: { label: { fontSize: 13, fontWeight: 'bold', color: am.value.emphasisLabel }, itemStyle: { areaColor: 'rgba(167,139,250,0.3)' } },
      data: mapData,
    }],
  })

  mapLoading.value = false
}

async function renderAlbumProvince(provinceId: number) {
  if (!mapChart) return
  mapLoading.value = true

  const province = provincesMap.get([...provincesMap.values()].find(p => p.id === provinceId)?.name || '')
  const adcode = provinceIdToAdcode.get(provinceId)
  if (!adcode) { mapLoading.value = false; return }

  let geo: any
  try {
    geo = await loadGeoJson(adcode)
  } catch {
    // 无详细 GeoJSON（台湾/香港/澳门），提取中国地图轮廓兜底
    const chinaGeo = geoJsonCache.get('100000')
    if (chinaGeo) {
      const feature = chinaGeo.features.find((f: any) => String(f.properties.adcode) === adcode)
      if (feature) geo = { type: 'FeatureCollection', features: [feature] }
    }
    if (!geo) { mapLoading.value = false; return }
  }
  const mapName = `album_prov_${adcode}`
  echarts.registerMap(mapName, geo)

  // 加载城市（用缓存数据，避免重复请求）
  const allCities = getCitiesByProvince(provinceId)
  citiesMap.clear()
  for (const c of allCities) {
    const apiName = normalizeName(c.name)
    for (const feat of geo.features) {
      if (normalizeName(feat.properties.name) === apiName) {
        citiesMap.set(String(feat.properties.adcode), c)
        break
      }
    }
  }

  // 高亮有相册的城市
  const mapData = geo.features.map((feat: any) => {
    const adcode = String(feat.properties.adcode)
    const cityName = feat.properties.name
    const city = citiesMap.get(adcode)
    const hasAlbum = city && albumCityIds.value.has(city.id)
    return {
      name: cityName,
      itemStyle: hasAlbum ? { areaColor: 'rgba(167,139,250,0.5)', borderColor: 'rgba(167,139,250,0.6)' } : { areaColor: am.value.area, borderColor: am.value.noAlbumBorder },
      label: hasAlbum ? { color: '#fff', fontWeight: 'bold' } : {},
    }
  })

  mapChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}' },
    series: [{ type: 'map', map: mapName, roam: true, scaleLimit: { min: 1, max: 4 }, zoom: 1.58,
      label: { show: true, fontSize: 11, color: am.value.label },
      itemStyle: { areaColor: am.value.area, borderColor: am.value.border, borderWidth: 1 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold', color: am.value.emphasisLabel }, itemStyle: { areaColor: 'rgba(167,139,250,0.3)' } },
      data: mapData,
    }],
  }, true)

  mapLoading.value = false
}

async function onAlbumMapClick(params: any) {
  if (!params.name || !mapChart) return

  if (mapLevel.value === 'china') {
    // 点击省份 → 下钻 + 筛选该省所有相册
    const geo = geoJsonCache.get('100000')
    let pid: number | undefined
    if (provincesMap.has(params.name)) pid = provincesMap.get(params.name).id
    else {
      for (const [key, val] of provincesMap) {
        if (normalizeName(key) === normalizeName(params.name)) { pid = val.id; break }
      }
    }
    if (!pid) return

    // 筛选该省份
    mapFilterProvince.value = params.name
    mapSelectedCity.value = null

    mapLevel.value = pid
    mapBreadcrumb.value = [{ name: '全国', id: 'china' }, { name: params.name, id: pid }]

    for (const feat of geo.features) {
      if (feat.properties.name === params.name) {
        const center = getFeatureCenter(feat)
        mapChart.setOption({ series: [{ center, zoom: 5 }] })
        break
      }
    }
    await new Promise(r => setTimeout(r, 600))
    await renderAlbumProvince(pid)
    await loadAlbums()
  } else {
    // 点击城市 → 进一步筛选
    const cityName = params.name
    mapSelectedCity.value = cityName === mapSelectedCity.value ? null : cityName
    mapFilterProvince.value = null
    await loadAlbums()
  }
}

async function goAlbumMapLevel(item: { name: string; id: number | 'china' }) {
  if (item.id === 'china') {
    mapFilterProvince.value = null
    mapSelectedCity.value = null
    await renderAlbumChina()
    await loadAlbums()
  } else {
    mapLevel.value = item.id as number
    mapBreadcrumb.value = mapBreadcrumb.value.filter(b => b.id !== 'china' && (b.id as number) <= (item.id as number))
    await renderAlbumProvince(item.id as number)
  }
}

function switchMapMode() {
  mapMode.value = true
}

// 监听 mapMode 切换 → 渲染地图
watch(mapMode, async (val) => {
  if (!val) return
  await nextTick()
  await new Promise(r => setTimeout(r, 300)) // 等 CSS 布局完成
  try {
    if (mapChartRef.value && mapChartRef.value.offsetHeight > 0) {
      await renderAlbumChina()
    }
  } catch (e) {
    console.error('地图渲染失败:', e)
  }
  mapLoading.value = false
})

async function clearFilter() {
  mapFilterProvince.value = null
  mapSelectedCity.value = null
  await renderAlbumChina()
  await loadAlbums()
}

function switchListMode() {
  mapMode.value = false
  mapFilterProvince.value = null
  mapSelectedCity.value = null
  if (mapChart) { mapChart.dispose(); mapChart = null }
  loadAlbums()
}

function handleMapResize() {
  mapChart?.resize()
}

onBeforeUnmount(() => {
  mapChart?.dispose()
  window.removeEventListener('resize', handleMapResize)
})

// 主题切换时重绘地图
watch(albumTheme, async () => {
  if (!mapChart) return
  mapLoading.value = true
  if (mapLevel.value === 'china') {
    await renderAlbumChina()
  } else {
    await renderAlbumProvince(mapLevel.value)
  }
  mapLoading.value = false
})

async function loadAlbums() {
  loading.value = true
  try {
    albums.value = await getMyAlbums()
    const source = mapSelectedCity.value ? filteredAlbums.value : albums.value
    grouped.value = groupByProvince(source)
  } catch { /* 未登录等 */ }
  loading.value = false
}

async function handleUploadCover(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  uploadingCover.value = true
  try {
    const url = await http.post<string>('/file/upload', form).then(extractData)
    createForm.value.coverImage = url
  } catch { /* ignore */ }
  uploadingCover.value = false
}

function addInviteeRow() {
  createForm.value.inviteeIds.push('')
}

function removeInviteeRow(index: number) {
  if (createForm.value.inviteeIds.length <= 1) {
    createForm.value.inviteeIds[0] = ''
    return
  }
  createForm.value.inviteeIds.splice(index, 1)
}

async function handleCreate() {
  createError.value = ''
  if (!createForm.value.title.trim()) {
    createError.value = '请输入相册标题'
    return
  }
  if (!createForm.value.regionId) {
    createError.value = '请选择地点'
    return
  }
  // 解析邀请 ID（过滤空值和无效数字）
  const inviteeIds = createForm.value.isShared
    ? createForm.value.inviteeIds.map(Number).filter(n => n > 0 && Number.isFinite(n))
    : undefined
  if (createForm.value.isShared && (!inviteeIds || inviteeIds.length === 0)) {
    createError.value = '开启共同相册后请至少输入一个有效的用户ID'
    return
  }

  createLoading.value = true
  try {
    await createAlbum({
      regionId: createForm.value.regionId,
      title: createForm.value.title.trim(),
      description: createForm.value.description.trim() || undefined,
      coverImage: createForm.value.coverImage || undefined,
      isShared: createForm.value.isShared,
      inviteeIds,
    })
    showCreate.value = false
    createForm.value = { title: '', description: '', coverImage: '', regionId: null, isShared: false, inviteeIds: [''] }
    await loadAlbums()
  } catch (err: any) {
    createError.value = err.message || '创建失败'
  }
  createLoading.value = false
}


</script>

<template>
  <div class="album-page">
    <div class="album-container">
      <div class="album-header">
        <h1>我的旅游相册</h1>
        <div class="header-actions">
          <div class="mode-toggle">
            <button :class="{ active: !mapMode }" @click="switchListMode">列表</button>
            <button :class="{ active: mapMode }" @click="switchMapMode">地图</button>
          </div>
          <button class="create-btn" @click="openCreate">+ 创建相册</button>
        </div>
      </div>

      <!-- 地图模式 -->
      <div v-if="mapMode" class="map-mode-wrap">
        <div class="map-breadcrumb" v-if="mapBreadcrumb.length">
          <span v-for="(item, idx) in mapBreadcrumb" :key="item.id" class="map-breadcrumb-item" :class="{ clickable: idx < mapBreadcrumb.length - 1 }" @click="idx < mapBreadcrumb.length - 1 && goAlbumMapLevel(item)">
            {{ item.name }}<span v-if="idx < mapBreadcrumb.length - 1" class="sep"> &gt; </span>
          </span>
        </div>
        <div class="map-chart-wrap">
          <div v-if="mapLoading" class="map-loading">加载中...</div>
          <div ref="mapChartRef" class="map-chart"></div>
          <div class="zoom-badge">{{ zoomPercent }}%</div>
        </div>
        <p v-if="mapFilterProvince || mapSelectedCity" class="map-filter-hint">
          当前筛选：{{ mapSelectedCity || mapFilterProvince }}（{{ filteredAlbums.length }} 个相册）
          <span class="clear-filter" @click="clearFilter()">✕ 清除</span>
        </p>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>

      <!-- 按省份分组（两种模式共用） -->
      <div v-else-if="grouped.length" class="album-groups">
        <section v-for="group in grouped" :key="group.province" class="province-group">
          <h2 class="province-title">{{ group.province }}</h2>
          <div class="album-grid">
            <div
              v-for="album in group.items"
              :key="album.id"
              class="album-card"
              @click="openDetail(album)"
            >
              <div class="card-cover">
                <img v-if="album.coverImage" :src="thumbUrl(album.coverImage)" @error="(e) => (e.target as HTMLImageElement).src = album.coverImage!" alt="" />
                <span v-else class="no-cover">📷</span>
              </div>
              <div class="card-body">
                <button class="card-delete-btn" title="删除相册" @click="handleDeleteAlbum(album, $event)">🗑</button>
                <div class="card-title">
                  {{ album.title }}
                  <span v-if="album.isShared" class="shared-badge" title="多人共同相册">👥</span>
                </div>
                <div class="card-meta">
                  <span v-if="album.regionId">{{ getCityName(album.regionId) }}</span>
                  <span>{{ album.photoCount || 0 }} 张照片</span>
                </div>
                <div class="card-desc" v-if="album.description">{{ album.description }}</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 无相册 -->
      <div v-else class="empty-state">
        <div class="empty-icon">📸</div>
        <p>还没有相册，创建你的第一个旅行相册吧</p>
        <button class="empty-create" @click="openCreate">创建相册</button>
      </div>
    </div>

    <!-- 创建相册弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal-card">
        <h3>创建相册</h3>
        <div class="field">
          <label>相册标题 <span class="req">*</span></label>
          <input v-model="createForm.title" maxlength="100" placeholder="如：北京之旅" />
        </div>
        <div class="field">
          <label>描述</label>
          <textarea v-model="createForm.description" rows="2" maxlength="200" placeholder="简单描述一下这次旅行..."></textarea>
        </div>
        <div class="field">
          <label>地点 <span class="req">*</span></label>
          <div class="location-row">
            <select v-model="selProvinceId" @change="onProvinceChange(selProvinceId)">
              <option value="">选择省份</option>
              <option v-for="p in getProvinces()" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
            <select v-model="selCityId" :disabled="selProvinceId === ''" @change="onCityChange(selCityId)">
              <option value="">{{ selProvinceId === '' ? '请先选省份' : '选择城市' }}</option>
              <option v-for="c in availableCities" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label>封面图片</label>
          <div class="cover-upload">
            <input v-model="createForm.coverImage" placeholder="图片URL 或点击右侧上传" />
            <label class="upload-btn">
              {{ uploadingCover ? '...' : '上传' }}
              <input type="file" accept="image/*" style="display:none" @change="handleUploadCover" />
            </label>
          </div>
          <img v-if="createForm.coverImage" :src="createForm.coverImage" class="cover-preview" alt="" />
        </div>

        <div class="field">
          <label class="shared-label">
            <input type="checkbox" v-model="createForm.isShared" />
            设为多人共同相册
          </label>
        </div>

        <div class="field" v-if="createForm.isShared">
          <label>邀请用户ID</label>
          <div v-for="(_, idx) in createForm.inviteeIds" :key="idx" class="invitee-row">
            <input v-model="createForm.inviteeIds[idx]" type="number" min="1" placeholder="输入用户ID" />
            <button type="button" class="btn-remove-invitee" @click="removeInviteeRow(idx)" title="移除">−</button>
          </div>
          <button type="button" class="btn-add-invitee" @click="addInviteeRow">+ 添加用户</button>
          <span class="field-hint">被邀请的用户将在通知栏收到邀请，同意后即可共同管理相册</span>
        </div>

        <p v-if="createError" class="create-error">{{ createError }}</p>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showCreate = false">取消</button>
          <button class="btn-submit" :disabled="createLoading" @click="handleCreate">
            {{ createLoading ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 相册详情弹窗 -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="detail-card" v-if="detailAlbum">
        <div class="detail-header">
          <h2>{{ detailAlbum.title }} <span class="detail-id">#{{ detailAlbum.id }}</span></h2>
          <div class="header-actions">
            <button v-if="!selectMode && !addingPhoto" class="add-photo-btn-sm" @click="triggerPhotoUpload">+ 照片</button>
            <button v-if="!addingPhoto" :class="['add-photo-btn-sm', { on: selectMode }]" @click="toggleSelectMode">
              {{ selectMode ? '取消' : '管理' }}
            </button>
            <button v-if="selectMode && selectedPhotoIds.size" class="delete-batch-btn" :disabled="deletingPhotos" @click="handleBatchDelete">
              删除{{ selectedPhotoIds.size }}张
            </button>
            <button class="detail-close" @click="showDetail = false">✕</button>
          </div>
        </div>
        <p class="detail-desc" v-if="detailAlbum.description">{{ detailAlbum.description }}</p>

        <!-- 添加照片中 — 多选 -->
        <div v-if="addingPhoto" class="upload-panel">
          <div class="upload-panel-header">
            <div>
              <span>{{ pendingPhotos.length ? `已选 ${pendingPhotos.length} 张` : '选择照片' }}</span>
              <span v-if="pendingPhotos.length" class="size-info" :class="{ 'size-exceeded': sizeExceeded }">
                · 总大小 {{ formatSize(pendingTotalSize) }} / 200MB
              </span>
              <div class="size-limit-hint">单文件 ≤ 20MB，总大小 ≤ 200MB</div>
            </div>
            <div class="upload-panel-actions">
              <button v-if="!pendingPhotos.length" class="select-btn-sm" @click="triggerPhotoUpload">选择照片</button>
              <template v-else>
                <button class="select-btn-sm" @click="triggerPhotoUpload">追加</button>
                <button class="confirm-btn-sm" :disabled="uploadingPhoto || sizeExceeded" @click="confirmUpload">
                  {{ uploadingPhoto ? '上传中...' : sizeExceeded ? '超出限制' : `上传全部` }}
                </button>
                <button class="cancel-btn-sm" @click="cancelAddPhoto">取消</button>
              </template>
            </div>
          </div>
          <!-- 上传进度条 -->
          <div v-if="uploadingPhoto" class="upload-progress-wrap">
            <div class="upload-progress-bar">
              <div class="upload-progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <span class="upload-progress-text">{{ uploadProgress }}%</span>
          </div>

          <div v-if="pendingPhotos.length" class="upload-list">
            <div v-for="(p, i) in pendingPhotos" :key="i" class="upload-item">
              <span class="upload-item-name">📄 {{ p.name }}</span>
              <input v-model="p.desc" placeholder="描述（选填）" class="photo-desc-input-sm" />
              <button class="remove-btn" @click="removePending(i)" :disabled="uploadingPhoto">✕</button>
            </div>
          </div>
        </div>
        <input ref="fileInput" type="file" accept="image/*" multiple style="display:none" @change="onFileSelected" />

        <!-- 照片网格：每行4张 -->
        <div class="photo-grid" v-if="visiblePhotos.length">
          <div v-for="photo in visiblePhotos" :key="photo.id" class="photo-item" :class="{ selected: selectedPhotoIds.has(photo.id) }">
            <img
              :src="thumbUrl(photo.url) || photo.url"
              :alt="photo.description || ''"
              loading="lazy"
              @error="(e) => (e.target as HTMLImageElement).src = photo.url"
              @click="selectMode ? togglePhotoSelect(photo.id) : openPreview(photo)"
            />
            <span class="photo-label" v-if="photo.description">{{ photo.description }}</span>
            <!-- 选择模式勾选框 -->
            <div v-if="selectMode" class="photo-check" :class="{ checked: selectedPhotoIds.has(photo.id) }" @click.stop="togglePhotoSelect(photo.id)">
              {{ selectedPhotoIds.has(photo.id) ? '✓' : '' }}
            </div>
            <!-- 非选择模式悬停删除 -->
            <button v-if="!selectMode" class="photo-delete-btn" title="删除照片" @click.stop="handleDeletePhoto(photo.id)">✕</button>
          </div>
        </div>
        <div v-else class="no-photos">暂无照片，点击上方添加</div>

        <!-- 加载更多 -->
        <div class="load-more" v-if="hasMorePhotos">
          <button class="load-more-btn" @click="loadMorePhotos">加载更多（已显示 {{ visiblePhotos.length }} / {{ detailAlbum.photos.length }}）</button>
        </div>
      </div>
    </div>
    <!-- 照片预览 — 明信片 + 翻页 -->
    <div v-if="previewPhoto" class="lightbox" @click="previewIndex = -1">
      <div class="postcard" @click.stop>
        <button class="lightbox-close" @click="previewIndex = -1">✕</button>

        <!-- 翻页按钮 -->
        <button class="nav-btn prev" :class="{ disabled: !canPrev }" @click="prevPhoto" :disabled="!canPrev">◂</button>
        <button class="nav-btn next" :class="{ disabled: !canNext }" @click="nextPhoto" :disabled="!canNext">▸</button>

        <div class="postcard-left">
          <img :src="previewPhoto.url" alt="" />
        </div>
        <div class="postcard-right">
          <div class="postcard-stamp">🏔️</div>

          <!-- 描述（可编辑） -->
          <div v-if="editingDesc" class="postcard-desc-edit">
            <textarea v-model="editDescText" rows="3" placeholder="输入描述..."></textarea>
            <div class="edit-actions">
              <button class="save-btn" @click="saveDesc">保存</button>
              <button class="cancel-edit-btn" @click="cancelEditDesc">取消</button>
            </div>
          </div>
          <div v-else class="postcard-desc" @dblclick="startEditDesc" title="双击编辑描述">
            {{ previewPhoto.description || '山河卷 · 旅途记忆' }}
          </div>

          <div class="postcard-lines">
            <div class="postcard-line"></div>
            <div class="postcard-line"></div>
            <div class="postcard-line short"></div>
          </div>
          <span class="photo-time">{{ (previewPhoto as any).createdAt?.replace('T', ' ').substring(0, 16) || '' }}</span>
        </div>
        <div class="photo-counter">{{ previewIndex + 1 }} / {{ visiblePhotos.length }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.album-page {
  min-height: calc(100vh - 73px);
  background: var(--bg-page);
}

.album-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

.album-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.album-header h1 {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--text-accent), var(--text-accent2));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mode-toggle {
  display: flex;
  background: var(--bg-panel);
  border-radius: 8px;
  overflow: hidden;
}

.mode-toggle button {
  padding: 7px 16px;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-label);
  cursor: pointer;
  transition: all 0.2s;
}

.mode-toggle button.active {
  background: var(--btn-primary);
  color: var(--btn-primary-text);
}

.create-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  background: var(--btn-primary);
  color: var(--btn-primary-text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.create-btn:hover { opacity: 0.9; }

/* 地图模式 */
.map-mode-wrap {
  margin-bottom: 24px;
}

.map-breadcrumb {
  padding: 8px 0;
  font-size: 14px;
  color: var(--text-label);
}

.map-breadcrumb-item { user-select: none; transition: color 0.2s; }
.map-breadcrumb-item.clickable { color: var(--text-accent); cursor: pointer; font-weight: 500; }
.map-breadcrumb-item.clickable:hover { color: var(--text-accent2); }
.map-breadcrumb-item .sep { margin: 0 6px; color: var(--text-placeholder); }

.map-chart-wrap {
  position: relative;
  height: 500px;
  background: var(--bg-panel);
  border-radius: 12px;
  border: 1px solid var(--border-divider);
}

.map-chart {
  width: 100%;
  height: 100%;
}

.zoom-badge {
  position: absolute;
  bottom: 14px;
  right: 16px;
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

.map-loading {
  position: absolute;
  inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.4);
  z-index: 10;
  font-size: 15px;
  color: var(--text-accent);
}

.map-filter-hint {
  margin-top: 10px;
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
}

.clear-filter {
  color: var(--color-error);
  cursor: pointer;
  margin-left: 8px;
}

.clear-filter:hover { text-decoration: underline; }

/* 分组 */
.province-group {
  margin-bottom: 32px;
}

.province-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 16px;
  border-left: 3px solid var(--text-accent);
  padding-left: 12px;
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.album-card {
  background: var(--bg-card);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.album-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(167,139,250,0.2);
  border-color: rgba(167,139,250,0.3);
}

.card-cover {
  height: 180px;
  background: var(--bg-panel);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  position: relative;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-delete-btn {
  position: absolute;
  top: 50%;
  right: 12px;
  transform: translateY(-50%);
  width: 26px; height: 26px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.08);
  font-size: 13px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.album-card:hover .card-delete-btn {
  opacity: 1;
}

.card-delete-btn:hover { background: rgba(231,76,60,0.15); color: var(--color-error); }

.card-body {
  position: relative;
  padding: 14px 16px;
}

.shared-badge {
  font-size: 14px;
  margin-left: 4px;
  vertical-align: middle;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-body);
  margin-bottom: 6px;
}

.card-id {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-placeholder);
}

.card-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.card-desc {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  color: var(--text-dim);
  margin-bottom: 20px;
}

.empty-create {
  padding: 10px 28px;
  border: none;
  border-radius: 10px;
  background: var(--btn-primary);
  color: var(--btn-primary-text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.loading-state {
  text-align: center;
  color: var(--text-dim);
  padding: 80px 0;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  width: 440px;
  max-height: 85vh;
  overflow-y: auto;
  background: var(--modal-bg);
  border-radius: 14px;
  padding: 28px 32px;
  box-shadow: var(--shadow-modal);
}

.modal-card h3 {
  margin: 0 0 20px;
  font-size: 18px;
  color: var(--text-heading);
}

.field {
  margin-bottom: 14px;
}

.field label {
  display: block;
  font-size: 13px;
  color: var(--text-label);
  margin-bottom: 4px;
}

.req { color: var(--color-error); }

.field input,
.field textarea {
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

.field input:focus,
.field textarea:focus {
  border-color: var(--border-input-focus);
}

.location-row {
  display: flex;
  gap: 10px;
}

.location-row select {
  flex: 1;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--border-input);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: var(--bg-input);
  color: var(--text-primary);
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23888' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

[data-theme="dark"] .location-row select {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

[data-theme="dark"] .location-row select option {
  background: #1e1e2e;
  color: #fff;
}

.location-row select:focus {
  border-color: var(--border-input-focus);
}

.location-row select option {
  background: var(--bg-input);
  color: var(--text-primary);
}

.location-row select:disabled {
  background: var(--bg-panel);
  color: var(--text-placeholder);
  cursor: not-allowed;
}

.cover-upload {
  display: flex;
  gap: 8px;
}

.cover-upload input {
  flex: 1;
}

.upload-btn {
  padding: 10px 16px;
  background: #667eea;
  color: var(--btn-primary-text);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.cover-preview {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  margin-top: 8px;
}

.bind-hint {
  font-size: 13px;
  color: var(--text-accent);
  margin: -6px 0 8px;
}

.create-error {
  color: var(--color-error);
  font-size: 13px;
  margin: 8px 0 0;
}

.shared-label {
  display: flex !important;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px !important;
  color: #555 !important;
}

.shared-label input {
  width: auto !important;
}

.invitee-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.invitee-row input {
  flex: 1;
}

.btn-remove-invitee {
  width: 32px;
  height: 32px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  color: #e74c3c;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-remove-invitee:hover {
  background: #fee;
  border-color: #e74c3c;
}

.btn-add-invitee {
  border: 1px dashed #667eea;
  border-radius: 8px;
  background: transparent;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-add-invitee:hover {
  background: #f0edff;
  border-style: solid;
}

.field-hint {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid var(--border-input);
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-label);
  cursor: pointer;
}

.btn-submit {
  padding: 8px 24px;
  border: none;
  border-radius: 8px;
  background: var(--btn-primary);
  color: var(--btn-primary-text);
  font-weight: 500;
  cursor: pointer;
}

.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- 详情弹窗 ---- */
.detail-card {
  width: 760px;
  max-width: 95vw;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--modal-bg);
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: var(--shadow-modal);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.detail-header h2 {
  font-size: 20px;
  color: var(--text-heading);
}

.detail-id {
  font-size: 14px;
  font-weight: 400;
  color: var(--text-placeholder);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-close {
  width: 32px; height: 32px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.15);
  color: #fff; font-size: 16px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}

.detail-close:hover { background: rgba(0,0,0,0.4); }

.detail-desc {
  font-size: 14px; color: var(--text-muted);
  margin: 0 0 14px;
}

.add-photo-btn-sm {
  padding: 5px 14px;
  border: 1px solid var(--text-accent);
  border-radius: 6px;
  background: transparent;
  color: var(--text-accent);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.add-photo-btn-sm:hover, .add-photo-btn-sm.on {
  background: #667eea;
  color: var(--btn-primary-text);
}

.delete-batch-btn {
  padding: 5px 12px;
  border: 1px solid #e74c3c;
  border-radius: 6px;
  background: #e74c3c;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.delete-batch-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 上传面板 */
.upload-panel {
  margin-bottom: 16px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  overflow: hidden;
}

.upload-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--bg-panel);
  font-size: 13px;
  color: var(--text-label);
}

.upload-panel-actions {
  display: flex;
  gap: 6px;
}

.size-info {
  color: var(--text-muted);
  font-size: 12px;
}
.size-exceeded {
  color: #e74c3c;
  font-weight: 600;
}
.size-limit-hint {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 上传进度条 */
.upload-progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-panel);
}

.upload-progress-bar {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: var(--bg-input);
  overflow: hidden;
}

.upload-progress-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #667eea, #a78bfa);
  transition: width 0.3s ease;
}

.upload-progress-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-accent);
  min-width: 36px;
  text-align: right;
}

.upload-list {
  max-height: 200px;
  overflow-y: auto;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-top: 1px solid var(--border-subtle);
}

.upload-item-name {
  font-size: 12px;
  color: var(--text-label);
  width: 140px;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.photo-desc-input-sm {
  flex: 1;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--border-input);
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  min-width: 100px;
  background: var(--bg-input);
  color: var(--text-primary);
}

.photo-desc-input-sm:focus { border-color: var(--border-input-focus); }

.select-btn-sm, .confirm-btn-sm {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: #667eea;
  color: var(--btn-primary-text);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.select-btn-sm:hover, .confirm-btn-sm:hover { opacity: 0.85; }
.confirm-btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }

.cancel-btn-sm {
  padding: 5px 12px;
  border: 1px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg-input);
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.cancel-btn-sm:hover { color: var(--color-error); }

.remove-btn {
  width: 22px; height: 22px;
  flex-shrink: 0;
  border: none; border-radius: 50%;
  background: var(--bg-input);
  color: var(--text-dim);
  font-size: 11px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}

.remove-btn:hover { background: #fee; color: var(--color-error); }
.remove-btn:disabled { opacity: 0.3; cursor: not-allowed; }

/* 照片网格 */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.photo-item {
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-panel);
  position: relative;
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.photo-item:hover img {
  transform: scale(1.08);
}

.photo-item.selected {
  outline: 3px solid #667eea;
  outline-offset: -3px;
}

.photo-check {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 24px; height: 24px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
}

.photo-check.checked {
  background: #667eea;
  border-color: #667eea;
}

.photo-delete-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 26px; height: 26px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.4);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-item:hover .photo-delete-btn {
  opacity: 1;
}

.photo-delete-btn:hover { background: rgba(231,76,60,0.8); }

.photo-label {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  padding: 4px 8px;
  background: linear-gradient(transparent, rgba(0,0,0,0.6));
  color: #fff;
  font-size: 12px;
}

.no-photos {
  text-align: center;
  color: var(--text-placeholder);
  padding: 40px 0;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more-btn {
  padding: 8px 28px;
  border: 1px solid var(--border-input);
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-accent);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.load-more-btn:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-input-focus);
}

/* ---- 照片预览 — 明信片 ---- */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.lightbox-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 30px; height: 30px;
  border: none; border-radius: 50%;
  background: rgba(0,0,0,0.4);
  color: #fff; font-size: 16px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.lightbox-close:hover { background: rgba(0,0,0,0.7); }

.postcard {
  display: flex;
  width: 880px;
  height: 550px;
  background: #fffef9;
  border-radius: 4px;
  box-shadow:
    0 2px 0 #e8e4d8,
    0 4px 0 #dcd8cc,
    0 6px 0 #d0ccc0,
    0 8px 20px rgba(0,0,0,0.4);
  position: relative;
  overflow: hidden;
  animation: postcardIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes postcardIn {
  from { opacity: 0; transform: scale(0.85) rotate(-2deg); }
  to { opacity: 1; transform: scale(1) rotate(0); }
}

.postcard-left {
  flex: 0 0 60%;
  background: #e8e4d8;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

.postcard-left img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: #e8e4d8;
}

.postcard-lines {
  margin-top: auto;
}

.photo-time {
  font-size: 11px;
  color: #b5a890;
  text-align: right;
  margin-top: 8px;
}

.postcard-right {
  flex: 0 0 40%;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  border-left: 2px dashed #e0dcd0;
}

.postcard-stamp {
  align-self: flex-end;
  font-size: 32px;
  margin-bottom: 20px;
  opacity: 0.7;
}

.postcard-desc {
  font-family: 'KaiTi', 'STKaiti', '楷体', serif;
  font-size: 16px;
  color: #5a5040;
  line-height: 1.8;
  flex: 1;
  cursor: default;
}

.postcard-desc:hover {
  background: rgba(0,0,0,0.03);
}

.postcard-desc-edit {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.postcard-desc-edit textarea {
  width: 100%;
  flex: 1;
  padding: 8px;
  border: 1px solid #d0ccc0;
  border-radius: 4px;
  font-family: 'KaiTi', 'STKaiti', '楷体', serif;
  font-size: 15px;
  color: #5a5040;
  line-height: 1.6;
  outline: none;
  resize: none;
  background: rgba(255,255,255,0.8);
}

.edit-actions {
  display: flex;
  gap: 6px;
}

.save-btn {
  padding: 4px 14px;
  border: none;
  border-radius: 4px;
  background: #667eea;
  color: var(--btn-primary-text);
  font-size: 12px;
  cursor: pointer;
}

.cancel-edit-btn {
  padding: 4px 12px;
  border: 1px solid #d0ccc0;
  border-radius: 4px;
  background: #fff;
  color: #888;
  font-size: 12px;
  cursor: pointer;
}

.postcard-line {
  height: 1px;
  background: #d0ccc0;
  margin-top: 12px;
}

.postcard-line.short {
  width: 60%;
}

/* 翻页按钮 */
.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px; height: 44px;
  border: 2px solid rgba(255,255,255,0.6);
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
  z-index: 5;
}

.nav-btn:hover:not(.disabled) {
  background: rgba(0,0,0,0.75);
  border-color: #fff;
}

.nav-btn.prev { left: 12px; }
.nav-btn.next { right: 12px; }

.nav-btn.disabled {
  opacity: 0.15;
  cursor: default;
}

.photo-counter {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  background: rgba(0,0,0,0.4);
  padding: 2px 10px;
  border-radius: 10px;
}
</style>
