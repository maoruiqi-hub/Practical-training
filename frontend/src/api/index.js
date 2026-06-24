import axios from 'axios'

const api = axios.create({
  baseURL: '/practical-training',
  timeout: 30000
})

// ============ 学生 ============
export const studentLogin = (data) => api.post('/student/login', data)
export const studentRegister = (data) => api.post('/student/register', data)
export const getStudentList = () => api.get('/student/list')
export const getStudentByNo = (no) => api.get(`/student/${no}`)
export const updateStudent = (no, data) => api.put(`/student/${no}`, data)
export const deleteStudent = (no) => api.delete(`/student/${no}`)

// ============ 教师 ============
export const teacherLogin = (data) => api.post('/teacher/login', data)
export const teacherRegister = (data) => api.post('/teacher/register', data)
export const searchTeacher = (keyword) => api.get('/teacher/search', { params: { keyword } })
export const searchStudent = (keyword) => api.get('/student/search', { params: { keyword } })
export const getTeacherList = () => api.get('/teacher/list')
export const getTeacherByNo = (no) => api.get(`/teacher/${no}`)
export const updateTeacher = (no, data) => api.put(`/teacher/${no}`, data)
export const deleteTeacher = (no) => api.delete(`/teacher/${no}`)

// ============ 课程 ============
export const searchCourse = (keyword) => api.get('/course/search', { params: { keyword } })
export const getCourseLessons = (code) => api.get(`/course/${code}/lessons`)
export const getCourseList = () => api.get('/course/list')
export const getCourseByCode = (code) => api.get(`/course/${code}`)
export const addCourse = (data) => api.post('/course', data)
export const updateCourse = (code, data) => api.put(`/course/${code}`, data)
export const deleteCourse = (code) => api.delete(`/course/${code}`)

// ============ 课时 ============
export const getLessonList = (code) => api.get(`/lesson/${code}`)
export const getLessonDetail = (lessonNo) => api.get(`/lesson/detail/${lessonNo}`)
export const searchLesson = (keyword) => api.get('/lesson/search', { params: { keyword } })
export const addLesson = (formData) => api.post('/lesson', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const updateLesson = (code, lessonNo, formData) => api.put(`/lesson/${code}/${lessonNo}`, formData)
export const deleteLesson = (code, lessonNo) => api.delete(`/lesson/${code}/${lessonNo}`)

// ============ 学习任务 ============
export const getTaskList = (code) => api.get(`/task/${code}`)
export const searchTask = (keyword) => api.get('/task/search', { params: { keyword } })
export const addTask = (data) => {
  if (data instanceof FormData) return api.post('/task', data)
  const formData = new FormData()
  Object.entries(data).forEach(([key, value]) => formData.append(key, value ?? ''))
  return api.post('/task', formData)
}
export const getTaskDetail = (taskNo) => api.get('/task/detail/' + taskNo)
export const updateTask = (code, no, data) => api.put(`/task/${code}/${no}`, data)
export const deleteTask = (code, no) => api.delete(`/task/${code}/${no}`)

// ============ 任务提交 ============
export const submitTask = (formData) => api.post('/submission', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getMySubmissions = () => api.get('/submission/my')
export const getSubmissionsByTask = (taskNo) => api.get(`/submission/task/${taskNo}`)
export const getGradeDetail = (submissionId) => api.get(`/submission/grade/${submissionId}`)
export const gradeSubmission = (id, data) => api.put(`/submission/${id}`, data)
export const generateAiReview = (id) => api.post(`/submission/${id}/ai-review`)
export const getAiReview = (id) => api.get(`/submission/${id}/ai-review`)

// ============ 成绩统计 ============
export const getStudentStats = (studentNo) => api.get(`/stats/student/${studentNo}`)
export const getCourseStats = (courseCode) => api.get(`/stats/course/${courseCode}`)
export const getStudentWrongQuestions = (studentNo, params) => api.get(`/analysis/student/${studentNo}/wrong-questions`, { params })
export const getCourseWrongQuestions = (courseCode) => api.get(`/analysis/course/${courseCode}/wrong-questions`)

// ============ 知识点 / 知识图谱 ============
export const getKnowledgePoints = (courseCode, params) => api.get(`/knowledge/course/${courseCode}/points`, { params })
export const addKnowledgePoint = (courseCode, data) => api.post(`/knowledge/course/${courseCode}/points`, data)
export const updateKnowledgePoint = (pointId, data) => api.put(`/knowledge/points/${pointId}`, data)
export const deleteKnowledgePoint = (pointId) => api.delete(`/knowledge/points/${pointId}`)
export const getKnowledgeGraph = (courseCode) => api.get(`/knowledge/course/${courseCode}/graph`)

// ============ 题库 / 测验 ============
export const getQuestionById = (id) => api.get(`/question/${id}`)
export const getQuestionsByCourse = (courseCode) => api.get(`/question/course/${courseCode}`)
export const getQuestionsByLesson = (lessonNo) => api.get(`/question/lesson/${lessonNo}`)
export const searchQuestion = (keyword) => api.get('/question/search', { params: { keyword } })
export const filterQuestion = (params) => api.get('/question/filter', { params })
export const generatePaper = (courseCode, data) => api.post(`/question/course/${courseCode}/generate`, data)
export const generatePaperVersion = (courseCode, data) => api.post(`/question/course/${courseCode}/paper`, data)
export const bindPaperToTask = (paperId, taskNo) => api.put(`/question/paper/${paperId}/task/${taskNo}`)
export const addQuestion = (data) => api.post('/question', data)
export const updateQuestion = (id, data) => api.put(`/question/${id}`, data)
export const deleteQuestion = (id) => api.delete(`/question/${id}`)
export const getTaskQuestions = (taskNo) => api.get(`/question/task/${taskNo}`)
export const addQuestionsToTask = (taskNo, questionIds) => api.post(`/question/task/${taskNo}`, questionIds)
