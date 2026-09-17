<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">S</div>
        <div>{{ t('brandShort') }}</div>
      </div>
      <router-link class="menu-item" to="/dashboard">{{ t('dashboard') }}</router-link>
      <router-link class="menu-item" to="/shoes">{{ t('shoes') }}</router-link>
      <router-link class="menu-item" to="/suppliers">{{ t('suppliers') }}</router-link>
      <router-link class="menu-item" to="/customers">{{ t('customers') }}</router-link>
      <router-link class="menu-item" to="/quotations">{{ t('quotations') }}</router-link>
      <router-link class="menu-item" to="/orders">{{ t('orders') }}</router-link>
      <router-link class="menu-item" to="/shipments">{{ t('shipments') }}</router-link>
      <router-link v-if="user.role === 'ADMIN'" class="menu-item" to="/users">{{ t('users') }}</router-link>
    </aside>
    <div class="content">
      <header class="topbar">
        <strong>{{ t('brand') }}</strong>
        <div style="display:flex;gap:10px;align-items:center">
          <el-select v-model="locale" size="small" style="width:110px" @change="changeLocale">
            <el-option label="中文" value="zh" />
            <el-option label="English" value="en" />
          </el-select>
          <span>{{ user.realName || user.username }}</span>
          <el-button size="small" @click="logout">{{ t('logout') }}</el-button>
        </div>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

const { t, locale } = useI18n()
const router = useRouter()
const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
})

const changeLocale = (value) => {
  localStorage.setItem('locale', value)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
