<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('users') }}</h2>
    <div class="toolbar">
      <el-input v-model="query.keyword" :placeholder="t('keyword')" style="width:220px" clearable @keyup.enter="load" />
      <el-select v-model="query.role" clearable :placeholder="t('role')" style="width:140px">
        <el-option v-for="r in ROLES" :key="r" :label="label(r)" :value="r" />
      </el-select>
      <el-button type="primary" @click="load">{{ t('search') }}</el-button>
      <el-button @click="open()">{{ t('create') }}</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="username" :label="t('username')" />
      <el-table-column prop="realName" :label="t('realName')" />
      <el-table-column :label="t('role')" width="130">
        <template #default="{ row }">
          <el-tag size="small" :type="roleTag(row.role)">{{ label(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" :label="t('email')" />
      <el-table-column prop="phone" :label="t('phone')" />
      <el-table-column :label="t('enabled')" width="90">
        <template #default="{ row }">{{ row.enabled ? t('yes') : t('no') }}</template>
      </el-table-column>
      <el-table-column :label="t('actions')" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">{{ t('edit') }}</el-button>
          <el-button link type="danger" @click="remove(row)">{{ t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:14px" background layout="total, prev, pager, next"
      :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="load" />

    <el-dialog v-model="visible" :title="form.id ? t('edit') : t('create')" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="t('username')"><el-input v-model="form.username" /></el-form-item>
        <el-form-item :label="t('password')"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item :label="t('realName')"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item :label="t('role')">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="r in ROLES" :key="r" :label="label(r)" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('email')"><el-input v-model="form.email" /></el-form-item>
        <el-form-item :label="t('phone')"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item :label="t('enabled')"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
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
import { userApi } from '../api'
import { enumLabel } from '../labels'
import { ROLES } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const rows = ref([])
const total = ref(0)
const visible = ref(false)
const query = reactive({ keyword: '', role: '', page: 1, size: 10 })
const form = reactive({})

const roleTag = (role) => (role === 'ADMIN' ? 'danger' : role === 'SALES' ? 'warning' : role === 'FINANCE' ? 'success' : 'info')

const load = async () => {
  const res = await userApi.page(query)
  rows.value = res.data.records
  total.value = res.data.total
}

const open = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row, password: '' } : { enabled: true, role: 'SALES' })
  visible.value = true
}

const save = async () => {
  if (form.id) await userApi.update(form.id, form)
  else await userApi.create(form)
  ElMessage.success(t('success'))
  visible.value = false
  load()
}

const remove = async (row) => {
  await ElMessageBox.confirm(t('confirmDelete'))
  await userApi.remove(row.id)
  ElMessage.success(t('success'))
  load()
}

onMounted(load)
</script>
