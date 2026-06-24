import axios from 'axios'

const api = axios.create({
  baseURL: '/practical-training',
  timeout: 30000
})

// ============ 学生 ============
export const studentLogin = (data) => api.post('/api/students/login', data)
export const studentRegister = (data) => api.post('/api/students/register', data)
export const getStudentList = () => api.get('/api/students/list')
export const getStudentByNo = (no) => api.get(`/api/students/${no}`)
export const updateStudent = (no, data) => api.put(`/api/students/${no}`, data)
export const deleteStudent = (no) => api.delete(`/api/students/${no}`)

// ============ 教师 ============
export const teacherLogin = (data) => api.post('/api/teachers/login', data)
export const teacherRegister = (data) => api.post('/api/teachers/register', data)
export const searchTeacher = (keyword) => api.get('/api/teachers/search', { params: { keyword } })
export const searchStudent = (keyword) => api.get('/api/students/search', { params: { keyword } })
export const getTeacherList = () => api.get('/api/teachers/list')
export const getTeacherByNo = (no) => api.get(`/api/teachers/${no}`)
export const updateTeacher = (no, data) => api.put(`/api/teachers/${no}`, data)
export const deleteTeacher = (no) => api.delete(`/api/teachers/${no}`)

// ============ 课程 ============
export const searchCourse = (keyword) => api.get('/api/courses/search', { params: { keyword } })
export const getCourseLessons = (code) => api.get(`/api/courses/${code}/lessons`)
export const getCourseList = () => api.get('/api/courses/list')
export const getCourseByCode = (code) => api.get(`/api/courses/${code}`)
export const addCourse = (data) => api.post('/api/courses', data)
export const updateCourse = (code, data) => api.put(`/api/courses/${code}`, data)
export const deleteCourse = (code) => api.delete(`/api/courses/${code}`)

// ============ 课程资源 ============
export const getCourseResources = (courseCode, filters = {}) => api.get('/api/resources', { params: { courseCode, ...filters } })
export const getCourseResourcePreview = (resourceId) => api.get(`/api/resources/${resourceId}/preview`)
export const recordCourseResourceView = (resourceId, data) => api.post(`/api/resources/${resourceId}/view-events`, data)

// ============ 知识图谱 ============
export const getKnowledgeGraph = (courseCode) => api.get('/api/knowledge-graph', { params: { courseCode } })
export const getKnowledgePointDetail = (id) => api.get(`/api/knowledge-points/${id}`)
export const getKnowledgePointPrerequisites = (id) => api.get(`/api/knowledge-points/${id}/prerequisites`)
export const getKnowledgeRelations = (courseCode) => api.get('/api/knowledge-relations', { params: { courseCode } })

// ============ 课时 ============
export const getLessonList = (code) => api.get(`/api/lessons/${code}`)
export const getLessonDetail = (lessonNo) => api.get(`/api/lessons/detail/${lessonNo}`)
export const searchLesson = (keyword) => api.get('/api/lessons/search', { params: { keyword } })
export const addLesson = (formData) => api.post('/api/lessons', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const updateLesson = (code, lessonNo, formData) => api.put(`/api/lessons/${code}/${lessonNo}`, formData)
export const deleteLesson = (code, lessonNo) => api.delete(`/api/lessons/${code}/${lessonNo}`)

// ============ 学习任务 ============
export const getTaskList = (code) => api.get(`/api/tasks/${code}`)
export const searchTask = (keyword) => api.get('/api/tasks/search', { params: { keyword } })
export const addTask = (data) => {
  if (data instanceof FormData) return api.post('/api/tasks', data)
  const formData = new FormData()
  Object.entries(data).forEach(([key, value]) => formData.append(key, value ?? ''))
  return api.post('/api/tasks', formData)
}
export const getTaskDetail = (taskNo) => api.get('/api/tasks/detail/' + taskNo)
export const updateTask = (code, no, data) => api.put(`/api/tasks/${code}/${no}`, data)
export const deleteTask = (code, no) => api.delete(`/api/tasks/${code}/${no}`)

// ============ 任务提交 ============
export const submitTask = (formData) => api.post(`/api/tasks/${formData.get('taskNo')}/submit`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getMySubmissions = () => api.get('/api/students/me/submissions')
export const getSubmissionsByTask = (taskNo) => api.get(`/api/tasks/${taskNo}/submissions`)
export const getGradeDetail = (submissionId) => api.get(`/api/submissions/${submissionId}/grade`)
export const gradeSubmission = (id, data) => api.put(`/api/submissions/${id}`, data)
export const generateAiReview = (id) => api.post(`/api/submissions/${id}/ai-review`)
export const getAiReview = (id) => api.get(`/api/submissions/${id}/ai-review`)

// ============ 成绩统计 ============
export const getStudentStats = (studentNo) => api.get(`/api/students/${studentNo}/stats`)
export const getCourseStats = (courseCode) => api.get(`/api/courses/${courseCode}/stats`)
export const getStudentWrongQuestions = (studentNo, params) => api.get(`/api/students/${studentNo}/mistakes`, { params })
export const getCourseWrongQuestions = (courseCode) => api.get(`/api/courses/${courseCode}/mistake-stats`)

// ============ 知识点 / 知识图谱 ============
export const getKnowledgePoints = (courseCode, params = {}) => api.get('/api/knowledge-points', { params: { ...params, courseCode } })
export const addKnowledgePoint = (courseCode, data) => api.post('/api/knowledge-points', { ...data, courseCode })
export const updateKnowledgePoint = (pointId, data) => api.put(`/api/knowledge-points/${pointId}`, data)
export const deleteKnowledgePoint = (pointId) => api.delete(`/api/knowledge-points/${pointId}`)

// ============ 题库 / 测验 ============
export const getQuestionById = (id) => api.get(`/api/questions/${id}`)
export const getQuestionsByCourse = (courseCode) => api.get(`/api/questions/course/${courseCode}`)
export const getQuestionsByLesson = (lessonNo) => api.get(`/api/questions/lesson/${lessonNo}`)
export const searchQuestion = (keyword) => api.get('/api/questions/search', { params: { keyword } })
export const filterQuestion = (params) => api.get('/api/questions/filter', { params })
export const generateExam = (courseCode, data) => api.post('/api/exams/generate', data, { params: { courseCode } })
export const generateExamVersion = (courseCode, data) => api.post('/api/exams', data, { params: { courseCode } })
export const bindExamToTask = (examId, taskNo) => api.put(`/api/exams/${examId}/tasks/${taskNo}`)
export const addQuestion = (data) => api.post('/api/questions', data)
export const updateQuestion = (id, data) => api.put(`/api/questions/${id}`, data)
export const deleteQuestion = (id) => api.delete(`/api/questions/${id}`)
export const getTaskQuestions = (taskNo) => api.get(`/api/questions/task/${taskNo}`)
export const addQuestionsToTask = (taskNo, questionIds) => api.post(`/api/questions/task/${taskNo}`, questionIds)
