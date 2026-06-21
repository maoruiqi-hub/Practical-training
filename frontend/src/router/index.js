import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../views/MainLayout.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'courses', name: 'CourseList', component: () => import('../views/CourseList.vue') },
      { path: 'course/:code', name: 'CourseDetail', component: () => import('../views/CourseDetail.vue') },
      { path: 'lesson/:lessonNo', name: 'LessonDetail', component: () => import('../views/LessonDetail.vue') },
      { path: 'task/detail/:taskNo', name: 'TaskDetail', component: () => import('../views/TaskDetail.vue') },
      { path: 'task/:courseCode/submit/:taskNo', name: 'TaskSubmit', component: () => import('../views/TaskSubmit.vue') },
      { path: 'task/:courseCode', name: 'TaskList', component: () => import('../views/TaskList.vue') },
      { path: 'stats', name: 'Stats', component: () => import('../views/StatsView.vue') },
      { path: 'admin/students', name: 'StudentManage', component: () => import('../views/StudentManage.vue') },
      { path: 'admin/teachers', name: 'TeacherManage', component: () => import('../views/TeacherManage.vue') },
      { path: 'admin/courses', name: 'CourseManage', component: () => import('../views/CourseManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
