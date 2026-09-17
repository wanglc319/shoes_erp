export const enumLabel = (t, te, value) => {
  if (value === null || value === undefined || value === '') return ''
  const key = `enum.${value}`
  return te(key) ? t(key) : value
}
