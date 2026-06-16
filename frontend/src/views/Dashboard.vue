<template>
  <div>
    <div class="welcome-banner">
      <el-avatar :size="56">{{ user.name.charAt(0) }}</el-avatar>
      <div class="banner-text">
        <h2>欢迎回来，{{ user.name }} <el-tag :type="tagType" size="small">{{ roleText }}</el-tag></h2>
        <p>{{ welcomeText }}</p>
      </div>
    </div>

    <!-- 学生快捷入口 -->
    <el-row v-if="userRole==='student'" :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/courses')">
          <el-icon :size="32" color="#409eff"><Reading /></el-icon>
          <h4>课程列表</h4>
          <p class="card-desc">浏览课程与课时资源</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/stats')">
          <el-icon :size="32" color="#67c23a"><TrendCharts /></el-icon>
          <h4>成绩统计</h4>
          <p class="card-desc">查看我的成绩趋势</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 教师快捷入口 -->
    <el-row v-if="userRole==='teacher' && !isAdmin" :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/courses')">
          <el-icon :size="32" color="#409eff"><Reading /></el-icon>
          <h4>我的课程</h4>
          <p class="card-desc">管理课程内容与课时</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/stats')">
          <el-icon :size="32" color="#67c23a"><TrendCharts /></el-icon>
          <h4>成绩总览</h4>
          <p class="card-desc">查看班级成绩统计</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理员快捷入口 -->
    <el-row v-if="isAdmin" :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/admin/students')">
          <el-icon :size="32" color="#f56c6c"><User /></el-icon>
          <h4>学生管理</h4>
          <p class="card-desc">增删改查学生信息</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/admin/teachers')">
          <el-icon :size="32" color="#909399"><Edit /></el-icon>
          <h4>教师管理</h4>
          <p class="card-desc">管理教师账号</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/admin/courses')">
          <el-icon :size="32" color="#409eff"><Reading /></el-icon>
          <h4>课程管理</h4>
          <p class="card-desc">增删改查全部课程</p>
        </el-card>
      </el-col>
      <el-col :span="8" style="margin-top:20px">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/stats')">
          <el-icon :size="32" color="#67c23a"><TrendCharts /></el-icon>
          <h4>成绩总览</h4>
          <p class="card-desc">全局成绩统计</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const isAdmin = computed(() => userRole === 'admin')
const roleText = computed(() => isAdmin.value ? '管理员' : userRole === 'student' ? '学生' : '教师')
const tagType = computed(() => isAdmin.value ? 'danger' : userRole === 'student' ? 'primary' : 'success')
const welcomeText = computed(() => {
  if (userRole === 'student') return '探索课程、完成任务、提升自我'
  if (isAdmin.value) return '管理平台全部用户与课程资源'
  return '管理课程内容，指导学生成长'
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; padding: 28px 36px; color: #fff;
  display: flex; align-items: center; gap: 20px; margin-bottom: 24px;
}
.banner-text h2 { margin: 0 0 6px 0; font-size: 20px; display: flex; align-items: center; gap: 10px; }
.banner-text p { color: rgba(255,255,255,.8); margin: 0; font-size: 14px; }
.quick-card { cursor: pointer; text-align: center; padding: 20px 0; transition: transform .2s; }
.quick-card:hover { transform: translateY(-4px); }
.quick-card h4 { margin: 10px 0 5px 0; }
.card-desc { color: #999; font-size: 13px; margin: 0; }
</style>
