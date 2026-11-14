<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('shoes') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.category" clearable :placeholder="t('category')" style="width:150px">
        <el-option v-for="c in SHOE_CATEGORIES" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="query.gender" clearable :placeholder="t('gender')" style="width:120px">
        <el-option v-for="g in GENDERS" :key="g" :label="label(g)" :value="g" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
      <el-button type="success" @click="openInquiry">{{ t('generateInquiry') }}</el-button>
      <el-button @click="exportStickers">{{ t('printStickers') }}</el-button>
      <el-button @click="openShare">{{ t('shareAlbum') }}</el-button>
      <el-button @click="openShowcase">{{ t('showcase') }}</el-button>
    </div>
    <el-table :data="rows" stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" />
      <el-table-column :label="t('images')" width="90">
        <template #default="{ row }">
          <el-image
            v-if="coverOf(row)"
            :src="coverOf(row)"
            :preview-src-list="imageUrlsOf(row)"
            :initial-index="0"
            fit="cover"
            preview-teleported
            style="width:44px;height:44px;border-radius:6px;cursor:pointer"
          />
          <span v-else class="muted">{{ t('noImage') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="styleNo" :label="t('styleNo')" width="120" />
      <el-table-column prop="name" :label="t('name')" />
      <el-table-column prop="nameEn" :label="t('nameEn')" />
      <el-table-column :label="t('category')" width="110">
        <template #default="{ row }">{{ row.category }}</template>
      </el-table-column>
      <el-table-column :label="t('gender')" width="90">
        <template #default="{ row }">{{ label(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="color" :label="t('color')" width="130" />
      <el-table-column prop="sizeRange" :label="t('sizeRange')" width="100" />
      <el-table-column prop="sellingPrice" :label="t('sellingPrice')" width="110" />
      <el-table-column prop="moq" :label="t('moq')" width="90" />
      <el-table-column :label="t('status')" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">{{ t('edit') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="820px">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="t('styleNo')">
              <div style="width:100%">
                <el-input v-model="form.styleNo" :placeholder="t('styleNoAuto')" @blur="checkStyleNo" />
                <div v-if="styleNoState.checked" class="style-no-hint" :class="{ conflict: styleNoState.conflict }">
                  {{ styleNoState.message }}
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item :label="t('category')">
            <el-select v-model="form.category" filterable allow-create style="width:100%">
              <el-option v-for="c in SHOE_CATEGORIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('name')"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('nameEn')"><el-input v-model="form.nameEn" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('gender')">
            <el-select v-model="form.gender" style="width:100%">
              <el-option v-for="g in GENDERS" :key="g" :label="label(g)" :value="g" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('color')">
            <el-select v-model="form.color" filterable allow-create style="width:100%">
              <el-option v-for="c in SHOE_COLORS" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('sizeRange')">
            <el-select v-model="form.sizeRange" filterable allow-create style="width:100%">
              <el-option v-for="s in SIZE_RANGES" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('supplier')">
            <el-select v-model="form.supplierId" filterable clearable style="width:100%" @change="onSupplierChange">
              <el-option v-for="s in suppliers" :key="s.id" :label="factoryLabel(s)" :value="s.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('upperMaterial')">
            <el-select v-model="form.upperMaterial" filterable allow-create style="width:100%">
              <el-option v-for="m in UPPER_MATERIALS" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('soleMaterial')">
            <el-select v-model="form.soleMaterial" filterable allow-create style="width:100%">
              <el-option v-for="m in SOLE_MATERIALS" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('costPrice')"><el-input-number v-model="form.costPrice" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('sellingPrice')"><el-input-number v-model="form.sellingPrice" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('moq')"><el-input-number v-model="form.moq" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item :label="t('priceOptions')">
            <div style="width:100%">
              <div v-for="(p, i) in priceRows" :key="i" style="display:flex;gap:8px;margin-bottom:6px">
                <el-input v-model="p.label" :placeholder="t('material')" style="width:220px" />
                <el-input-number v-model="p.price" :min="0" :precision="2" style="width:160px" />
                <el-button link type="danger" @click="priceRows.splice(i, 1)">X</el-button>
              </div>
              <el-button size="small" @click="priceRows.push({ label: '', price: 0 })">+ {{ t('priceOptions') }}</el-button>
            </div>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('status')">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in SHOE_STATUSES" :key="s" :label="label(s)" :value="s" />
            </el-select>
          </el-form-item></el-col>
        </el-row>

        <el-form-item :label="t('images')">
          <div style="width:100%">
            <div class="image-grid">
              <div v-for="(img, idx) in form.images" :key="idx" class="image-cell">
                <div class="image-thumb">
                  <img :src="img.imageUrl" alt="" />
                  <span v-if="img.primaryImage" class="image-badge">{{ t('primary') }}</span>
                </div>
                <el-input v-model="img.title" size="small" :placeholder="t('imageTitle')" style="margin-top:6px" />
                <div class="image-actions">
                  <el-button link type="primary" size="small" :disabled="img.primaryImage" @click="setPrimary(idx)">
                    {{ t('setPrimary') }}
                  </el-button>
                  <el-button link type="danger" size="small" @click="removeImage(idx)">{{ t('delete') }}</el-button>
                </div>
              </div>
            </div>
            <el-upload
              drag
              multiple
              :show-file-list="false"
              accept="image/*"
              :http-request="doUpload"
              :disabled="uploading"
              style="width:100%;margin-top:10px"
            >
              <div class="upload-drop">
                <div class="upload-plus">+</div>
                <div>{{ t('dropHere') }}</div>
              </div>
            </el-upload>
            <div class="toolbar" style="margin:10px 0 0 0">
              <el-button @click="urlPanel = !urlPanel">{{ t('addImageUrl') }}</el-button>
            </div>
            <div v-if="urlPanel" style="margin-top:8px">
              <el-input
                v-model="urlInput"
                type="textarea"
                :rows="3"
                :placeholder="t('imageUrlPlaceholder')"
              />
              <el-button type="primary" size="small" style="margin-top:6px" @click="addUrls">{{ t('save') }}</el-button>
            </div>
            <div class="muted" style="margin-top:6px">{{ t('imageHint') }}</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">{{ t('cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inquiryVisible" :title="t('generateInquiry')" width="520px">
      <el-form :model="inquiryForm" label-width="110px">
        <el-form-item :label="t('selectFactory')">
          <el-select v-model="inquiryForm.supplierId" filterable clearable style="width:100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="factoryLabel(s)" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('remark')">
          <el-input v-model="inquiryForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div class="muted">{{ t('selectShoes') }}: {{ selectedRows.length }}</div>
      <template #footer>
        <el-button @click="inquiryVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" @click="submitInquiry">{{ t('save') }}</el-button>
      </template>
    </el-dialog>

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
import { shoeApi, supplierApi, fileApi, inquiryApi, sampleShareApi } from '../api'
import { downloadExcel } from '../csv'
import { enumLabel } from '../labels'
import {
  SHOE_CATEGORIES, SHOE_COLORS, SHOE_STATUSES, GENDERS,
  UPPER_MATERIALS, SOLE_MATERIALS, SIZE_RANGES
} from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const uploading = ref(false)
const urlPanel = ref(false)
const urlInput = ref('')
const suppliers = ref([])
const query = reactive({ keyword: '', category: '', gender: '', page: 1, size: 10 })
const form = reactive({ images: [] })
const selectedRows = ref([])
const inquiryVisible = ref(false)
const inquiryForm = reactive({ supplierId: null, remark: '' })
const priceRows = ref([])
const styleNoState = ref({ checked: false, conflict: false, message: '' })

const shareVisible = ref(false)
const shareForm = reactive({ title: '', expireDays: 30 })
const albums = ref([])

const showcase = reactive({ visible: false, shoeIndex: 0, imageIndex: 0 })
const showcaseRows = ref([])
let touchStartX = 0

const factoryLabel = (s) => (s.shortCode ? `${s.name} (${s.shortCode})` : s.name)

const onSelectionChange = (selection) => {
  selectedRows.value = selection
}

const openInquiry = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning(t('selectShoes'))
    return
  }
  if (!suppliers.value.length) {
    suppliers.value = (await supplierApi.options()).data
  }
  inquiryForm.supplierId = null
  inquiryForm.remark = ''
  inquiryVisible.value = true
}

const submitInquiry = async () => {
  await inquiryApi.fromShoes({
    supplierId: inquiryForm.supplierId,
    shoeIds: selectedRows.value.map((row) => row.id),
    remark: inquiryForm.remark
  })
  inquiryVisible.value = false
  ElMessage.success(t('success'))
}

const onSupplierChange = async (supplierId) => {
  if (form.id || form.styleNo || !supplierId) return
  const res = await shoeApi.nextStyleNo(supplierId)
  form.styleNo = res.data
}

const checkStyleNo = async () => {
  const value = (form.styleNo || '').trim()
  if (!value) {
    styleNoState.value = { checked: false, conflict: false, message: '' }
    return
  }
  const res = await shoeApi.checkStyleNo(value, form.id)
  const data = res.data || {}
  styleNoState.value = data.exists
    ? {
        checked: true,
        conflict: true,
        message: t('styleNoConflict', { styleNo: data.styleNo, name: data.name || '-' })
      }
    : { checked: true, conflict: false, message: t('styleNoAvailable') }
}

const parsePriceOptions = (text) => {
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

const coverOf = (row) => row.imageUrl || (row.images && row.images[0]?.imageUrl) || ''
const imageUrlsOf = (row) => (row.images || []).map((i) => i.imageUrl)

const load = async () => {
  const res = await shoeApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const open = async (row) => {
  if (!suppliers.value.length) {
    const res = await supplierApi.options()
    suppliers.value = res.data
  }
  Object.keys(form).forEach((k) => delete form[k])
  urlPanel.value = false
  urlInput.value = ''
  styleNoState.value = { checked: false, conflict: false, message: '' }
  if (row) {
    const res = await shoeApi.get(row.id)
    Object.assign(form, res.data, { images: (res.data.images || []).map((i) => ({ ...i })) })
    priceRows.value = parsePriceOptions(res.data.priceOptions)
  } else {
    Object.assign(form, {
      status: 'ACTIVE', enabled: true, category: 'Sneaker', gender: 'Unisex',
      upperMaterial: 'Mesh', soleMaterial: 'EVA', sizeRange: '36-41',
      color: 'White', costPrice: 0, sellingPrice: 0, moq: 600, images: []
    })
    priceRows.value = []
  }
  visible.value = true
}

const doUpload = async ({ file }) => {
  uploading.value = true
  try {
    const res = await fileApi.upload(file, 'shoes')
    form.images.push({
      imageUrl: res.data,
      title: file.name,
      primaryImage: form.images.length === 0,
      sortOrder: form.images.length
    })
    ElMessage.success(t('uploadSuccess'))
  } finally {
    uploading.value = false
  }
}

const addUrls = () => {
  const urls = urlInput.value.split('\n').map((u) => u.trim()).filter(Boolean)
  if (!urls.length) return
  urls.forEach((url) => {
    form.images.push({ imageUrl: url, primaryImage: form.images.length === 0, sortOrder: form.images.length })
  })
  urlInput.value = ''
  urlPanel.value = false
}

const setPrimary = (idx) => {
  form.images.forEach((img, i) => { img.primaryImage = i === idx })
}

const removeImage = (idx) => {
  const wasPrimary = form.images[idx]?.primaryImage
  form.images.splice(idx, 1)
  form.images.forEach((img, i) => { img.sortOrder = i })
  if (wasPrimary && form.images.length) form.images[0].primaryImage = true
}

const save = async () => {
  if (styleNoState.value.conflict) {
    ElMessage.error(styleNoState.value.message)
    return
  }
  const validPrices = priceRows.value.filter((p) => p.label && p.price != null)
  const payload = {
    ...form,
    priceOptions: validPrices.length ? JSON.stringify(validPrices) : null,
    images: form.images.map((img, i) => ({ ...img, sortOrder: i }))
  }
  if (payload.id) await shoeApi.update(payload.id, payload)
  else await shoeApi.create(payload)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await shoeApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

const exportStickers = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning(t('selectShoes'))
    return
  }
  const ids = selectedRows.value.map((row) => row.id).join(',')
  await downloadExcel(`/shoes/stickers/export?ids=${ids}&perRow=3`, 'sample-stickers.xlsx')
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

const currentImages = computed(() => {
  const shoe = currentShoe.value
  if (!shoe) return []
  const urls = (shoe.images || []).map((image) => image.imageUrl).filter(Boolean)
  if (urls.length) return urls
  return shoe.imageUrl ? [shoe.imageUrl] : []
})

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
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.image-cell { border: 1px solid #edf0f4; border-radius: 10px; padding: 8px; background: #fff; }
.image-thumb { position: relative; width: 100%; height: 96px; border-radius: 8px; overflow: hidden; background: #f4f6f8; }
.image-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.image-badge { position: absolute; top: 6px; left: 6px; background: #c9a227; color: #1b1403; font-size: 11px; padding: 1px 6px; border-radius: 6px; }
.image-actions { display: flex; justify-content: space-between; margin-top: 2px; }
.muted { color: #98a2b3; font-size: 12px; }
.upload-drop { padding: 18px; color: #667085; font-size: 13px; }
.upload-plus { font-size: 26px; line-height: 1; color: #98a2b3; margin-bottom: 4px; }
.style-no-hint { font-size: 12px; margin-top: 4px; color: #12b76a; }
.style-no-hint.conflict { color: #f04438; }

.showcase {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: #0b0f14;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.showcase-close {
  position: absolute;
  top: 16px;
  right: 20px;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 34px;
  line-height: 1;
  cursor: pointer;
  z-index: 3;
}
.showcase-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.12);
  border: none;
  color: #fff;
  font-size: 32px;
  width: 46px;
  height: 72px;
  border-radius: 8px;
  cursor: pointer;
  z-index: 3;
}
.showcase-nav.prev { left: 14px; }
.showcase-nav.next { right: 14px; }
.showcase-body { width: 100%; height: 100%; display: flex; flex-direction: column; }
.showcase-image { position: relative; flex: 1; display: flex; align-items: center; justify-content: center; padding: 24px; min-height: 0; }
.showcase-image img { max-width: 100%; max-height: 100%; object-fit: contain; }
.showcase-empty { color: #667085; }
.img-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.14);
  border: none;
  color: #fff;
  font-size: 24px;
  width: 40px;
  height: 60px;
  border-radius: 8px;
  cursor: pointer;
}
.img-nav.prev { left: 14px; }
.img-nav.next { right: 14px; }
.showcase-info { padding: 14px 22px 26px; text-align: center; }
.showcase-style { font-size: 24px; font-weight: 700; letter-spacing: 1px; }
.showcase-name { font-size: 15px; color: #d0d5dd; margin-top: 4px; }
.showcase-meta { display: flex; flex-wrap: wrap; gap: 14px; justify-content: center; margin-top: 10px; color: #98a2b3; font-size: 13px; }
.showcase-dots { display: flex; gap: 8px; justify-content: center; margin-top: 12px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.3); cursor: pointer; }
.dot.active { background: #c9a227; }
.showcase-count { margin-top: 10px; color: #667085; font-size: 12px; }

@media (max-width: 768px) {
  .showcase-nav { display: none; }
  .showcase-style { font-size: 20px; }
  .showcase-image { padding: 10px; }
}
</style>
