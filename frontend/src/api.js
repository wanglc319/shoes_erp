import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router'
import { clearSession } from './permissions'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || 'Request failed')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearSession()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
    ElMessage.error(error.response?.data?.message || error.message || 'Network error')
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me')
}

export const fileApi = {
  upload: (file, category) => {
    const data = new FormData()
    data.append('file', file)
    if (category) data.append('category', category)
    return http.post('/files/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const resourceApi = (path) => ({
  page: (params) => http.get(path, { params }),
  get: (id) => http.get(`${path}/${id}`),
  create: (data) => http.post(path, data),
  update: (id, data) => http.put(`${path}/${id}`, data),
  remove: (id) => http.delete(`${path}/${id}`),
  options: () => http.get(`${path}/options`)
})

export const shoeApi = resourceApi('/shoes')
export const supplierApi = resourceApi('/suppliers')
export const customerApi = resourceApi('/customers')
export const userApi = resourceApi('/users')
export const quotationApi = {
  ...resourceApi('/quotations'),
  updateStatus: (id, status) => http.post(`/quotations/${id}/status`, null, { params: { status } }),
  toOrder: (id) => http.post(`/quotations/${id}/to-order`)
}
export const orderApi = {
  ...resourceApi('/orders'),
  updateStatus: (id, status) => http.post(`/orders/${id}/status`, null, { params: { status } }),
  updateProduction: (id, productionStatus) =>
    http.post(`/orders/${id}/production-status`, null, { params: { productionStatus } }),
  toShipment: (id) => http.post(`/orders/${id}/to-shipment`)
}
export const shipmentApi = {
  ...resourceApi('/shipments'),
  updateStatus: (id, status) => http.post(`/shipments/${id}/status`, null, { params: { status } })
}
export const purchaseApi = {
  ...resourceApi('/purchases'),
  updateStatus: (id, status) => http.post(`/purchases/${id}/status`, null, { params: { status } })
}
export const paymentApi = {
  ...resourceApi('/finance/payments')
}
export const receivableApi = {
  page: (params) => http.get('/finance/receivables', { params }),
  detail: (orderId) => http.get(`/finance/receivables/${orderId}`),
  summary: () => http.get('/finance/receivables/summary')
}
export const fxApi = {
  rates: (base) => http.get('/fx/rates', { params: { base } }),
  convert: (params) => http.get('/fx/convert', { params })
}
export const permissionApi = {
  all: () => http.get('/permissions'),
  catalog: () => http.get('/permissions/catalog'),
  save: (role, menuKeys) => http.put(`/permissions/${role}`, menuKeys)
}
export const dashboardApi = {
  stats: () => http.get('/dashboard/stats')
}
