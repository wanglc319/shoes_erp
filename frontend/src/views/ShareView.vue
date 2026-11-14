<template>
  <div class="share-page">
    <header class="share-header">
      <div class="brand">SINOPARK</div>
      <h1 class="title">{{ album.title || 'Sample Album' }}</h1>
      <p v-if="album.expireAt" class="expire">Valid until {{ album.expireAt }}</p>
    </header>

    <div v-if="loading" class="hint">Loading…</div>
    <div v-else-if="error" class="hint error">{{ error }}</div>
    <div v-else-if="!album.shoes || !album.shoes.length" class="hint">No samples in this album.</div>

    <div v-else class="grid">
      <div v-for="(shoe, index) in album.shoes" :key="index" class="card" @click="openViewer(index)">
        <div class="thumb">
          <img v-if="cover(shoe)" :src="cover(shoe)" :alt="shoe.styleNo" loading="lazy" />
          <div v-else class="no-image">No image</div>
        </div>
        <div class="card-body">
          <div class="style-no">{{ shoe.styleNo }}</div>
          <div class="name">{{ shoe.nameEn || shoe.name }}</div>
          <div class="meta">
            <span v-if="shoe.upperMaterial">Upper: {{ shoe.upperMaterial }}</span>
            <span v-if="shoe.soleMaterial">Sole: {{ shoe.soleMaterial }}</span>
            <span v-if="shoe.sizeRange">Size: {{ shoe.sizeRange }}</span>
            <span v-if="shoe.color">{{ shoe.color }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="viewer.visible" class="viewer" @click.self="closeViewer">
      <button class="close" @click="closeViewer">×</button>
      <button v-if="album.shoes.length > 1" class="nav prev" @click.stop="moveShoe(-1)">‹</button>
      <button v-if="album.shoes.length > 1" class="nav next" @click.stop="moveShoe(1)">›</button>
      <div class="viewer-body" @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="viewer-image">
          <img v-if="currentImage" :src="currentImage" :alt="currentShoe?.styleNo" />
          <div v-else class="no-image">No image</div>
          <button v-if="currentImages.length > 1" class="img-nav prev" @click.stop="moveImage(-1)">‹</button>
          <button v-if="currentImages.length > 1" class="img-nav next" @click.stop="moveImage(1)">›</button>
        </div>
        <div class="viewer-info">
          <div class="viewer-style">{{ currentShoe?.styleNo }}</div>
          <div class="viewer-name">{{ currentShoe?.nameEn || currentShoe?.name }}</div>
          <div class="viewer-meta">
            <span v-if="currentShoe?.upperMaterial">Upper: {{ currentShoe.upperMaterial }}</span>
            <span v-if="currentShoe?.soleMaterial">Sole: {{ currentShoe.soleMaterial }}</span>
            <span v-if="currentShoe?.sizeRange">Size: {{ currentShoe.sizeRange }}</span>
            <span v-if="currentShoe?.color">Color: {{ currentShoe.color }}</span>
          </div>
          <div class="viewer-count">{{ viewer.shoeIndex + 1 }} / {{ album.shoes.length }}</div>
        </div>
      </div>
    </div>

    <footer class="share-footer">Jianshe Trade ERP · Sample Album</footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { publicAlbumApi } from '../api'

const route = useRoute()
const album = ref({ shoes: [] })
const loading = ref(true)
const error = ref('')
const viewer = reactive({ visible: false, shoeIndex: 0, imageIndex: 0 })
let touchStartX = 0

const cover = (shoe) => (shoe.images && shoe.images.length ? shoe.images[0] : '')

const currentShoe = computed(() => album.value.shoes[viewer.shoeIndex] || null)
const currentImages = computed(() => (currentShoe.value?.images || []))
const currentImage = computed(() => currentImages.value[viewer.imageIndex] || '')

const openViewer = (index) => {
  viewer.shoeIndex = index
  viewer.imageIndex = 0
  viewer.visible = true
}

const closeViewer = () => {
  viewer.visible = false
}

const moveShoe = (delta) => {
  const count = album.value.shoes.length
  if (!count) return
  viewer.shoeIndex = (viewer.shoeIndex + delta + count) % count
  viewer.imageIndex = 0
}

const moveImage = (delta) => {
  const count = currentImages.value.length
  if (!count) return
  viewer.imageIndex = (viewer.imageIndex + delta + count) % count
}

const onTouchStart = (event) => {
  touchStartX = event.changedTouches[0].clientX
}

const onTouchEnd = (event) => {
  const delta = event.changedTouches[0].clientX - touchStartX
  if (Math.abs(delta) < 40) return
  moveShoe(delta < 0 ? 1 : -1)
}

const onKeydown = (event) => {
  if (!viewer.visible) return
  if (event.key === 'Escape') closeViewer()
  if (event.key === 'ArrowLeft') moveShoe(-1)
  if (event.key === 'ArrowRight') moveShoe(1)
}

const load = async () => {
  try {
    const body = await publicAlbumApi.get(route.params.token)
    if (body && typeof body.code === 'number' && body.code !== 0) {
      error.value = body.message || 'Album not available'
      return
    }
    album.value = body.data || { shoes: [] }
  } catch (e) {
    error.value = 'Album not available'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 20px;
}
.share-header {
  padding: 26px 20px 18px;
  background: #101828;
  color: #fff;
}
.brand {
  font-size: 12px;
  letter-spacing: 3px;
  color: #c9a227;
  font-weight: 700;
}
.title {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 600;
}
.expire {
  margin: 6px 0 0;
  font-size: 12px;
  color: #98a2b3;
}
.hint {
  padding: 40px 20px;
  text-align: center;
  color: #667085;
  font-size: 14px;
}
.hint.error { color: #f04438; }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  padding: 14px;
}
.card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.08);
  cursor: pointer;
}
.thumb {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f2f4f7;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #98a2b3;
  font-size: 12px;
  min-height: 120px;
}
.card-body { padding: 10px 12px 12px; }
.style-no { font-size: 14px; font-weight: 700; color: #101828; }
.name { font-size: 12px; color: #475467; margin-top: 2px; }
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 8px;
  font-size: 11px;
  color: #98a2b3;
}
.viewer {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: #0b0f14;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close {
  position: absolute;
  top: 14px;
  right: 18px;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 32px;
  cursor: pointer;
  z-index: 3;
}
.nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.12);
  border: none;
  color: #fff;
  font-size: 30px;
  width: 42px;
  height: 68px;
  border-radius: 8px;
  cursor: pointer;
  z-index: 3;
}
.nav.prev { left: 10px; }
.nav.next { right: 10px; }
.viewer-body { width: 100%; height: 100%; display: flex; flex-direction: column; }
.viewer-image {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  min-height: 0;
}
.viewer-image img { max-width: 100%; max-height: 100%; object-fit: contain; }
.img-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.14);
  border: none;
  color: #fff;
  font-size: 22px;
  width: 38px;
  height: 56px;
  border-radius: 8px;
  cursor: pointer;
}
.img-nav.prev { left: 12px; }
.img-nav.next { right: 12px; }
.viewer-info { padding: 12px 20px 26px; text-align: center; }
.viewer-style { font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.viewer-name { font-size: 14px; color: #d0d5dd; margin-top: 4px; }
.viewer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  justify-content: center;
  margin-top: 10px;
  color: #98a2b3;
  font-size: 12px;
}
.viewer-count { margin-top: 10px; color: #667085; font-size: 12px; }
.share-footer {
  text-align: center;
  color: #98a2b3;
  font-size: 11px;
  padding: 16px 0 8px;
}
@media (max-width: 768px) {
  .nav { display: none; }
  .grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
