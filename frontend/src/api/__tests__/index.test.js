import { describe, it, expect, vi, beforeEach } from 'vitest'

// ======================== Mock axios 后导入真实 API 模块 ========================

// 使用 vi.hoisted 确保 mock factory 中可访问
const mockAxiosInstance = vi.hoisted(() => ({
  get: vi.fn(() => Promise.resolve({ data: {} })),
  post: vi.fn(() => Promise.resolve({ data: {} })),
  put: vi.fn(() => Promise.resolve({ data: {} })),
  delete: vi.fn(() => Promise.resolve({ data: {} }))
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockAxiosInstance)
  }
}))

// 导入真实 API 模块（axios 已被 mock）
import {
  studentLogin, teacherLogin, getStudentByNo, searchStudent,
  getCourseByCode, addCourse, updateCourse, deleteCourse,
  getTaskList, addTask, deleteTask, toggleTaskStatus,
  getCourseResources, generateAiReview, getAiReview, gradeSubmission,
  generatePaper, getQuestionsByKnowledgePoint, addQuestionsToTask,
  enrollClassStudent, enrollClassStudents, resolveRiskAlert,
  extractKnowledgeCandidates, addKnowledgeRelation, deleteKnowledgeRelation,
  getLeaderboard, getCourseStats,
  // 补充未覆盖的函数
  getStudentRoster, getTowerMap, sendGameEvent,
  getStudentCourseStats, getStudentProgress, getCourseProgress,
  getSubmissionsByTask, getMySubmissions, getGradeDetail,
  getKnowledgeGraph, getKnowledgePointDetail, getKnowledgeRelations,
  searchTask, getTaskDetail, updateTask,
  searchCourse, getCourseLessons, getCourseConfig, updateCourseConfig,
  searchLesson, getLessonList, getLessonDetail, addLesson, updateLesson, deleteLesson,
  getClassList, addClass, updateClass, deleteClass, getClassDetail,
  getClassRiskAlerts, detectClassRisks, generateTeachingSuggestions, getTeachingSuggestions,
  getClassFeedbackSummary, getStudentWrongQuestions, getCourseWrongQuestions,
  removeClassStudent, enrollClassStudentsByClassName,
  getBehaviorLogs, getBehaviorLogsByUser, getBehaviorLogsByTask, reportBehaviorLog,
  searchQuestion, getQuestionById, getQuestionsByCourse, getQuestionsByLesson,
  addQuestion, updateQuestion, deleteQuestion, getTaskQuestions,
  acceptKnowledgeCandidate, rejectKnowledgeCandidate, getKnowledgeExtractionCandidates,
  getAbilityMap, generateAbilityMap, addAbilityPoint, updateAbilityPoint, deleteAbilityPoint,
  bindAbilityKnowledgePoint, unbindAbilityKnowledgePoint,
  searchTeacher, getTeacherList, getTeacherByNo, updateTeacher, deleteTeacher,
  getStudentList, updateStudent, deleteStudent,
  getKnowledgePoints, filterQuestion, bindExamToTask,
  getStudentProfile, submitTask, getCourseResourcePreview, recordCourseResourceView,
  getKnowledgePointPrerequisites
} from '../index'

describe('API 方法调用 — 学生模块', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('studentLogin 调用 POST /api/students/login', async () => {
    await studentLogin({ username: 'S1', password: 'pw' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/students/login', { username: 'S1', password: 'pw' })
  })

  it('teacherLogin 调用 POST /api/teachers/login', async () => {
    await teacherLogin({ username: 'T1', password: 'pw' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/teachers/login', { username: 'T1', password: 'pw' })
  })

  it('getStudentByNo 构造含学号的路径', async () => {
    await getStudentByNo('S2024001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/S2024001')
  })

  it('searchStudent 传递 keyword 参数', async () => {
    await searchStudent('张三')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/search', { params: { keyword: '张三' } })
  })

  it('getLeaderboard 传递课程ID和类型', async () => {
    await getLeaderboard('CS101', 'progress')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/leaderboard', {
      params: { course_id: 'CS101', type: 'progress' }
    })
  })
})

describe('API 方法调用 — 课程模块', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getCourseByCode 按课程编号获取', async () => {
    await getCourseByCode('CS101')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/CS101')
  })

  it('addCourse 发送 POST', async () => {
    await addCourse({ name: 'Python', code: 'CS101' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/courses', { name: 'Python', code: 'CS101' })
  })

  it('updateCourse 路径含课程编号', async () => {
    await updateCourse('CS101', { name: 'Python进阶' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/courses/CS101', { name: 'Python进阶' })
  })

  it('deleteCourse 使用课程编号', async () => {
    await deleteCourse('CS101')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/courses/CS101')
  })

  it('getCourseResources 支持额外查询参数', async () => {
    await getCourseResources('CS101', { type: 'ppt' })
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/resources', {
      params: { type: 'ppt', courseCode: 'CS101' }
    })
  })

  it('getCourseStats 构造统计路径', async () => {
    await getCourseStats('CS101')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/CS101/stats')
  })
})

describe('API 方法调用 — 任务模块', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getTaskList 传递 course_id 参数', async () => {
    await getTaskList('CS101')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/tasks', { params: { course_id: 'CS101' } })
  })

  it('addTask 将对象转换为 FormData 发送', async () => {
    await addTask({ taskName: '作业一', courseCode: 'CS101' })
    expect(mockAxiosInstance.post).toHaveBeenCalled()
    const callArgs = mockAxiosInstance.post.mock.calls[0]
    expect(callArgs[0]).toBe('/api/tasks')
    expect(callArgs[1] instanceof FormData).toBe(true)
    expect(callArgs[1].get('taskName')).toBe('作业一')
  })

  it('addTask 接收 FormData 时直接发送', async () => {
    const formData = new FormData()
    formData.append('taskName', '作业二')
    await addTask(formData)
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/tasks', formData)
  })

  it('deleteTask 路径包含确认参数', async () => {
    await deleteTask('CS101', 'task01', true)
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/tasks/CS101/task01?confirm=true')
  })

  it('deleteTask 不确认时传 false', async () => {
    await deleteTask('CS101', 'task01', false)
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/tasks/CS101/task01?confirm=false')
  })

  it('toggleTaskStatus 发送 PUT 带状态', async () => {
    await toggleTaskStatus('CS101', 'task01', 'active')
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/tasks/CS101/task01/status', { status: 'active' })
  })
})

describe('API 方法调用 — 提交与评分', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('gradeSubmission 发送 PUT 到正确路径', async () => {
    await gradeSubmission('sub001', { score: 85, feedback: '不错' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/submissions/sub001', { score: 85, feedback: '不错' })
  })
})

describe('API 方法调用 — 题库与考试', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('generatePaper 传递课程编号和策略数据', async () => {
    await generatePaper('CS101', { strategy: 'random', count: 10 })
    expect(mockAxiosInstance.post).toHaveBeenCalled()
    const callArgs = mockAxiosInstance.post.mock.calls[0]
    expect(callArgs[0]).toBe('/api/exams/generate')
    expect(callArgs[1]).toEqual({ strategy: 'random', count: 10 })
  })

  it('getQuestionsByKnowledgePoint 传递多个查询参数', async () => {
    await getQuestionsByKnowledgePoint('CS101', 'KP-1', { difficulty: 'hard' })
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions', {
      params: { course_id: 'CS101', knowledge_point_id: 'KP-1', difficulty: 'hard' }
    })
  })

  it('addQuestionsToTask 发送题目ID数组', async () => {
    await addQuestionsToTask('task01', ['q1', 'q2', 'q3'])
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/questions/task/task01', ['q1', 'q2', 'q3'])
  })
})

describe('API 方法调用 — AI 评阅', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('generateAiReview 调用 POST 触发评阅', async () => {
    await generateAiReview('sub001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/submission/ai-review/sub001')
  })

  it('getAiReview 调用 GET 获取评阅结果', async () => {
    await getAiReview('sub001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/submission/ai-review/sub001')
  })
})

describe('API 方法调用 — 班级运营', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('enrollClassStudent 传递 studentId', async () => {
    await enrollClassStudent('class-1', 'S1001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes/class-1/enroll', { studentId: 'S1001' })
  })

  it('enrollClassStudents 批量注册', async () => {
    await enrollClassStudents('class-1', ['S1', 'S2'])
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes/class-1/students/batch', { studentIds: ['S1', 'S2'] })
  })

  it('resolveRiskAlert 发送 PUT', async () => {
    await resolveRiskAlert('alert-1')
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/risk-alerts/alert-1/resolve')
  })
})

describe('API 方法调用 — 知识图谱', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('extractKnowledgeCandidates 传递 resourceId', async () => {
    await extractKnowledgeCandidates('CS101', 'res-1')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/courses/CS101/knowledge-extraction', null, { params: { resourceId: 'res-1' } })
  })

  it('addKnowledgeRelation 发送 POST', async () => {
    await addKnowledgeRelation({ source: 'KP1', target: 'KP2', type: 'prerequisite' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/knowledge-relations', { source: 'KP1', target: 'KP2', type: 'prerequisite' })
  })

  it('deleteKnowledgeRelation 路径含 relationId', async () => {
    await deleteKnowledgeRelation('rel-1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/knowledge-relations/rel-1')
  })
})

// ======================== 补充覆盖 — 学生/进度/课时/班级/行为/题库 ========================

describe('补充覆盖 — 学生进度与游戏事件', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getStudentRoster 有 classId 时传递 class_id', async () => {
    await getStudentRoster('class-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students', { params: { class_id: 'class-1' } })
  })

  it('getStudentRoster 无 classId 时传空 params', async () => {
    await getStudentRoster(null)
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students', { params: {} })
  })

  it('getTowerMap 传递学生ID和课程ID', async () => {
    await getTowerMap('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/S1/tower-map', { params: { course_id: 'C001' } })
  })

  it('sendGameEvent 发送 POST 携带事件体', async () => {
    await sendGameEvent('S1', { type: 'ATTACK', target: 'boss' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/students/S1/game-event', { type: 'ATTACK', target: 'boss' })
  })

  it('getStudentCourseStats 构造正确路径', async () => {
    await getStudentCourseStats('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/student/S1/course/C001')
  })

  it('getStudentProgress 传递 courseCode 参数', async () => {
    await getStudentProgress('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/S1/progress', { params: { courseCode: 'C001' } })
  })

  it('getCourseProgress 构造课程进度路径', async () => {
    await getCourseProgress('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/C001/progress')
  })

  it('getStudentProfile 传递 course_id', async () => {
    await getStudentProfile('S1', 'C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/S1/profile', { params: { course_id: 'C001' } })
  })
})

describe('补充覆盖 — 提交与查询', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getMySubmissions 查询我的提交', async () => {
    await getMySubmissions()
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/submissions/my')
  })

  it('getSubmissionsByTask 查询任务提交列表', async () => {
    await getSubmissionsByTask('task01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/tasks/task01/submissions')
  })

  it('getGradeDetail 查询评分详情', async () => {
    await getGradeDetail('sub001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/submissions/sub001/grade')
  })
})

describe('补充覆盖 — 课程查询与管理', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('searchCourse 传递 keyword', async () => {
    await searchCourse('Python')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/search', { params: { keyword: 'Python' } })
  })

  it('getCourseLessons 获取课程课时列表', async () => {
    await getCourseLessons('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/C001/lessons')
  })

  it('getCourseConfig 获取课程配置', async () => {
    await getCourseConfig('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/C001/config')
  })

  it('updateCourseConfig 更新课程配置', async () => {
    await updateCourseConfig('C001', { maxStudents: 50 })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/courses/C001/config', { maxStudents: 50 })
  })
})

describe('补充覆盖 — 课时管理', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getLessonList 按课程编号获取课时', async () => {
    await getLessonList('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/lessons/C001')
  })

  it('getLessonDetail 获取课时详情', async () => {
    await getLessonDetail('lesson01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/lessons/detail/lesson01')
  })

  it('searchLesson 按关键词搜索', async () => {
    await searchLesson('Python')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/lessons/search', { params: { keyword: 'Python' } })
  })

  it('addLesson 发送 FormData', async () => {
    const fd = new FormData()
    fd.append('name', '第一课')
    await addLesson(fd)
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/lessons', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  })

  it('updateLesson 发送 PUT', async () => {
    const fd = new FormData()
    await updateLesson('C001', 'lesson01', fd)
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/lessons/C001/lesson01', fd)
  })

  it('deleteLesson 发送 DELETE', async () => {
    await deleteLesson('C001', 'lesson01')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/lessons/C001/lesson01')
  })
})

describe('补充覆盖 — 任务查询与更新', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('searchTask 按关键词搜索', async () => {
    await searchTask('作业')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/tasks/search', { params: { keyword: '作业' } })
  })

  it('getTaskDetail 获取任务详情', async () => {
    await getTaskDetail('task01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/tasks/task01')
  })

  it('updateTask 发送 PUT JSON', async () => {
    await updateTask('C001', 'task01', { taskName: '更新作业' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/tasks/C001/task01', { taskName: '更新作业' }, {
      headers: { 'Content-Type': 'application/json' }
    })
  })
})

describe('补充覆盖 — 班级管理', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getClassList 传递 teacherId', async () => {
    await getClassList('T1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes', { params: { teacherId: 'T1' } })
  })

  it('getClassList 无 teacherId', async () => {
    await getClassList(null)
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes', { params: {} })
  })

  it('addClass 新增班级', async () => {
    await addClass({ name: '计科201' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes', { name: '计科201' })
  })

  it('updateClass 更新班级', async () => {
    await updateClass('class-1', { name: '计科202' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/classes/class-1', { name: '计科202' })
  })

  it('deleteClass 删除班级', async () => {
    await deleteClass('class-1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/classes/class-1')
  })

  it('getClassDetail 获取班级详情', async () => {
    await getClassDetail('class-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes/class-1')
  })

  it('removeClassStudent 移除学生', async () => {
    await removeClassStudent('class-1', 'S1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/classes/class-1/students/S1')
  })

  it('enrollClassStudentsByClassName 按班级名导入', async () => {
    await enrollClassStudentsByClassName('class-1', '计科201')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes/class-1/students/by-class', { className: '计科201' })
  })
})

describe('补充覆盖 — 风险预警与建议', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getClassRiskAlerts 获取活跃风险', async () => {
    await getClassRiskAlerts('class-1', 'active')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes/class-1/risk-alerts', { params: { status: 'active' } })
  })

  it('detectClassRisks 触发风险检测', async () => {
    await detectClassRisks('class-1', 'C001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes/class-1/risk-detect', null, { params: { courseId: 'C001' } })
  })

  it('generateTeachingSuggestions 生成教学建议', async () => {
    await generateTeachingSuggestions('class-1', 'C001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/classes/class-1/teaching-suggestions', null, { params: { courseId: 'C001' } })
  })

  it('getTeachingSuggestions 获取已有建议', async () => {
    await getTeachingSuggestions('class-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes/class-1/teaching-suggestions')
  })

  it('getClassFeedbackSummary 获取反馈摘要', async () => {
    await getClassFeedbackSummary('class-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/classes/class-1/feedback-summary')
  })
})

describe('补充覆盖 — 错题本与行为日志', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getStudentWrongQuestions 传递查询参数', async () => {
    await getStudentWrongQuestions('S1', { courseCode: 'C001' })
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/S1/mistakes', { params: { courseCode: 'C001' } })
  })

  it('getCourseWrongQuestions 获取课程错题统计', async () => {
    await getCourseWrongQuestions('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/C001/mistake-stats')
  })

  it('reportBehaviorLog 发送行为日志', async () => {
    await reportBehaviorLog({ action: 'view', target: 'lesson-1' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/learning-logs', { action: 'view', target: 'lesson-1' })
  })

  it('getBehaviorLogs 传递查询参数', async () => {
    await getBehaviorLogs({ userId: 'S1' })
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/learning-logs', { params: { userId: 'S1' } })
  })

  it('getBehaviorLogsByUser 按用户查询', async () => {
    await getBehaviorLogsByUser('S1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/learning-logs/user/S1')
  })

  it('getBehaviorLogsByTask 按任务查询', async () => {
    await getBehaviorLogsByTask('task01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/learning-logs/task/task01')
  })
})

describe('补充覆盖 — 题库管理', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getQuestionById 按ID查询', async () => {
    await getQuestionById('q1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/q1')
  })

  it('getQuestionsByCourse 按课程查询', async () => {
    await getQuestionsByCourse('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/course/C001')
  })

  it('getQuestionsByLesson 按课时查询', async () => {
    await getQuestionsByLesson('lesson01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/lesson/lesson01')
  })

  it('searchQuestion 按关键词搜索', async () => {
    await searchQuestion('递归')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/search', { params: { keyword: '递归' } })
  })

  it('addQuestion 新增题目', async () => {
    await addQuestion({ title: '判断题', type: 'judge' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/questions', { title: '判断题', type: 'judge' })
  })

  it('updateQuestion 更新题目', async () => {
    await updateQuestion('q1', { title: '更新题' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/questions/q1', { title: '更新题' })
  })

  it('deleteQuestion 删除题目', async () => {
    await deleteQuestion('q1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/questions/q1')
  })

  it('getTaskQuestions 获取任务绑定题目', async () => {
    await getTaskQuestions('task01')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/task/task01')
  })

  it('getKnowledgePoints 按课程获取知识点', async () => {
    await getKnowledgePoints('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/knowledge-points', { params: { courseCode: 'C001' } })
  })

  it('filterQuestion 多条件筛选', async () => {
    await filterQuestion({ difficulty: 'hard', type: 'choice' })
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/questions/filter', { params: { difficulty: 'hard', type: 'choice' } })
  })

  it('bindExamToTask 绑定考试到任务', async () => {
    await bindExamToTask('exam1', 'task01')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/exams/exam1/tasks/task01')
  })
})

describe('补充覆盖 — 知识图谱', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getKnowledgeGraph 获取知识图谱', async () => {
    await getKnowledgeGraph('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/knowledge-graph', { params: { courseCode: 'C001' } })
  })

  it('getKnowledgePointDetail 获取知识点详情', async () => {
    await getKnowledgePointDetail('KP-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/knowledge-points/KP-1')
  })

  it('getKnowledgePointPrerequisites 获取前置知识点', async () => {
    await getKnowledgePointPrerequisites('KP-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/knowledge-points/KP-1/prerequisites')
  })

  it('getKnowledgeRelations 获取知识点关联', async () => {
    await getKnowledgeRelations('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/knowledge-relations', { params: { courseCode: 'C001' } })
  })

  it('acceptKnowledgeCandidate 接受候选知识点', async () => {
    await acceptKnowledgeCandidate('C001', 'cand-1', { name: '新知识点' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/courses/C001/knowledge-extraction/cand-1/accept', { name: '新知识点' })
  })

  it('rejectKnowledgeCandidate 拒绝候选知识点', async () => {
    await rejectKnowledgeCandidate('C001', 'cand-1')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/courses/C001/knowledge-extraction/cand-1/reject')
  })

  it('getKnowledgeExtractionCandidates 获取候选列表', async () => {
    await getKnowledgeExtractionCandidates('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/courses/C001/knowledge-extraction')
  })
})

describe('补充覆盖 — 能力图谱', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getAbilityMap 获取能力图谱', async () => {
    await getAbilityMap('C001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/ability-map', { params: { courseCode: 'C001' } })
  })

  it('generateAbilityMap 生成能力图谱', async () => {
    await generateAbilityMap('C001')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/ability-map/generate', null, { params: { courseCode: 'C001' } })
  })

  it('addAbilityPoint 新增能力点', async () => {
    await addAbilityPoint({ name: '算法思维', courseCode: 'C001' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/ability-map', { name: '算法思维', courseCode: 'C001' })
  })

  it('updateAbilityPoint 更新能力点', async () => {
    await updateAbilityPoint('ap1', { name: '高级算法' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/ability-map/ap1', { name: '高级算法' })
  })

  it('deleteAbilityPoint 删除能力点', async () => {
    await deleteAbilityPoint('ap1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/ability-map/ap1')
  })

  it('bindAbilityKnowledgePoint 绑定能力-知识点', async () => {
    await bindAbilityKnowledgePoint('ap1', 'kp1')
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/ability-map/ap1/knowledge-points/kp1')
  })

  it('unbindAbilityKnowledgePoint 解绑能力-知识点', async () => {
    await unbindAbilityKnowledgePoint('ap1', 'kp1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/ability-map/ap1/knowledge-points/kp1')
  })
})

describe('补充覆盖 — 教师与学生管理', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('searchTeacher 按关键词搜索教师', async () => {
    await searchTeacher('王')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/teachers/search', { params: { keyword: '王' } })
  })

  it('getTeacherList 获取全部教师', async () => {
    await getTeacherList()
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/teachers/list')
  })

  it('getTeacherByNo 按工号查询教师', async () => {
    await getTeacherByNo('T001')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/teachers/T001')
  })

  it('updateTeacher 更新教师信息', async () => {
    await updateTeacher('T001', { name: '王老师' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/teachers/T001', { name: '王老师' })
  })

  it('deleteTeacher 删除教师', async () => {
    await deleteTeacher('T001')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/teachers/T001')
  })

  it('getStudentList 获取全部学生', async () => {
    await getStudentList()
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/students/list')
  })

  it('updateStudent 更新学生信息', async () => {
    await updateStudent('S1', { name: '李四' })
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/students/S1', { name: '李四' })
  })

  it('deleteStudent 删除学生', async () => {
    await deleteStudent('S1')
    expect(mockAxiosInstance.delete).toHaveBeenCalledWith('/api/students/S1')
  })
})

describe('补充覆盖 — 资源预览', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('getCourseResourcePreview 获取资源预览', async () => {
    await getCourseResourcePreview('res-1')
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/resources/res-1/preview')
  })

  it('recordCourseResourceView 记录资源查看事件', async () => {
    await recordCourseResourceView('res-1', { studentNo: 'S1' })
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/resources/res-1/view-events', { studentNo: 'S1' })
  })
})

// ======================== FormData 转换逻辑 ========================

describe('addTask FormData 转换细节', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('null 值转为空字符串', async () => {
    await addTask({ desc: null })
    const formData = mockAxiosInstance.post.mock.calls[0][1]
    expect(formData.get('desc')).toBe('')
  })

  it('空对象无多余字段', async () => {
    await addTask({})
    const formData = mockAxiosInstance.post.mock.calls[0][1]
    expect([...formData.keys()].length).toBe(0)
  })
})
