import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock axios 全局（profile.js 内部 import api from './index' 会使用 mock）
const mockAxiosInstance = vi.hoisted(() => ({
  get: vi.fn(() => Promise.resolve({ data: {} })),
  post: vi.fn(() => Promise.resolve({ data: {} })),
  put: vi.fn(() => Promise.resolve({ data: {} })),
  delete: vi.fn(() => Promise.resolve({ data: {} }))
}))

vi.mock('axios', () => ({
  default: { create: vi.fn(() => mockAxiosInstance) }
}))

import {
  getProfileSummary, getCompetency, getRecommendations,
  generateRecommendations, feedbackRecommendation,
  getAchievements, getTitle, getLeaderboard,
  importStudents, exportStudents,
  getCompetencyHistory, getGrowthHistory,
  getTestFeedback, getCourseStudentProfiles
} from '../profile'

describe('画像 API — 查询类', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getProfileSummary 构造正确路径', async () => {
    await getProfileSummary('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001')
  })

  it('getCompetency 能力维度查询', async () => {
    await getCompetency('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/competency')
  })

  it('getRecommendations 推荐查询', async () => {
    await getRecommendations('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/recommendations')
  })

  it('generateRecommendations 触发生成', async () => {
    await generateRecommendations('S1', 'C001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/profile/S1/C001/recommendations/generate')
  })

  it('feedbackRecommendation 提交反馈', async () => {
    await feedbackRecommendation('rec-1', 'useful')
    expect(mockAxiosInstance.put).toHaveBeenCalledWith(
      '/api/profile/recommendations/rec-1/feedback', null,
      { params: { feedback: 'useful' } }
    )
  })

  it('getAchievements 成就查询', async () => {
    await getAchievements('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/achievements')
  })

  it('getTitle 称号查询', async () => {
    await getTitle('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/title')
  })

  it('getLeaderboard 排行榜', async () => {
    await getLeaderboard('C001', 'progress')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/leaderboard', {
      params: { courseCode: 'C001', type: 'progress' }
    })
  })
})

describe('画像 API — 导入导出', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('importStudents multipart/form-data 上传', async () => {
    const fd = new FormData()
    fd.append('file', new Blob())
    await importStudents(fd)
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/students/import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  })

  it('exportStudents 以 blob 下载', async () => {
    await exportStudents()
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/export', {
      responseType: 'blob'
    })
  })
})

describe('画像 API — 历史与教师端', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getCompetencyHistory 按能力点查询历史', async () => {
    await getCompetencyHistory('S1', 'C001', 'ap1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/competency/history', {
      params: { abilityPointId: 'ap1' }
    })
  })

  it('getGrowthHistory 成长历史', async () => {
    await getGrowthHistory('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S1/C001/growth/history')
  })

  it('getTestFeedback 触发测试反馈', async () => {
    await getTestFeedback('S1', 'C001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/profile/S1/C001/feedback')
  })

  it('getCourseStudentProfiles 教师端课程学生画像', async () => {
    await getCourseStudentProfiles('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/teacher/course/C001/students')
  })
})

// ======================== 边界情况 ========================

describe('画像 API — 边界', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('空字符串参数不报错', async () => {
    await getProfileSummary('', '')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile//')
  })

  it('特殊字符学号', async () => {
    await getCompetency('S-001_test', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/profile/S-001_test/C001/competency')
  })
})
