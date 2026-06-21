<template>
  <div>
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <el-avatar :size="56">{{ user.name?.[0] }}</el-avatar>
      <div>
        <h2>欢迎回来，{{ user.name }}</h2>
        <p>{{ summaryText }}</p>
      </div>
    </div>

    <!-- 教师/管理员：待批改列表 -->
    <el-card v-if="userRole!=='student'">
      <template #header>
        <div class="card-header">
          <span>我的任务</span>
          <span style="color:#f56c6c">{{ pendingCount }}个待批改</span>
        </div>
      </template>
      <el-table :data="pendingSubmissions?.slice(0, 10)" size="small">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="taskType" label="任务" width="120" />
        <el-table-column label="内容" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content || (row.filePath ? '附件提交' : '无内容') }}
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
      </el-table>
      <el-empty v-if="!pendingSubmissions?.length" description="暂无待批改" :image-size="60" />
    </el-card>

    <!-- 学生：待完成/待批改 -->
    <el-card v-if="userRole==='student'" style="margin-bottom:20px">
      <template #header>
        <div class="card-header">
          <span>我的任务</span>
          <span style="color:#f56c6c">{{ unsubmittedCount }}个待提交</span>
        </div>
      </template>
      <el-table :data="myTasks" size="small">
        <el-table-column prop="taskType" label="任务" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status==='未提交'" type="danger" size="small">未提交</el-tag>
            <el-tag v-else type="warning" size="small">待批改</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status==='未提交'" size="small" type="primary" @click="$router.push('/task/' + row.courseCode + '/submit/' + row.taskNo)">去提交</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!myTasks.length" description="暂无任务" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchCourse } from '../api'
import axios from 'axios'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const courses = ref([])
const myTasks = ref([])
const pendingSubmissions = ref([])
const pendingCount = ref(0)

const unsubmittedCount = computed(() => myTasks.value.filter(t => t.status === '未提交').length)

const summaryText = computed(() => {
  if (userRole === 'student') return '加油完成今天的学习任务'
  return `${pendingCount.value} 份待批改`
})

onMounted(async () => {
  try {
    const r = await searchCourse('Python')
    if (r.data.code === 200) courses.value = r.data.data
  } catch {}

  if (userRole === 'student') {
    // 获取学生已提交记录
    const subRes = await axios.get('/practical-training/submission/my')
    const subs = subRes.data.code === 200 ? subRes.data.data : []
    // 遍历课程拿到所有任务，对比提交状态
    for (const c of courses.value) {
      try {
        const tRes = await axios.get(`/practical-training/task/${c.courseCode}`)
        if (tRes.data.code === 200) {
          for (const t of tRes.data.data) {
            const sub = subs.find(s => s.taskNo === t.taskNo)
            if (!sub) {
              myTasks.value.push({ ...t, courseCode: c.courseCode, status: '未提交', score: null })
            } else if (sub.status === 'submitted') {
              myTasks.value.push({ ...t, courseCode: c.courseCode, status: 'submitted', score: null })
            }
            // 已批改的不显示在首页
          }
        }
      } catch {}
    }
  } else {
    // 教师：待批改
    for (const c of courses.value.slice(0, 5)) {
      try {
        const tRes = await axios.get(`/practical-training/task/${c.courseCode}`)
        if (tRes.data.code === 200) {
          for (const t of tRes.data.data) {
            const sRes = await axios.get(`/practical-training/submission/task/${t.taskNo}`)
            if (sRes.data.code === 200) {
              sRes.data.data.forEach(s => {
                if (s.status !== 'graded') {
                  pendingSubmissions.value.push(s)
                  pendingCount.value++
                }
              })
            }
          }
        }
      } catch {}
    }
  }
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; padding: 28px 32px; color: #fff;
  display: flex; align-items: center; gap: 16px; margin-bottom: 20px;
}
.welcome-banner h2 { margin: 0 0 4px 0; font-size: 20px; }
.welcome-banner p { margin: 0; opacity: .85; font-size: 14px; }
.card-header { display:flex; justify-content:space-between; align-items:center; }
</style>
