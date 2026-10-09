<template>
  <div class="layout">
    <div v-if="menuOpen" class="sidebar-backdrop" @click="menuOpen = false" />
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand">
        <div class="brand-mark">S</div>
        <div>{{ t('brandShort') }}</div>
        <button class="menu-close" aria-label="close" @click="menuOpen = false"><span class="close-icon"></span></button>
      </div>
      <router-link v-for="item in menus" :key="item.key" class="menu-item" :to="item.path" @click="menuOpen = false">
        {{ t(item.titleKey) }}
      </router-link>
    </aside>
    <div class="content">
      <header class="topbar">
        <div class="topbar-left">
          <button class="menu-toggle" aria-label="menu" @click="menuOpen = true"><span class="burger-icon"></span></button>
          <strong class="topbar-brand">{{ t('brand') }}</strong>
        </div>
        <div class="topbar-right">
          <el-select v-model="locale" size="small" style="width:110px" @change="changeLocale">
            <el-option label="中文" value="zh" />
            <el-option label="English" value="en" />
          </el-select>
          <span class="topbar-user">{{ user.realName || user.username }}</span>
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MENUS } from '../enums'
import { session, can, clearSession } from '../permissions'

const { t, locale } = useI18n()
const router = useRouter()
const user = computed(() => session.user)
const menuOpen = ref(false)

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
