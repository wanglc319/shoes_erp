<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('inquiries') }}</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.status" clearable :placeholder="t('status')" style="width:150px">
        <el-option v-for="s in INQUIRY_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="openCreate">{{ t('create') }}</el-button>
      <el-button @click="exportAll">{{ t('exportExcel') }}</el-button>
    </div>

    <el-table :data="rows" stripe>
      <el-table-column prop="inquiryNo" :label="t('inquiryNo')" width="160" />
      <el-table-column prop="supplierName" :label="t('supplier')" />
      <el-table-column prop="inquiryDate" :label="t('inquiryDate')" width="120" />
      <el-table-column prop="deadline" :label="t('deadline')" width="120" />
      <el-table-column prop="currency" :label="t('currency')" width="90" />
      <el-table-column :label="t('status')" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="300">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ t('edit') }}</el-button>
          <el-button link @click="exportOne(row)">{{ t('exportExcel') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="createVisible" :title="t('generateInquiry')" width="900px">
      <el-form :model="createForm" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="t('selectFactory')">
            <el-select v-model="createForm.supplierId" filterable style="width:100%">
              <el-option v-for="s in factories" :key="s.id" :label="factoryLabel(s)" :value="s.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('inquiryDate')">
            <el-date-picker v-model="createForm.inquiryDate" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item></el-col>
        </el-row>
        <el-form-item :label="t('shoes')">
          <el-select v-model="createForm.shoeIds" multiple filterable style="width:100%" :placeholder="t('selectShoes')">
            <el-option v-for="s in shoes" :key="s.id" :label="s.styleNo + ' ' + (s.nameEn || s.name || '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('remark')">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" @click="submitCreate">{{ t('save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="detail.inquiryNo || t('inquiries')" width="1080px">
      <el-form :model="detail" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item :label="t('selectFactory')"><el-input :model-value="detail.supplierName" disabled /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('inquiryDate')"><el-date-picker v-model="detail.inquiryDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('deadline')"><el-date-picker v-model="detail.deadline" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <el-table :data="detail.items || []" size="small">
        <el-table-column :label="t('images')" width="90">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:60px;height:46px" fit="cover" :preview-src-list="[row.imageUrl]" preview-teleported />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="styleNo" :label="t('styleNo')" width="150" />
        <el-table-column prop="productName" :label="t('name')" min-width="160" />
        <el-table-column prop="upperMaterial" :label="t('upperMaterial')" width="120" />
        <el-table-column prop="soleMaterial" :label="t('soleMaterial')" width="120" />
        <el-table-column prop="quantity" :label="t('quantity')" width="90" />
        <el-table-column :label="t('targetPrice')" width="110">
          <template #default="{ row }"><el-input-number v-model="row.targetPrice" :min="0" :precision="2" :controls="false" style="width:100%" /></template>
        </el-table-column>
        <el-table-column :label="t('quotedPrice')" width="110">
          <template #default="{ row }"><el-input-number v-model="row.quotedPrice" :min="0" :precision="2" :controls="false" style="width:100%" /></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('cancel') }}</el-button>
        <el-button @click="exportOne(detail)">{{ t('exportExcel') }}</el-button>
        <el-button type="primary" @click="saveDetail">{{ t('save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inquiryApi, supplierApi, shoeApi } from '../api'
import { exportCsv, downloadExcel } from '../csv'
import { enumLabel } from '../labels'
import { INQUIRY_STATUSES, today } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)

const rows = ref([])
const total = ref(0)
const factories = ref([])
const shoes = ref([])
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

const createVisible = ref(false)
const createForm = reactive({ supplierId: null, inquiryDate: today(), remark: '', shoeIds: [] })

const detailVisible = ref(false)
const detail = reactive({ items: [] })

const statusTag = (status) => (['QUOTED', 'CLOSED'].includes(status) ? 'success' : status === 'SENT' ? 'warning' : status === 'CANCELLED' ? 'danger' : 'info')
const factoryLabel = (s) => (s.shortCode ? `${s.name} (${s.shortCode})` : s.name)

const load = async () => {
  const res = await inquiryApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const ensureOptions = async () => {
  if (!factories.value.length) factories.value = (await supplierApi.options()).data
  if (!shoes.value.length) shoes.value = (await shoeApi.options()).data
}

const openCreate = async () => {
  await ensureOptions()
  createForm.supplierId = null
  createForm.inquiryDate = today()
  createForm.remark = ''
  createForm.shoeIds = []
  createVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.shoeIds.length) {
    ElMessage.warning(t('selectShoes'))
    return
  }
  const res = await inquiryApi.fromShoes({
    supplierId: createForm.supplierId,
    shoeIds: createForm.shoeIds,
    remark: createForm.remark
  })
  createVisible.value = false
  ElMessage.success(t('success'))
  await load()
  openDetail(res.data)
}

const openDetail = async (row) => {
  const res = await inquiryApi.get(row.id)
  Object.assign(detail, res.data)
  detailVisible.value = true
}

const saveDetail = async () => {
  await inquiryApi.update(detail.id, {
    id: detail.id,
    inquiryNo: detail.inquiryNo,
    supplierId: detail.supplierId,
    inquiryDate: detail.inquiryDate,
    deadline: detail.deadline,
    status: detail.status,
    currency: detail.currency,
    remark: detail.remark,
    items: (detail.items || []).map((item) => ({
      shoeId: item.shoeId,
      styleNo: item.styleNo,
      productName: item.productName,
      imageUrl: item.imageUrl,
      upperMaterial: item.upperMaterial,
      soleMaterial: item.soleMaterial,
      color: item.color,
      sizeRange: item.sizeRange,
      quantity: item.quantity,
      unit: item.unit,
      targetPrice: item.targetPrice,
      quotedPrice: item.quotedPrice,
      remark: item.remark
    }))
  })
  ElMessage.success(t('success'))
  detailVisible.value = false
  load()
}

const exportOne = async (row) => {
  await downloadExcel(`/inquiries/${row.id}/export`, `RFQ-${row.inquiryNo}.xlsx`)
}

const exportAll = async () => {
  const res = await inquiryApi.page({ ...query, page: 1, size: 200 })
  const records = res.data.records || []
  exportCsv('inquiries', [
    t('inquiryNo'), t('supplier'), t('inquiryDate'), t('deadline'), t('currency'), t('status')
  ], records.map((row) => [
    row.inquiryNo, row.supplierName, row.inquiryDate, row.deadline, row.currency, label(row.status)
  ]))
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await inquiryApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

onMounted(load)
</script>
