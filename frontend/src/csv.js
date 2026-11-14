/**
 * Export rows as an Excel-friendly CSV file (UTF-8 with BOM).
 * @param {string} filename  download file name (without extension)
 * @param {string[]} headers column headers
 * @param {Array<Array<any>>} rows data rows, aligned with headers
 */
export function exportCsv(filename, headers, rows) {
  const escape = (value) => {
    if (value === null || value === undefined) return ''
    const text = String(value)
    if (/[",\n\r]/.test(text)) {
      return `"${text.replace(/"/g, '""')}"`
    }
    return text
  }
  const lines = [headers.map(escape).join(','), ...rows.map((row) => row.map(escape).join(','))]
  const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

/**
 * Download a backend-generated Excel file with the JWT attached.
 * @param {string} path   API path, e.g. `/quotations/12/export`
 * @param {string} filename downloaded file name including extension
 */
export async function downloadExcel(path, filename) {
  const token = localStorage.getItem('token')
  const response = await fetch(`${import.meta.env.BASE_URL}api${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error(`导出失败 (${response.status})`)
  }
  const blob = await response.blob()
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}
