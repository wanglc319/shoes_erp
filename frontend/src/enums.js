export const SHOE_CATEGORIES = [
  'Sneaker', 'Running', 'Boot', 'Sandal', 'Casual', 'Basketball', 'Loafer', 'Slipper', 'Hiking', 'Safety'
]

export const GENDERS = ['Men', 'Women', 'Unisex', 'Kids']

export const SHOE_COLORS = [
  'White', 'Black', 'Brown', 'Navy', 'Red', 'Blue', 'Grey', 'Beige',
  'Green', 'Pink', 'Khaki', 'White/Black', 'Navy/Red', 'Black/Gold', 'Multi'
]

export const SHOE_STATUSES = ['ACTIVE', 'INACTIVE', 'DISCONTINUED']

export const UPPER_MATERIALS = [
  'Mesh', 'Knit', 'PU', 'Leather', 'Synthetic Leather', 'Canvas', 'Suede', 'Microfiber', 'Crazy Horse Leather'
]

export const SOLE_MATERIALS = ['EVA', 'Rubber', 'TPR', 'Phylon', 'MD', 'PVC', 'TPU']

export const SIZE_RANGES = ['27-35', '35-41', '36-45', '39-46', '40-47', '41-48']

export const SUPPLIER_TYPES = ['Factory', 'Mould', 'Material', 'Accessory', 'Trading', 'Agent']

export const COUNTRIES = [
  'China', 'Vietnam', 'Indonesia', 'India', 'Bangladesh', 'Cambodia', 'Thailand',
  'Germany', 'USA', 'UK', 'France', 'Spain', 'Italy', 'Netherlands', 'Poland',
  'Australia', 'Canada', 'Mexico', 'Brazil', 'Chile',
  'UAE', 'Saudi Arabia', 'Japan', 'South Korea', 'Russia', 'South Africa'
]

export const CUSTOMER_LEVELS = ['A', 'B', 'C']

export const INCOTERMS = ['EXW', 'FCA', 'FOB', 'CFR', 'CIF', 'CPT', 'CIP', 'DAP', 'DPU', 'DDP']

export const CURRENCIES = ['USD', 'EUR', 'GBP', 'CNY', 'AUD', 'CAD', 'JPY', 'HKD']

export const CURRENCY_NAMES = {
  USD: { zh: '美元', en: 'US Dollar' },
  EUR: { zh: '欧元', en: 'Euro' },
  GBP: { zh: '英镑', en: 'British Pound' },
  CNY: { zh: '人民币', en: 'Chinese Yuan' },
  AUD: { zh: '澳元', en: 'Australian Dollar' },
  CAD: { zh: '加元', en: 'Canadian Dollar' },
  JPY: { zh: '日元', en: 'Japanese Yen' },
  HKD: { zh: '港币', en: 'Hong Kong Dollar' }
}

export const PAYMENT_TERMS = [
  'T/T 30% deposit, 70% before shipment',
  'T/T 30% deposit, 70% against BL copy',
  'T/T 40% deposit, 60% before shipment',
  '100% T/T in advance',
  'L/C at sight',
  'L/C 60 days',
  'D/P at sight',
  'CAD',
  'Net 30',
  'Net 60'
]

export const QUOTATION_STATUSES = ['DRAFT', 'SENT', 'WON', 'LOST']

export const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PARTIAL_SHIPPED', 'SHIPPED', 'CANCELLED']

export const PRODUCTION_STATUSES = ['NOT_STARTED', 'IN_PRODUCTION', 'COMPLETED']

export const SHIPMENT_STATUSES = ['PREPARING', 'DEPARTED', 'ARRIVED', 'CLEARED', 'DELIVERED']

export const ROLES = ['ADMIN', 'SALES', 'OPS', 'FINANCE']

export const MENUS = [
  { key: 'dashboard', titleKey: 'dashboard' },
  { key: 'shoes', titleKey: 'shoes' },
  { key: 'suppliers', titleKey: 'suppliers' },
  { key: 'customers', titleKey: 'customers' },
  { key: 'quotations', titleKey: 'quotations' },
  { key: 'orders', titleKey: 'orders' },
  { key: 'shipments', titleKey: 'shipments' },
  { key: 'purchases', titleKey: 'purchases' },
  { key: 'finance', titleKey: 'finance' },
  { key: 'fx', titleKey: 'fx' },
  { key: 'users', titleKey: 'users' },
  { key: 'permissions', titleKey: 'permissions' }
]

export const PURCHASE_STATUSES = ['DRAFT', 'CONFIRMED', 'IN_PRODUCTION', 'DELIVERED', 'CANCELLED']

export const PAYMENT_METHODS = ['T/T', 'L/C', 'D/P', 'CAD', 'PayPal', 'Western Union', 'Cash']

export const RECEIVABLE_STATUSES = ['UNPAID', 'PARTIAL', 'PAID', 'OVERDUE']

export const WORLD_CLOCKS = [
  { city: 'Beijing', cityZh: '北京', zone: 'Asia/Shanghai' },
  { city: 'Rotterdam', cityZh: '鹿特丹', zone: 'Europe/Amsterdam' },
  { city: 'Hamburg', cityZh: '汉堡', zone: 'Europe/Berlin' },
  { city: 'London', cityZh: '伦敦', zone: 'Europe/London' },
  { city: 'Moscow', cityZh: '莫斯科', zone: 'Europe/Moscow' },
  { city: 'New York', cityZh: '纽约', zone: 'America/New_York' },
  { city: 'Los Angeles', cityZh: '洛杉矶', zone: 'America/Los_Angeles' },
  { city: 'Dubai', cityZh: '迪拜', zone: 'Asia/Dubai' },
  { city: 'Singapore', cityZh: '新加坡', zone: 'Asia/Singapore' },
  { city: 'Tokyo', cityZh: '东京', zone: 'Asia/Tokyo' },
  { city: 'Sydney', cityZh: '悉尼', zone: 'Australia/Sydney' },
  { city: 'Sao Paulo', cityZh: '圣保罗', zone: 'America/Sao_Paulo' }
]

export const PORT_OF_LOADING = [
  'Ningbo', 'Shanghai', 'Shenzhen', 'Qingdao', 'Xiamen', 'Guangzhou', 'Tianjin',
  'Haiphong', 'Ho Chi Minh', 'Bangkok'
]

export const PORT_OF_DESTINATION = [
  'Hamburg', 'Rotterdam', 'Felixstowe', 'Antwerp', 'Los Angeles', 'New York', 'Long Beach',
  'Sydney', 'Melbourne', 'Jebel Ali', 'Singapore', 'Busan', 'Santos'
]

export const today = () => new Date().toISOString().slice(0, 10)

export const addDays = (days) => {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}
