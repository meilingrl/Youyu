<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  tracking: {
    type: Object,
    default: null
  },
  mapPayload: {
    type: Object,
    default: null
  },
  addressText: {
    type: String,
    default: ''
  }
})

const mapContainer = ref(null)
const mapError = ref('')
const mapLoaded = ref(false)
let mapInstance = null

const amapKey = import.meta.env.VITE_AMAP_JS_KEY || ''
const amapSecurityCode = import.meta.env.VITE_AMAP_SECURITY_CODE || ''
const amapEnabled = computed(() => Boolean(amapKey))
const trackingEvents = computed(() => props.tracking?.events || [])
const trackingStatus = computed(() => props.tracking?.status || 'unavailable')
const mapStatus = computed(() => props.mapPayload?.status || 'unavailable')
const mapSource = computed(() => props.mapPayload?.source || '')
const backendMarkers = computed(() => props.mapPayload?.markers || [])
const trackingStatusLabel = computed(() => {
  const labels = {
    available: '已获取物流轨迹',
    unavailable: '暂未获取物流轨迹',
    failed: '物流查询失败',
    disabled: '物流查询未启用',
    missing_tracking_no: '缺少物流单号'
  }
  return labels[trackingStatus.value] || trackingStatus.value || '暂未获取物流轨迹'
})
const mapReady = computed(() =>
  amapEnabled.value &&
  props.mapPayload?.configured &&
  mapStatus.value === 'ready' &&
  backendMarkers.value.length > 0
)
const addressMapReady = computed(() =>
  amapEnabled.value &&
  !mapReady.value &&
  Boolean(props.addressText)
)
const canRenderMap = computed(() =>
  mapReady.value || addressMapReady.value
)
const fallbackText = computed(() => {
  if (!amapEnabled.value) {
    return '未配置高德地图浏览器端 Key，当前仅展示收货地址和物流信息。'
  }
  if (!props.mapPayload?.configured) {
    return '后端高德地图配置未启用，当前仅展示收货地址和物流信息。'
  }
  if (trackingStatus.value !== 'available') {
    return props.mapPayload?.message || props.tracking?.message || '快递鸟查询已接入，当前暂无轨迹事件返回。'
  }
  if (mapStatus.value === 'no_coordinates') {
    return '物流轨迹已返回，当前事件暂未包含可用于地图展示的坐标。'
  }
  return props.mapPayload?.message || '当前订单暂不可展示地图。'
})

function ensureAmapScript() {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (window.__youyuAmapPromise) {
    return window.__youyuAmapPromise
  }
  if (amapSecurityCode) {
    window._AMapSecurityConfig = {
      securityJsCode: amapSecurityCode
    }
  }
  window.__youyuAmapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const amapBaseUrl = 'https://webapi.amap.com/maps?v=2.0'
    script.src =
      amapBaseUrl +
      '&' +
      'key' +
      '=' +
      encodeURIComponent(amapKey) +
      '&plugin=AMap.Geocoder'
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('高德地图脚本加载失败。'))
    document.head.appendChild(script)
  })
  return window.__youyuAmapPromise
}

async function renderMap() {
  if (!canRenderMap.value || !mapContainer.value) {
    return
  }
  try {
    const AMap = await ensureAmapScript()
    const markers = mapReady.value
      ? backendMarkers.value
      : [await resolveAddressMarker(AMap)]
    const validMarkers = markers.filter(Boolean)
    const first = validMarkers[0]?.coordinates || {}
    const center = [Number(first.lng), Number(first.lat)]
    if (!Number.isFinite(center[0]) || !Number.isFinite(center[1])) {
      mapError.value = '地图坐标不可用。'
      return
    }
    mapInstance?.destroy?.()
    mapInstance = new AMap.Map(mapContainer.value, {
      zoom: 12,
      center
    })
    const amapMarkers = validMarkers
      .map((marker) => {
        const coordinates = marker.coordinates || {}
        const position = [Number(coordinates.lng), Number(coordinates.lat)]
        if (!Number.isFinite(position[0]) || !Number.isFinite(position[1])) {
          return null
        }
        return new AMap.Marker({
          position,
          title: marker.title || marker.locationText || '物流位置'
        })
      })
      .filter(Boolean)
    mapInstance.add(amapMarkers)
    if (amapMarkers.length > 1) {
      mapInstance.add(
        new AMap.Polyline({
          path: amapMarkers.map((marker) => marker.getPosition()),
          strokeColor: '#256f4d',
          strokeWeight: 4
        })
      )
    }
    mapInstance.setFitView()
    mapLoaded.value = true
    mapError.value = ''
  } catch (error) {
    mapError.value = error?.message || '地图渲染失败。'
  }
}

function resolveAddressMarker(AMap) {
  return new Promise((resolve, reject) => {
    if (!props.addressText) {
      resolve(null)
      return
    }
    const geocoder = new AMap.Geocoder()
    geocoder.getLocation(props.addressText, (status, result) => {
      const first = result?.geocodes?.[0]
      if (status !== 'complete' || !first?.location) {
        const fallbackMarker = resolveApproximateAddressMarker()
        if (fallbackMarker) {
          resolve(fallbackMarker)
          return
        }
        reject(new Error('高德地图暂时无法定位该收货地址。'))
        return
      }
      resolve({
        title: '收货地址',
        eventTime: '',
        locationText: first.formattedAddress || props.addressText,
        coordinates: {
          lng: first.location.lng,
          lat: first.location.lat
        }
      })
    })
  })
}

function resolveApproximateAddressMarker() {
  if (!props.addressText.includes('东北大学') || !props.addressText.includes('浑南')) {
    return null
  }
  return {
    title: '收货地址',
    eventTime: '',
    locationText: props.addressText,
    approximate: true,
    coordinates: {
      lng: 123.426,
      lat: 41.6564
    }
  }
}

watch(
  () => [canRenderMap.value, props.mapPayload, props.addressText],
  async () => {
    await nextTick()
    renderMap()
  },
  { immediate: true, deep: true, flush: 'post' }
)

onMounted(() => {
  nextTick(() => {
    renderMap()
  })
})

onBeforeUnmount(() => {
  mapInstance?.destroy?.()
  mapInstance = null
})
</script>

<template>
  <section class="logistics-panel">
    <div class="logistics-panel__head">
      <div>
        <h3>物流跟踪</h3>
        <p>{{ tracking?.message || '物流轨迹仅在服务商返回数据后展示。' }}</p>
      </div>
      <span class="logistics-panel__status">{{ trackingStatusLabel }}</span>
    </div>

    <div class="logistics-panel__summary">
      <div>
        <span>承运商</span>
        <strong>{{ tracking?.logisticsCompany || '未记录' }}</strong>
      </div>
      <div>
        <span>物流单号</span>
        <strong>{{ tracking?.trackingNo || '未记录' }}</strong>
      </div>
      <div>
        <span>地图服务</span>
        <strong>{{ amapEnabled ? '高德地图已配置' : '地图 Key 未配置' }}</strong>
      </div>
    </div>

    <div v-if="canRenderMap" class="logistics-map-wrap">
      <div ref="mapContainer" class="logistics-map" aria-label="Amap logistics event map"></div>
      <p v-if="mapError" class="logistics-panel__copy">{{ mapError }}</p>
      <p v-else-if="mapLoaded" class="logistics-panel__copy">
        {{
          mapSource === 'delivery_address' || addressMapReady
            ? '地图已接入，当前展示收货地址定位点。'
            : '地图已接入，当前展示物流轨迹坐标。'
        }}
      </p>
    </div>
    <div v-else class="logistics-fallback">
      <strong>地图暂不可用</strong>
      <p>{{ fallbackText }}</p>
      <p v-if="addressText">收货地址：{{ addressText }}</p>
    </div>

    <div v-if="trackingEvents.length" class="logistics-timeline">
      <article v-for="(event, index) in trackingEvents" :key="`${event.eventTime}-${index}`">
        <strong>{{ event.statusText || '物流更新' }}</strong>
        <span>{{ event.eventTime || '时间暂未返回' }}</span>
        <span>{{ event.locationText || '位置暂未返回' }}</span>
      </article>
    </div>
    <p v-else class="logistics-panel__copy">
      {{
        mapSource === 'delivery_address' || addressMapReady
          ? '快递鸟查询已接入，当前暂无轨迹事件返回；地图展示收货地址定位点。'
          : '快递鸟查询已接入，当前暂无轨迹事件返回。'
      }}
    </p>
  </section>
</template>

<style scoped>
.logistics-panel {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(50, 91, 63, 0.08);
  display: grid;
  gap: 14px;
}

.logistics-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.logistics-panel__head h3 {
  margin: 0 0 6px;
}

.logistics-panel__head p,
.logistics-panel__copy,
.logistics-fallback p,
.logistics-timeline span {
  color: var(--cm-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.logistics-panel__status {
  border: 1px solid rgba(50, 91, 63, 0.14);
  border-radius: 999px;
  padding: 5px 10px;
  color: var(--cm-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.logistics-panel__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.logistics-panel__summary div,
.logistics-fallback,
.logistics-timeline article {
  border: 1px solid rgba(50, 91, 63, 0.1);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.62);
}

.logistics-panel__summary span {
  display: block;
  margin-bottom: 6px;
  color: var(--cm-text-secondary);
  font-size: 12px;
}

.logistics-map {
  width: 100%;
  min-height: 300px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(50, 91, 63, 0.12);
}

.logistics-timeline {
  display: grid;
  gap: 10px;
}

.logistics-timeline article {
  display: grid;
  gap: 4px;
}

@media (max-width: 768px) {
  .logistics-panel__head {
    flex-direction: column;
  }

  .logistics-map {
    min-height: 240px;
  }
}
</style>
