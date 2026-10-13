<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('orders') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.status" clearable :placeholder="t('status')" style="width:170px">
        <el-option v-for="s in ORDER_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-select v-model="query.productionStatus" clearable :placeholder="t('productionStatus')" style="width:150px">
        <el-option v-for="s in PRODUCTION_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
      <el-button @click="exportRows">{{ t('export') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="orderNo" :label="t('orderNo')" width="150" />
      <el-table-column prop="customerName" :label="t('customer')" />
      <el-table-column prop="orderDate" :label="t('orderDate')" width="120" />
      <el-table-column prop="deliveryDate" :label="t('deliveryDate')" width="120" />
      <el-table-column prop="totalAmount" :label="t('totalAmount')" width="120" />
      <el-table-column :label="t('grossProfit')" width="150">
        <template #default="{ row }">
          <span :class="{ 'profit-neg': marginOf(row) < 0 }">{{ marginText(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('status')" width="140">
        <template #default="{ row }">
          <el-tag size="small" :type="orderTag(row.status)">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('productionStatus')" width="130">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="prodTag(row.productionStatus)">{{ label(row.productionStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="340">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">{{ t('edit') }}</el-button>
          <el-button link @click="print(row)">{{ t('printContract') }}</el-button>
          <el-button link type="success" @click="toShipment(row)">{{ t('convertToShipment') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="980px">
      <el-form :model="form" label-width="130px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="t('customer')">
            <el-select v-model="form.customerId" filterable style="width:100%">
              <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('status')">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in ORDER_STATUSES" :key="s" :label="label(s)" :value="s" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('orderDate')"><el-date-picker v-model="form.orderDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('deliveryDate')"><el-date-picker v-model="form.deliveryDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('productionStatus')">
            <el-select v-model="form.productionStatus" style="width:100%">
              <el-option v-for="s in PRODUCTION_STATUSES" :key="s" :label="label(s)" :value="s" />
            </el-select>
          </el-form-item></el-col>
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
          <el-col :span="12"><el-form-item :label="t('paymentTerms')">
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
        <el-table-column :label="t('quantity')" width="110">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :controls="false" /></template>
        </el-table-column>
        <el-table-column :label="t('unitPrice')" width="120">
          <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :controls="false" /></template>
        </el-table-column>
        <el-table-column prop="shippedQuantity" :label="'Shipped'" width="90" />
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
import { orderApi, customerApi, shoeApi } from '../api'
import { printOrder } from '../print'
import { exportCsv } from '../csv'
import { enumLabel } from '../labels'
import { ORDER_STATUSES, PRODUCTION_STATUSES, INCOTERMS, CURRENCIES, PAYMENT_TERMS, today, addDays } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const customers = ref([])
const shoes = ref([])
const query = reactive({ keyword: '', status: '', productionStatus: '', page: 1, size: 10 })
const form = reactive({ items: [] })

const marginOf = (row) => {
  const items = row.items || []
  if (!items.length) return null
  return items.reduce((sum, item) => {
    const amount = Number(item.amount || 0)
    const cost = Number(item.costPrice || 0) * Number(item.quantity || 0)
    return sum + amount - cost
  }, 0)
}

const hasCostData = (row) => (row.items || []).some((item) => Number(item.costPrice || 0) > 0)

const marginText = (row) => {
  if (!hasCostData(row)) return '-'
  const margin = marginOf(row)
  const pct = Number(row.totalAmount) > 0 ? ((margin / Number(row.totalAmount)) * 100).toFixed(1) + '%' : '-'
  return `${margin.toFixed(2)} (${pct})`
}

const exportRows = async () => {
  const res = await orderApi.page({ ...query, page: 1, size: 200 })
  const records = res.data.records || []
  exportCsv('orders', [
    t('orderNo'), t('customer'), t('orderDate'), t('deliveryDate'), t('totalAmount'),
    t('costTotal'), t('grossProfit'), t('status'), t('productionStatus')
  ], records.map((row) => [
    row.orderNo, row.customerName, row.orderDate, row.deliveryDate, row.totalAmount,
    hasCostData(row) ? Number((row.items || []).reduce((s, i) => s + Number(i.costPrice || 0) * Number(i.quantity || 0), 0)).toFixed(2) : '',
    hasCostData(row) ? (marginOf(row) || 0).toFixed(2) : '',
    label(row.status), label(row.productionStatus)
  ]))
}

const orderTag = (status) => (['SHIPPED', 'CONFIRMED'].includes(status) ? 'success' : status === 'CANCELLED' ? 'danger' : status === 'PARTIAL_SHIPPED' ? 'warning' : 'info')
const prodTag = (status) => (status === 'COMPLETED' ? 'success' : status === 'IN_PRODUCTION' ? 'warning' : 'info')

const load = async () => {
  const res = await orderApi.page(query)
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
    const detail = (await orderApi.get(row.id)).data
    Object.assign(form, detail)
    form.items = (detail.items || []).map((i) => ({ ...i }))
  } else {
    Object.assign(form, {
      currency: 'USD', status: 'PENDING', productionStatus: 'NOT_STARTED',
      tradeTerms: 'FOB', paymentTerms: 'T/T 30% deposit, 70% before shipment',
      orderDate: today(), deliveryDate: addDays(45), items: []
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
  row.color = shoe.color
  row.sizeRange = shoe.sizeRange
  row.unitPrice = Number(shoe.sellingPrice || 0)
  row.costPrice = Number(shoe.costPrice || 0)
}

const save = async () => {
  if (!form.customerId) {
    ElMessage.warning(t('customerRequired'))
    return
  }
  if (form.id) await orderApi.update(form.id, form)
  else await orderApi.create(form)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const toShipment = async (row) => {
  await orderApi.toShipment(row.id)
  ElMessage.success(t('success'))
  load()
}

const print = async (row) => {
  const detail = (await orderApi.get(row.id)).data
  printOrder(detail)
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await orderApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

onMounted(load)
</script>

<style scoped>
.profit-neg {
  color: #f56c6c;
  font-weight: 600;
}
</style>
