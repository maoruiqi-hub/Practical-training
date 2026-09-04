<template>
  <router-view v-if="isStudent" />

  <el-container v-else>
    <el-header class="header">
      <span class="title">AI智慧课程平台</span>
      <div class="user-info">
        <el-tag>{{ userRoleLabel }}</el-tag>
        <span style="margin:0 10px">{{ user.name }}</span>
        <el-button type="danger" size="small" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/dashboard"><el-icon><HomeFilled /></el-icon>首页</el-menu-item>
          <el-menu-item index="/courses"><el-icon><Reading /></el-icon>课程列表</el-menu-item>
          <el-menu-item index="/stats"><el-icon><TrendCharts /></el-icon>成绩统计</el-menu-item>
          <el-menu-item index="/profile" v-if="user.role==='student'"><el-icon><User /></el-icon>我的画像</el-menu-item>
          <el-menu-item v-if="!isStudent" index="/learning-analysis"><el-icon><TrendCharts /></el-icon>学情分析</el-menu-item>
          <el-menu-item v-if="!isStudent" index="/class-operations"><el-icon><DataAnalysis /></el-icon>班级运营</el-menu-item>
          <el-menu-item v-if="!isStudent" index="/ability-map"><el-icon><DocumentChecked /></el-icon>能力图谱</el-menu-item>
          <el-menu-item v-if="!isStudent" index="/teacher/student-profiles"><el-icon><User /></el-icon>学生画像</el-menu-item>
          <el-menu-item v-if="isStudent" index="/wrong-book"><el-icon><DocumentChecked /></el-icon>错题本</el-menu-item>
          <el-menu-item index="/progress"><el-icon><DataAnalysis /></el-icon>学习进度</el-menu-item>
          <template v-if="isAdmin">
            <el-sub-menu index="admin">
              <template #title><el-icon><Setting /></el-icon>管理后台</template>
              <el-menu-item index="/admin/students">学生管理</el-menu-item>
              <el-menu-item index="/admin/teachers">教师管理</el-menu-item>
              <el-menu-item index="/admin/courses">课程管理</el-menu-item>
              <el-menu-item index="/admin/questions">题库管理</el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>
      <el-main class="main-view">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, DocumentChecked, HomeFilled, Reading, Setting, TrendCharts, User } from '@element-plus/icons-vue'
import { getCurrentUser } from '../utils/authContext'

const route = useRoute()
const router = useRouter()
const user = getCurrentUser()
const isAdmin = computed(() => user.role === 'admin')
const isStudent = computed(() => user.role === 'student')
const userRoleLabel = computed(() => isAdmin.value ? '管理员' : user.role === 'student' ? '学生' : '教师')

const logout = () => {
  localStorage.removeItem('user')
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
  color: #fff;
  background: #409eff;
}
.header .title { font-size:20px; font-weight:bold; }
.user-info { display:flex; align-items:center; }
.aside { background:#fff; border-right:1px solid #e6e6e6; min-height:calc(100vh - 60px); }
.main-view { background:#f5f7fb; min-height:calc(100vh - 60px); }
</style>
