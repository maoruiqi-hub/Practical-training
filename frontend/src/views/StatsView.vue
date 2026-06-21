<template>
  <div>
    <template v-if="userRole === 'student'">
      <h3>我的成绩</h3>
      <div v-loading="loading" style="min-height:200px">
        <el-row :gutter="20" v-if="stats">
          <el-col :span="8"><el-statistic title="提交数" :value="stats.totalSubmissions" /></el-col>
          <el-col :span="8"><el-statistic title="已批改" :value="stats.gradedCount" /></el-col>
          <el-col :span="8"><el-statistic title="平均分" :value="stats.averageScore" :precision="1" /></el-col>
        </el-row>
        <el-divider />
        <h4>成绩趋势</h4>
        <div ref="chartRef" style="width:100%;height:350px"></div>
        <el-divider />
        <h4>成绩明细</h4>
        <el-table :data="stats?.details" style="width:100%">
          <el-table-column prop="taskType" label="任务类型" width="120" />
          <el-table-column label="得分" width="80">
            <template #default="{ row }">{{ row.score || '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
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
            <el-col :span="12"><el-statistic title="任务数" :value="courseStats.taskCount" /></el-col>
          </el-row>
          <div ref="courseChartRef" style="width:100%;height:350px;margin-bottom:20px"></div>
          <el-table :data="courseStats.taskStats" style="width:100%">
            <el-table-column prop="taskType" label="任务类型" width="120" />
            <el-table-column prop="submittedCount" label="提交人数" width="100" />
            <el-table-column prop="gradedCount" label="已批改" width="100" />
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
import { ref, onMounted, nextTick } from 'vue'
import { getStudentStats, getCourseStats, searchCourse } from '../api'
import * as echarts from 'echarts'

const user = JSON.parse(localStorage.getItem('user') || '{}')
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

const drawChart = (details) => {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const scored = details.filter(d => d.score != null)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: scored.map(d => d.taskType) },
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
    xAxis: { type: 'category', data: taskStats.map(t => t.taskType) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ name: '平均分', data: taskStats.map(t => t.averageScore), type: 'bar', label: { show: true } }]
  })
}

onMounted(async () => {
  if (userRole === 'student') {
    try {
      const res = await getStudentStats(user.studentNo || '1')
      if (res.data.code === 200) {
        stats.value = res.data.data
        await nextTick()
        drawChart(res.data.data.details)
      }
    } finally { loading.value = false }
  } else {
    loading.value = false
    const res = await searchCourse('Python')
    if (res.data.code === 200) {
      courses.value = res.data.data
      if (courses.value.length > 0) {
        selectedCourse.value = courses.value[0].courseCode
        loadCourseStats(selectedCourse.value)
      }
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
    }
  } finally { courseLoading.value = false }
}
</script>
