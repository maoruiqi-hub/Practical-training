<template>
  <div>
    <template v-if="userRole === 'student'">
      <h3>我的成绩</h3>
      <div v-loading="loading" style="min-height:200px">
        <el-row :gutter="20" v-if="stats">
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="总提交数" :value="stats.totalSubmissions" /></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="已完成" :value="stats.completedCount || 0" /></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="逾期次数" :value="stats.overdueCount || 0" :value-style="{ color: (stats.overdueCount||0) > 0 ? '#f56c6c' : '' }" /></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="平均分" :value="stats.averageScore" :precision="1" suffix="分" /></el-card></el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top:16px" v-if="stats">
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="总学习时长" :value="formatDuration(stats.totalStudyDuration || 0)" /></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><el-statistic title="已批改" :value="stats.gradedCount" /></el-card></el-col>
        </el-row>
        <el-divider />
        <h4>成绩趋势</h4>
        <div ref="chartRef" style="width:100%;height:350px"></div>
        <el-divider />
        <h4>成绩明细</h4>
          <el-table :data="stats?.details" style="width:100%">
            <el-table-column label="任务名称" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.taskName || row.taskType }}</template>
            </el-table-column>
            <el-table-column prop="taskType" label="任务类型" width="120" />
            <el-table-column label="得分" width="80">
              <template #default="{ row }">{{ row.score ?? '-' }}</template>
            </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="逾期" width="80">
            <template #default="{ row }"><el-tag v-if="row.isOverdue" type="danger" size="small">逾期</el-tag><span v-else>-</span></template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="180" />
        </el-table>
      </div>
    </template>

    <template v-else>
      <h3>课程成绩总览</h3>
      <el-select v-model="selectedCourse" placeholder="选择课程" @change="loadCourseStats" style="width:300px;margin-bottom:20px">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="c.courseCode" />
      </el-select>
      <div v-loading="courseLoading" style="min-height:200px">
        <template v-if="courseStats">
          <el-row :gutter="20" style="margin-bottom:20px">
            <el-col :span="6"><el-statistic title="任务数" :value="courseStats.taskCount" /></el-col>
            <el-col :span="6"><el-statistic title="总提交" :value="courseStats.totalSubmissions || 0" /></el-col>
            <el-col :span="6"><el-statistic title="总逾期" :value="courseStats.totalOverdue || 0" :value-style="{ color: (courseStats.totalOverdue||0) > 0 ? '#f56c6c' : '' }" /></el-col>
          </el-row>
          <div ref="courseChartRef" style="width:100%;height:350px;margin-bottom:20px"></div>
          <el-table :data="courseStats.taskStats" style="width:100%">
            <el-table-column label="任务名称" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.taskName || row.taskType }}</template>
            </el-table-column>
            <el-table-column prop="taskType" label="任务类型" width="120" />
            <el-table-column prop="submittedCount" label="提交人数" width="100" />
            <el-table-column prop="gradedCount" label="已评阅" width="100" />
            <el-table-column prop="overdueCount" label="逾期" width="80">
              <template #default="{ row }"><el-tag v-if="row.overdueCount > 0" type="danger" size="small">{{ row.overdueCount }}</el-tag><span v-else>0</span></template>
            </el-table-column>
            <el-table-column label="平均分" width="100">
              <template #default="{ row }">{{ row.averageScore }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-if="!courseStats && !courseLoading" description="请选择课程查看统计" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { getCurrentUser } from '../utils/authContext'
import { ref, onMounted, nextTick } from 'vue'
import { getStudentStats, getCourseStats, searchCourse } from '../api'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const user = getCurrentUser()
const userRole = user.role
const stats = ref(null)
const courses = ref([])
const selectedCourse = ref('')
const courseStats = ref(null)
const chartRef = ref(null)
const courseChartRef = ref(null)
const loading = ref(true)
const courseLoading = ref(false)

let chartInstance = null
let courseChartInstance = null

const formatDuration = (sec) => {
  if (!sec) return '0分钟'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

const drawChart = (details) => {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const scored = details.filter(d => d.score != null)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: scored.map(d => d.taskName || d.taskType) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      data: scored.map(d => d.score), type: 'line', smooth: true,
      label: { show: true }, areaStyle: { opacity: 0.3 }
    }]
  })
}

const drawCourseChart = (taskStats) => {
  if (!courseChartRef.value) return
  if (!courseChartInstance) courseChartInstance = echarts.init(courseChartRef.value)
  courseChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: taskStats.map(t => t.taskName || t.taskType) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ name: '平均分', data: taskStats.map(t => t.averageScore), type: 'bar', label: { show: true } }]
  })
}

onMounted(async () => {
  if (userRole === 'student') {
    try {
      if (!user.studentNo) {
        ElMessage.error('未找到学生信息，请重新登录')
        return
      }
      const res = await getStudentStats(user.studentNo)
      if (res.data.code === 200) {
        stats.value = res.data.data
        await nextTick()
        drawChart(res.data.data.details)
      } else ElMessage.error(res.data.msg)
    } catch {
      ElMessage.error('成绩加载失败')
    } finally { loading.value = false }
  } else {
    loading.value = false
    try {
      const res = await searchCourse('')
      if (res.data.code === 200) {
        courses.value = userRole === 'admin' ? res.data.data : res.data.data.filter(c => c.teacher === user.name)
        if (courses.value.length > 0) {
          selectedCourse.value = courses.value[0].courseCode
          loadCourseStats(selectedCourse.value)
        }
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch {
      ElMessage.error('课程加载失败')
    }
  }
})

const loadCourseStats = async (code) => {
  courseLoading.value = true
  try {
    const res = await getCourseStats(code)
    if (res.data.code === 200) {
      courseStats.value = res.data.data
      await nextTick()
      drawCourseChart(res.data.data.taskStats)
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('课程统计加载失败')
  } finally { courseLoading.value = false }
}
</script>
