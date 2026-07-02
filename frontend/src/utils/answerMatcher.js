const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
const MARKED_OPTION_RE = /^\s*([A-Za-z])\s*(?:[.)]|[:：]|\u3001|\uFF0E)\s*(.*)$/
const SPACED_OPTION_RE = /^\s*([A-Za-z])\s+(.+)$/

const stringify = value => value == null ? '' : String(value)

const normalizeText = value => stringify(value)
  .replace(/\u3000/g, ' ')
  .trim()
  .replace(/\s+/g, ' ')
  .toLowerCase()

const normalizeFillText = value => normalizeText(value)

const isSingleUpperLetter = value => /^[A-Z]$/.test(stringify(value).trim())

const markedOption = value => {
  const text = stringify(value)
  const marked = text.match(MARKED_OPTION_RE)
  if (marked) {
    return {
      letter: marked[1].toUpperCase(),
      rest: stringify(marked[2]).trim()
    }
  }
  const spaced = text.match(SPACED_OPTION_RE)
  if (spaced) {
    return {
      letter: spaced[1].toUpperCase(),
      rest: stringify(spaced[2]).trim()
    }
  }
  return null
}

const addAlias = (aliases, value) => {
  const normalized = normalizeText(value)
  if (normalized) aliases.add(normalized)
}

const optionEntry = (fallbackLetter, rawValue, key = '') => {
  const raw = stringify(rawValue).trim()
  const keyText = stringify(key).trim()
  const marked = markedOption(raw)
  const keyLetter = /^[A-Za-z]$/.test(keyText) ? keyText.toUpperCase() : ''
  const letter = marked?.letter || keyLetter || fallbackLetter
  const display = marked?.rest || raw
  const aliases = new Set()

  addAlias(aliases, raw)
  addAlias(aliases, display)
  if (keyText) {
    addAlias(aliases, keyText)
    addAlias(aliases, `${keyText}. ${display}`)
    addAlias(aliases, `${keyText}\u3001${display}`)
  }
  if (letter) {
    addAlias(aliases, `${letter}. ${display}`)
    addAlias(aliases, `${letter}\u3001${display}`)
  }

  return { letter, display, aliases }
}

export const parseOptionEntries = options => {
  if (!options) return []

  if (Array.isArray(options)) {
    return options.map((item, index) => optionEntry(LETTERS[index] || String(index + 1), item))
  }

  try {
    const parsed = JSON.parse(options)
    if (Array.isArray(parsed)) {
      return parsed.map((item, index) => optionEntry(LETTERS[index] || String(index + 1), item))
    }
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed).map(([key, value], index) =>
        optionEntry(LETTERS[index] || String(index + 1), value, key)
      )
    }
  } catch {
    // Fall through to newline parsing.
  }

  return stringify(options)
    .split(/\r?\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .map((item, index) => optionEntry(LETTERS[index] || String(index + 1), item))
}

export const parseQuestionOptions = options =>
  parseOptionEntries(options).map(entry => entry.display)

const canonicalChoiceToken = (value, question) => {
  const text = stringify(value).trim()
  if (!text) return ''

  const entries = parseOptionEntries(question?.options)
  const entryByLetter = new Map(entries.map(entry => [entry.letter, entry]))

  if (isSingleUpperLetter(text)) return `option:${text}`

  const marked = markedOption(text)
  if (marked?.letter && (entryByLetter.has(marked.letter) || /^[A-Z]$/.test(marked.letter))) {
    return `option:${marked.letter}`
  }

  const normalized = normalizeText(text)
  for (const entry of entries) {
    if (entry.aliases.has(normalized)) return `option:${entry.letter}`
  }

  return `text:${normalized}`
}

const splitAnswerTokens = value => {
  if (Array.isArray(value)) return value
  return stringify(value)
    .split(/[,，;；]/)
    .map(item => item.trim())
    .filter(Boolean)
}

const canonicalChoiceSet = (value, question) =>
  new Set(splitAnswerTokens(value).map(item => canonicalChoiceToken(item, question)).filter(Boolean))

export const isQuestionAnswerCorrect = (question, answer) => {
  if (!question || question.answer == null) return false
  if (question.type === 'multi') {
    const expected = canonicalChoiceSet(question.answer, question)
    const actual = canonicalChoiceSet(answer, question)
    return expected.size > 0 &&
      expected.size === actual.size &&
      [...expected].every(item => actual.has(item))
  }
  if (question.type === 'single') {
    const expected = canonicalChoiceToken(question.answer, question)
    const actual = canonicalChoiceToken(answer, question)
    return Boolean(expected) && expected === actual
  }
  return normalizeFillText(answer) === normalizeFillText(question.answer)
}
