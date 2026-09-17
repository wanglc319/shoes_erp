<template>
  <div class="page-card">
    <h2 class="page-title">{{ t('permissions') }}</h2>
    <div class="toolbar">
      <span style="color:#667085;font-size:13px">{{ t('permissionHint') }}</span>
      <el-button type="primary" @click="save">{{ t('save') }}</el-button>
    </div>
    <el-table :data="roles" stripe>
      <el-table-column :label="t('role')" width="160">
        <template #default="{ row }">
          <el-tag size="small" :type="roleTag(row)">{{ label(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('menus')">
        <template #default="{ row }">
          <el-checkbox-group v-model="matrix[row]" :disabled="row === 'ADMIN'">
            <el-checkbox v-for="m in catalog" :key="m.key" :value="m.key" :label="m.key">
              {{ t(m.titleKey) }}
            </el-checkbox>
          </el-checkbox-group>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { permissionApi } from '../api'
import { enumLabel } from '../labels'
import { ROLES, MENUS } from '../enums'

const { t, te } = useI18n()
const label = (v) => enumLabel(t, te, v)
const roles = ref([...ROLES])
const catalog = ref([...MENUS])
const matrix = reactive({})

const roleTag = (role) => (role === 'ADMIN' ? 'danger' : role === 'SALES' ? 'warning' : role === 'FINANCE' ? 'success' : 'info')

const load = async () => {
  const cat = await permissionApi.catalog()
  if (cat.data?.length) catalog.value = cat.data
  const res = await permissionApi.all()
  const data = res.data || {}
  roles.value.forEach((role) => {
    matrix[role] = data[role] ? [...data[role]] : []
  })
}

const save = async () => {
  await Promise.all(roles.value.filter((r) => r !== 'ADMIN').map((role) => permissionApi.save(role, matrix[role] || [])))
  ElMessage.success(t('success'))
}

onMounted(load)
</script>
