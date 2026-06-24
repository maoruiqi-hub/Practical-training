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

    <!-- 教师/管理员：待复核列表 -->
    <el-card v-if="userRole!=='student'">
      <template #header>
        <div class="card-header">
          <span>我的任务</span>
          <span style="color:#f56c6c">{{ pendingLoading ? '正在加载待复核任务...' : pendingCount + '个待复核' }}</span>
        </div>
      </template>
      <el-table :data="pendingSubmissions?.slice(0, 10)" size="small" v-loading="pendingLoading" element-loading-text="正在加载待复核任务...">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip />
        <el-table-column label="任务" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.taskName || row.description || row.taskType }}</template>
        </el-table-column>
        <el-table-column label="内容" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content || (row.filePath ? '附件提交' : '无内容') }}
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGrade(row)">{{ row.status === 'graded' ? '查看评阅' : '教师复核' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!pendingLoading && !pendingSubmissions?.length" description="暂无待复核" :image-size="60" />
    </el-card>

    <!-- 系统评阅 / 教师复核弹窗 -->
    <el-dialog v-model="gradeDialog" title="系统评阅 / 教师复核" width="650px" v-loading="gradeLoading">
      <div v-if="gradeDetails.length">
        <div v-for="(d,i) in gradeDetails" :key="i" style="margin-bottom:12px;padding:10px;background:#f9f9f9;border-radius:6px;text-align:left">
          <div><b>{{ i+1 }}.</b> {{ d.stem }} <el-tag size="small" style="margin-left:6px">{{ d.type }}</el-tag> <span style="color:#999;font-size:12px">{{ d.score }}分</span></div>
          <div style="margin-top:4px">📝 学生答案：<span :style="{color:d.studentAnswer===d.correctAnswer?'green':'red'}">{{ d.studentAnswer || '(空)' }}</span></div>
          <div v-if="d.type==='single'||d.type==='multi'" style="color:#67c23a">✅ 正确答案：{{ d.correctAnswer }}</div>
          <div v-else style="color:#909399">✅ 参考答案：{{ d.correctAnswer }}</div>
        </div>
      </div>
      <div v-if="!gradeDetails.length && !gradeLoading" style="text-align:center;color:#999;padding:20px">非测验提交</div>
      <div v-if="!gradeDetails.length" class="ai-review-panel">
        <div class="ai-review-head">
          <h4>智能辅助评价</h4>
          <el-button size="small" type="primary" :loading="aiReviewLoading" @click="requestAiReview">请求智能评价</el-button>
        </div>
        <template v-if="aiReview">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="建议分数">{{ aiReview.aiScore ?? '-' }} 分</el-descriptions-item>
            <el-descriptions-item label="风险等级">{{ riskLabel(aiReview.riskLevel) }}</el-descriptions-item>
            <el-descriptions-item label="评价摘要" :span="2">{{ aiReview.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="dimension-list">
            <div v-for="item in reviewDimensions" :key="item.name" class="dimension-item">
              <span>{{ item.name }}</span>
              <el-progress :percentage="item.score" :stroke-width="8" />
            </div>
          </div>
          <div v-if="reviewSuggestions.length" class="suggestion-box">
            <div v-for="(item, index) in reviewSuggestions" :key="index">- {{ item }}</div>
          </div>
          <el-button size="small" @click="useAiSuggestions">采用建议到评语</el-button>
        </template>
        <el-empty v-else-if="!aiReviewLoading" description="暂无智能评价" :image-size="48" />
      </div>
      <el-form label-width="60px" style="margin-top:16px">
        <el-form-item label="总分"><el-input-number v-model="gradeForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="gradeForm.feedback" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="gradeDialog=false">取消</el-button><el-button type="primary" @click="doGrade">确认复核</el-button></template>
    </el-dialog>

    <!-- 学生：今日学习建议 (R7.4) -->
    <el-card v-if="userRole==='student'" style="margin-bottom:20px" class="suggestion-card">
      <template #header>
        <div class="card-header">
          <span>今日学习建议</span>
          <el-button size="small" @click="loadSuggestion" :loading="suggestionLoading">刷新</el-button>
        </div>
      </template>
      <div v-if="suggestion">
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="stat-item">状态: <el-tag :type="suggestion.status==='正常学习'?'':'warning'">{{ suggestion.status }}</el-tag></div>
            <div class="stat-item">HP: {{ suggestion.hp }} | ATK: {{ suggestion.atk }} | DEF: {{ suggestion.def }}</div>
          </el-col>
          <el-col :span="12">
            <div v-if="suggestion.weakPoints && suggestion.weakPoints.length">
              <p style="margin:0;color:#f56c6c">薄弱知识点:</p>
              <el-tag v-for="wp in suggestion.weakPoints" :key="wp.name" size="small" style="margin:2px">{{ wp.name }}({{ wp.score }})</el-tag>
            </div>
          </el-col>
        </el-row>
        <el-alert :title="suggestion.nextAction" type="info" :closable="false" style="margin-top:12px" show-icon />
      </div>
      <el-empty v-else description="暂无建议数据" :image-size="60" />
    </el-card>

    <!-- 学生：待完成/待评阅 -->
    <el-card v-if="userRole==='student'" style="margin-bottom:20px">
      <template #header>
        <div class="card-header">
          <span>我的任务</span>
          <span style="color:#f56c6c">{{ unsubmittedCount }}个待提交</span>
        </div>
      </template>
      <el-table :data="myTasks" size="small" v-loading="myTasksLoading" element-loading-text="正在加载我的任务..." empty-text="暂无任务">
        <el-table-column label="任务">
          <template #default="{ row }">{{ row.description || row.taskType }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status==='未提交'" type="danger" size="small">未提交</el-tag>
          <el-tag v-else type="warning" size="small">待系统评阅</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status==='未提交'" size="small" type="primary" @click="isQuizType(row.taskType) ? $router.push('/quiz/take/' + row.taskNo) : $router.push('/task/' + row.courseCode + '/submit/' + row.taskNo)">{{ isQuizType(row.taskType) ? '去答题' : '去提交' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!myTasksLoading && !myTasks.length" description="暂无任务" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { searchCourse, getGradeDetail, gradeSubmission, getMySubmissions, getTaskList, getSubmissionsByTask, generateAiReview, getAiReview } from '../api'
import { getProfileSummary, getTestFeedback } from '../api/profile'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const ONLINE_QUIZ_TYPE = '在线测验'
const isQuizType = type => type === ONLINE_QUIZ_TYPE
const courses = ref([])
const myTasks = ref([])
const myTasksLoading = ref(false)
const pendingSubmissions = ref([])
const pendingCount = ref(0)
const pendingLoading = ref(false)
const gradeDialog = ref(false)
const gradeLoading = ref(false)
const gradeDetails = ref([])
const gradeForm = reactive({ submissionId:'', studentName:'', content:'', score:null, feedback:'' })
const aiReview = ref(null)
const aiReviewLoading = ref(false)

const unsubmittedCount = computed(() => myTasks.value.filter(t => t.status === '未提交').length)
const suggestion = ref(null)
const suggestionLoading = ref(false)

const loadSuggestion = async () => {
  suggestionLoading.value = true
  try {
    const studentNo = parseInt(user.studentNo) || 1
    const { data } = await getTestFeedback(studentNo, 1)
    if (data.code === 200) suggestion.value = data.data
  } catch (e) { suggestion.value = null }
  suggestionLoading.value = false
}

const openGrade = async (row) => {
  Object.assign(gradeForm, row); gradeDialog.value = true; gradeLoading.value = true
  gradeDetails.value = []
  aiReview.value = null
  try {
    const res = await getGradeDetail(row.submissionId)
    if (res.data.code === 200) {
      gradeDetails.value = res.data.data.details || []
      gradeForm.score = res.data.data.score ?? row.score ?? null
      gradeForm.feedback = res.data.data.feedback || row.feedback || ''
      if (!gradeDetails.value.length) loadAiReview(row.submissionId, false)
    }
    else ElMessage.error(res.data.msg)
  } catch {
    gradeDetails.value = []
    ElMessage.error('评阅详情加载失败')
  } finally { gradeLoading.value = false }
}

const loadAiReview = async (submissionId, showError = true) => {
  aiReviewLoading.value = true
  try {
    const res = await getAiReview(submissionId)
    if (res.data.code === 200) aiReview.value = res.data.data
    else if (showError) ElMessage.warning(res.data.msg || '暂无智能评价')
  } catch {
    if (showError) ElMessage.error('智能评价加载失败')
  } finally {
    aiReviewLoading.value = false
  }
}

const requestAiReview = async () => {
  if (!gradeForm.submissionId) return
  aiReviewLoading.value = true
  try {
    const res = await generateAiReview(gradeForm.submissionId)
    if (res.data.code === 200) {
      aiReview.value = res.data.data
      if (gradeForm.score == null) gradeForm.score = res.data.data.aiScore
      ElMessage.success('智能评价已生成')
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('智能评价生成失败')
  } finally {
    aiReviewLoading.value = false
  }
}

const parseJson = (value, fallback) => {
  if (!value) return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

const reviewDimensions = computed(() => {
  const dimensions = parseJson(aiReview.value?.dimensions, {})
  return Object.entries(dimensions).map(([name, score]) => ({ name, score: Number(score) || 0 }))
})

const reviewSuggestions = computed(() => parseJson(aiReview.value?.suggestions, []))

const riskLabel = risk => ({ low: '低风险', medium: '中风险', high: '高风险' }[risk] || risk || '-')

const useAiSuggestions = () => {
  const lines = []
  if (aiReview.value?.summary) lines.push(aiReview.value.summary)
  reviewSuggestions.value.forEach(item => lines.push(item))
  gradeForm.feedback = lines.join('\n')
  if (gradeForm.score == null && aiReview.value?.aiScore != null) gradeForm.score = aiReview.value.aiScore
}
const doGrade = async () => {
  try {
    const res = await gradeSubmission(gradeForm.submissionId, { score: gradeForm.score, feedback: gradeForm.feedback, status: 'graded' })
    if (res.data.code === 200) {
      ElMessage.success('已完成复核')
      gradeDialog.value = false
      pendingSubmissions.value = pendingSubmissions.value.filter(s => s.submissionId !== gradeForm.submissionId)
      pendingCount.value = pendingSubmissions.value.length
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('复核失败')
  }
}

const summaryText = computed(() => {
  if (userRole === 'student') return '加油完成今天的学习任务'
  if (pendingLoading.value) return '正在加载待复核任务...'
  return `${pendingCount.value} 份待复核`
})

onMounted(async () => {
  try {
    const r = await searchCourse('')
    if (r.data.code === 200) courses.value = r.data.data
  } catch {
    ElMessage.error('课程加载失败')
  }

  if (userRole === 'student') {
    loadSuggestion()
    // 获取学生已提交记录
    myTasksLoading.value = true
    try {
      const subRes = await getMySubmissions()
      const subs = subRes.data.code === 200 ? subRes.data.data : []
      const taskResults = await Promise.allSettled(courses.value.map(async c => {
        const tRes = await getTaskList(c.courseCode)
        if (tRes.data.code !== 200) return []
        return tRes.data.data.map(t => ({ ...t, courseCode: c.courseCode }))
      }))
      const tasks = taskResults.flatMap(r => r.status === 'fulfilled' ? r.value : [])
      myTasks.value = tasks.reduce((items, t) => {
        const sub = subs.find(s => s.taskNo === t.taskNo)
        if (!sub) {
          items.push({ ...t, status: '未提交', score: null })
        } else if (sub.status === 'submitted') {
          items.push({ ...t, status: 'submitted', score: null })
        }
        return items
      }, [])
    } catch {
      ElMessage.error('我的任务加载失败')
    } finally {
      myTasksLoading.value = false
    }
  } else {
    // 教师：待复核
    pendingLoading.value = true
    try {
      const taskResults = await Promise.allSettled(courses.value.map(async c => {
        const tRes = await getTaskList(c.courseCode)
        if (tRes.data.code !== 200) return []
        return tRes.data.data.map(t => ({ ...t, courseCode: c.courseCode, courseName: c.courseName }))
      }))
      const tasks = taskResults.flatMap(r => r.status === 'fulfilled' ? r.value : [])

      const submissionResults = await Promise.allSettled(tasks.map(async t => {
        const sRes = await getSubmissionsByTask(t.taskNo)
        if (sRes.data.code !== 200) return []
        return sRes.data.data
          .filter(s => s.status !== 'graded')
          .map(s => ({
            ...s,
            courseCode: t.courseCode,
            courseName: t.courseName,
            taskName: s.taskName || t.description,
            taskType: s.taskType || t.taskType
          }))
      }))

      pendingSubmissions.value = submissionResults
        .flatMap(r => r.status === 'fulfilled' ? r.value : [])
        .sort((a, b) => String(b.submitTime || '').localeCompare(String(a.submitTime || '')))
      pendingCount.value = pendingSubmissions.value.length
    } catch {
      ElMessage.error('待复核任务加载失败')
    } finally {
      pendingLoading.value = false
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
.suggestion-card .stat-item { margin:4px 0; font-size:14px; }
.suggestion-card { border-left: 4px solid #409eff; }
.ai-review-panel {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
.ai-review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.ai-review-head h4 { margin: 0; }
.dimension-list { margin-top: 10px; }
.dimension-item {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.suggestion-box {
  margin: 10px 0;
  padding: 10px;
  background: #fff;
  border-radius: 6px;
  color: #606266;
  line-height: 1.7;
}
</style>
