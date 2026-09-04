import { describe, expect, it } from 'vitest'
import { correctRateOr, correctRatePercent, firstCorrectRate } from '../correctRate'

describe('正确率归一化', () => {
  it('保留 0%，不会把全错误判为 100%', () => {
    expect(correctRateOr(0, 1)).toBe(0)
    expect(firstCorrectRate(0, 0.8, 1)).toBe(0)
    expect(correctRatePercent(0, 0.8)).toBe(0)
  })

  it('正确显示部分正确和全对', () => {
    expect(correctRatePercent(2 / 3)).toBe(67)
    expect(correctRatePercent(1)).toBe(100)
  })

  it('仅在值真正缺失时采用后备结果', () => {
    expect(firstCorrectRate(null, undefined, 0.75)).toBe(0.75)
    expect(correctRateOr(undefined, 1)).toBe(1)
  })

  it('把异常范围限制在 0% 到 100%', () => {
    expect(correctRateOr(-0.2)).toBe(0)
    expect(correctRateOr(1.4)).toBe(1)
    expect(correctRateOr('not-a-number', 0.5)).toBe(0.5)
  })
})
