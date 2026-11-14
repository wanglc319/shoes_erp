<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('shoeImages') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.category" clearable :placeholder="t('category')" style="width:150px">
        <el-option v-for="c in SHOE_CATEGORIES" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="query.gender" clearable :placeholder="t('gender')" style="width:120px">
        <el-option v-for="g in GENDERS" :key="g" :label="label(g)" :value="g" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="openShare">{{ t('shareAlbum') }}</el-button>
      <el-button type="success" @click="openShowcase">{{ t('showcase') }}</el-button>
      <span class="muted">{{ t('selectShoes') }}: {{ selectedRows.length }}</span>
    </div>

    <div v-if="rows.length" class="shoe-gallery">
      <div
        v-for="row in rows"
        :key="row.id"
        class="gallery-card"
        :class="{ selected: isSelected(row) }"
        @click="toggleSelection(row)"
      >
        <el-checkbox
          class="gallery-check"
          :model-value="isSelected(row)"
          :aria-label="row.styleNo"
          @click.stop
          @change="toggleSelection(row)"
        />
        <div class="gallery-image">
          <img v-if="coverOf(row)" :src="coverOf(row)" :alt="row.styleNo" />
          <div v-else class="gallery-empty">{{ t('noImage') }}</div>
          <span v-if="imageUrlsOf(row).length > 1" class="image-count">+{{ imageUrlsOf(row).length - 1 }}</span>
        </div>
        <div class="gallery-info">
          <strong>{{ row.styleNo }}</strong>
          <span>{{ row.nameEn || row.name }}</span>
          <small>{{ [row.category, row.color].filter(Boolean).join(' · ') }}</small>
        </div>
      </div>
    </div>
    <el-empty v-else />

    <el-pagination
      style="margin-top:18px"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      v-model:current-page="query.page"
      @current-change="load"
    />

    <el-dialog v-model="shareVisible" :title="t('shareAlbum')" width="640px">
      <el-form :model="shareForm" label-width="140px">
        <el-form-item :label="t('albumTitle')">
          <el-input v-model="shareForm.title" />
        </el-form-item>
        <el-form-item :label="t('albumExpire')">
          <el-input-number v-model="shareForm.expireDays" :min="0" :max="3650" style="width:180px" />
        </el-form-item>
      </el-form>
      <div class="muted" style="margin-bottom:8px">{{ t('selectShoes') }}: {{ selectedRows.length }}</div>
      <el-button type="primary" @click="createAlbum">{{ t('shareAlbum') }}</el-button>

      <h4 style="margin:18px 0 8px">{{ t('albumList') }}</h4>
      <div v-if="!albums.length" class="muted">{{ t('albumEmpty') }}</div>
      <el-table v-else :data="albums" size="small">
        <el-table-column prop="title" :label="t('albumTitle')" min-width="140" />
        <el-table-column prop="expireAt" :label="t('albumExpire')" width="120" />
        <el-table-column :label="t('actions')" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyLink(row.token)">{{ t('copyLink') }}</el-button>
            <el-button link @click="openAlbum(row.token)">{{ t('showcase') }}</el-button>
            <el-button link type="danger" @click="removeAlbum(row)">{{ t('delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <div v-if="showcase.visible" class="showcase" @click.self="closeShowcase">
      <button class="showcase-close" @click="closeShowcase">×</button>
      <button v-if="showcaseRows.length > 1" class="showcase-nav prev" @click.stop="moveShoe(-1)">‹</button>
      <button v-if="showcaseRows.length > 1" class="showcase-nav next" @click.stop="moveShoe(1)">›</button>
      <div class="showcase-body" @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="showcase-image">
          <img v-if="currentImage" :src="currentImage" alt="" />
          <div v-else class="showcase-empty">{{ t('noImage') }}</div>
          <button v-if="currentImages.length > 1" class="img-nav prev" @click.stop="moveImage(-1)">‹</button>
          <button v-if="currentImages.length > 1" class="img-nav next" @click.stop="moveImage(1)">›</button>
        </div>
        <div class="showcase-info">
          <div class="showcase-style">{{ currentShoe?.styleNo }}</div>
          <div class="showcase-name">{{ currentShoe?.nameEn || currentShoe?.name }}</div>
          <div class="showcase-meta">
            <span v-if="currentShoe?.upperMaterial">Upper: {{ currentShoe.upperMaterial }}</span>
            <span v-if="currentShoe?.soleMaterial">Sole: {{ currentShoe.soleMaterial }}</span>
            <span v-if="currentShoe?.sizeRange">Size: {{ currentShoe.sizeRange }}</span>
            <span v-if="currentShoe?.color">Color: {{ currentShoe.color }}</span>
          </div>
          <div class="showcase-dots" v-if="currentImages.length > 1">
            <span
              v-for="(img, i) in currentImages"
              :key="i"
              class="dot"
              :class="{ active: i === showcase.imageIndex }"
              @click="showcase.imageIndex = i"
            />
          </div>
          <div class="showcase-count">{{ showcase.shoeIndex + 1 }} / {{ showcaseRows.length }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shoeApi, sampleShareApi } from '../api'
import { enumLabel } from '../labels'
import { SHOE_CATEGORIES, GENDERS } from '../enums'

const { t, te } = useI18n()
const label = (value) => enumLabel(t, te, value)
const rows = ref([])
const total = ref(0)
const selectedRows = ref([])
const query = reactive({ keyword: '', category: '', gender: '', page: 1, size: 12 })

const shareVisible = ref(false)
const shareForm = reactive({ title: '', expireDays: 30 })
const albums = ref([])

const showcase = reactive({ visible: false, shoeIndex: 0, imageIndex: 0 })
const showcaseRows = ref([])
let touchStartX = 0

const coverOf = (row) => row.imageUrl || row.images?.[0]?.imageUrl || ''

const imageUrlsOf = (row) => {
  if (!row) return []
  const urls = (row.images || []).map((image) => image.imageUrl).filter(Boolean)
  if (urls.length) return urls
  return row.imageUrl ? [row.imageUrl] : []
}

const isSelected = (row) => selectedRows.value.some((item) => item.id === row.id)

const toggleSelection = (row) => {
  if (isSelected(row)) {
    selectedRows.value = selectedRows.value.filter((item) => item.id !== row.id)
  } else {
    selectedRows.value = [...selectedRows.value, row]
  }
}

const load = async () => {
  const res = await shoeApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
  selectedRows.value = []
}

const albumLink = (token) => `${window.location.origin}${import.meta.env.BASE_URL}share/${token}`

const loadAlbums = async () => {
  albums.value = (await sampleShareApi.list()).data || []
}

const openShare = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning(t('selectShoes'))
    return
  }
  shareForm.title = `Samples ${new Date().toISOString().slice(0, 10)}`
  shareForm.expireDays = 30
  await loadAlbums()
  shareVisible.value = true
}

const createAlbum = async () => {
  const res = await sampleShareApi.create({
    title: shareForm.title,
    expireDays: shareForm.expireDays,
    shoeIds: selectedRows.value.map((row) => row.id)
  })
  await loadAlbums()
  await copyLink(res.data.token)
}

const copyLink = async (token) => {
  const link = albumLink(token)
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success(`${t('linkCopied')}: ${link}`)
  } catch (e) {
    ElMessageBox.alert(link, t('copyLink'))
  }
}

const openAlbum = (token) => {
  window.open(albumLink(token), '_blank')
}

const removeAlbum = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await sampleShareApi.remove(row.id)
  await loadAlbums()
}

const currentShoe = computed(() => showcaseRows.value[showcase.shoeIndex] || null)
const currentImages = computed(() => imageUrlsOf(currentShoe.value))
const currentImage = computed(() => currentImages.value[showcase.imageIndex] || '')

const openShowcase = () => {
  const source = selectedRows.value.length ? selectedRows.value : rows.value
  if (!source.length) return
  showcaseRows.value = source
  showcase.shoeIndex = 0
  showcase.imageIndex = 0
  showcase.visible = true
}

const closeShowcase = () => {
  showcase.visible = false
}

const moveShoe = (delta) => {
  const count = showcaseRows.value.length
  if (!count) return
  showcase.shoeIndex = (showcase.shoeIndex + delta + count) % count
  showcase.imageIndex = 0
}

const moveImage = (delta) => {
  const count = currentImages.value.length
  if (!count) return
  showcase.imageIndex = (showcase.imageIndex + delta + count) % count
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
  if (!showcase.visible) return
  if (event.key === 'Escape') closeShowcase()
  if (event.key === 'ArrowLeft') moveShoe(-1)
  if (event.key === 'ArrowRight') moveShoe(1)
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
.muted { color: #98a2b3; font-size: 12px; }
.shoe-gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 16px; margin-top: 18px; }
.gallery-card { position: relative; overflow: hidden; border: 2px solid transparent; border-radius: 12px; background: #fff; box-shadow: 0 2px 10px rgba(16, 24, 40, 0.08); cursor: pointer; transition: border-color 0.2s, transform 0.2s; }
.gallery-card:hover { transform: translateY(-2px); }
.gallery-card.selected { border-color: #c9a227; }
.gallery-check { position: absolute; top: 10px; left: 10px; z-index: 2; height: 28px; padding: 0 7px; border-radius: 6px; background: rgba(255, 255, 255, 0.9); }
.gallery-image { position: relative; height: 230px; background: #f4f6f8; }
.gallery-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.gallery-empty { display: flex; align-items: center; justify-content: center; height: 100%; color: #98a2b3; }
.image-count { position: absolute; right: 10px; bottom: 10px; padding: 3px 8px; border-radius: 10px; background: rgba(15, 23, 42, 0.72); color: #fff; font-size: 12px; }
.gallery-info { display: flex; flex-direction: column; gap: 5px; padding: 12px 14px 15px; }
.gallery-info strong { color: #1d2939; font-size: 16px; }
.gallery-info span { overflow: hidden; color: #475467; text-overflow: ellipsis; white-space: nowrap; }
.gallery-info small { color: #98a2b3; }

.showcase { position: fixed; inset: 0; z-index: 3000; display: flex; align-items: center; justify-content: center; background: #0b0f14; color: #fff; }
.showcase-close { position: absolute; top: 16px; right: 20px; z-index: 3; border: none; background: transparent; color: #fff; font-size: 34px; line-height: 1; cursor: pointer; }
.showcase-nav { position: absolute; top: 50%; z-index: 3; width: 46px; height: 72px; transform: translateY(-50%); border: none; border-radius: 8px; background: rgba(255, 255, 255, 0.12); color: #fff; font-size: 32px; cursor: pointer; }
.showcase-nav.prev { left: 14px; }
.showcase-nav.next { right: 14px; }
.showcase-body { display: flex; flex-direction: column; width: 100%; height: 100%; }
.showcase-image { position: relative; display: flex; flex: 1; align-items: center; justify-content: center; min-height: 0; padding: 24px; }
.showcase-image img { max-width: 100%; max-height: 100%; object-fit: contain; }
.showcase-empty { color: #667085; }
.img-nav { position: absolute; top: 50%; width: 40px; height: 60px; transform: translateY(-50%); border: none; border-radius: 8px; background: rgba(255, 255, 255, 0.14); color: #fff; font-size: 24px; cursor: pointer; }
.img-nav.prev { left: 14px; }
.img-nav.next { right: 14px; }
.showcase-info { padding: 14px 22px 26px; text-align: center; }
.showcase-style { font-size: 24px; font-weight: 700; letter-spacing: 1px; }
.showcase-name { margin-top: 4px; color: #d0d5dd; font-size: 15px; }
.showcase-meta { display: flex; flex-wrap: wrap; justify-content: center; gap: 14px; margin-top: 10px; color: #98a2b3; font-size: 13px; }
.showcase-dots { display: flex; justify-content: center; gap: 8px; margin-top: 12px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.3); cursor: pointer; }
.dot.active { background: #c9a227; }
.showcase-count { margin-top: 10px; color: #667085; font-size: 12px; }

@media (max-width: 768px) {
  .shoe-gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .gallery-image { height: 180px; }
  .gallery-info { padding: 10px; }
  .gallery-info span { font-size: 12px; }
  .showcase-nav { display: none; }
  .showcase-style { font-size: 20px; }
  .showcase-image { padding: 10px; }
}
</style>
