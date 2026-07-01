import axios from 'axios'

const api = axios.create({
  baseURL: '/practical-training',
  timeout: 30000
})

// ============ 学生 (/api/students) ============
export const studentLogin = (data) => api.post('/api/students/login', data)
export const studentRegister = (data) => api.post('/api/students/register', data)
export const getStudentList = () => api.get('/api/students/list')
export const getStudentRoster = (classId) => api.get('/api/students', { params: classId ? { class_id: classId } : {} })
export const getStudentByNo = (no) => api.get(`/api/students/${no}`)
export const updateStudent = (no, data) => api.put(`/api/students/${no}`, data)
export const deleteStudent = (no) => api.delete(`/api/students/${no}`)
export const searchStudent = (keyword) => api.get('/api/students/search', { params: { keyword } })
export const getStudentProfile = (studentId, courseId) =>
  api.get(`/api/students/${studentId}/profile`, { params: { course_id: courseId } })
export const getTowerMap = (studentId, courseId) =>
  api.get(`/api/students/${studentId}/tower-map`, { params: { course_id: courseId } })
export const sendGameEvent = (studentId, body) =>
  api.post(`/api/students/${studentId}/game-event`, body)
export const getLeaderboard = (courseId, type = 'progress') =>
  api.get('/api/leaderboard', { params: { course_id: courseId, type } })

// ============ 教师 (/api/teachers) ============
export const teacherLogin = (data) => api.post('/api/teachers/login', data)
export const teacherRegister = (data) => api.post('/api/teachers/register', data)
export const searchTeacher = (keyword) => api.get('/api/teachers/search', { params: { keyword } })
export const getTeacherList = () => api.get('/api/teachers/list')
export const getTeacherByNo = (no) => api.get(`/api/teachers/${no}`)
export const updateTeacher = (no, data) => api.put(`/api/teachers/${no}`, data)
export const deleteTeacher = (no) => api.delete(`/api/teachers/${no}`)

// ============ 课程 (/api/courses) ============
export const searchCourse = (keyword) => api.get('/api/courses/search', { params: { keyword } })
export const getCourseLessons = (code) => api.get(`/api/courses/${code}/lessons`)
export const getCourseList = () => api.get('/api/courses/list')
export const getCourseByCode = (code) => api.get(`/api/courses/${code}`)
export const addCourse = (data) => api.post('/api/courses', data)
export const updateCourse = (code, data) => api.put(`/api/courses/${code}`, data)
export const deleteCourse = (code) => api.delete(`/api/courses/${code}`)
export const getCourseConfig = (code) => api.get(`/api/courses/${code}/config`)
export const updateCourseConfig = (code, data) => api.put(`/api/courses/${code}/config`, data)

// ============ 课时 (/api/lessons) ============
export const getLessonList = (code) => api.get(`/api/lessons/${code}`)
export const getLessonDetail = (lessonNo) => api.get(`/api/lessons/detail/${lessonNo}`)
export const searchLesson = (keyword) => api.get('/api/lessons/search', { params: { keyword } })
export const addLesson = (formData) => api.post('/api/lessons', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const updateLesson = (code, lessonNo, formData) => api.put(`/api/lessons/${code}/${lessonNo}`, formData)
export const deleteLesson = (code, lessonNo) => api.delete(`/api/lessons/${code}/${lessonNo}`)

// ============ 学习任务 (/api/tasks) ============
export const getTaskList = (code) => api.get('/api/tasks', { params: { course_id: code } })
export const searchTask = (keyword) => api.get('/api/tasks/search', { params: { keyword } })
export const addTask = (data) => {
  if (data instanceof FormData) return api.post('/api/tasks', data)
  const formData = new FormData()
  Object.entries(data).forEach(([key, value]) => formData.append(key, value ?? ''))
  return api.post('/api/tasks', formData)
}
export const assignTask = (taskNo, data) => api.post(`/api/tasks/${taskNo}/assign`, data)
export const getAssignedTasks = (studentNo, params = {}) => api.get(`/api/students/${studentNo}/assigned-tasks`, { params })
export const cancelTaskAssignment = (assignmentId) => api.delete(`/api/task-assignments/${assignmentId}`)
export const getTaskDetail = (taskNo) => api.get('/api/tasks/' + taskNo)
export const updateTask = (code, no, data) => api.put(`/api/tasks/${code}/${no}`, data, {
  headers: { 'Content-Type': 'application/json' }
})
export const deleteTask = (code, no, confirm) => api.delete(`/api/tasks/${code}/${no}?confirm=${confirm ? 'true' : 'false'}`)
export const toggleTaskStatus = (code, no, status) => api.put(`/api/tasks/${code}/${no}/status`, { status })

// ============ 任务提交 (/api/tasks, /api/submissions) ============
export const submitTask = (formData) => api.post(`/api/tasks/${formData.get('taskNo')}/submit`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getMySubmissions = () => api.get('/api/submissions/my')
export const getSubmissionsByTask = (taskNo) => api.get(`/api/tasks/${taskNo}/submissions`)
export const getGradeDetail = (submissionId) => api.get(`/api/submissions/${submissionId}/grade`)
export const gradeSubmission = (id, data) => api.put(`/api/submissions/${id}`, data)

// ============ 成绩统计 (/api) ============
export const getStudentStats = (studentNo) => api.get(`/api/students/${studentNo}/stats`)
export const getStudentCourseStats = (studentNo, courseCode) => api.get(`/api/student/${studentNo}/course/${courseCode}`)
export const getCourseStats = (courseCode) => api.get(`/api/courses/${courseCode}/stats`)

// ============ 学习进度 (/api/*/progress) ============
export const getStudentProgress = (studentNo, courseCode) => api.get(`/api/students/${studentNo}/progress`, { params: { courseCode } })
export const getCourseProgress = (courseCode) => api.get(`/api/courses/${courseCode}/progress`)

// ============ 行为日志 (/api/learning-logs) ============
export const reportBehaviorLog = (data) => api.post('/api/learning-logs', data)
export const getBehaviorLogs = (params) => api.get('/api/learning-logs', { params })
export const getBehaviorLogsByUser = (userId) => api.get('/api/learning-logs/user/' + userId)
export const getBehaviorLogsByTask = (taskNo) => api.get('/api/learning-logs/task/' + taskNo)

// ============ 题库 (/api/questions) ============
export const getQuestionById = (id) => api.get(`/api/questions/${id}`)
export const getQuestionsByCourse = (courseCode) => api.get(`/api/questions/course/${courseCode}`)
export const getQuestionsByLesson = (lessonNo) => api.get(`/api/questions/lesson/${lessonNo}`)
export const searchQuestion = (keyword) => api.get('/api/questions/search', { params: { keyword } })
export const generatePaper = (courseCode, data) => api.post('/api/exams/generate', data, { params: { courseCode } })
export const addQuestion = (data) => api.post('/api/questions', data)
export const updateQuestion = (id, data) => api.put(`/api/questions/${id}`, data)
export const deleteQuestion = (id) => api.delete(`/api/questions/${id}`)
export const getTaskQuestions = (taskNo) => api.get(`/api/questions/task/${taskNo}`)
export const addQuestionsToTask = (taskNo, questionIds) => api.post(`/api/questions/task/${taskNo}`, questionIds)
export const getKnowledgePoints = (courseCode) => api.get('/api/knowledge-points', { params: { courseCode } })
export const filterQuestion = (params) => api.get('/api/questions/filter', { params })

// 兼容旧版课程详情页
export const generateExamVersion = (courseCode, data) => generatePaper(courseCode, data)
export const bindExamToTask = (examId, taskNo) => api.post(`/api/exams/${examId}/tasks/${taskNo}`)

// ============ AI 评阅 ============
export const generateAiReview = (id) => api.post(`/submission/ai-review/${id}`)
export const getAiReview = (id) => api.get(`/submission/ai-review/${id}`)

// ============ 课程资源与知识图谱 (/api/resources, /api/knowledge-*) ============
export const getCourseResources = (courseCode, params = {}) => api.get('/api/resources', {
  params: { ...params, courseCode }
})
export const uploadCourseResource = (data) => api.post('/api/resources', data, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const deleteCourseResource = (resourceId) => api.delete(`/api/resources/${resourceId}`)
export const getCourseResourcePreview = (resourceId) => api.get(`/api/resources/${resourceId}/preview`)
export const recordCourseResourceView = (resourceId, data) => api.post(`/api/resources/${resourceId}/view-events`, data)
export const getKnowledgeGraph = (courseCode) => api.get('/api/knowledge-graph', { params: { courseCode } })
export const getKnowledgePointDetail = (knowledgePointId) => api.get(`/api/knowledge-points/${knowledgePointId}`)
export const getKnowledgePointPrerequisites = (knowledgePointId) => api.get(`/api/knowledge-points/${knowledgePointId}/prerequisites`)
export const getQuestionsByKnowledgePoint = (courseCode, knowledgePointId, params = {}) =>
  api.get('/api/questions', {
    params: { course_id: courseCode, knowledge_point_id: knowledgePointId, ...params }
  })
export const extractKnowledgeCandidates = (courseCode, resourceId) => api.post(`/api/courses/${courseCode}/knowledge-extraction`, null, { params: { resourceId } })
export const getKnowledgeExtractionCandidates = (courseCode) => api.get(`/api/courses/${courseCode}/knowledge-extraction`)
export const acceptKnowledgeCandidate = (courseCode, candidateId, data) => api.post(`/api/courses/${courseCode}/knowledge-extraction/${candidateId}/accept`, data)
export const rejectKnowledgeCandidate = (courseCode, candidateId) => api.post(`/api/courses/${courseCode}/knowledge-extraction/${candidateId}/reject`)
export const getKnowledgeRelations = (courseCode) => api.get('/api/knowledge-relations', { params: { courseCode } })
export const addKnowledgeRelation = (data) => api.post('/api/knowledge-relations', data)
export const deleteKnowledgeRelation = (relationId) => api.delete(`/api/knowledge-relations/${relationId}`)

// ============ 能力图谱 (/api/ability-map) ============
export const getAbilityMap = (courseCode) => api.get('/api/ability-map', { params: { courseCode } })
export const generateAbilityMap = (courseCode) => api.post('/api/ability-map/generate', null, { params: { courseCode } })
export const addAbilityPoint = (data) => api.post('/api/ability-map', data)
export const updateAbilityPoint = (abilityPointId, data) => api.put(`/api/ability-map/${abilityPointId}`, data)
export const deleteAbilityPoint = (abilityPointId) => api.delete(`/api/ability-map/${abilityPointId}`)
export const bindAbilityKnowledgePoint = (abilityPointId, knowledgePointId) => api.post(`/api/ability-map/${abilityPointId}/knowledge-points/${knowledgePointId}`)
export const unbindAbilityKnowledgePoint = (abilityPointId, knowledgePointId) => api.delete(`/api/ability-map/${abilityPointId}/knowledge-points/${knowledgePointId}`)

// ============ 班级运营与风险预警 (/api/classes, /api/risk-alerts) ============
export const getClassList = (teacherId) => api.get('/api/classes', { params: teacherId ? { teacherId } : {} })
export const addClass = (data) => api.post('/api/classes', data)
export const updateClass = (id, data) => api.put(`/api/classes/${id}`, data)
export const deleteClass = (id) => api.delete(`/api/classes/${id}`)
export const getClassDetail = (id) => api.get(`/api/classes/${id}`)
export const enrollClassStudent = (id, studentId) => api.post(`/api/classes/${id}/enroll`, { studentId })
export const enrollClassStudents = (id, studentIds) => api.post(`/api/classes/${id}/students/batch`, { studentIds })
export const enrollClassStudentsByClassName = (id, className) => api.post(`/api/classes/${id}/students/by-class`, { className })
export const removeClassStudent = (id, studentId) => api.delete(`/api/classes/${id}/students/${studentId}`)
export const getClassRiskAlerts = (id, status = 'active') => api.get(`/api/classes/${id}/risk-alerts`, { params: { status } })
export const detectClassRisks = (id, courseId) => api.post(`/api/classes/${id}/risk-detect`, null, { params: { courseId } })
export const resolveRiskAlert = (id) => api.put(`/api/risk-alerts/${id}/resolve`)
export const generateProblemCluster = (classId, courseId) => api.post(`/api/classes/${classId}/problem-cluster`, null, { params: { courseId } })
export const getProblemCluster = (classId) => api.get(`/api/classes/${classId}/problem-cluster`)
export const generateTeachingSuggestions = (classId, courseId) => api.post(`/api/classes/${classId}/teaching-suggestions`, null, { params: { courseId } })
export const getTeachingSuggestions = (classId) => api.get(`/api/classes/${classId}/teaching-suggestions`)
export const generateStudentIntervention = (studentId, courseId) => api.post(`/api/students/${studentId}/intervention`, null, { params: { courseId } })
export const getClassFeedbackSummary = (classId) => api.get(`/api/classes/${classId}/feedback-summary`)
export const exportClassReport = (classId, data) => api.post(`/api/classes/${classId}/reports/export`, data)

// ============ 错题本与学情分析 (/api/*/mistakes) ============
export const getStudentWrongQuestions = (studentNo, params = {}) => api.get(`/api/students/${studentNo}/mistakes`, { params })
export const getCourseWrongQuestions = (courseCode) => api.get(`/api/courses/${courseCode}/mistake-stats`)

export default api
