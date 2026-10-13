<template>
  <div>
    <h2 class="page-title">{{ t('dashboard') }}</h2>
    <div v-if="alerts.length" class="page-card alert-panel">
      <div class="alert-head">
        <h3>{{ t('alerts.title') }}</h3>
        <el-tag :type="highCount ? 'danger' : 'warning'" size="small">
          {{ t('alerts.high') }} {{ highCount }} / {{ t('alerts.medium') }} {{ alerts.length - highCount }}
        </el-tag>
      </div>
      <div class="alert-list">
        <div v-for="(a, i) in alerts" :key="i" class="alert-item" @click="goAlert(a)">
          <el-tag :type="a.level === 'HIGH' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ t('alerts.' + a.type) }}
          </el-tag>
          <span class="alert-no">{{ a.refNo }}</span>
          <span class="alert-name">{{ a.name }}</span>
          <span class="alert-date">{{ a.date }}</span>
          <span v-if="a.amount !== null && a.amount !== undefined" class="alert-amount">{{ money(a.amount) }}</span>
          <span class="alert-arrow">→</span>
        </div>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card" v-for="item in cards" :key="item.label">
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
        <div class="hint">{{ item.hint }}</div>
      </div>
    </div>
    <div class="dash-grid dash-grid-a">
      <div class="page-card">
        <h3>{{ t('stats.monthly') }}</h3>
        <el-table :data="stats.monthlyTrend || []" size="small">
          <el-table-column prop="month" :label="t('stats.monthly')" />
          <el-table-column prop="amount" :label="t('totalAmount')" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>{{ t('stats.orderStatus') }}</h3>
        <el-table :data="stats.orderStatusDistribution || []" size="small">
          <el-table-column prop="status" :label="t('status')" />
          <el-table-column prop="count" :label="t('quantity')" />
        </el-table>
      </div>
    </div>
    <div class="dash-grid dash-grid-b">
      <div class="page-card">
        <h3>{{ t('stats.topProducts') }}</h3>
        <el-table :data="stats.topProducts || []" size="small">
          <el-table-column prop="styleNo" :label="t('styleNo')" />
          <el-table-column prop="productName" :label="t('name')" />
          <el-table-column prop="quantity" :label="t('quantity')" />
          <el-table-column prop="amount" :label="t('amount')" />
        </el-table>
      </div>
      <div class="page-card">
        <h3>{{ t('stats.countries') }}</h3>
        <el-table :data="stats.countryDistribution || []" size="small">
          <el-table-column prop="country" :label="t('country')" />
          <el-table-column prop="amount" :label="t('amount')" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { dashboardApi } from '../api'

const { t } = useI18n()
const router = useRouter()
const stats = ref({})

const alerts = computed(() => stats.value.alerts || [])
const highCount = computed(() => alerts.value.filter((a) => a.level === 'HIGH').length)

const alertRoutes = {
  QUOTATION_EXPIRING: '/quotations',
  ORDER_OVERDUE: '/orders',
  ORDER_DUE_SOON: '/orders',
  RECEIVABLE_OVERDUE: '/finance'
}

const goAlert = (alert) => {
  const route = alertRoutes[alert.type]
  if (route) router.push(route)
}

const cards = computed(() => [
  { label: t('stats.shoes'), value: stats.value.shoeCount || 0, hint: t('shoes') },
  { label: t('stats.customers'), value: stats.value.customerCount || 0, hint: t('customers') },
  { label: t('stats.orderAmount'), value: money(stats.value.totalOrderAmount), hint: 'USD' },
  { label: t('stats.profit'), value: money(stats.value.totalProfit), hint: 'USD' },
  { label: t('stats.quotations'), value: stats.value.quotationCount || 0, hint: t('stats.pendingQuotes') + ': ' + (stats.value.pendingQuotationCount || 0) },
  { label: t('stats.orders'), value: stats.value.orderCount || 0, hint: t('stats.pendingOrders') + ': ' + (stats.value.pendingOrderCount || 0) },
  { label: t('stats.shipments'), value: stats.value.shipmentCount || 0, hint: t('stats.shippedAmount') },
  { label: t('stats.shippedAmount'), value: money(stats.value.totalShippedAmount), hint: 'USD' }
])

const money = (v) => Number(v || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

onMounted(async () => {
  const res = await dashboardApi.stats()
  stats.value = res.data || {}
})
</script>

<style scoped>
.alert-panel {
  margin-bottom: 14px;
  border-left: 4px solid #f56c6c;
}
.alert-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.alert-head h3 {
  margin: 0;
}
.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 7px 4px;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
  font-size: 13px;
}
.alert-item:last-child {
  border-bottom: none;
}
.alert-item:hover {
  background: #fdf6ec;
}
.alert-no {
  font-weight: 600;
  color: #303133;
  min-width: 110px;
}
.alert-name {
  color: #606266;
  flex: 1;
}
.alert-date {
  color: #909399;
  font-size: 12px;
}
.alert-amount {
  color: #f56c6c;
  font-weight: 600;
}
.alert-arrow {
  color: #c0c4cc;
}
</style>
