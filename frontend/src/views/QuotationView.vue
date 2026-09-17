<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('quotations') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.status" clearable :placeholder="t('status')" style="width:150px">
        <el-option v-for="s in QUOTATION_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="quoteNo" :label="t('quoteNo')" width="150" />
      <el-table-column prop="customerName" :label="t('customer')" />
      <el-table-column prop="quoteDate" :label="t('quoteDate')" width="120" />
      <el-table-column prop="tradeTerms" :label="t('tradeTerms')" width="130" />
      <el-table-column prop="currency" :label="t('currency')" width="90" />
      <el-table-column prop="totalAmount" :label="t('totalAmount')" width="120" />
      <el-table-column :label="t('status')" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="300">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">{{ t('edit') }}</el-button>
          <el-button link @click="print(row)">{{ t('printPi') }}</el-button>
          <el-button link type="success" @click="toOrder(row)">{{ t('convertToOrder') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="980px">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="t('customer')">
            <el-select v-model="form.customerId" filterable style="width:100%">
              <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('status')">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in QUOTATION_STATUSES" :key="s" :label="label(s)" :value="s" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('quoteDate')"><el-date-picker v-model="form.quoteDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('validUntil')"><el-date-picker v-model="form.validUntil" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('tradeTerms')">
            <el-select v-model="form.tradeTerms" style="width:100%">
              <el-option v-for="i in INCOTERMS" :key="i" :label="i" :value="i" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('currency')">
            <el-select v-model="form.currency" style="width:100%">
              <el-option v-for="c in CURRENCIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item :label="t('paymentTerms')">
            <el-select v-model="form.paymentTerms" filterable allow-create clearable style="width:100%">
              <el-option v-for="p in PAYMENT_TERMS" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <el-button size="small" @click="addItem">+ {{ t('create') }}</el-button>
      <el-table :data="form.items" size="small" style="margin-top:10px">
        <el-table-column :label="t('styleNo')" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.shoeId" filterable @change="(v) => fillShoe(row, v)">
              <el-option v-for="s in shoes" :key="s.id" :label="s.styleNo + ' ' + s.nameEn" :value="s.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('quantity')" width="120">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :controls="false" /></template>
        </el-table-column>
        <el-table-column :label="t('unitPrice')" width="130">
          <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :controls="false" /></template>
        </el-table-column>
        <el-table-column :label="t('amount')" width="110">
          <template #default="{ row }">{{ ((row.quantity || 0) * (row.unitPrice || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column width="70">
          <template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index,1)">X</el-button></template>
        </el-table-column>
      </el-table>
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
import { quotationApi, customerApi, shoeApi } from '../api'
import { printQuotation } from '../print'
import { enumLabel } from '../labels'
import { QUOTATION_STATUSES, INCOTERMS, CURRENCIES, PAYMENT_TERMS, today, addDays } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const customers = ref([])
const shoes = ref([])
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const form = reactive({ items: [] })

const statusTag = (status) => (status === 'WON' ? 'success' : status === 'LOST' ? 'danger' : status === 'SENT' ? 'warning' : 'info')

const load = async () => {
  const res = await quotationApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const ensureOptions = async () => {
  if (!customers.value.length) customers.value = (await customerApi.options()).data
  if (!shoes.value.length) shoes.value = (await shoeApi.options()).data
}

const open = async (row) => {
  await ensureOptions()
  Object.keys(form).forEach((k) => delete form[k])
  if (row) {
    const detail = (await quotationApi.get(row.id)).data
    Object.assign(form, detail)
    form.items = (detail.items || []).map((i) => ({ ...i }))
  } else {
    Object.assign(form, {
      currency: 'USD', status: 'DRAFT', tradeTerms: 'FOB',
      paymentTerms: 'T/T 30% deposit, 70% before shipment',
      quoteDate: today(), validUntil: addDays(30), items: []
    })
  }
  visible.value = true
}

const addItem = () => {
  form.items = form.items || []
  form.items.push({ quantity: 0, unitPrice: 0, unit: 'PRS' })
}

const fillShoe = (row, shoeId) => {
  const shoe = shoes.value.find((s) => s.id === shoeId)
  if (!shoe) return
  row.styleNo = shoe.styleNo
  row.productName = shoe.nameEn
  row.material = shoe.upperMaterial
  row.color = shoe.color
  row.sizeRange = shoe.sizeRange
  row.unitPrice = Number(shoe.sellingPrice || 0)
  row.costPrice = Number(shoe.costPrice || 0)
  row.quantity = row.quantity || shoe.moq || 0
}

const save = async () => {
  if (form.id) await quotationApi.update(form.id, form)
  else await quotationApi.create(form)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const toOrder = async (row) => {
  await quotationApi.toOrder(row.id)
  ElMessage.success(t('success'))
  load()
}

const print = async (row) => {
  const detail = (await quotationApi.get(row.id)).data
  printQuotation(detail)
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await quotationApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

onMounted(load)
</script>
