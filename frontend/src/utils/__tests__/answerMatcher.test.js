import { describe, it, expect } from 'vitest'
import {
  parseOptionEntries,
  parseQuestionOptions,
  isQuestionAnswerCorrect
} from '../answerMatcher'

describe('parseOptionEntries', () => {
  it('returns empty array for null/undefined input', () => {
    expect(parseOptionEntries(null)).toEqual([])
    expect(parseOptionEntries(undefined)).toEqual([])
  })

  it('parses array of strings', () => {
    const result = parseOptionEntries(['北京', '上海', '广州'])
    expect(result).toHaveLength(3)
    expect(result[0].letter).toBe('A')
    expect(result[0].display).toBe('北京')
    expect(result[1].letter).toBe('B')
    expect(result[1].display).toBe('上海')
  })

  it('parses JSON array string', () => {
    const result = parseOptionEntries('["苹果", "香蕉", "橘子"]')
    expect(result).toHaveLength(3)
    expect(result[0].letter).toBe('A')
    expect(result[1].letter).toBe('B')
    expect(result[2].letter).toBe('C')
  })

  it('parses JSON object with keys', () => {
    const result = parseOptionEntries(JSON.stringify({ A: '选项A', B: '选项B' }))
    expect(result).toHaveLength(2)
    expect(result[0].letter).toBe('A')
    expect(result[0].display).toBe('选项A')
    expect(result[1].letter).toBe('B')
  })

  it('parses newline-separated strings', () => {
    const result = parseOptionEntries('选项1\n选项2\n选项3')
    expect(result).toHaveLength(3)
    expect(result[0].letter).toBe('A')
    expect(result[1].letter).toBe('B')
    expect(result[2].letter).toBe('C')
  })

  it('handles marked options like "A. content"', () => {
    const result = parseOptionEntries(['A. Java', 'B. Python'])
    expect(result[0].letter).toBe('A')
    expect(result[0].display).toBe('Java')
    expect(result[1].letter).toBe('B')
    expect(result[1].display).toBe('Python')
  })

  it('handles Chinese marked options like "A、内容"', () => {
    const result = parseOptionEntries(['A、Java', 'B、Python'])
    expect(result[0].letter).toBe('A')
    expect(result[0].display).toBe('Java')
  })

  it('creates aliases for matching', () => {
    const result = parseOptionEntries(['A. 正确答案'])
    expect(result[0].aliases).toBeInstanceOf(Set)
    expect(result[0].aliases.size).toBeGreaterThan(0)
  })
})

describe('parseQuestionOptions', () => {
  it('extracts display strings from option entries', () => {
    const result = parseQuestionOptions(['A. Red', 'B. Blue', 'C. Green'])
    expect(result).toEqual(['Red', 'Blue', 'Green'])
  })
})

describe('isQuestionAnswerCorrect', () => {
  it('returns false when question is null', () => {
    expect(isQuestionAnswerCorrect(null, 'anything')).toBe(false)
  })

  it('returns false when answer is null', () => {
    const question = { type: 'single', answer: 'A' }
    expect(isQuestionAnswerCorrect(question, null)).toBe(false)
  })

  it('correctly matches single-choice answer by letter', () => {
    const question = {
      type: 'single',
      answer: 'B',
      options: ['A. Java', 'B. Python', 'C. C++']
    }
    expect(isQuestionAnswerCorrect(question, 'B')).toBe(true)
    expect(isQuestionAnswerCorrect(question, 'A')).toBe(false)
  })

  it('correctly matches single-choice answer by content', () => {
    const question = {
      type: 'single',
      answer: 'B',
      options: ['A. Java', 'B. Python', 'C. C++']
    }
    expect(isQuestionAnswerCorrect(question, 'Python')).toBe(true)
    expect(isQuestionAnswerCorrect(question, 'Java')).toBe(false)
  })

  it('correctly matches multi-choice answer', () => {
    const question = {
      type: 'multi',
      answer: 'A,C',
      options: ['A. Java', 'B. Python', 'C. C++']
    }
    expect(isQuestionAnswerCorrect(question, 'A,C')).toBe(true)
    expect(isQuestionAnswerCorrect(question, 'A,B')).toBe(false)
  })

  it('correctly matches multi-choice answer regardless of order', () => {
    const question = {
      type: 'multi',
      answer: 'A,C',
      options: ['A. Java', 'B. Python', 'C. C++']
    }
    expect(isQuestionAnswerCorrect(question, 'C,A')).toBe(true)
  })

  it('returns false for incomplete multi-choice answer', () => {
    const question = {
      type: 'multi',
      answer: 'A,C',
      options: ['A. Java', 'B. Python', 'C. C++']
    }
    expect(isQuestionAnswerCorrect(question, 'A')).toBe(false)
  })

  it('correctly matches fill-in-the-blank by normalized text', () => {
    const question = {
      type: 'fill',
      answer: 'hello world'
    }
    expect(isQuestionAnswerCorrect(question, 'hello world')).toBe(true)
    expect(isQuestionAnswerCorrect(question, 'HELLO WORLD')).toBe(true)
    expect(isQuestionAnswerCorrect(question, '  hello   world  ')).toBe(true)
  })

  it('returns false for wrong fill-in answer', () => {
    const question = {
      type: 'fill',
      answer: 'correct answer'
    }
    expect(isQuestionAnswerCorrect(question, 'wrong answer')).toBe(false)
  })

  it('handles answer as array for multi-choice', () => {
    const question = {
      type: 'multi',
      answer: ['A', 'B'],
      options: ['A. Option A', 'B. Option B']
    }
    expect(isQuestionAnswerCorrect(question, ['A', 'B'])).toBe(true)
  })

  it('returns false for empty multi-choice answer', () => {
    const question = {
      type: 'multi',
      answer: [],
      options: ['A. Option A']
    }
    expect(isQuestionAnswerCorrect(question, 'A')).toBe(false)
  })
})
