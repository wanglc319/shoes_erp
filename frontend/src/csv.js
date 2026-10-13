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
