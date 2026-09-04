const clampRate = value => Math.max(0, Math.min(1, value))

const numericRate = value => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? clampRate(parsed) : null
}

export const correctRateOr = (value, fallback = 0) => {
  const rate = numericRate(value)
  return rate === null ? (numericRate(fallback) ?? 0) : rate
}

export const firstCorrectRate = (...values) => {
  for (const value of values) {
    const rate = numericRate(value)
    if (rate !== null) return rate
  }
  return 0
}

export const correctRatePercent = (...values) =>
  Math.round(firstCorrectRate(...values) * 100)
