<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('shipments') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="query.status" clearable :placeholder="t('status')" style="width:150px">
        <el-option v-for="s in SHIPMENT_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="shipmentNo" :label="t('shipmentNo')" width="150" />
      <el-table-column prop="orderNo" :label="t('orderNo')" width="140" />
      <el-table-column prop="customerName" :label="t('customer')" />
      <el-table-column prop="shipDate" :label="t('shipDate')" width="120" />
      <el-table-column prop="containerNo" :label="t('containerNo')" width="140" />
      <el-table-column prop="vessel" :label="t('vessel')" width="130" />
      <el-table-column prop="totalQuantity" :label="t('quantity')" width="90" />
      <el-table-column prop="totalAmount" :label="t('totalAmount')" width="120" />
      <el-table-column :label="t('status')" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="300">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">{{ t('edit') }}</el-button>
          <el-button link @click="printList(row)">{{ t('printPackingList') }}</el-button>
          <el-button link @click="printInv(row)">{{ t('printInvoice') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="980px">
      <el-form :model="form" label-width="140px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="t('orderNo')">
            <el-select v-model="form.orderId" filterable style="width:100%" @change="fillOrder">
              <el-option v-for="o in orders" :key="o.id" :label="o.orderNo + ' / ' + o.customerName" :value="o.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('status')">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in SHIPMENT_STATUSES" :key="s" :label="label(s)" :value="s" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('shipDate')"><el-date-picker v-model="form.shipDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('containerNo')"><el-input v-model="form.containerNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('blNo')"><el-input v-model="form.blNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('vessel')"><el-input v-model="form.vessel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('pol')">
            <el-select v-model="form.portOfLoading" filterable allow-create clearable style="width:100%">
              <el-option v-for="p in PORT_OF_LOADING" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="t('pod')">
            <el-select v-model="form.portOfDestination" filterable allow-create clearable style="width:100%">
              <el-option v-for="p in PORT_OF_DESTINATION" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <el-table :data="form.items" size="small">
        <el-table-column prop="styleNo" :label="t('styleNo')" />
        <el-table-column prop="productName" :label="t('name')" />
        <el-table-column :label="t('quantity')" width="110">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :controls="false" /></template>
        </el-table-column>
        <el-table-column :label="t('cartons')" width="110">
          <template #default="{ row }"><el-input-number v-model="row.cartons" :min="0" :controls="false" /></template>
        </el-table-column>
        <el-table-column :label="t('unitPrice')" width="120">
          <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" :controls="false" /></template>
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
import { shipmentApi, orderApi } from '../api'
import { printPackingList, printInvoice } from '../print'
import { enumLabel } from '../labels'
import { SHIPMENT_STATUSES, PORT_OF_LOADING, PORT_OF_DESTINATION, today } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const orders = ref([])
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const form = reactive({ items: [] })

const statusTag = (status) => (status === 'DELIVERED' ? 'success' : status === 'CLEARED' ? 'warning' : status === 'DEPARTED' ? 'warning' : 'info')

const load = async () => {
  const res = await shipmentApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const open = async (row) => {
  if (!orders.value.length) {
    const res = await orderApi.page({ page: 1, size: 100 })
    orders.value = res.data.records
  }
  Object.keys(form).forEach((k) => delete form[k])
  if (row) {
    const detail = (await shipmentApi.get(row.id)).data
    Object.assign(form, detail)
    form.items = (detail.items || []).map((i) => ({ ...i }))
  } else {
    Object.assign(form, {
      status: 'PREPARING', shipDate: today(),
      portOfLoading: 'Ningbo', portOfDestination: 'Hamburg', items: []
    })
  }
  visible.value = true
}

const fillOrder = async (orderId) => {
  const detail = (await orderApi.get(orderId)).data
  form.items = (detail.items || []).map((i) => ({
    styleNo: i.styleNo,
    productName: i.productName,
    color: i.color,
    sizeRange: i.sizeRange,
    quantity: Math.max((i.quantity || 0) - (i.shippedQuantity || 0), 0),
    unit: i.unit,
    cartons: Math.max(Math.ceil(((i.quantity || 0) - (i.shippedQuantity || 0)) / 12), 0),
    unitPrice: i.unitPrice
  }))
}

const save = async () => {
  if (form.id) await shipmentApi.update(form.id, form)
  else await shipmentApi.create(form)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await shipmentApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

const printList = async (row) => {
  const detail = (await shipmentApi.get(row.id)).data
  printPackingList(detail)
}

const printInv = async (row) => {
  const detail = (await shipmentApi.get(row.id)).data
  printInvoice(detail)
}

onMounted(load)
</script>
