<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('suppliers') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:220px" clearable @keyup.enter="load" />
      <el-select v-model="query.type" clearable :placeholder="t('category')" style="width:150px">
        <el-option v-for="ty in SUPPLIER_TYPES" :key="ty" :label="label(ty)" :value="ty" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="code" :label="t('code')" width="140" />
      <el-table-column prop="name" :label="t('name')" />
      <el-table-column prop="nameEn" :label="t('nameEn')" />
      <el-table-column prop="contactPerson" :label="t('contact')" width="120" />
      <el-table-column prop="country" :label="t('country')" width="110" />
      <el-table-column :label="t('category')" width="110">
        <template #default="{ row }">{{ label(row.type) }}</template>
      </el-table-column>
      <el-table-column :label="t('rating')" width="140">
        <template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template>
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

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="680px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="t('code')"><el-input v-model="form.code" :placeholder="t('optional')" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('category')">
            <el-select v-model="form.type" style="width:100%">
              <el-option v-for="ty in SUPPLIER_TYPES" :key="ty" :label="label(ty)" :value="ty" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('name')"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('nameEn')"><el-input v-model="form.nameEn" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('contact')"><el-input v-model="form.contactPerson" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('phone')"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('email')"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('country')">
            <el-select v-model="form.country" filterable allow-create style="width:100%">
              <el-option v-for="c in COUNTRIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('paymentTerms')">
            <el-select v-model="form.paymentTerms" filterable allow-create style="width:100%">
              <el-option v-for="p in PAYMENT_TERMS" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('rating')"><el-rate v-model="form.rating" /></el-form-item></el-col>
        </el-row>
        <el-form-item :label="t('remark')"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">{{ t('cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { supplierApi } from '../api'
import { enumLabel } from '../labels'
import { SUPPLIER_TYPES, COUNTRIES, PAYMENT_TERMS } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const query = reactive({ keyword: '', type: '', page: 1, size: 10 })
const form = reactive({})

const load = async () => {
  const res = await supplierApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const open = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { enabled: true, rating: 5, type: 'Factory', country: 'China' })
  visible.value = true
}

const save = async () => {
  if (form.id) await supplierApi.update(form.id, form)
  else await supplierApi.create(form)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await supplierApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

onMounted(load)
</script>
