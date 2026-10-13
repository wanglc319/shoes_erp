import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import LayoutView from './views/LayoutView.vue'
import DashboardView from './views/DashboardView.vue'
import ShoeView from './views/ShoeView.vue'
import SupplierView from './views/SupplierView.vue'
import CustomerView from './views/CustomerView.vue'
import QuotationView from './views/QuotationView.vue'
import OrderView from './views/OrderView.vue'
import ShipmentView from './views/ShipmentView.vue'
import PurchaseView from './views/PurchaseView.vue'
import FinanceView from './views/FinanceView.vue'
import FxView from './views/FxView.vue'
import UserView from './views/UserView.vue'
import PermissionView from './views/PermissionView.vue'
import { can } from './permissions'

const routes = [
  { path: '/login', component: LoginView },
  {
    path: '/',
    component: LayoutView,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: DashboardView, meta: { menu: 'dashboard' } },
      { path: 'shoes', component: ShoeView, meta: { menu: 'shoes' } },
      { path: 'suppliers', component: SupplierView, meta: { menu: 'suppliers' } },
      { path: 'customers', component: CustomerView, meta: { menu: 'customers' } },
      { path: 'quotations', component: QuotationView, meta: { menu: 'quotations' } },
      { path: 'orders', component: OrderView, meta: { menu: 'orders' } },
      { path: 'shipments', component: ShipmentView, meta: { menu: 'shipments' } },
      { path: 'purchases', component: PurchaseView, meta: { menu: 'purchases' } },
      { path: 'finance', component: FinanceView, meta: { menu: 'finance' } },
      { path: 'fx', component: FxView, meta: { menu: 'fx' } },
      { path: 'users', component: UserView, meta: { menu: 'users' } },
      { path: 'permissions', component: PermissionView, meta: { menu: 'permissions' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

const defaultPath = () => {
  const order = ['dashboard', 'orders', 'shipments', 'purchases', 'shoes', 'suppliers',
    'customers', 'quotations', 'finance', 'fx', 'users', 'permissions']
  const pathOf = {
    dashboard: '/dashboard', orders: '/orders', shipments: '/shipments', purchases: '/purchases',
    shoes: '/shoes', suppliers: '/suppliers', customers: '/customers', quotations: '/quotations',
    finance: '/finance', fx: '/fx', users: '/users', permissions: '/permissions'
  }
  const found = order.find((key) => can(key))
  return found ? pathOf[found] : null
}

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return defaultPath() || '/dashboard'
  }
  const menu = to.meta?.menu
  if (token && menu && !can(menu)) {
    return defaultPath() || '/dashboard'
  }
})

export default router
