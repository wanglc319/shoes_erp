const esc = (value) => {
  if (value === null || value === undefined) return ''
  return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const money = (value, currency) => {
  const num = Number(value || 0)
  return `${currency ? currency + ' ' : ''}${num.toFixed(2)}`
}

const dateText = (value) => (value ? String(value).slice(0, 10) : '')

const openDocument = (title, body) => {
  const win = window.open('', '_blank', 'width=900,height=1000')
  if (!win) return
  win.document.write(`<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<title>${esc(title)}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: "Segoe UI", Arial, "Microsoft YaHei", sans-serif; color: #1a1a1a; padding: 32px 40px; }
  .doc-head { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 3px solid #1a1a1a; padding-bottom: 14px; }
  .company { font-size: 22px; font-weight: 700; letter-spacing: .5px; }
  .company-sub { font-size: 12px; color: #555; margin-top: 4px; }
  .doc-title { font-size: 22px; font-weight: 700; text-transform: uppercase; text-align: right; }
  .meta { display: flex; justify-content: space-between; margin-top: 18px; font-size: 13px; }
  .meta-table { border-collapse: collapse; }
  .meta-table td { padding: 2px 10px 2px 0; vertical-align: top; }
  .meta-table td.k { color: #555; white-space: nowrap; }
  table.items { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 12.5px; }
  table.items th, table.items td { border: 1px solid #999; padding: 6px 8px; }
  table.items th { background: #f0f0f0; text-align: left; }
  table.items td.num, table.items th.num { text-align: right; }
  .totals { margin-top: 14px; width: 320px; margin-left: auto; font-size: 13px; }
  .totals td { padding: 4px 8px; }
  .totals td.k { color: #555; }
  .totals tr.grand td { border-top: 2px solid #1a1a1a; font-weight: 700; font-size: 15px; }
  .sign { display: flex; justify-content: space-between; margin-top: 60px; font-size: 13px; }
  .sign div { width: 45%; }
  .sign .line { margin-top: 46px; border-top: 1px solid #333; padding-top: 6px; color: #555; }
  @media print { body { padding: 12px 18px; } .no-print { display: none; } }
</style>
</head>
<body>${body}</body>
</html>`)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 300)
}

const header = (title) => `
  <div class="doc-head">
    <div>
      <div class="company">ShoeERP Footwear Co., Ltd.</div>
      <div class="company-sub">Footwear Export &amp; Trading &nbsp;|&nbsp; Ningbo, China</div>
    </div>
    <div class="doc-title">${esc(title)}</div>
  </div>`

const metaRows = (pairs) =>
  pairs
    .filter(Boolean)
    .map(([k, v]) => `<tr><td class="k">${esc(k)}</td><td>${esc(v)}</td></tr>`)
    .join('')

export const printQuotation = (data) => {
  const items = data.items || []
  const rows = items
    .map(
      (it, idx) => `<tr>
        <td>${idx + 1}</td>
        <td>${esc(it.styleNo)}</td>
        <td>${esc(it.productName)}</td>
        <td>${esc(it.color)}</td>
        <td>${esc(it.sizeRange)}</td>
        <td class="num">${esc(it.quantity)}</td>
        <td>${esc(it.unit || 'PRS')}</td>
        <td class="num">${Number(it.unitPrice || 0).toFixed(2)}</td>
        <td class="num">${Number(it.amount || 0).toFixed(2)}</td>
      </tr>`
    )
    .join('')
  const body = `
    ${header('Proforma Invoice')}
    <div class="meta">
      <table class="meta-table">${metaRows([
        ['PI No.', data.quoteNo],
        ['Date', dateText(data.quoteDate)],
        ['Valid Until', dateText(data.validUntil)]
      ])}</table>
      <table class="meta-table">${metaRows([
        ['Buyer', data.customerName],
        ['Trade Terms', data.tradeTerms],
        ['Payment', data.paymentTerms],
        ['Currency', data.currency]
      ])}</table>
    </div>
    <table class="items">
      <thead><tr>
        <th>#</th><th>Style No.</th><th>Description</th><th>Color</th><th>Size</th>
        <th class="num">Qty</th><th>Unit</th><th class="num">Unit Price</th><th class="num">Amount</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <table class="totals">
      <tr class="grand"><td class="k">Total</td><td class="num">${money(data.totalAmount, data.currency)}</td></tr>
    </table>
    <div class="sign">
      <div><div class="line">Seller Signature</div></div>
      <div><div class="line">Buyer Signature</div></div>
    </div>`
  openDocument(`${data.quoteNo} Proforma Invoice`, body)
}

export const printOrder = (data) => {
  const items = data.items || []
  const rows = items
    .map(
      (it, idx) => `<tr>
        <td>${idx + 1}</td>
        <td>${esc(it.styleNo)}</td>
        <td>${esc(it.productName)}</td>
        <td>${esc(it.color)}</td>
        <td>${esc(it.sizeRange)}</td>
        <td class="num">${esc(it.quantity)}</td>
        <td>${esc(it.unit || 'PRS')}</td>
        <td class="num">${Number(it.unitPrice || 0).toFixed(2)}</td>
        <td class="num">${Number(it.amount || 0).toFixed(2)}</td>
      </tr>`
    )
    .join('')
  const body = `
    ${header('Sales Contract')}
    <div class="meta">
      <table class="meta-table">${metaRows([
        ['Contract No.', data.orderNo],
        ['Date', dateText(data.orderDate)],
        ['Delivery', dateText(data.deliveryDate)]
      ])}</table>
      <table class="meta-table">${metaRows([
        ['Buyer', data.customerName],
        ['Trade Terms', data.tradeTerms],
        ['Payment', data.paymentTerms],
        ['Currency', data.currency]
      ])}</table>
    </div>
    <table class="items">
      <thead><tr>
        <th>#</th><th>Style No.</th><th>Description</th><th>Color</th><th>Size</th>
        <th class="num">Qty</th><th>Unit</th><th class="num">Unit Price</th><th class="num">Amount</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <table class="totals">
      <tr><td class="k">Deposit</td><td class="num">${money(data.depositAmount, data.currency)}</td></tr>
      <tr class="grand"><td class="k">Total</td><td class="num">${money(data.totalAmount, data.currency)}</td></tr>
    </table>
    <div class="sign">
      <div><div class="line">Seller Signature</div></div>
      <div><div class="line">Buyer Signature</div></div>
    </div>`
  openDocument(`${data.orderNo} Sales Contract`, body)
}

export const printPackingList = (data) => {
  const items = data.items || []
  const totalCartons = items.reduce((sum, it) => sum + Number(it.cartons || 0), 0)
  const rows = items
    .map(
      (it, idx) => `<tr>
        <td>${idx + 1}</td>
        <td>${esc(it.styleNo)}</td>
        <td>${esc(it.productName)}</td>
        <td>${esc(it.color)}</td>
        <td>${esc(it.sizeRange)}</td>
        <td class="num">${esc(it.quantity)}</td>
        <td class="num">${esc(it.cartons)}</td>
        <td class="num">${it.netWeight != null ? esc(it.netWeight) : ''}</td>
        <td class="num">${it.grossWeight != null ? esc(it.grossWeight) : ''}</td>
      </tr>`
    )
    .join('')
  const body = `
    ${header('Packing List')}
    <div class="meta">
      <table class="meta-table">${metaRows([
        ['P/L No.', data.shipmentNo],
        ['Order No.', data.orderNo],
        ['Ship Date', dateText(data.shipDate)]
      ])}</table>
      <table class="meta-table">${metaRows([
        ['Buyer', data.customerName],
        ['Container No.', data.containerNo],
        ['Vessel', data.vessel],
        ['From / To', `${data.portOfLoading || ''} -> ${data.portOfDestination || ''}`]
      ])}</table>
    </div>
    <table class="items">
      <thead><tr>
        <th>#</th><th>Style No.</th><th>Description</th><th>Color</th><th>Size</th>
        <th class="num">Qty</th><th class="num">Cartons</th><th class="num">N.W.(kg)</th><th class="num">G.W.(kg)</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <table class="totals">
      <tr><td class="k">Total Qty</td><td class="num">${esc(data.totalQuantity)}</td></tr>
      <tr class="grand"><td class="k">Total Cartons</td><td class="num">${esc(totalCartons || data.totalCartons || 0)}</td></tr>
    </table>
    <div class="sign">
      <div><div class="line">Shipper Signature</div></div>
      <div><div class="line">Carrier / Forwarder</div></div>
    </div>`
  openDocument(`${data.shipmentNo} Packing List`, body)
}

export const printInvoice = (data) => {
  const items = data.items || []
  const rows = items
    .map(
      (it, idx) => `<tr>
        <td>${idx + 1}</td>
        <td>${esc(it.styleNo)}</td>
        <td>${esc(it.productName)}</td>
        <td>${esc(it.color)}</td>
        <td class="num">${esc(it.quantity)}</td>
        <td class="num">${Number(it.unitPrice || 0).toFixed(2)}</td>
        <td class="num">${Number(it.amount || 0).toFixed(2)}</td>
      </tr>`
    )
    .join('')
  const body = `
    ${header('Commercial Invoice')}
    <div class="meta">
      <table class="meta-table">${metaRows([
        ['Invoice No.', data.shipmentNo],
        ['Order No.', data.orderNo],
        ['Date', dateText(data.shipDate)]
      ])}</table>
      <table class="meta-table">${metaRows([
        ['Buyer', data.customerName],
        ['Container No.', data.containerNo],
        ['Vessel', data.vessel],
        ['B/L No.', data.blNo]
      ])}</table>
    </div>
    <table class="items">
      <thead><tr>
        <th>#</th><th>Style No.</th><th>Description</th><th>Color</th>
        <th class="num">Qty</th><th class="num">Unit Price</th><th class="num">Amount</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <table class="totals">
      <tr class="grand"><td class="k">Total</td><td class="num">${money(data.totalAmount, 'USD')}</td></tr>
    </table>
    <div class="sign">
      <div><div class="line">Authorized Signature</div></div>
      <div></div>
    </div>`
  openDocument(`${data.shipmentNo} Commercial Invoice`, body)
}

export const printPurchase = (data) => {
  const items = data.items || []
  const rows = items
    .map(
      (it, idx) => `<tr>
        <td>${idx + 1}</td>
        <td>${esc(it.styleNo)}</td>
        <td>${esc(it.productName)}</td>
        <td>${esc(it.color)}</td>
        <td>${esc(it.sizeRange)}</td>
        <td class="num">${esc(it.quantity)}</td>
        <td>${esc(it.unit || 'PRS')}</td>
        <td class="num">${Number(it.unitPrice || 0).toFixed(2)}</td>
        <td class="num">${Number(it.amount || 0).toFixed(2)}</td>
      </tr>`
    )
    .join('')
  const body = `
    ${header('Purchase Order')}
    <div class="meta">
      <table class="meta-table">${metaRows([
        ['PO No.', data.purchaseNo],
        ['Date', dateText(data.purchaseDate)],
        ['Expected', dateText(data.expectedDate)]
      ])}</table>
      <table class="meta-table">${metaRows([
        ['Supplier', data.supplierName],
        ['Buyer', data.buyer],
        ['Payment', data.paymentTerms],
        ['Currency', data.currency]
      ])}</table>
    </div>
    <table class="items">
      <thead><tr>
        <th>#</th><th>Style No.</th><th>Description</th><th>Color</th><th>Size</th>
        <th class="num">Qty</th><th>Unit</th><th class="num">Unit Price</th><th class="num">Amount</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <table class="totals">
      <tr class="grand"><td class="k">Total</td><td class="num">${money(data.totalAmount, data.currency)}</td></tr>
    </table>
    <div class="sign">
      <div><div class="line">Buyer Signature</div></div>
      <div><div class="line">Supplier Confirmation</div></div>
    </div>`
  openDocument(`${data.purchaseNo} Purchase Order`, body)
}
