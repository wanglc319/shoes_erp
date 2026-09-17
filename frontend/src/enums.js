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
