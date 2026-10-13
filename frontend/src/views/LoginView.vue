<template>
  <div class="login-wrap">
    <div class="login-hero">
      <img class="brand-logo" style="width:48px;height:48px;margin-bottom:18px" src="../assets/logo.jpg" alt="logo" />
      <h1>{{ t('heroTitle') }}</h1>
      <p>{{ t('heroDesc') }}</p>
    </div>
    <div class="login-panel">
      <div class="login-box">
        <h2>{{ t('loginTitle') }}</h2>
        <div class="sub">{{ t('loginSub') }}</div>
        <el-form @submit.prevent="onLogin">
          <el-form-item>
            <el-input v-model="form.username" size="large" :placeholder="t('username')" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password :placeholder="t('password')" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">
            {{ t('login') }}
          </el-button>
        </el-form>
        <div style="margin-top:16px;display:flex;justify-content:space-between;color:#667085;font-size:13px">
          <span>{{ t('demoHint') }}</span>
          <el-select v-model="locale" size="small" style="width:110px" @change="changeLocale">
            <el-option label="中文" value="zh" />
            <el-option label="English" value="en" />
          </el-select>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { setSession } from '../permissions'

const { t, locale } = useI18n()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const changeLocale = (value) => {
  localStorage.setItem('locale', value)
}

const onLogin = async () => {
  loading.value = true
  try {
    const res = await authApi.login(form)
    localStorage.setItem('token', res.data.token)
    setSession(res.data)
    ElMessage.success(t('success'))
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
