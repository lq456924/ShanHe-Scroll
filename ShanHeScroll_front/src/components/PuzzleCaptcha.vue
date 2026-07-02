<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'

const emit = defineEmits<{
  (e: 'verified'): void
  (e: 'close'): void
}>()

const W = 300
const H = 160
const PIECE_W = 50
const PIECE_H = 50
const TOLERANCE = 4 // 允许误差像素

const canvasRef = ref<HTMLCanvasElement>()
const pieceCanvasRef = ref<HTMLCanvasElement>()
const sliderValue = ref(0)
const verified = ref(false)
const failMsg = ref('')
const targetX = ref(0)
const localY = ref(0)

// 随机生成背景图案
function drawBg(ctx: CanvasRenderingContext2D) {
  // 随机渐变色
  const hues = [Math.random() * 360, Math.random() * 360]
  const grad = ctx.createLinearGradient(0, 0, W, H)
  grad.addColorStop(0, `hsl(${hues[0]}, 60%, 65%)`)
  grad.addColorStop(0.5, `hsl(${hues[1]}, 55%, 70%)`)
  grad.addColorStop(1, `hsl(${(hues[0] + 40) % 360}, 50%, 60%)`)
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, W, H)

  // 随机几何装饰
  const shapes = 8 + Math.floor(Math.random() * 10)
  for (let i = 0; i < shapes; i++) {
    const x = Math.random() * W
    const y = Math.random() * H
    const r = 6 + Math.random() * 20
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${Math.random() * 360}, 50%, 80%, ${0.3 + Math.random() * 0.3})`
    ctx.fill()
  }

  // 随机线条
  const lines = 3 + Math.floor(Math.random() * 4)
  for (let i = 0; i < lines; i++) {
    ctx.beginPath()
    ctx.moveTo(Math.random() * W, Math.random() * H)
    ctx.lineTo(Math.random() * W, Math.random() * H)
    ctx.strokeStyle = `hsla(${Math.random() * 360}, 40%, 75%, 0.4)`
    ctx.lineWidth = 1 + Math.random() * 3
    ctx.stroke()
  }
}

// 挖掉拼图块
function cutPiece(ctx: CanvasRenderingContext2D, x: number, y: number) {
  ctx.clearRect(x, y, PIECE_W, PIECE_H)
  // 描边缺口
  ctx.strokeStyle = 'rgba(0,0,0,0.25)'
  ctx.lineWidth = 1.5
  ctx.setLineDash([3, 2])
  ctx.strokeRect(x + 0.75, y + 0.75, PIECE_W - 1.5, PIECE_H - 1.5)
  ctx.setLineDash([])
}

function initPuzzle() {
  sliderValue.value = 0
  verified.value = false
  failMsg.value = ''

  const canvas = canvasRef.value
  const pieceCanvas = pieceCanvasRef.value
  if (!canvas || !pieceCanvas) return

  const ctx = canvas.getContext('2d')!
  const pctx = pieceCanvas.getContext('2d')!

  // 清除
  ctx.clearRect(0, 0, W, H)
  pctx.clearRect(0, 0, PIECE_W, PIECE_H)

  // 随机位置（避开边缘）
  const py = 15 + Math.floor(Math.random() * (H - PIECE_H - 30))
  targetX.value = 15 + Math.floor(Math.random() * (W - PIECE_W - 30))
  localY.value = py

  // 先画背景到主 canvas
  drawBg(ctx)

  // 提取拼图块像素
  const imageData = ctx.getImageData(targetX.value, py, PIECE_W, PIECE_H)
  pctx.putImageData(imageData, 0, 0)

  // 在主 canvas 挖洞
  cutPiece(ctx, targetX.value, py)

  // 给拼图块加阴影边框
  pctx.strokeStyle = 'rgba(255,255,255,0.8)'
  pctx.lineWidth = 1
  pctx.strokeRect(0.5, 0.5, PIECE_W - 1, PIECE_H - 1)
}

function handleSliderChange(val: number | string) {
  if (verified.value) return
  sliderValue.value = Number(val)
}

function handleVerify() {
  if (verified.value) return
  const diff = Math.abs(sliderValue.value - targetX.value)
  if (diff <= TOLERANCE) {
    verified.value = true
    failMsg.value = ''
    setTimeout(() => emit('verified'), 300)
  } else {
    failMsg.value = '位置不对，请重试'
    setTimeout(() => {
      initPuzzle()
    }, 400)
  }
}

onMounted(() => {
  nextTick(() => initPuzzle())
})
</script>

<template>
  <div class="captcha-overlay" @click.self="emit('close')">
    <div class="captcha-box">
      <div class="captcha-header">
        <span>安全验证</span>
        <button class="captcha-close" @click="emit('close')">✕</button>
      </div>

      <div class="captcha-hint">拖动滑块，使拼图块对齐缺口</div>

      <!-- 主图（带缺口） -->
      <div class="canvas-wrapper">
        <canvas ref="canvasRef" :width="W" :height="H" class="main-canvas"></canvas>
        <!-- 拼图块跟随滑块 -->
        <canvas
          ref="pieceCanvasRef"
          :width="PIECE_W"
          :height="PIECE_H"
          class="piece-canvas"
          :style="{ left: sliderValue + 'px', top: localY + 'px' }"
        ></canvas>
      </div>

      <!-- 滑块 -->
      <div class="slider-row">
        <div class="slider-track">
          <div
            class="slider-fill"
            :style="{ width: sliderValue + 'px' }"
          ></div>
          <input
            type="range"
            :min="0"
            :max="W - PIECE_W"
            :value="sliderValue"
            :disabled="verified"
            class="slider-input"
            @input="handleSliderChange(($event.target as HTMLInputElement).value)"
          />
          <div
            class="slider-thumb"
            :class="{ verified: verified }"
            :style="{ left: sliderValue + 'px' }"
          >
            {{ verified ? '✓' : '⟷' }}
          </div>
        </div>
        <button
          class="verify-btn"
          :class="{ done: verified }"
          :disabled="verified"
          @click="handleVerify"
        >
          {{ verified ? '通过' : '验证' }}
        </button>
      </div>

      <p v-if="failMsg" class="fail-msg">{{ failMsg }}</p>
    </div>
  </div>
</template>

<style scoped>
.captcha-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-box {
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px 20px;
  width: 370px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.25);
}

.captcha-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.captcha-close {
  border: none;
  background: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.captcha-hint {
  font-size: 13px;
  color: #888;
  margin-bottom: 14px;
}

.canvas-wrapper {
  position: relative;
  width: 300px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  margin: 0 auto;
}

.main-canvas {
  display: block;
  border-radius: 8px;
}

.piece-canvas {
  position: absolute;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
  pointer-events: none;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.slider-track {
  flex: 1;
  height: 40px;
  background: #f0f0f0;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
}

.slider-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #e8e0ff, #c4b5fd);
  border-radius: 20px 0 0 20px;
  pointer-events: none;
  transition: width 0.05s;
}

.slider-input {
  position: absolute;
  inset: 0;
  width: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.slider-thumb {
  position: absolute;
  top: 2px;
  width: 36px;
  height: 36px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
  pointer-events: none;
  z-index: 1;
  transition: background 0.2s;
}

.slider-thumb.verified {
  background: #27ae60;
  color: #fff;
}

.verify-btn {
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.verify-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.verify-btn.done {
  background: #27ae60;
}

.fail-msg {
  margin: 10px 0 0;
  font-size: 13px;
  color: #e74c3c;
  text-align: center;
}
</style>
