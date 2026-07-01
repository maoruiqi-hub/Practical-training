<template>
  <div class="dashboard-page">
    <!-- 欢迎横幅 -->
    <div v-if="userRole==='student'" class="welcome-banner">
      <el-avatar :size="48" class="welcome-avatar">{{ user.name?.[0] }}</el-avatar>
      <div class="welcome-copy">
        <div class="eyebrow">教师工作台</div>
        <h2>{{ dashboardTitle }}</h2>
        <p v-if="welcomeSubtitle">{{ welcomeSubtitle }}</p>
      </div>
      <div class="welcome-date">{{ todayText }}</div>
    </div>

    <!-- 教师/管理员：首页只负责发现问题 -->
    <section v-if="userRole!=='student'" class="teacher-dashboard" v-loading="pendingLoading" element-loading-text="正在整理待处理事项...">
      <div class="teacher-canvas" :class="focusIssue.level">
        <section class="mission-panel">
          <div class="mission-top">
            <div class="mission-userbox">
              <el-avatar :size="42" class="mission-avatar">{{ user.name?.[0] || '管' }}</el-avatar>
              <div>
                <div class="mission-user">{{ user.name || '管理员' }}</div>
                <div class="mission-role">欢迎回来</div>
              </div>
            </div>
            <div class="mission-date">{{ todayText }}</div>
          </div>

          <div class="mission-content">
            <div class="focus-main">
            <div class="focus-head">
              <div>
                <div class="section-kicker">今日焦点</div>
                <h3>{{ focusIssue.label }}</h3>
              </div>
              <div class="focus-icon"><el-icon><component :is="focusIssue.icon" /></el-icon></div>
            </div>

            <div class="focus-number">
              <span>{{ focusIssue.value }}</span>
              <em>{{ focusIssue.unit }}</em>
            </div>
            <p>{{ focusIssue.description }}</p>
            <div class="focus-meta">{{ focusIssue.meta }}</div>
            <el-button type="primary" plain @click="goIssue(focusIssue)">{{ focusIssue.action }}</el-button>
            </div>

            <div class="signal-strip">
              <div v-for="item in auxiliarySignals" :key="item.key" class="signal-item" :class="item.level">
                <div class="signal-icon"><el-icon><component :is="item.icon" /></el-icon></div>
                <div class="signal-copy">
                  <div class="signal-label">{{ item.label }}</div>
                  <div class="signal-meta">{{ item.meta }}</div>
                </div>
                <div class="signal-value">{{ item.value }}<small>{{ item.unit }}</small></div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel queue-panel">
            <div class="panel-head">
              <div>
                <h3>待处理队列</h3>
                <p>只列出需要教师现在关注的事项</p>
              </div>
              <el-tag :type="priorityCount > 0 ? 'danger' : 'success'" effect="plain">
                {{ priorityCount > 0 ? priorityCount + ' 个优先事项' : '暂无高优先级事项' }}
              </el-tag>
            </div>

            <div v-if="priorityQueue.length" class="queue-list">
              <div v-for="(item, index) in priorityQueue" :key="item.id" class="queue-item" :class="item.level">
                <div class="queue-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="queue-main">
                  <div class="queue-title">
                    <span>{{ item.title }}</span>
                    <em>{{ item.priority }}</em>
                  </div>
                  <div class="queue-desc">{{ item.description }}</div>
                  <div class="queue-meta">{{ item.meta }}</div>
                </div>
                <el-button text type="primary" @click="goIssue(item)">{{ item.action }}</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无队列事项" :image-size="70" />
          </section>

          <section class="panel insight-panel">
            <div class="panel-head compact">
              <div>
                <h3>异常提醒</h3>
                <p>系统从任务、提交和学情中提取异常</p>
              </div>
            </div>

            <div class="anomaly-list">
              <div v-for="item in anomalyCards" :key="item.key" class="observe-row" :class="item.level">
                <span class="observe-marker"></span>
                <div class="observe-copy">
                  <div class="observe-title">{{ item.title }}</div>
                  <div class="observe-desc">{{ item.description }}</div>
                </div>
                <div class="observe-value">{{ item.value }}</div>
                <el-button text type="primary" @click="goIssue(item)">{{ item.action }}</el-button>
              </div>
            </div>
          </section>

          <section class="panel activity-panel">
            <div class="panel-head compact">
              <div>
                <h3>最近变化</h3>
                <p>最新提交与临近截止</p>
              </div>
            </div>

            <div v-if="recentChanges.length" class="activity-list">
              <div v-for="row in recentChanges" :key="row.id" class="observe-row activity-item">
                <el-tag size="small" :type="changeTypeTag(row.type)" effect="plain">{{ row.type }}</el-tag>
                <div class="observe-copy">
                  <div class="observe-title">{{ row.content }}</div>
                  <div class="observe-desc">{{ row.time }} / {{ row.source }}</div>
                </div>
                <el-button text type="primary" @click="goIssue(row)">{{ row.action }}</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无最近变化" :image-size="54" />
          </section>
      </div>
    </section>

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
      <div v-if="showAiReviewPanel" class="ai-review-panel">
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
import { useRouter } from 'vue-router'
import { searchCourse, getGradeDetail, gradeSubmission, getMySubmissions, getTaskList, getSubmissionsByTask, generateAiReview, getAiReview } from '../api'
import { getProfileSummary, getTestFeedback } from '../api/profile'
import { ElMessage } from 'element-plus'
import { DocumentChecked, MagicStick, Timer, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const ONLINE_QUIZ_TYPE = '在线测验'
const isQuizType = type => type === ONLINE_QUIZ_TYPE
const courses = ref([])
const teacherTasks = ref([])
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
const dashboardTitle = computed(() => {
  if (userRole === 'student') return `欢迎回来，${user.name || '同学'}`
  return `${user.name || '老师'}，欢迎回来`
})
const welcomeSubtitle = computed(() => {
  if (userRole === 'student') return summaryText.value
  return ''
})
const todayText = computed(() => new Date().toLocaleDateString('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}))

const now = () => new Date()

const parseDate = value => {
  if (!value) return null
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const dueSoonTasks = computed(() => {
  const current = now().getTime()
  const threeDays = 3 * 24 * 60 * 60 * 1000
  return teacherTasks.value.filter(task => {
    const deadline = parseDate(task.deadline)
    if (!deadline || task.status === 'closed') return false
    const diff = deadline.getTime() - current
    return diff >= 0 && diff <= threeDays
  })
})

const overdueTasks = computed(() => {
  const current = now().getTime()
  return teacherTasks.value.filter(task => {
    const deadline = parseDate(task.deadline)
    return deadline && deadline.getTime() < current && task.status !== 'closed'
  })
})

const riskCount = computed(() => overdueTasks.value.length + pendingSubmissions.value.filter(s => s.isOverdue === 1).length)
const aiReviewCount = computed(() => 0)
const feedbackCount = computed(() => 0)
const priorityCount = computed(() => priorityQueue.value.filter(item => item.level === 'high').length)

const issueStats = computed(() => [
  { key: 'grading', label: '待批改提交', value: pendingCount.value, unit: '份', meta: `来自 ${uniqueCount(pendingSubmissions.value, 'taskNo')} 个任务`, level: pendingCount.value ? 'warning' : 'normal', icon: DocumentChecked },
  { key: 'risk', label: '风险信号', value: riskCount.value, unit: '条', meta: overdueTasks.value.length ? `${overdueTasks.value.length} 个任务逾期` : '暂无逾期集中', level: riskCount.value ? 'danger' : 'normal', icon: WarningFilled },
  { key: 'due', label: '即将截止', value: dueSoonTasks.value.length, unit: '个', meta: '未来 3 天内', level: dueSoonTasks.value.length ? 'warning' : 'normal', icon: Timer },
  { key: 'ai', label: 'AI 待审核', value: aiReviewCount.value, unit: '项', meta: '知识点提取候选', level: aiReviewCount.value ? 'warning' : 'normal', icon: MagicStick }
])

const focusIssue = computed(() => {
  const firstPending = pendingSubmissions.value[0]
  if (pendingCount.value) {
    return {
      ...issueStats.value[0],
      description: `优先处理 ${firstPending?.courseName || '课程'} 中的最新提交，避免成绩反馈继续延迟。`,
      meta: firstPending ? `最近提交：${formatTime(firstPending.submitTime)} / ${firstPending.studentName || '学生'}` : issueStats.value[0].meta,
      action: '查看待批改',
      submission: firstPending
    }
  }

  const firstRisk = overdueTasks.value[0]
  if (riskCount.value) {
    return {
      ...issueStats.value[1],
      description: '存在超过截止时间仍需关注的任务，建议先确认学生进度和补交通道。',
      meta: firstRisk ? `逾期任务：${firstRisk.taskName || firstRisk.description || firstRisk.taskType || '未命名任务'}` : issueStats.value[1].meta,
      action: '查看风险',
      route: firstRisk ? `/task/${firstRisk.courseCode}` : '/progress'
    }
  }

  const firstDue = dueSoonTasks.value[0]
  if (dueSoonTasks.value.length) {
    return {
      ...issueStats.value[2],
      description: '未来 3 天内有任务截止，可以提前查看提交进度并提醒学生。',
      meta: firstDue ? `最近截止：${formatTime(firstDue.deadline)} / ${firstDue.courseName || firstDue.courseCode || '-'}` : issueStats.value[2].meta,
      action: '查看任务',
      route: firstDue ? `/task/${firstDue.courseCode}` : '/progress'
    }
  }

  return {
    key: 'calm',
    label: '今日暂无待办',
    value: courses.value.length || 0,
    unit: '门课程',
    meta: courses.value.length ? '课程数据已同步' : '暂无课程数据',
    level: 'normal',
    icon: DocumentChecked,
    description: '当前没有必须立即处理的批改、逾期或临近截止事项。',
    action: '查看课程',
    route: '/courses'
  }
})

const auxiliarySignals = computed(() => {
  const focusKey = focusIssue.value.key
  return issueStats.value
    .filter(item => item.key !== focusKey)
    .slice(0, 3)
})

const priorityQueue = computed(() => {
  const items = []
  const firstPending = pendingSubmissions.value[0]
  if (firstPending) {
    const count = pendingSubmissions.value.filter(s => s.taskNo === firstPending.taskNo).length
    items.push({
      id: 'pending-' + firstPending.taskNo,
      level: count >= 5 ? 'high' : 'medium',
      priority: count >= 5 ? '高' : '中',
      title: `${firstPending.taskName || '任务提交'}：${count} 份提交待批改`,
      description: `课程：${firstPending.courseName || '-'} / 学生：${firstPending.studentName || '多名学生'}`,
      meta: `最近提交：${formatTime(firstPending.submitTime)}`,
      action: '查看待批改',
      route: null,
      submission: firstPending
    })
  }

  const firstOverdue = overdueTasks.value[0]
  if (firstOverdue) {
    items.push({
      id: 'overdue-' + firstOverdue.taskNo,
      level: 'high',
      priority: '高',
      title: `${firstOverdue.taskName || firstOverdue.description || '任务'} 已超过截止时间`,
      description: `课程：${firstOverdue.courseName || firstOverdue.courseCode || '-'} / 类型：${firstOverdue.taskType || '-'}`,
      meta: `截止时间：${formatTime(firstOverdue.deadline)}`,
      action: '查看任务',
      route: `/task/${firstOverdue.courseCode}`
    })
  }

  const firstDueSoon = dueSoonTasks.value[0]
  if (firstDueSoon) {
    items.push({
      id: 'due-' + firstDueSoon.taskNo,
      level: 'medium',
      priority: '中',
      title: `${firstDueSoon.taskName || firstDueSoon.description || '任务'} 即将截止`,
      description: `课程：${firstDueSoon.courseName || firstDueSoon.courseCode || '-'} / 需要关注提交情况`,
      meta: `截止时间：${formatTime(firstDueSoon.deadline)}`,
      action: '查看任务',
      route: `/task/${firstDueSoon.courseCode}`
    })
  }

  return items.slice(0, 3)
})

const anomalyCards = computed(() => [
  {
    key: 'completion',
    level: overdueTasks.value.length ? 'danger' : 'normal',
    title: '任务完成率异常',
    value: overdueTasks.value.length ? `${overdueTasks.value.length} 个任务逾期` : '暂无异常',
    description: overdueTasks.value.length ? '存在超过截止时间仍需关注的任务' : '当前未发现明显逾期任务',
    action: '查看',
    route: overdueTasks.value[0] ? `/task/${overdueTasks.value[0].courseCode}` : '/progress'
  },
  {
    key: 'weak',
    level: 'normal',
    title: '薄弱知识点升高',
    value: '暂无异常',
    description: '当前没有需要优先处理的知识点波动',
    action: '查看',
    route: '/learning-analysis'
  },
  {
    key: 'score',
    level: pendingCount.value >= 10 ? 'warning' : 'normal',
    title: '低分与复核压力',
    value: pendingCount.value ? `${pendingCount.value} 份待复核` : '暂无压力',
    description: pendingCount.value ? '建议优先处理积压提交，避免成绩反馈延迟' : '当前没有待复核提交',
    action: '查看',
    submission: pendingSubmissions.value[0]
  },
  {
    key: 'late',
    level: riskCount.value ? 'danger' : 'normal',
    title: '风险学生信号',
    value: riskCount.value ? `${riskCount.value} 条信号` : '暂无风险',
    description: riskCount.value ? '建议查看逾期任务与学生进度' : '当前未发现明显风险信号',
    action: '查看',
    route: '/progress'
  }
])

const recentChanges = computed(() => {
  const submissions = pendingSubmissions.value.slice(0, 5).map(sub => ({
    id: 'recent-sub-' + sub.submissionId,
    time: formatTime(sub.submitTime),
    type: '新提交',
    content: `${sub.studentName || '学生'} 提交了《${sub.taskName || sub.taskType || '任务'}》`,
    source: sub.courseName || '-',
    action: '复核',
    submission: sub
  }))

  const dueItems = dueSoonTasks.value.slice(0, 2).map(task => ({
    id: 'recent-due-' + task.taskNo,
    time: formatTime(task.deadline),
    type: '将截止',
    content: `《${task.taskName || task.description || '任务'}》即将截止`,
    source: task.courseName || task.courseCode || '-',
    action: '查看',
    route: `/task/${task.courseCode}`
  }))

  return [...submissions, ...dueItems].slice(0, 3)
})

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
      if (!gradeDetails.value.length || hasAiReviewableQuestions.value) loadAiReview(row.submissionId, false)
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
  if (Array.isArray(value) || typeof value === 'object') return value
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

const hasAiReviewableQuestions = computed(() => {
  return gradeDetails.value.some(item => ['fill', 'essay', 'program'].includes(item.type))
})

const showAiReviewPanel = computed(() => {
  return !gradeLoading.value && (!gradeDetails.value.length || hasAiReviewableQuestions.value)
})

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
  return ''
})

const uniqueCount = (items, key) => new Set(items.map(item => item?.[key]).filter(Boolean)).size

const formatTime = value => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const changeTypeTag = type => {
  if (type === '新提交') return 'warning'
  if (type === '将截止') return 'danger'
  if (type === '风险') return 'danger'
  if (type === 'AI建议') return 'success'
  return 'info'
}

const goIssue = item => {
  if (!item) return
  if (item.submission) {
    openGrade(item.submission)
    return
  }
  if (item.route) router.push(item.route)
}

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
      teacherTasks.value = tasks

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
.dashboard-page {
  max-width: 1280px;
  margin: 0 auto;
  color: #1f2937;
}
.welcome-banner {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.94), rgba(248, 250, 252, 0.98) 54%, rgba(245, 247, 250, 0.92)),
    #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 18px 22px;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}
.welcome-banner::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  width: 36%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(64, 112, 180, 0.08));
  pointer-events: none;
}
.welcome-avatar {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  background: #3568a6;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(53, 104, 166, 0.2);
}
.welcome-copy {
  position: relative;
  z-index: 1;
  min-width: 0;
  flex: 1;
}
.eyebrow {
  margin-bottom: 5px;
  color: #3568a6;
  font-size: 12px;
  font-weight: 700;
}
.welcome-banner h2 {
  margin: 0 0 5px 0;
  font-size: 22px;
  line-height: 1.25;
}
.welcome-banner p {
  margin: 0;
  color: #52616f;
  font-size: 14px;
  line-height: 1.6;
}
.welcome-date {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  padding: 8px 12px;
  border: 1px solid rgba(53, 104, 166, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}
.card-header { display:flex; justify-content:space-between; align-items:center; }
.suggestion-card .stat-item { margin:4px 0; font-size:14px; }
.suggestion-card { border-left: 4px solid #409eff; }
.teacher-dashboard {
  min-height: 420px;
}
.teacher-canvas {
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1fr) minmax(320px, 0.82fr);
  grid-template-areas:
    "mission queue queue"
    "mission insight activity";
  gap: 14px;
  align-items: stretch;
}
.mission-panel {
  grid-area: mission;
  min-height: 0;
  padding: 20px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.94), rgba(255, 255, 255, 0.98) 54%, rgba(245, 247, 250, 0.9)),
    #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}
.mission-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.mission-userbox {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.mission-avatar {
  flex: 0 0 auto;
  background: #3568a6;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(53, 104, 166, 0.16);
}
.mission-user {
  color: #111827;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
}
.mission-role {
  margin-top: 3px;
  color: #7a8794;
  font-size: 12px;
  line-height: 1.35;
}
.mission-date {
  flex: 0 0 auto;
  padding: 6px 10px;
  border: 1px solid rgba(53, 104, 166, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.3;
}
.mission-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.focus-main {
  min-height: 210px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.teacher-canvas.warning .mission-panel {
  border-color: #ead5af;
  background:
    linear-gradient(135deg, rgba(255, 247, 230, 0.95), rgba(255, 255, 255, 0.98) 55%, rgba(239, 246, 255, 0.78)),
    #fff;
}
.teacher-canvas.danger .mission-panel {
  border-color: #efc9c7;
  background:
    linear-gradient(135deg, rgba(255, 241, 241, 0.95), rgba(255, 255, 255, 0.98) 55%, rgba(239, 246, 255, 0.78)),
    #fff;
}
.focus-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}
.section-kicker {
  margin-bottom: 8px;
  color: #3568a6;
  font-size: 12px;
  font-weight: 700;
}
.teacher-canvas.warning .focus-main .section-kicker { color: #a76618; }
.teacher-canvas.danger .focus-main .section-kicker { color: #b93333; }
.focus-head h3 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  line-height: 1.3;
}
.focus-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef4ff;
  color: #3568a6;
  font-size: 21px;
}
.teacher-canvas.warning .focus-icon {
  background: #fff3da;
  color: #b7791f;
}
.teacher-canvas.danger .focus-icon {
  background: #fff1f1;
  color: #c24141;
}
.focus-number {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 14px;
}
.focus-number span {
  color: #0f172a;
  font-size: 44px;
  font-weight: 750;
  line-height: 0.95;
}
.focus-number em {
  color: #64748b;
  font-size: 15px;
  font-style: normal;
  font-weight: 600;
}
.focus-main p {
  max-width: 620px;
  margin: 12px 0 0;
  color: #4b5c6b;
  font-size: 14px;
  line-height: 1.65;
}
.focus-meta {
  margin: 7px 0 14px;
  color: #7a8794;
  font-size: 13px;
  line-height: 1.45;
}
.signal-strip {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  padding-top: 10px;
}
.signal-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}
.signal-item:last-child {
  border-bottom: 0;
}
.signal-icon {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef4ff;
  color: #3568a6;
  font-size: 14px;
}
.signal-item.warning .signal-icon {
  background: #fff3da;
  color: #b7791f;
}
.signal-item.danger .signal-icon {
  background: #fff1f1;
  color: #c24141;
}
.signal-copy {
  min-width: 0;
}
.signal-label {
  color: #4b5c6b;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}
.signal-value {
  color: #111827;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}
.signal-value small {
  margin-left: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}
.signal-meta {
  margin-top: 4px;
  color: #8a96a3;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panel {
  padding: 16px 16px 14px;
  border: 1px solid #e4e9ef;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}
.queue-panel {
  grid-area: queue;
}
.queue-panel .panel-head {
  margin-bottom: 10px;
}
.queue-panel .queue-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-top: 1px solid #e7ebf0;
}
.queue-panel .queue-item {
  min-height: 66px;
}
.queue-panel .queue-item .el-button {
  justify-self: flex-end;
  padding-left: 0;
}
.insight-panel {
  grid-area: insight;
}
.activity-panel {
  grid-area: activity;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-head.compact { margin-bottom: 10px; }
.panel-head h3 {
  margin: 0 0 4px;
  color: #1f2937;
  font-size: 17px;
  line-height: 1.35;
}
.panel-head p {
  margin: 0;
  color: #7a8794;
  font-size: 13px;
  line-height: 1.5;
}
.queue-list {
  display: flex;
  flex-direction: column;
}
.queue-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid #e7ebf0;
  border-radius: 0;
  background: transparent;
  transition: background 0.18s ease, border-color 0.18s ease;
}
.queue-item:last-child {
  border-bottom: 0;
}
.queue-item.high {
  background: transparent;
}
.queue-item.medium {
  background: transparent;
}
.queue-index {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f6f8;
  color: #64748b;
  font-weight: 700;
  font-size: 12px;
}
.queue-item.high .queue-index {
  background: #fff1f1;
  color: #c24141;
}
.queue-item.medium .queue-index {
  background: #fff3da;
  color: #b7791f;
}
.queue-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
  font-weight: 600;
  line-height: 1.35;
  word-break: break-word;
}
.queue-title span {
  min-width: 0;
}
.queue-title em {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 999px;
  background: #f3f6f8;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}
.queue-item.high .queue-title em {
  background: #fff1f1;
  color: #c24141;
}
.queue-item.medium .queue-title em {
  background: #fff3da;
  color: #b7791f;
}
.queue-desc {
  margin-top: 3px;
  color: #5d6b7a;
  font-size: 13px;
  line-height: 1.45;
}
.queue-meta {
  margin-top: 4px;
  color: #8a96a3;
  font-size: 12px;
}
.anomaly-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid #e7ebf0;
}
.observe-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 9px 0;
  border-bottom: 1px solid #e7ebf0;
}
.observe-row:last-child {
  border-bottom: 0;
}
.observe-copy {
  min-width: 0;
}
.observe-title {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.observe-marker {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #9fb2bd;
}
.observe-row.warning .observe-marker { background: #d99026; }
.observe-row.danger .observe-marker { background: #d94d4d; }
.observe-row.normal .observe-marker { background: #6b7c93; }
.observe-row .el-button {
  padding-left: 0;
  padding-right: 0;
}
.observe-value {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}
.observe-desc {
  margin-top: 4px;
  color: #7a8794;
  font-size: 12px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-top: 1px solid #e7ebf0;
}
.activity-item {
  grid-template-columns: 72px minmax(0, 1fr) auto;
}
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
@media (max-width: 960px) {
  .welcome-banner {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 20px;
  }
  .welcome-date {
    margin-left: 64px;
  }
  .teacher-canvas {
    grid-template-columns: 1fr;
    grid-template-areas:
      "mission"
      "queue"
      "insight"
      "activity";
  }
  .mission-content {
    grid-template-columns: 1fr;
  }
  .signal-strip {
    border-left: 0;
    border-top: 1px solid rgba(148, 163, 184, 0.22);
    padding-left: 0;
    padding-top: 10px;
  }
  .signal-item {
    grid-template-columns: 28px minmax(0, 1fr) auto;
    min-height: 48px;
    padding: 8px 0;
    border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  }
  .signal-item:last-child {
    border-bottom: 0;
    padding-bottom: 0;
  }
  .signal-meta {
    white-space: normal;
  }
  .signal-value {
    grid-column: auto;
    font-size: 22px;
  }
  .observe-row {
    grid-template-columns: 10px minmax(0, 1fr) auto;
  }
  .observe-row .el-button {
    grid-column: 2;
    justify-self: flex-start;
  }
  .observe-desc,
  .observe-title {
    white-space: normal;
  }
  .queue-item {
    grid-template-columns: 36px minmax(0, 1fr);
  }
  .queue-item .el-button {
    grid-column: 2;
    justify-self: flex-start;
  }
  .activity-item {
    grid-template-columns: 72px minmax(0, 1fr);
  }
}
</style>
