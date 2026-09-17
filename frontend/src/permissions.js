import { reactive } from 'vue'

const readUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
}

export const session = reactive({
  user: readUser()
})

export const setSession = (data) => {
  localStorage.setItem('user', JSON.stringify(data))
  session.user = { ...data }
}

export const clearSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  session.user = {}
}

export const currentMenus = () => {
  const user = session.user
  if (!user || !user.role) return []
  if (user.role === 'ADMIN') return null
  return Array.isArray(user.menus) ? user.menus : []
}

export const can = (menuKey) => {
  const menus = currentMenus()
  return menus === null || menus.includes(menuKey)
}
