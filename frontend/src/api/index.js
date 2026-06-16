import axios from 'axios'

const api = axios.create({
  baseURL: '/practical-training',
  timeout: 10000
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

// ============ 学习任务 ============
export const getTaskList = (code) => api.get(`/task/${code}`)
export const searchTask = (keyword) => api.get('/task/search', { params: { keyword } })
export const addTask = (data) => api.post('/task', data)
export const updateTask = (code, no, data) => api.put(`/task/${code}/${no}`, data)
export const deleteTask = (code, no) => api.delete(`/task/${code}/${no}`)

// ============ 任务提交 ============
export const submitTask = (formData) => api.post('/submission', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getMySubmissions = () => api.get('/submission/my')
export const getSubmissionsByTask = (taskNo) => api.get(`/submission/task/${taskNo}`)
export const gradeSubmission = (id, data) => api.put(`/submission/${id}`, data)

// ============ 成绩统计 ============
export const getStudentStats = (studentNo) => api.get(`/stats/student/${studentNo}`)
export const getCourseStats = (courseCode) => api.get(`/stats/course/${courseCode}`)
