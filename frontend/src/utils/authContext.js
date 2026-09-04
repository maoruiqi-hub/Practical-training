const readJson = key => {
  try {
    return JSON.parse(localStorage.getItem(key) || 'null')
  } catch {
    return null
  }
}

export const getCurrentUser = () => readJson('user') || {}

export const getStudentId = (user = getCurrentUser()) =>
  String(user.studentNo || user.student_no || user.id || '').trim()

export const getTeacherId = (user = getCurrentUser()) =>
  String(user.teacherNo || user.teacher_no || user.id || '').trim()

export const getCourseId = (route, { required = false } = {}) => {
  const query = route?.query || {}
  const params = route?.params || {}
  const value = query.courseId || query.course_id || params.courseId || params.courseCode || params.code || localStorage.getItem('courseId') || ''
  return required && !String(value).trim() ? '' : String(value).trim()
}

export const hasStudentContext = (route, user = getCurrentUser()) =>
  Boolean(getStudentId(user) && getCourseId(route, { required: true }))
