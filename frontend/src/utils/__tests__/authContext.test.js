import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCourseId, getCurrentUser, getStudentId, getTeacherId } from '../authContext'

const values = {}

vi.stubGlobal('localStorage', {
  getItem: vi.fn(key => values[key] || null),
  setItem: vi.fn((key, value) => { values[key] = value }),
  removeItem: vi.fn(key => { delete values[key] }),
  clear: vi.fn(() => Object.keys(values).forEach(key => delete values[key]))
})

describe('身份和课程上下文', () => {
  beforeEach(() => Object.keys(values).forEach(key => delete values[key]))

  it('损坏的登录态不会抛错，也不会产生固定学生号', () => {
    values.user = '{broken'
    expect(getCurrentUser()).toEqual({})
    expect(getStudentId()).toBe('')
  })

  it('分别读取学生号和教师号', () => {
    expect(getStudentId({ studentNo: 'S1' })).toBe('S1')
    expect(getTeacherId({ teacherNo: 'T1' })).toBe('T1')
  })

  it('课程缺失时返回空值，不默认到课程 1', () => {
    expect(getCourseId({ query: {} })).toBe('')
  })

  it('优先使用路由课程参数，其次读取已选择课程', () => {
    values.courseId = 'C2'
    expect(getCourseId({ query: { courseId: 'C1' } })).toBe('C1')
    expect(getCourseId({ query: {} })).toBe('C2')
  })

  it('支持课程详情路由参数作为课程上下文', () => {
    expect(getCourseId({ params: { code: 'C3' } })).toBe('C3')
  })
})
