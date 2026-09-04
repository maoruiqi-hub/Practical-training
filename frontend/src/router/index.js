import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../views/MainLayout.vue'
import { getCourseId } from '../utils/authContext'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { requiresAuth: true }, component: () => import('../views/Dashboard.vue') },
      { path: 'tower-map', name: 'TowerMap', meta: { requiresAuth: true }, component: () => import('../views/TowerMap.vue') },
      { path: 'data-center', name: 'DataCenter', meta: { requiresAuth: true, roles: ['student'] }, component: () => import('../views/DataCenter.vue') },
      { path: 'floor/:kpId', name: 'FloorView', meta: { requiresAuth: true, roles: ['student'] }, component: () => import('../views/FloorView.vue') },
      { path: 'courses', name: 'CourseList', meta: { requiresAuth: true }, component: () => import('../views/CourseList.vue') },
      { path: 'quiz/take/:taskNo', name: 'QuizTake', meta: { requiresAuth: true, roles: ['student'] }, component: () => import('../views/QuizTake.vue') },
      { path: 'course/:code', name: 'CourseDetail', meta: { requiresAuth: true }, component: () => import('../views/CourseDetail.vue') },
      { path: 'course/:code/resources', name: 'CourseResources', meta: { requiresAuth: true }, component: () => import('../views/CourseResourceList.vue') },
      { path: 'course-resource/:resourceId/preview', name: 'CourseResourcePreview', meta: { requiresAuth: true }, component: () => import('../views/CourseResourcePreview.vue') },
      { path: 'course/:code/knowledge-graph', name: 'KnowledgeGraph', meta: { requiresAuth: true }, component: () => import('../views/KnowledgeGraph.vue') },
      { path: 'knowledge-point/:knowledgePointId', name: 'KnowledgePointDetail', meta: { requiresAuth: true }, component: () => import('../views/KnowledgePointDetail.vue') },
      { path: 'lesson/:lessonNo', name: 'LessonDetail', meta: { requiresAuth: true }, component: () => import('../views/LessonDetail.vue') },
      { path: 'task/detail/:taskNo', name: 'TaskDetail', meta: { requiresAuth: true }, component: () => import('../views/TaskDetail.vue') },
      { path: 'task/:courseCode/submit/:taskNo', name: 'TaskSubmit', meta: { requiresAuth: true }, component: () => import('../views/TaskSubmit.vue') },
      { path: 'task/:courseCode', name: 'TaskList', meta: { requiresAuth: true }, component: () => import('../views/TaskList.vue') },
      { path: 'profile', name: 'StudentProfile', meta: { requiresAuth: true, roles: ['student'] }, component: () => import('../views/StudentProfile.vue') },
      { path: 'wrong-book', name: 'WrongBook', meta: { requiresAuth: true, roles: ['student'] }, component: () => import('../views/WrongBook.vue') },
      { path: 'learning-analysis', name: 'LearningAnalysis', meta: { requiresAuth: true }, component: () => import('../views/LearningAnalysis.vue') },
      { path: 'ability-map', name: 'AbilityMap', meta: { requiresAuth: true }, component: () => import('../views/AbilityMapView.vue') },
      { path: 'class-operations', name: 'ClassOperations', meta: { requiresAuth: true, roles: ['teacher', 'admin'] }, component: () => import('../views/ClassOperations.vue') },
      { path: 'stats', name: 'Stats', meta: { requiresAuth: true }, component: () => import('../views/StatsView.vue') },
      { path: 'progress', name: 'Progress', meta: { requiresAuth: true }, component: () => import('../views/ProgressView.vue') },
      { path: 'admin/students', name: 'StudentManage', meta: { requiresAuth: true, roles: ['admin'] }, component: () => import('../views/StudentManage.vue') },
      { path: 'admin/teachers', name: 'TeacherManage', meta: { requiresAuth: true, roles: ['admin'] }, component: () => import('../views/TeacherManage.vue') },
      { path: 'admin/courses', name: 'CourseManage', meta: { requiresAuth: true, roles: ['admin', 'teacher'] }, component: () => import('../views/CourseManage.vue') },
      { path: 'admin/questions', name: 'QuestionManage', meta: { requiresAuth: true, roles: ['admin', 'teacher'] }, component: () => import('../views/QuestionManage.vue') },
      { path: 'teacher/student-profiles', name: 'TeacherStudentProfile', meta: { requiresAuth: true, roles: ['admin', 'teacher'] }, component: () => import('../views/TeacherStudentProfile.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const studentRoomRedirects = {
  '/dashboard': 'start',
  '/stats': 'event',
  '/profile': 'rest',
  '/wrong-book': 'shop',
  '/progress': 'progress',
  '/learning-analysis': 'event'
}

router.beforeEach(to => {
  let user = {}
  try { user = JSON.parse(localStorage.getItem('user') || '{}') || {} } catch { user = {} }
  if (to.meta.requiresAuth && !user.role) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.roles?.length && !to.meta.roles.includes(user.role)) {
    return user.role ? '/dashboard' : { path: '/login', query: { redirect: to.fullPath } }
  }
  const requiresCourse = user.role === 'student' &&
    ['/tower-map', '/data-center', '/profile', '/progress', '/stats', '/wrong-book', '/learning-analysis'].includes(to.path) ||
    (user.role === 'student' && to.path.startsWith('/floor/'))
  if (requiresCourse && !getCourseId(to, { required: true })) {
    return { path: '/courses', query: { redirect: to.fullPath } }
  }
  if (user.role !== 'student') return true

  const room = studentRoomRedirects[to.path]
  if (room) {
    return {
      path: '/tower-map',
      query: { room }
    }
  }
  return true
})

export default router
