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
      { path: 'quiz/take/:taskNo', name: 'QuizTake', component: () => import('../views/QuizTake.vue') },
      { path: 'course/:code', name: 'CourseDetail', component: () => import('../views/CourseDetail.vue') },
      { path: 'course/:code/resources', name: 'CourseResourceList', component: () => import('../views/CourseResourceList.vue') },
      { path: 'course/:code/knowledge-graph', name: 'KnowledgeGraph', component: () => import('../views/KnowledgeGraph.vue') },
      { path: 'knowledge-point/:knowledgePointId', name: 'KnowledgePointDetail', component: () => import('../views/KnowledgePointDetail.vue') },
      { path: 'course-resource/:resourceId/preview', name: 'CourseResourcePreview', component: () => import('../views/CourseResourcePreview.vue') },
      { path: 'lesson/:lessonNo', name: 'LessonDetail', component: () => import('../views/LessonDetail.vue') },
      { path: 'task/detail/:taskNo', name: 'TaskDetail', component: () => import('../views/TaskDetail.vue') },
      { path: 'task/:courseCode/submit/:taskNo', name: 'TaskSubmit', component: () => import('../views/TaskSubmit.vue') },
      { path: 'task/:courseCode', name: 'TaskList', component: () => import('../views/TaskList.vue') },
      { path: 'profile', name: 'StudentProfile', component: () => import('../views/StudentProfile.vue') },
      { path: 'stats', name: 'Stats', component: () => import('../views/StatsView.vue') },
      { path: 'learning-analysis', name: 'LearningAnalysis', component: () => import('../views/LearningAnalysis.vue') },
      { path: 'wrong-book', name: 'WrongBook', component: () => import('../views/WrongBook.vue') },
      { path: 'admin/students', name: 'StudentManage', component: () => import('../views/StudentManage.vue') },
      { path: 'admin/teachers', name: 'TeacherManage', component: () => import('../views/TeacherManage.vue') },
      { path: 'admin/courses', name: 'CourseManage', component: () => import('../views/CourseManage.vue') },
      { path: 'admin/questions', name: 'QuestionManage', component: () => import('../views/QuestionManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
