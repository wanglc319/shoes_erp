<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('fx') }}</h2>

    <el-row :gutter="14">
      <el-col :span="14">
        <div class="fx-panel">
          <div class="fx-head">
            <span>{{ t('forexBoard') }}</span>
            <div style="display:flex;gap:8px;align-items:center">
              <el-select v-model="base" size="small" style="width:110px" @change="loadRates">
                <el-option v-for="c in CURRENCIES" :key="c" :label="c" :value="c" />
              </el-select>
              <el-button size="small" @click="loadRates">{{ t('refresh') }}</el-button>
            </div>
          </div>
          <el-alert v-if="stale" :title="t('loadFailed')" type="warning" :closable="false" show-icon style="margin-bottom:10px" />
          <div class="fx-grid">
            <div v-for="c in CURRENCIES" :key="c" class="fx-cell" :class="{ active: c === base }">
              <div class="fx-code">{{ c }}</div>
              <div class="fx-rate">{{ rateOf(c) }}</div>
            </div>
          </div>
          <div class="fx-updated">{{ t('updatedAt') }}: {{ updatedAt || '-' }}</div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="fx-panel">
          <div class="fx-head">{{ t('currencyConverter') }}</div>
          <el-form label-width="90px">
            <el-form-item :label="t('amountIn')">
              <el-input-number v-model="convertForm.amount" :min="0" :precision="2" :controls="false" style="width:100%" />
            </el-form-item>
            <el-form-item :label="t('base')">
              <el-select v-model="convertForm.from" style="width:100%" @change="doConvert">
                <el-option v-for="c in CURRENCIES" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('target')">
              <el-select v-model="convertForm.to" style="width:100%" @change="doConvert">
                <el-option v-for="c in CURRENCIES" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="fx-result">
            <div class="fx-result-num">{{ convertResult.converted ?? '-' }} {{ convertForm.to }}</div>
            <div class="fx-result-sub">1 {{ convertForm.from }} = {{ convertResult.rate ?? '-' }} {{ convertForm.to }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="fx-panel" style="margin-top:14px">
      <div class="fx-head">{{ t('worldClock') }}</div>
      <div class="clock-grid">
        <div v-for="c in WORLD_CLOCKS" :key="c.zone" class="clock-cell">
          <div class="clock-city">{{ c.city }}</div>
          <div class="clock-time">{{ clockOf(c.zone) }}</div>
          <div class="clock-zone">{{ c.zone }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fxApi } from '../api'
import { CURRENCIES, WORLD_CLOCKS } from '../enums'

const { t } = useI18n()
const base = ref('USD')
const rates = ref({})
const updatedAt = ref('')
const stale = ref(false)
const convertForm = reactive({ amount: 100, from: 'USD', to: 'EUR' })
const convertResult = reactive({ converted: null, rate: null })
const now = ref(new Date())
let timer = null

const rateOf = (code) => {
  if (code === base.value) return '1.0000'
  const value = rates.value?.[code]
  return value === undefined ? '-' : Number(value).toFixed(4)
}

const clockOf = (zone) => {
  try {
    return now.value.toLocaleTimeString('en-GB', { timeZone: zone, hour12: false })
  } catch {
    return '-'
  }
}

const loadRates = async () => {
  try {
    const res = await fxApi.rates(base.value)
    rates.value = res.data.rates || {}
    updatedAt.value = res.data.updatedAt || ''
    stale.value = Boolean(res.data.stale)
  } catch {
    stale.value = true
  }
}

const doConvert = async () => {
  if (convertForm.amount === null || convertForm.amount === undefined) return
  const res = await fxApi.convert({ from: convertForm.from, to: convertForm.to, amount: convertForm.amount })
  convertResult.converted = res.data.converted
  convertResult.rate = res.data.rate
}

onMounted(() => {
  loadRates()
  doConvert()
  timer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.fx-panel {
  background: #fff;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 16px;
  height: 100%;
}
.fx-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #101828;
  margin-bottom: 14px;
}
.fx-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.fx-cell {
  border: 1px solid #eaecf0;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fcfcfd;
}
.fx-cell.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.fx-code {
  font-size: 12px;
  color: #667085;
}
.fx-rate {
  font-size: 18px;
  font-weight: 600;
  color: #101828;
}
.fx-updated {
  margin-top: 12px;
  font-size: 12px;
  color: #98a2b3;
}
.fx-result {
  background: #f9fafb;
  border-radius: 10px;
  padding: 14px;
  text-align: center;
}
.fx-result-num {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}
.fx-result-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
}
.clock-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}
.clock-cell {
  border: 1px solid #eaecf0;
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  background: #fcfcfd;
}
.clock-city {
  font-size: 13px;
  color: #667085;
}
.clock-time {
  font-size: 20px;
  font-weight: 600;
  color: #101828;
  font-variant-numeric: tabular-nums;
}
.clock-zone {
  font-size: 11px;
  color: #98a2b3;
}
</style>
