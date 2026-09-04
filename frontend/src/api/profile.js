import api from './index'

export const getProfileSummary = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}`)

export const getCompetency = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/competency`)

export const getTrueCompetency = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/true-competency`)

export const getRecommendations = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/recommendations`)

export const generateRecommendations = (studentNo, courseCode) =>
    api.post(`/api/profile/${studentNo}/${courseCode}/recommendations/generate`)

export const feedbackRecommendation = (id, feedback) =>
    api.put(`/api/profile/recommendations/${id}/feedback`, null, { params: { feedback } })

export const getAchievements = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/achievements`)

export const getTitle = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/title`)

export const getLeaderboard = (courseCode, type) =>
    api.get('/api/profile/leaderboard', { params: { courseCode, type } })

export const importStudents = (formData) =>
    api.post('/api/students/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const exportStudents = () =>
    api.get('/api/students/export', { responseType: 'blob' })

export const getCompetencyHistory = (studentNo, courseCode, abilityPointId) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/competency/history`, { params: { abilityPointId } })

export const getGrowthHistory = (studentNo, courseCode) =>
    api.get(`/api/profile/${studentNo}/${courseCode}/growth/history`)

export const getTestFeedback = (studentNo, courseCode) =>
    api.post(`/api/profile/${studentNo}/${courseCode}/feedback`)

// 教师端：获取课程下所有学生画像摘要
export const getCourseStudentProfiles = (courseCode) =>
    api.get(`/api/profile/teacher/course/${courseCode}/students`)
