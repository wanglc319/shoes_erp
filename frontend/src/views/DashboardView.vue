<template>
  <div>
    <h2 class="page-title">{{ t('dashboard') }}</h2>
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
import { dashboardApi } from '../api'

const { t } = useI18n()
const stats = ref({})

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
