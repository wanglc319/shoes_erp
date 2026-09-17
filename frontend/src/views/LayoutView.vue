<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">S</div>
        <div>{{ t('brandShort') }}</div>
      </div>
      <router-link v-for="item in menus" :key="item.key" class="menu-item" :to="item.path">
        {{ t(item.titleKey) }}
      </router-link>
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
import { MENUS } from '../enums'
import { session, can, clearSession } from '../permissions'

const { t, locale } = useI18n()
const router = useRouter()
const user = computed(() => session.user)

const paths = {
  dashboard: '/dashboard', shoes: '/shoes', suppliers: '/suppliers', customers: '/customers',
  quotations: '/quotations', orders: '/orders', shipments: '/shipments', purchases: '/purchases',
  finance: '/finance', fx: '/fx', users: '/users', permissions: '/permissions'
}

const menus = computed(() =>
  MENUS.filter((item) => can(item.key)).map((item) => ({ ...item, path: paths[item.key] }))
)

const changeLocale = (value) => {
  localStorage.setItem('locale', value)
}

const logout = () => {
  clearSession()
  router.push('/login')
}
</script>
