<template>
  <div class="layout">
    <div v-if="menuOpen" class="sidebar-backdrop" @click="menuOpen = false" />
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand">
        <img class="brand-logo" src="../assets/logo.jpg" alt="logo" />
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
          <el-dropdown trigger="click" @command="onUserCommand">
            <span class="topbar-user">{{ user.realName || user.username }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">{{ t('changePassword') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ t('logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>

    <el-dialog v-model="pwdDialog" :title="t('changePassword')" width="380px">
      <el-form label-width="110px">
        <el-form-item :label="t('oldPassword')">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('newPassword')">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('confirmPassword')">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MENUS } from '../enums'
import { session, can, clearSession } from '../permissions'
import { authApi } from '../api'

const { t, locale } = useI18n()
const router = useRouter()
const user = computed(() => session.user)
const menuOpen = ref(false)
const pwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

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

const onUserCommand = (command) => {
  if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
    pwdDialog.value = true
  } else if (command === 'logout') {
    logout()
  }
}

const submitPassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning(t('pwdRequired'))
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning(t('pwdMismatch'))
    return
  }
  pwdLoading.value = true
  try {
    await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    pwdDialog.value = false
    ElMessage.success(t('pwdChanged'))
  } finally {
    pwdLoading.value = false
  }
}

const logout = () => {
  clearSession()
  router.push('/login')
}
</script>
