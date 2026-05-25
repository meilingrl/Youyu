<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  metrics: {
    type: Array,
    default: () => []
  },
  activeMetricId: {
    type: String,
    default: ''
  },
  reducedMotion: {
    type: Boolean,
    default: false
  }
})

const canvasRef = ref(null)
const isCompact = ref(false)

const activeMetric = computed(() => {
  return props.metrics.find((metric) => metric.id === props.activeMetricId) || props.metrics[0] || null
})

const activeTheme = computed(() => activeMetric.value?.id || 'students')
const canAnimate = computed(() => !props.reducedMotion && !isCompact.value)

const pointer = {
  x: 0,
  y: 0,
  active: false,
  intensity: 0
}

let resizeObserver = null
let compactQuery = null
let frameId = 0
let startTime = 0
let displayedTheme = activeTheme.value
let transitionState = null

const palette = {
  paper: '#fffaf3',
  paperWarm: '#f7efe5',
  paperSoft: '#efe4d6',
  primary: [182, 95, 59],
  orange: [213, 122, 74],
  green: [105, 121, 95],
  dark: [36, 25, 20]
}

function clamp(value, min = 0, max = 1) {
  return Math.min(max, Math.max(min, value))
}

function lerp(start, end, progress) {
  return start + (end - start) * progress
}

function lerpPoint(start, end, progress) {
  return {
    x: lerp(start.x, end.x, progress),
    y: lerp(start.y, end.y, progress)
  }
}

function mixSegment(start, end, progress) {
  return {
    p0: lerpPoint(start.p0, end.p0, progress),
    c1: lerpPoint(start.c1, end.c1, progress),
    c2: lerpPoint(start.c2, end.c2, progress),
    p3: lerpPoint(start.p3, end.p3, progress)
  }
}

function easeInOutCubic(value) {
  return value < 0.5 ? 4 * value * value * value : 1 - Math.pow(-2 * value + 2, 3) / 2
}

function easeOutCubic(value) {
  return 1 - Math.pow(1 - value, 3)
}

function colorFromIndex(index, alpha = 1) {
  const base = index % 7 === 0
    ? palette.dark
    : index % 3 === 0
      ? palette.green
      : index % 2 === 0
        ? palette.orange
        : palette.primary

  return `rgba(${base[0]}, ${base[1]}, ${base[2]}, ${alpha})`
}

function pointOnCubic(segment, progress) {
  const inverse = 1 - progress
  const inverseSquare = inverse * inverse
  const progressSquare = progress * progress

  return {
    x:
      inverseSquare * inverse * segment.p0.x +
      3 * inverseSquare * progress * segment.c1.x +
      3 * inverse * progressSquare * segment.c2.x +
      progressSquare * progress * segment.p3.x,
    y:
      inverseSquare * inverse * segment.p0.y +
      3 * inverseSquare * progress * segment.c1.y +
      3 * inverse * progressSquare * segment.c2.y +
      progressSquare * progress * segment.p3.y
  }
}

function applyPointer(point, strength = 18, radius = 150) {
  if (!canAnimate.value || pointer.intensity <= 0.02) {
    return point
  }

  const dx = point.x - pointer.x
  const dy = point.y - pointer.y
  const distance = Math.max(1, Math.sqrt(dx * dx + dy * dy))

  if (distance > radius) {
    return point
  }

  const force = Math.pow(1 - distance / radius, 2) * strength * pointer.intensity
  const nx = dx / distance
  const ny = dy / distance
  const swirl = force * 0.34

  return {
    x: point.x + nx * force - ny * swirl,
    y: point.y + ny * force + nx * swirl
  }
}

function setupCanvas() {
  const canvas = canvasRef.value

  if (!canvas) {
    return null
  }

  const rect = canvas.getBoundingClientRect()
  const width = Math.max(1, Math.floor(rect.width))
  const height = Math.max(1, Math.floor(rect.height))
  const ratio = Math.min(window.devicePixelRatio || 1, 2)
  const targetWidth = Math.floor(width * ratio)
  const targetHeight = Math.floor(height * ratio)

  if (canvas.width !== targetWidth || canvas.height !== targetHeight) {
    canvas.width = targetWidth
    canvas.height = targetHeight
  }

  const ctx = canvas.getContext?.('2d')

  if (!ctx) {
    return null
  }

  ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  ctx.clearRect(0, 0, width, height)
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'

  return { ctx, width, height }
}

function drawBackground(ctx, width, height) {
  const wash = ctx.createLinearGradient(0, 0, 0, height)
  wash.addColorStop(0, 'rgba(255, 250, 243, 0.12)')
  wash.addColorStop(0.54, 'rgba(247, 239, 229, 0.18)')
  wash.addColorStop(1, 'rgba(239, 228, 214, 0.1)')
  ctx.fillStyle = wash
  ctx.fillRect(0, 0, width, height)

  const lowerGlow = ctx.createRadialGradient(width * 0.5, height * 1.02, height * 0.05, width * 0.5, height * 1.02, height * 0.78)
  lowerGlow.addColorStop(0, 'rgba(213, 122, 74, 0.16)')
  lowerGlow.addColorStop(0.42, 'rgba(182, 95, 59, 0.06)')
  lowerGlow.addColorStop(1, 'rgba(182, 95, 59, 0)')
  ctx.fillStyle = lowerGlow
  ctx.fillRect(0, 0, width, height)

  const topGlow = ctx.createRadialGradient(width * 0.55, height * 0.1, 0, width * 0.55, height * 0.1, height * 0.56)
  topGlow.addColorStop(0, 'rgba(255, 250, 243, 0.46)')
  topGlow.addColorStop(1, 'rgba(255, 250, 243, 0)')
  ctx.fillStyle = topGlow
  ctx.fillRect(0, 0, width, height)
}

function sceneSegment(theme, index, total, width, height, time) {
  const center = { x: width * 0.5, y: height * 0.55 }
  const t = total <= 1 ? 0.5 : index / (total - 1)
  const wave = Math.sin(t * Math.PI * 2 + time * 0.65)

  if (theme === 'shops') {
    const leftY = height * (0.2 + t * 0.62)
    const rightY = height * (0.8 - t * 0.62)
    const sideDrift = Math.sin(index * 0.47) * height * 0.006

    return {
      p0: { x: width * 0.11, y: leftY + sideDrift },
      c1: { x: width * 0.3, y: leftY + sideDrift * 0.5 },
      c2: { x: width * 0.7, y: rightY - sideDrift * 0.5 },
      p3: { x: width * 0.89, y: rightY - sideDrift }
    }
  }

  if (theme === 'products') {
    const column = t
    const top = height * (0.18 + 0.1 * Math.sin(column * Math.PI * 2 + time * 0.7))
    const bottom = height * (0.84 + 0.06 * Math.sin(column * Math.PI * 2 + 1.6 + time * 0.5))
    const x = width * (0.1 + column * 0.8)
    const drift = Math.sin(index * 0.2 + time * 0.8) * width * 0.012

    return {
      p0: { x: x + drift, y: bottom },
      c1: { x: x + drift * 0.35, y: lerp(bottom, top, 0.64) },
      c2: { x: x - drift * 0.35, y: lerp(bottom, top, 0.36) },
      p3: { x: x - drift, y: top }
    }
  }

  if (theme === 'regions') {
    const globe = getGlobeGeometry(width, height)
    const edgeT = (index * 0.61803398875) % 1
    const theta = Math.PI * (0.07 + edgeT * 0.88)
    const edgeRadius = globe.radius * (0.78 + (index % 6) * 0.038)
    const hub = {
      x: globe.center.x - globe.radius * 0.38,
      y: globe.center.y - globe.radius * globe.yScale * 0.5
    }
    const start = hub
    const end = pointOnGlobeDome(globe, theta, edgeRadius)
    const lift = globe.radius * (0.16 + (index % 7) * 0.018)

    return {
      p0: start,
      c1: {
        x: lerp(start.x, end.x, 0.28),
        y: lerp(start.y, end.y, 0.18) - lift * 0.32
      },
      c2: {
        x: lerp(start.x, end.x, 0.72),
        y: Math.min(start.y, end.y) - lift
      },
      p3: end
    }
  }

  const angle = t * Math.PI * 2 + Math.sin(index * 1.73) * 0.2
  const layer = index % 5
  const base = Math.min(width, height)
  const outerAngle = angle + ((index % 3) - 1) * 0.2 + Math.sin(time * 0.22 + index) * 0.04
  const outerRadius = base * (0.39 + layer * 0.064) + Math.sin(index * 2.1) * 20
  const verticalScale = 0.64
  const start = {
    x: center.x,
    y: center.y
  }
  const end = {
    x: center.x + Math.cos(outerAngle) * (outerRadius + wave * 6),
    y: height * 0.52 + Math.sin(outerAngle) * (outerRadius * verticalScale + wave * 5)
  }
  const dx = end.x - start.x
  const dy = end.y - start.y

  return {
    p0: start,
    c1: {
      x: start.x + dx / 3,
      y: start.y + dy / 3
    },
    c2: {
      x: start.x + dx * 2 / 3,
      y: start.y + dy * 2 / 3
    },
    p3: end
  }
}

function clusterSegment(index, total, width, height, time) {
  const center = { x: width * 0.5, y: height * 0.56 }
  const angle = index * 2.399 + time * 0.86
  const ring = 72 + (index % 29) * 3.2
  const wobble = Math.sin(time * 1.2 + index * 0.31) * 13
  const base = {
    x: center.x + Math.cos(angle) * (ring + wobble),
    y: center.y + Math.sin(angle) * (ring * 0.68 + wobble * 0.4)
  }

  return {
    p0: {
      x: base.x + Math.cos(angle + 0.8) * 18,
      y: base.y + Math.sin(angle + 0.8) * 12
    },
    c1: {
      x: base.x + Math.cos(angle + 1.6) * 15,
      y: base.y + Math.sin(angle + 1.6) * 10
    },
    c2: {
      x: base.x + Math.cos(angle + 2.4) * 15,
      y: base.y + Math.sin(angle + 2.4) * 10
    },
    p3: {
      x: base.x + Math.cos(angle + 3.2) * 18,
      y: base.y + Math.sin(angle + 3.2) * 12
    }
  }
}

function drawSegment(ctx, segment, index, total, time, options = {}) {
  const lineAlpha = options.lineAlpha ?? 0.2
  const dotAlpha = options.dotAlpha ?? 0.55
  const startDotAlpha = options.startDotAlpha ?? dotAlpha
  const endDotAlpha = options.endDotAlpha ?? dotAlpha
  const pulseAlpha = options.pulseAlpha ?? 0.7
  const width = options.width ?? 1
  const pointerStrength = options.pointerStrength ?? 18
  const pulseSpeedScale = options.pulseSpeedScale ?? 1

  const p0 = applyPointer(segment.p0, pointerStrength)
  const c1 = applyPointer(segment.c1, pointerStrength * 0.85)
  const c2 = applyPointer(segment.c2, pointerStrength * 0.85)
  const p3 = applyPointer(segment.p3, pointerStrength)

  ctx.beginPath()
  ctx.moveTo(p0.x, p0.y)
  ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, p3.x, p3.y)
  ctx.strokeStyle = colorFromIndex(index, lineAlpha)
  ctx.lineWidth = width
  ctx.stroke()

  const endpointRadius = index % 8 === 0 ? 2.6 : 1.8
  if (options.drawStartDot !== false) {
    drawDot(ctx, p0, endpointRadius, colorFromIndex(index, startDotAlpha))
  }

  if (options.drawEndDot !== false) {
    drawDot(ctx, p3, endpointRadius, colorFromIndex(index + 3, endDotAlpha * 0.9))
  }

  const pulseCount = options.pulseCount ?? 1
  for (let pulseIndex = 0; pulseIndex < pulseCount; pulseIndex += 1) {
    const progress = canAnimate.value
      ? (time * (0.14 + (index % 5) * 0.011) * pulseSpeedScale + index * 0.017 + pulseIndex * 0.48) % 1
      : ((index + pulseIndex * 9) % total) / total
    if (options.pulseFilter && !options.pulseFilter(progress)) {
      continue
    }
    const point = applyPointer(pointOnCubic(segment, progress), pointerStrength * 1.25)
    const size = 1.8 + (index % 4) * 0.28
    drawDot(ctx, point, size, colorFromIndex(index + 1, pulseAlpha), 'rgba(255, 250, 243, 0.62)')
  }
}

function drawDot(ctx, point, radius, fill, stroke = null) {
  ctx.beginPath()
  ctx.arc(point.x, point.y, radius, 0, Math.PI * 2)
  ctx.fillStyle = fill
  ctx.fill()

  if (stroke) {
    ctx.strokeStyle = stroke
    ctx.lineWidth = 0.8
    ctx.stroke()
  }
}

function getSegmentCount() {
  return isCompact.value ? 56 : 116
}

function drawTheme(theme, ctx, width, height, time) {
  const total = getSegmentCount()
  const isProductTheme = theme === 'products'
  const isStudentTheme = theme === 'students'
  const isShopTheme = theme === 'shops'
  const isRegionTheme = theme === 'regions'

  drawThemeGuides(theme, ctx, width, height, time)

  for (let index = 0; index < total; index += 1) {
    const segment = sceneSegment(theme, index, total, width, height, time)
    drawSegment(ctx, segment, index, total, time, {
      lineAlpha: isProductTheme ? 0.16 : isStudentTheme ? 0.14 : isShopTheme ? 0.16 : 0.2,
      dotAlpha: isProductTheme ? 0.42 : isStudentTheme ? 0.44 : isShopTheme ? 0.5 : 0.58,
      startDotAlpha: isStudentTheme ? 0.06 : undefined,
      endDotAlpha: isStudentTheme ? 0.5 : undefined,
      pulseAlpha: isStudentTheme ? 0.52 : isRegionTheme ? 0.7 : 0.66,
      width: isProductTheme ? 0.9 : isStudentTheme ? 0.96 : isRegionTheme ? 1.08 : 1.05,
      pulseCount: isStudentTheme ? 1 : 2,
      pulseSpeedScale: isShopTheme ? 0.5 : 1,
      pointerStrength: isProductTheme ? 14 : isRegionTheme ? 18 : 20,
      drawStartDot: !isStudentTheme && !isRegionTheme,
      drawEndDot: true,
      pulseFilter: isShopTheme
        ? (progress) => progress > 0.08 && progress < 0.92 && Math.abs(progress - 0.5) > 0.055
        : isRegionTheme
          ? (progress) => progress > 0.08
        : undefined
    })
  }
}

function drawThemeGuides(theme, ctx, width, height, time) {
  if (theme === 'regions') {
    drawRegionGuides(ctx, width, height, time)
    return
  }

  if (theme === 'shops') {
    return
  }

  if (theme === 'products') {
    return
  }
}

function drawShopGuides(ctx, width, height) {
  const center = { x: width * 0.5, y: height * 0.55 }
  const leftTop = { x: width * 0.12, y: height * 0.2 }
  const leftBottom = { x: width * 0.12, y: height * 0.82 }
  const rightTop = { x: width * 0.88, y: height * 0.2 }
  const rightBottom = { x: width * 0.88, y: height * 0.82 }

  ctx.strokeStyle = 'rgba(88, 62, 43, 0.1)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(leftTop.x, leftTop.y)
  ctx.lineTo(leftBottom.x, leftBottom.y)
  ctx.moveTo(rightTop.x, rightTop.y)
  ctx.lineTo(rightBottom.x, rightBottom.y)
  ctx.stroke()

  ctx.strokeStyle = 'rgba(182, 95, 59, 0.12)'
  for (let index = 0; index < 9; index += 1) {
    const t = index / 8
    const y = height * (0.22 + t * 0.58)
    ctx.beginPath()
    ctx.moveTo(width * 0.12, y)
    ctx.quadraticCurveTo(center.x, center.y, width * 0.88, height * (0.8 - t * 0.58))
    ctx.stroke()
  }
}

function getGlobeGeometry(width, height) {
  return {
    center: { x: width * 0.5, y: height * 0.92 },
    radius: Math.min(width * 0.42, height * 0.78),
    yScale: 0.9
  }
}

function pointOnGlobeDome(globe, theta, radius = globe.radius) {
  return {
    x: globe.center.x + Math.cos(theta) * radius,
    y: globe.center.y - Math.sin(theta) * radius * globe.yScale
  }
}

function pointOnGlobe(globe, longitude, latitude) {
  return {
    x: globe.center.x + Math.cos(longitude) * Math.cos(latitude) * globe.radius,
    y: globe.center.y - Math.sin(latitude) * globe.radius * globe.yScale
  }
}

function drawRegionGuides(ctx, width, height) {
  const globe = getGlobeGeometry(width, height)

  ctx.strokeStyle = 'rgba(88, 62, 43, 0.15)'
  ctx.lineWidth = 1.15
  ctx.beginPath()
  ctx.ellipse(globe.center.x, globe.center.y, globe.radius, globe.radius * globe.yScale, 0, Math.PI, Math.PI * 2)
  ctx.stroke()

  for (let index = 1; index <= 6; index += 1) {
    const ratio = index / 7
    const y = globe.center.y - globe.radius * globe.yScale * ratio
    const rx = globe.radius * Math.sqrt(1 - ratio * ratio)
    ctx.beginPath()
    ctx.ellipse(globe.center.x, y, rx, globe.radius * 0.06, 0, Math.PI, Math.PI * 2)
    ctx.strokeStyle = index % 2 === 0 ? 'rgba(105, 121, 95, 0.12)' : 'rgba(182, 95, 59, 0.1)'
    ctx.stroke()
  }

  for (let index = -5; index <= 5; index += 1) {
    const ratio = index / 5
    const start = pointOnGlobeDome(globe, Math.PI * (0.5 + ratio * 0.42), globe.radius)
    ctx.beginPath()
    ctx.moveTo(globe.center.x + ratio * globe.radius, globe.center.y)
    ctx.bezierCurveTo(
      globe.center.x + ratio * globe.radius * 0.92,
      globe.center.y - globe.radius * globe.yScale * 0.28,
      lerp(start.x, globe.center.x, 0.4),
      lerp(start.y, globe.center.y - globe.radius * globe.yScale, 0.34),
      start.x,
      start.y
    )
    ctx.strokeStyle = index % 2 === 0 ? 'rgba(88, 62, 43, 0.1)' : 'rgba(213, 122, 74, 0.085)'
    ctx.stroke()
  }
}

function drawTransition(ctx, width, height, time, timestamp) {
  if (!transitionState) {
    return false
  }

  const progress = clamp((timestamp - transitionState.startedAt) / transitionState.duration)
  const midpoint = 0.48
  const collapse = progress <= midpoint
  const phase = collapse
    ? easeInOutCubic(progress / midpoint)
    : easeOutCubic((progress - midpoint) / (1 - midpoint))
  const total = getSegmentCount()

  for (let index = 0; index < total; index += 1) {
    const cluster = clusterSegment(index, total, width, height, time)
    const source = sceneSegment(transitionState.from, index, total, width, height, time)
    const target = sceneSegment(transitionState.to, index, total, width, height, time)
    const segment = collapse
      ? mixSegment(source, cluster, phase)
      : mixSegment(cluster, target, phase)
    const visibility = collapse ? 1 - phase * 0.58 : 0.28 + phase * 0.72

    drawSegment(ctx, segment, index, total, time, {
      lineAlpha: 0.07 + visibility * 0.12,
      dotAlpha: 0.24 + visibility * 0.42,
      startDotAlpha: collapse ? 0.06 + visibility * 0.16 : 0.14 + visibility * 0.28,
      endDotAlpha: 0.2 + visibility * 0.36,
      pulseAlpha: 0.35 + visibility * 0.34,
      width: 0.8 + visibility * 0.35,
      pulseCount: collapse ? 1 : 2,
      pointerStrength: collapse ? 8 : 16,
      drawStartDot: !collapse
    })
  }

  if (progress >= 1) {
    displayedTheme = transitionState.to
    transitionState = null
  }

  return true
}

function drawScene(time = 0, timestamp = performance.now()) {
  const canvas = setupCanvas()

  if (!canvas) {
    return
  }

  const { ctx, width, height } = canvas
  pointer.intensity = pointer.active ? Math.min(1, pointer.intensity + 0.08) : pointer.intensity * 0.91

  drawBackground(ctx, width, height)

  if (!drawTransition(ctx, width, height, time, timestamp)) {
    drawTheme(displayedTheme, ctx, width, height, time)
  }
}

function stopLoop() {
  if (frameId && window.cancelAnimationFrame) {
    window.cancelAnimationFrame(frameId)
  }

  frameId = 0
}

function renderFrame(timestamp) {
  if (!startTime) {
    startTime = timestamp
  }

  drawScene((timestamp - startTime) / 1000, timestamp)

  if (canAnimate.value && window.requestAnimationFrame) {
    frameId = window.requestAnimationFrame(renderFrame)
  }
}

function startLoop() {
  stopLoop()
  startTime = 0

  if (canAnimate.value && window.requestAnimationFrame) {
    frameId = window.requestAnimationFrame(renderFrame)
    return
  }

  transitionState = null
  displayedTheme = activeTheme.value
  drawScene(5)
}

function refreshCanvas() {
  nextTick(() => {
    startLoop()
  })
}

function startThemeTransition(nextTheme) {
  if (nextTheme === displayedTheme && !transitionState) {
    refreshCanvas()
    return
  }

  if (!canAnimate.value) {
    transitionState = null
    displayedTheme = nextTheme
    refreshCanvas()
    return
  }

  transitionState = {
    from: transitionState?.to || displayedTheme,
    to: nextTheme,
    startedAt: performance.now(),
    duration: 1380
  }

  startLoop()
}

function updateCompactState(event) {
  isCompact.value = event.matches
}

function onWindowResize() {
  refreshCanvas()
}

function bindCompactQuery() {
  if (!window.matchMedia) {
    return
  }

  compactQuery = window.matchMedia('(max-width: 768px)')
  updateCompactState(compactQuery)

  if (compactQuery.addEventListener) {
    compactQuery.addEventListener('change', updateCompactState)
  } else {
    compactQuery.addListener(updateCompactState)
  }
}

function unbindCompactQuery() {
  if (!compactQuery) {
    return
  }

  if (compactQuery.removeEventListener) {
    compactQuery.removeEventListener('change', updateCompactState)
  } else {
    compactQuery.removeListener(updateCompactState)
  }

  compactQuery = null
}

function handlePointerMove(event) {
  if (!canAnimate.value || !canvasRef.value) {
    return
  }

  const rect = canvasRef.value.getBoundingClientRect()
  pointer.x = event.clientX - rect.left
  pointer.y = event.clientY - rect.top
  pointer.active = true
  pointer.intensity = Math.max(pointer.intensity, 0.7)
}

function handlePointerLeave() {
  pointer.active = false
}

watch(activeTheme, (nextTheme) => {
  startThemeTransition(nextTheme)
})

watch(canAnimate, () => {
  refreshCanvas()
})

onMounted(() => {
  displayedTheme = activeTheme.value
  bindCompactQuery()

  if (window.ResizeObserver && canvasRef.value) {
    resizeObserver = new ResizeObserver(refreshCanvas)
    resizeObserver.observe(canvasRef.value)
  } else {
    window.addEventListener('resize', onWindowResize)
  }

  refreshCanvas()
})

onBeforeUnmount(() => {
  stopLoop()
  unbindCompactQuery()

  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  } else {
    window.removeEventListener('resize', onWindowResize)
  }
})
</script>

<template>
  <div class="home-stats-network" aria-hidden="true">
    <canvas
      ref="canvasRef"
      class="home-stats-network__canvas"
      @pointermove="handlePointerMove"
      @pointerleave="handlePointerLeave"
    />
  </div>
</template>

<style scoped>
.home-stats-network {
  width: 100%;
  min-height: clamp(340px, 42vw, 540px);
}

.home-stats-network__canvas {
  display: block;
  width: 100%;
  height: clamp(340px, 42vw, 540px);
  cursor: default;
  touch-action: pan-y;
}

@media (max-width: 768px) {
  .home-stats-network,
  .home-stats-network__canvas {
    min-height: 280px;
    height: 280px;
  }

}
</style>
