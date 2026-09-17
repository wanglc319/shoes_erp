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
import UserView from './views/UserView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: LayoutView,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'shoes', component: ShoeView },
        { path: 'suppliers', component: SupplierView },
        { path: 'customers', component: CustomerView },
        { path: 'quotations', component: QuotationView },
        { path: 'orders', component: OrderView },
        { path: 'shipments', component: ShipmentView },
        { path: 'users', component: UserView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
})

export default router
