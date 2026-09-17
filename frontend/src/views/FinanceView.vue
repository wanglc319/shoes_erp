<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('finance') }}</h2>

    <el-row :gutter="14" style="margin-bottom:14px">
      <el-col :span="6"><div class="stat-card"><div class="stat-label">{{ t('totalAmount') }}</div><div class="stat-value">{{ summary.totalAmount ?? '-' }}</div></div></el-col>
      <el-col :span="6"><div class="stat-card"><div class="stat-label">{{ t('receivedAmount') }}</div><div class="stat-value" style="color:#12b76a">{{ summary.receivedAmount ?? '-' }}</div></div></el-col>
      <el-col :span="6"><div class="stat-card"><div class="stat-label">{{ t('outstanding') }}</div><div class="stat-value" style="color:#f79009">{{ summary.outstandingAmount ?? '-' }}</div></div></el-col>
      <el-col :span="6"><div class="stat-card"><div class="stat-label">Overdue</div><div class="stat-value" style="color:#f04438">{{ summary.overdueCount ?? 0 }}</div></div></el-col>
    </el-row>

    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:220px" clearable @keyup.enter="load" />
      <el-select v-model="query.status" clearable :placeholder="t('status')" style="width:150px">
        <el-option v-for="s in RECEIVABLE_STATUSES" :key="s" :label="label(s)" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="orderNo" :label="t('orderNo')" width="150" />
      <el-table-column prop="customerName" :label="t('customer')" />
      <el-table-column prop="orderDate" :label="t('orderDate')" width="120" />
      <el-table-column prop="currency" :label="t('currency')" width="90" />
      <el-table-column prop="totalAmount" :label="t('totalAmount')" width="120" />
      <el-table-column prop="receivedAmount" :label="t('receivedAmount')" width="110" />
      <el-table-column prop="outstandingAmount" :label="t('outstanding')" width="110" />
      <el-table-column :label="t('status')" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ label(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="register(row)">{{ t('registerPayment') }}</el-button>
          <el-button link @click="openHistory(row)">{{ t('paymentHistory') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="payVisible" :title="t('registerPayment')" width="520px">
      <el-form :model="payForm" label-width="120px">
        <el-form-item :label="t('orderNo')"><el-input v-model="payForm.orderNo" disabled /></el-form-item>
        <el-form-item :label="t('customer')"><el-input v-model="payForm.customerName" disabled /></el-form-item>
        <el-form-item :label="t('paymentDate')"><el-date-picker v-model="payForm.paymentDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item :label="t('method')">
          <el-select v-model="payForm.method" style="width:100%">
            <el-option v-for="m in PAYMENT_METHODS" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('amountIn')"><el-input-number v-model="payForm.amount" :min="0" :precision="2" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="Reference"><el-input v-model="payForm.reference" /></el-form-item>
        <el-form-item :label="t('remark')"><el-input v-model="payForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible=false">{{ t('cancel') }}</el-button>
        <el-button type="primary" @click="savePayment">{{ t('save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" :title="t('paymentHistory')" width="720px">
      <el-table :data="history" stripe>
        <el-table-column prop="paymentNo" :label="t('paymentNo')" width="150" />
        <el-table-column prop="paymentDate" :label="t('paymentDate')" width="120" />
        <el-table-column prop="method" :label="t('method')" width="110" />
        <el-table-column prop="amount" :label="t('amountIn')" width="120" />
        <el-table-column prop="reference" label="Reference" />
      </el-table>
      <div v-if="!history.length" style="text-align:center;color:#98a2b3;padding:14px">{{ t('noPayments') }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { receivableApi, paymentApi } from '../api'
import { enumLabel } from '../labels'
import { RECEIVABLE_STATUSES, PAYMENT_METHODS, today } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const summary = ref({})
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const payVisible = ref(false)
const historyVisible = ref(false)
const history = ref([])
const payForm = reactive({})

const statusTag = (status) => (status === 'PAID' ? 'success' : status === 'OVERDUE' ? 'danger' : status === 'PARTIAL' ? 'warning' : 'info')

const load = async () => {
  const res = await receivableApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
  summary.value = (await receivableApi.summary()).data
}

const register = (row) => {
  Object.keys(payForm).forEach((k) => delete payForm[k])
  Object.assign(payForm, {
    orderId: row.orderId,
    orderNo: row.orderNo,
    customerName: row.customerName,
    paymentDate: today(),
    method: 'T/T',
    amount: row.outstandingAmount
  })
  payVisible.value = true
}

const savePayment = async () => {
  await paymentApi.create(payForm)
  ElMessage.success(t('success'))
  payVisible.value = false
  load()
}

const openHistory = async (row) => {
  const res = await paymentApi.page({ orderId: row.orderId, page: 1, size: 100 })
  history.value = res.data.records
  historyVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.stat-card {
  background: #fff;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 14px 16px;
}
.stat-label {
  font-size: 13px;
  color: #667085;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #101828;
  margin-top: 6px;
}
</style>
