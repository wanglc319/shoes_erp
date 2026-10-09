function refresh(root) {
  const tables = root.querySelectorAll ? root.querySelectorAll('.el-table') : []
  tables.forEach((table) => {
    if (table.closest('.el-dialog')) {
      table.classList.remove('mobile-card')
      return
    }
    const headers = Array.from(table.querySelectorAll('.el-table__header-wrapper th')).map(
      (th) => (th.innerText || '').trim()
    )
    if (!headers.length) return
    table.querySelectorAll('.el-table__body-wrapper tr').forEach((tr) => {
      Array.from(tr.querySelectorAll(':scope > td.el-table__cell')).forEach((td, i) => {
        if (headers[i]) td.setAttribute('data-label', headers[i])
      })
    })
    table.classList.add('mobile-card')
  })
}

export function installMobileTable() {
  refresh(document)
  window.addEventListener('resize', () => refresh(document))
  const mo = new MutationObserver((mutations) => {
    let dirty = false
    for (const m of mutations) {
      for (const n of m.addedNodes) {
        if (n.nodeType === 1) dirty = true
      }
    }
    if (dirty) refresh(document)
  })
  mo.observe(document.body, { childList: true, subtree: true })
}
