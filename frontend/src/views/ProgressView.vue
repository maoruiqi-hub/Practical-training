<template>
  <div>
    <h3>学习进度</h3>

    <!-- ========== 学生端 ========== -->
    <template v-if="userRole === 'student'">
      <el-select v-model="selectedCourse" placeholder="选择课程" @change="loadStudentProgress" style="width:300px;margin-bottom:20px">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="c.courseCode" />
      </el-select>

      <div v-if="studentProgress" v-loading="sLoading">
        <!-- 概览卡片 -->
        <el-row :gutter="20" style="margin-bottom:20px">
          <el-col :span="6">
            <el-card shadow="hover">
              <el-statistic title="任务完成率" :value="studentProgress.completionRate" suffix="%" />
              <el-progress :percentage="studentProgress.completionRate" :status="studentProgress.completionRate >= 60 ? 'success' : 'exception'" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <el-statistic title="已完成" :value="studentProgress.completedCount + ' / ' + studentProgress.totalTasks" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <el-statistic title="平均分" :value="studentStats?.averageScore || 0" suffix="分" :precision="1" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <el-statistic title="总学习时长" :value="formatDuration(studentStats?.totalStudyDuration || 0)" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 完成率环图 -->
        <el-card style="margin-bottom:20px">
          <div ref="pieChartRef" style="width:100%;height:300px"></div>
        </el-card>

        <!-- 任务状态列表 -->
        <el-card header="任务详情">
          <el-table :data="studentProgress.taskStatusList" style="width:100%">
            <el-table-column prop="taskName" label="任务名称" min-width="160" />
            <el-table-column prop="taskType" label="类型" width="100" />
            <el-table-column prop="deadline" label="截止时间" width="170" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.submissionStatus === 'completed'" type="success">已完成</el-tag>
                <el-tag v-else-if="row.submissionStatus === 'submitted'" type="warning">待批改</el-tag>
                <el-tag v-else-if="row.submissionStatus === 'overdue'" type="danger">逾期提交</el-tag>
                <el-tag v-else-if="row.submissionStatus === 'overdue_missing'" type="danger">逾期未交</el-tag>
                <el-tag v-else type="info">待完成</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="得分" width="80">
              <template #default="{ row }">{{ row.studentScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button v-if="row.submitted" size="small" @click="$router.push(`/task/${selectedCourse}/submit/${row.taskNo}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 最近学习时间线 -->
        <el-card header="最近学习动态" style="margin-top:20px" v-if="studentProgress.timeline?.length">
          <el-timeline>
            <el-timeline-item v-for="(item, i) in studentProgress.timeline" :key="i"
              :timestamp="item.time" placement="top" :type="actionColor(item.actionType)">
              {{ actionLabel(item.actionType) }} · {{ item.resourceType }}
              <span v-if="item.duration" style="color:#909399"> · {{ Math.round(item.duration/60) }}分钟</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>
        <el-empty v-else description="暂无学习动态" />
      </div>
      <el-empty v-if="!studentProgress && !sLoading" description="请选择课程查看进度" />
    </template>

    <!-- ========== 教师端 ========== -->
    <template v-else>
      <el-select v-model="selectedCourse" placeholder="选择课程" @change="loadCourseProgress" style="width:300px;margin-bottom:20px">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="c.courseCode" />
      </el-select>

      <div v-if="courseProgress" v-loading="tLoading">
        <!-- 总览 -->
        <el-row :gutter="20" style="margin-bottom:20px">
          <el-col :span="6">
            <el-card shadow="hover"><el-statistic title="学生总数" :value="courseProgress.totalStudents" /></el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <el-statistic title="任务数" :value="courseProgress.totalTasks" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{ background: courseProgress.laggingCount > 0 ? '#fef0f0' : '' }">
              <el-statistic title="进度落后学生" :value="courseProgress.laggingCount" :value-style="{ color: courseProgress.laggingCount > 0 ? '#f56c6c' : '#67c23a' }" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" :body-style="{ background: courseProgress.riskCount > 0 ? '#fef0f0' : '' }">
              <el-statistic title="逾期风险学生" :value="courseProgress.riskCount" :value-style="{ color: courseProgress.riskCount > 0 ? '#f56c6c' : '#67c23a' }" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 全班进度矩阵表 -->
        <el-card header="全班进度矩阵（学生 × 任务）" body-style="overflow-x:auto;padding:0">
          <el-table :data="courseProgress.studentRows" border stripe
            :row-class-name="rowClassName" max-height="500" style="width:auto;white-space:nowrap">
            <el-table-column prop="studentName" label="学生" width="100" fixed="left" />
            <el-table-column prop="className" label="班级" width="100" />
            <el-table-column label="完成率" width="90" sortable prop="completionRate">
              <template #default="{ row }">
                <span :style="{ color: row.isLagging ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
                  {{ row.completionRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="逾期次数" width="90" sortable prop="overdueCount">
              <template #default="{ row }">
                <el-tag v-if="row.hasOverdueRisk" type="danger" size="small">{{ row.overdueCount }}</el-tag>
                <span v-else>{{ row.overdueCount }}</span>
              </template>
            </el-table-column>
            <el-table-column v-for="t in courseProgress.tasks" :key="t.taskNo" min-width="75" align="center">
              <template #header>
                <el-tooltip :content="t.taskName" placement="top">
                  <span style="font-size:12px">{{ t.taskName }}</span>
                </el-tooltip>
              </template>
              <template #default="{ row: studentRow }">
                <template v-for="cell in studentRow.taskCells" :key="cell.taskNo">
                  <span v-if="cell.taskNo === t.taskNo">
                    <el-tooltip :content="cellStatusText(cell)" placement="top">
                      <span v-if="cell.status === 'completed'" style="color:#67c23a;font-weight:bold;font-size:13px">{{ cell.score ?? '✓' }}</span>
                      <span v-else-if="cell.status === 'submitted'" style="color:#e6a23c;font-size:13px">待批</span>
                      <span v-else-if="cell.status === 'overdue_missing'" style="color:#f56c6c;font-size:13px">✗</span>
                      <span v-else-if="cell.status === 'unassigned'" style="color:#c0c4cc;font-size:13px">未分</span>
                      <span v-else style="color:#c0c4cc">-</span>
                    </el-tooltip>
                  </span>
                </template>
              </template>
            </el-table-column>
            <el-table-column label="最后活跃" width="140" prop="lastActive" fixed="right" />
          </el-table>
          <div style="margin-top:12px;display:flex;gap:20px;padding:0 20px 12px">
            <span><span style="display:inline-block;width:12px;height:12px;background:#f56c6c;border-radius:2px;margin-right:4px"></span> 进度落后（完成率&lt;50%）</span>
            <span><span style="display:inline-block;width:12px;height:12px;background:#e6a23c;border-radius:2px;margin-right:4px"></span> 逾期风险（逾期≥2次）</span>
          </div>
        </el-card>

        <!-- 进度分布图 -->
        <el-card style="margin-top:20px">
          <div ref="barChartRef" style="width:100%;height:300px"></div>
        </el-card>
      </div>
      <el-empty v-if="!courseProgress && !tLoading" description="请选择课程查看进度" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { searchCourse, getStudentProgress, getCourseProgress, getStudentCourseStats } from '../api'
import * as echarts from 'echarts'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const courses = ref([])
const selectedCourse = ref('')
const studentProgress = ref(null)
const courseProgress = ref(null)
const studentStats = ref({})
const sLoading = ref(false)
const tLoading = ref(false)
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChart = null
let barChart = null

const formatDuration = (sec) => {
  if (!sec) return '0分钟'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

const actionLabel = (a) => ({
  video_play: '观看视频', video_pause: '暂停视频', video_seek: '跳转视频', video_complete: '完成视频',
  ppt_view: '浏览PPT', pdf_view: '浏览PDF', quiz_start: '开始答题', quiz_submit: '提交答案',
  resource_download: '下载资源', report_submit: '提交报告', task_view: '查看任务'
}[a] || a)

const actionColor = (a) => {
  if (a?.includes('video') || a?.includes('complete')) return 'success'
  if (a?.includes('submit') || a?.includes('download')) return 'primary'
  return ''
}

const cellStatusText = (cell) => {
  if (cell.status === 'completed') return `已完成 · 得分${cell.score ?? '-'}`
  if (cell.status === 'submitted') return '已提交，待批改'
  if (cell.status === 'overdue_missing') return '逾期未提交'
  if (cell.status === 'unassigned') return '暂未分配'
  return '未开始'
}

const rowClassName = ({ row }) => {
  if (row.isLagging) return 'row-lagging'
  if (row.hasOverdueRisk) return 'row-risk'
  return ''
}

const loadCourses = async () => {
  try {
    const res = await searchCourse('')
    if (res.data.code === 200) courses.value = res.data.data
  } catch { /* ignore */ }
}

const loadStudentProgress = async () => {
  if (!selectedCourse.value) return
  sLoading.value = true
  try {
    const [pRes, sRes] = await Promise.all([
      getStudentProgress(user.studentNo || '1', selectedCourse.value),
      getStudentCourseStats(user.studentNo || '1', selectedCourse.value)
    ])
    if (pRes.data.code === 200) {
      studentProgress.value = pRes.data.data
      await nextTick()
      drawPieChart(pRes.data.data)
    }
    if (sRes.data.code === 200) studentStats.value = sRes.data.data
  } catch (e) { ElMessage.error('进度加载失败') }
  finally { sLoading.value = false }
}

const loadCourseProgress = async () => {
  if (!selectedCourse.value) return
  tLoading.value = true
  try {
    const res = await getCourseProgress(selectedCourse.value)
    if (res.data.code === 200) {
      courseProgress.value = res.data.data
      await nextTick()
      drawBarChart(res.data.data)
    } else ElMessage.error(res.data.msg)
  } catch { ElMessage.error('进度加载失败') }
  finally { tLoading.value = false }
}

const drawPieChart = (data) => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  const completed = data.completedCount || 0
  const remaining = (data.totalTasks || 1) - completed
  pieChart.setOption({
    title: { text: '任务完成进度', left: 'center' },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'],
      label: { formatter: '{b}\n{d}%' },
      data: [
        { name: '已完成', value: completed, itemStyle: { color: '#67c23a' } },
        { name: '待完成', value: Math.max(remaining, 0), itemStyle: { color: '#e0e0e0' } }
      ]
    }]
  })
}

const drawBarChart = (data) => {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)
  const rows = data.studentRows || []
  barChart.setOption({
    title: { text: '学生完成率分布', left: 'center' },
    xAxis: { type: 'category', data: rows.map(r => r.studentName), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', max: 100, name: '完成率(%)' },
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>完成率：${p[0].value}%<br/>已完成：${rows[p[0].dataIndex]?.completedCount}/${rows[p[0].dataIndex]?.totalTasks}` },
    series: [{
      data: rows.map(r => {
        const val = r.completionRate
        return { value: val, itemStyle: { color: val < 50 ? '#f56c6c' : val < 80 ? '#e6a23c' : '#67c23a' } }
      }), type: 'bar', label: { show: true, formatter: '{c}%' }
    }]
  })
}

onMounted(async () => {
  await loadCourses()
  // 默认选中第一个课程
  if (courses.value.length) {
    selectedCourse.value = courses.value[0].courseCode
    if (userRole === 'student') loadStudentProgress()
    else loadCourseProgress()
  }
})
</script>

<style scoped>
:deep(.row-lagging) { background-color: #fef0f0 !important; }
:deep(.row-risk) { background-color: #fdf6ec !important; }
</style>
