<template>
  <div class="task-submit-page" :style="taskPageStyle">
    <div class="task-submit-shell">
    <el-button class="task-back" @click="$router.back()">← 返回活动挑战</el-button>

    <!-- 任务信息卡片 -->
    <el-card v-if="task" class="task-brief">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span><b>{{ task.taskName || task.description }}</b></span>
          <el-tag :type="taskStatusTag.type">{{ taskStatusTag.text }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="任务类型">{{ task.taskType }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">
          <span :style="{ color: isOverdue ? '#f56c6c' : '' }">{{ task.deadline || '不限' }}</span>
          <el-tag v-if="isOverdue" type="danger" size="small" style="margin-left:4px">已截止</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分值">{{ task.score }}分</el-descriptions-item>
        <el-descriptions-item label="提交方式">{{ task.submitMethod }}</el-descriptions-item>
        <el-descriptions-item label="附件要求">{{ task.attachmentFormats || '不限格式' }}</el-descriptions-item>
        <el-descriptions-item label="提交次数">
          {{ task.maxAttempts || 3 }}次
          <span v-if="mySubmissions.length" class="submission-count">（已提交{{ mySubmissions.length }}次）</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="task.gradingRule" label="评分规则" :span="3">{{ task.gradingRule }}</el-descriptions-item>
        <el-descriptions-item v-if="task.resourceUrl" label="附件资源" :span="3">
          <el-link :href="'/practical-training/' + task.resourceUrl" target="_blank" type="primary">📎 下载</el-link>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 学生提交表单 -->
    <template v-if="userRole==='student'">
      <!-- 视频/阅读类：自动记录提示 -->
      <el-result v-if="isAutoType" icon="info" title="系统自动记录学习进度" sub-title="请前往课程资源进行学习，系统将自动记录您的学习时长和完成状态">
        <template #extra>
          <el-button type="primary" @click="$router.push(`/course/${task?.courseCode || route.params.courseCode}`)">前往课程</el-button>
        </template>
      </el-result>

      <!-- 普通任务类：提交表单 -->
      <el-card v-else v-loading="submitting" class="submit-panel">
        <!-- 提交历史 -->
        <div v-if="mySubmissions.length" style="margin-bottom:16px">
          <h4 style="margin-bottom:8px">提交记录</h4>
          <div v-for="(s, idx) in mySubmissions" :key="s.submissionId"
            class="submission-record" :class="{ superseded: s.status === 'superseded', graded: s.status === 'graded' }">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>
                <el-tag :type="s.status === 'superseded' ? 'info' : s.status === 'graded' ? 'success' : 'warning'" size="small">
                  {{ s.status === 'superseded' ? '已覆盖' : s.status === 'graded' ? '已批改' : '待批改' }}
                </el-tag>
                <span style="margin-left:8px;font-weight:500">第{{ s.attemptNumber || idx + 1 }}次提交</span>
                <span class="submission-time">{{ s.submitTime }}</span>
              </span>
                <span>
                  <el-tag v-if="s.status === 'graded' && s.score != null" type="success" size="small">{{ s.score }}分</el-tag>
                  <el-tag v-if="aiReviewBySubmission[s.submissionId]" :type="aiReviewTag(aiReviewBySubmission[s.submissionId])" size="small" style="margin-left:6px">
                    {{ aiReviewLabel(aiReviewBySubmission[s.submissionId]) }}
                  </el-tag>
                <el-link v-if="s.filePath" :href="'/practical-training/' + s.filePath" target="_blank" type="primary" :underline="false" style="margin-left:6px">📎</el-link>
                <span v-if="s.status !== 'superseded' && idx === 0" class="teacher-visible">← 教师可见</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 逾期警告 -->
        <el-alert v-if="isOverdue && !task?.allowLate" title="任务已截止，不允许逾期提交" type="error" :closable="false" style="margin-bottom:16px" />

        <!-- 提交次数达上限 -->
        <el-alert v-if="!canResubmit" :title="'已达最大提交次数（' + maxAttempts + '次），如需修改请联系教师'" type="warning" :closable="false" style="margin-bottom:16px" />

        <!-- 重新提交提示 -->
        <el-alert v-if="canResubmit && mySubmissions.length > 0" :title="'您可以重新提交（还有 ' + (maxAttempts - mySubmissions.length) + ' 次机会），新提交将覆盖旧提交'" type="info" :closable="false" style="margin-bottom:16px" />

        <!-- 提交/修改表单 -->
        <el-form label-width="100px" style="margin-top:16px">
          <el-form-item label="文字内容">
            <el-input v-model="content" type="textarea" :rows="5" placeholder="请输入提交内容或说明" :disabled="!canSubmit" />
          </el-form-item>
          <el-form-item :label="'上传文件'">
            <div style="display:flex;flex-direction:column;gap:8px">
              <el-upload :auto-upload="false" :limit="1" :on-change="handleFile" :accept="task?.attachmentFormats || ''" :disabled="!canSubmit">
                <el-button type="primary" :disabled="!canSubmit">选择文件</el-button>
              </el-upload>
              <span v-if="task?.attachmentFormats" class="attachment-hint">仅支持：{{ task.attachmentFormats }}</span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="submit" :disabled="!canSubmit" style="width:100%" size="large">
              {{ !canSubmit ? (isOverdue && !task?.allowLate ? '已截止，不可提交' : '已达最大提交次数') : mySubmissions.length > 0 ? '修改并重新提交（第' + (mySubmissions.length + 1) + '次）' : isOverdue && task?.allowLate ? '逾期提交' : '提交作业' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <!-- 教师查看提交列表 -->
    <template v-else>
      <h4>学生提交列表（共{{ submissions.length }}条）</h4>
      <el-table :data="submissions" style="width:100%" v-loading="subLoading" element-loading-text="正在加载..." empty-text="暂无提交记录">
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="attemptNumber" label="第几次" width="80">
          <template #default="{ row }">第{{ row.attemptNumber || 1 }}次</template>
        </el-table-column>
        <el-table-column label="提交内容" show-overflow-tooltip min-width="180">
          <template #default="{ row }">
            <span v-if="row.content">{{ row.content }}</span>
            <el-link v-if="row.filePath" :href="'/practical-training/' + row.filePath" target="_blank" type="primary" style="margin-left:5px">📎文件</el-link>
            <span v-if="!row.content && !row.filePath">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="逾期" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isOverdue" type="danger" size="small">逾期</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80">
          <template #default="{ row }">{{ row.score ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status==='graded'?'success':'warning'">{{ gradeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">{{ row.status==='graded' ? '查看' : '复核' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 系统评阅 / 教师复核弹窗 -->
    <el-dialog v-model="dialogVisible" title="提交详情" width="720px" v-loading="gradeLoading">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="学生">{{ grading.studentName }}</el-descriptions-item>
        <el-descriptions-item label="提交次数">第{{ grading.attemptNumber || 1 }}次</el-descriptions-item>
        <el-descriptions-item label="是否逾期">
          <el-tag v-if="grading.isOverdue" type="danger">逾期提交</el-tag>
          <span v-else>按时提交</span>
        </el-descriptions-item>
        <el-descriptions-item label="系统评阅">{{ systemScoreText }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ grading.status === 'graded' ? '已完成' : '待教师复核' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="gradeDetails.length" style="margin-top:16px">
        <div v-for="(d,i) in gradeDetails" :key="i" class="grade-question">
          <div><b>{{ i + 1 }}.</b> {{ d.stem }} <el-tag size="small" style="margin-left:6px">{{ typeLabel(d.type) }}</el-tag> <span class="question-score">{{ d.score }}分</span></div>
          <div class="student-answer">学生答案：<span :class="{ correct: d.autoGradable && d.correct, incorrect: d.autoGradable && !d.correct }">{{ d.studentAnswer || '(空)' }}</span></div>
          <div class="correct-answer">{{ d.autoGradable ? '正确答案' : '参考答案' }}：{{ d.correctAnswer || '(无)' }}</div>
          <div v-if="!d.autoGradable" class="manual-grade-row">
            <span>本题得分</span>
            <el-input-number v-model="d.earnedScore" :min="0" :max="d.score || 0" size="small" />
            <span>是否达标</span>
            <el-switch v-model="d.correct" inline-prompt active-text="是" inactive-text="否" />
          </div>
        </div>
      </div>
      <div v-else-if="!gradeLoading" class="non-quiz-note">非在线测验提交：{{ grading.content || '附件提交' }}</div>
      <div v-if="showAiReviewPanel" class="ai-review-panel">
        <div class="ai-review-head">
          <h4>AI 智能评价</h4>
          <div>
            <el-button size="small" :loading="aiReviewLoading" @click="loadAiReview(grading.submissionId)">查看已有评价</el-button>
            <el-button size="small" type="primary" :loading="aiReviewLoading" @click="requestAiReview">请求智能评价</el-button>
          </div>
        </div>
        <template v-if="aiReview">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="建议分数">{{ aiReview.aiScore ?? '-' }} 分</el-descriptions-item>
            <el-descriptions-item label="风险等级">{{ riskLabel(aiReview.riskLevel) }}</el-descriptions-item>
            <el-descriptions-item label="评价状态">{{ aiReviewLabel(aiReview) }}</el-descriptions-item>
            <el-descriptions-item label="可信度">{{ aiReview.confidence == null ? '-' : Math.round(aiReview.confidence * 100) + '%' }}</el-descriptions-item>
            <el-descriptions-item label="评价摘要" :span="2">{{ aiReview.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="reviewSuggestions.length" class="ai-suggestions">
            <div v-for="(item, index) in reviewSuggestions" :key="index">- {{ item }}</div>
          </div>
          <el-button size="small" type="success" plain @click="applyAiReview">采用到复核表单</el-button>
        </template>
        <el-empty v-else description="暂无智能评价" :image-size="48" />
      </div>
      <el-form label-width="80px" style="margin-top:20px" v-if="grading.status !== 'graded' || gradeDetails.some(d => !d.autoGradable)">
        <el-form-item label="最终得分"><el-input-number v-model="grading.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="反馈"><el-input v-model="grading.feedback" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button v-if="grading.status !== 'graded' || gradeDetails.some(d => !d.autoGradable)" type="primary" @click="doGrade">确认复核</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { getCurrentUser, getStudentId } from '../utils/authContext'
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitTask, getSubmissionsByTask, getGradeDetail, gradeSubmission, getTaskDetail, getMySubmissions, generateAiReview, getAiReview } from '../api'
import { gameBackgrounds } from '../data/gameAssetManifest'

const route = useRoute()
const taskNo = route.params.taskNo
const user = getCurrentUser()
const userRole = user.role
const taskPageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .2), rgba(6, 8, 12, .82)), url(${gameBackgrounds.mapAct1})`
}))
const task = ref(null)
const content = ref('')
const file = ref(null)
const submitting = ref(false)
const submissions = ref([])
const subLoading = ref(true)
const mySubmissions = ref([])
const aiReviewBySubmission = ref({})
const dialogVisible = ref(false)
const grading = ref({ submissionId: '', studentName: '', content: '', score: null, feedback: '' })
const gradeLoading = ref(false)
const gradeDetails = ref([])
const aiReview = ref(null)
const aiReviewLoading = ref(false)
const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答',program:'编程'}[t]||t)

const isOverdue = computed(() => {
  if (!task.value?.deadline) return false
  return new Date(task.value.deadline) < new Date()
})

const isAutoType = computed(() => {
  return task.value?.taskType === 'video' || task.value?.taskType === 'reading'
})

const lastSubmission = computed(() => {
  return mySubmissions.value.length ? mySubmissions.value[0] : null
})

const maxAttempts = computed(() => task.value?.maxAttempts || 3)

const canResubmit = computed(() => {
  if (!task.value) return false
  return mySubmissions.value.length < maxAttempts.value
})

const canSubmit = computed(() => {
  if (!task.value) return false
  if (task.value.status === 'closed') return false
  if (isOverdue.value && !task.value.allowLate) return false
  if (!canResubmit.value) return false
  if (isAutoType.value) return false
  return true
})

const taskStatusTag = computed(() => {
  if (!task.value) return { type: '', text: '' }
  if (task.value.status === 'closed') return { type: 'info', text: '已关闭' }
  if (isOverdue.value) return { type: 'danger', text: '已截止' }
  if (lastSubmission.value?.status === 'graded') return { type: 'success', text: '已完成' }
  if (lastSubmission.value) return { type: 'warning', text: '已提交' }
  return { type: '', text: '进行中' }
})

const statusText = (s) => ({ submitted: '待批改', graded: '已完成', returned: '已打回' }[s] || s)
const gradeLabel = (row) => {
  if (row.status === 'graded') return '已完成'
  if (row.taskType === 'quiz' || row.taskType === '在线测验') return '待复核'
  return '待教师评阅'
}

const handleFile = (f) => { file.value = f.raw }

const submit = async () => {
  submitting.value = true
  try {
    const fd = new FormData(); fd.append('taskNo', taskNo)
    if (content.value) fd.append('content', content.value)
    if (file.value) fd.append('file', file.value)
    const res = await submitTask(fd)
    if (res.data.code === 200) {
      ElMessage.success('提交成功')
      content.value = ''; file.value = null
      loadMySubmissions()
      loadTask()
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('提交失败')
  } finally { submitting.value = false }
}

const loadTask = async () => {
  try {
    const res = await getTaskDetail(taskNo)
    if (res.data.code === 200) task.value = res.data.data
  } catch { /* ignore */ }
}

const loadMySubmissions = async () => {
  if (userRole !== 'student') return
  try {
    const res = await getMySubmissions()
    if (res.data.code === 200) {
      mySubmissions.value = (res.data.data || [])
        .filter(s => String(s.taskNo) === String(taskNo))
        .sort((a, b) => new Date(b.submitTime) - new Date(a.submitTime))
      await loadMyAiReviews()
    }
  } catch { /* ignore */ }
}

const loadMyAiReviews = async () => {
  const entries = await Promise.all(mySubmissions.value.map(async submission => {
    try {
      const res = await getAiReview(submission.submissionId)
      return [submission.submissionId, res.data.code === 200 ? res.data.data : null]
    } catch { return [submission.submissionId, null] }
  }))
  aiReviewBySubmission.value = Object.fromEntries(entries.filter(([, review]) => review))
}

const openGradeDialog = async (row) => {
  grading.value = { ...row, score: row.score ?? null, feedback: row.feedback || '' }
  gradeDetails.value = []
  aiReview.value = null
  dialogVisible.value = true
  gradeLoading.value = true
  try {
    const res = await getGradeDetail(row.submissionId)
    if (res.data.code === 200) {
      gradeDetails.value = (res.data.data.details || []).map(item => ({
        ...item,
        earnedScore: item.earnedScore ?? 0,
        correct: item.correct === true
      }))
      grading.value.score = res.data.data.score ?? row.score ?? null
      grading.value.feedback = res.data.data.feedback || row.feedback || ''
      grading.value.status = res.data.data.status || row.status
      if (!gradeDetails.value.length) loadAiReview(row.submissionId, false)
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('评阅详情加载失败')
  } finally { gradeLoading.value = false }
}

const doGrade = async () => {
  try {
    const res = await gradeSubmission(grading.value.submissionId, {
      score: grading.value.score,
      feedback: grading.value.feedback,
      manualAnswers: gradeDetails.value.filter(item => !item.autoGradable).map(item => ({
        questionId: item.questionId,
        score: item.earnedScore,
        correct: item.correct === true
      }))
    })
    if (res.data.code === 200) { ElMessage.success('已完成复核'); dialogVisible.value = false; reloadSubmissions() }
    else ElMessage.error(res.data.msg)
  } catch { ElMessage.error('复核失败') }
}

const systemScoreText = computed(() => {
  if (!gradeDetails.value.length) return '不适用'
  if (grading.value.score == null) return grading.value.status === 'graded' ? '暂无系统评分' : '待教师复核'
  return `${grading.value.score} 分`
})

const hasAiReviewableQuestions = computed(() => {
  return gradeDetails.value.some(item => ['fill', 'essay', 'program'].includes(item.type))
})

const showAiReviewPanel = computed(() => {
  return !gradeLoading.value && (!gradeDetails.value.length || hasAiReviewableQuestions.value)
})

const parseJson = (value, fallback) => {
  if (!value) return fallback
  if (Array.isArray(value) || typeof value === 'object') return value
  try { return JSON.parse(value) } catch { return fallback }
}

const reviewSuggestions = computed(() => parseJson(aiReview.value?.suggestions, []))
const riskLabel = level => ({ low: '低', medium: '中', high: '高' }[level] || level || '-')
const aiReviewLabel = review => ({
  accepted: '已自动评价', generated: '已生成待复核', needs_review: '待教师复核',
  pending_review: '等待评价', provider_error: '评价失败'
}[review?.status] || '评价中')
const aiReviewTag = review => review?.status === 'accepted' ? 'success' : review?.status === 'needs_review' ? 'warning' : 'info'

const loadAiReview = async (submissionId, showError = true) => {
  if (!submissionId) return
  aiReviewLoading.value = true
  try {
    const res = await getAiReview(submissionId)
    if (res.data.code === 200) {
      aiReview.value = res.data.data
      aiReviewBySubmission.value = { ...aiReviewBySubmission.value, [submissionId]: res.data.data }
    }
    else if (showError) ElMessage.warning(res.data.msg || '暂无智能评价')
  } catch {
    if (showError) ElMessage.error('智能评价加载失败')
  } finally { aiReviewLoading.value = false }
}

const requestAiReview = async () => {
  if (!grading.value.submissionId) return
  aiReviewLoading.value = true
  try {
    const res = await generateAiReview(grading.value.submissionId)
    if (res.data.code === 200) {
      aiReview.value = res.data.data
      aiReviewBySubmission.value = { ...aiReviewBySubmission.value, [grading.value.submissionId]: res.data.data }
      if (grading.value.score == null) grading.value.score = res.data.data.aiScore
      ElMessage.success('智能评价已生成')
    } else ElMessage.error(res.data.msg)
  } catch { ElMessage.error('智能评价生成失败') }
  finally { aiReviewLoading.value = false }
}

const applyAiReview = () => {
  if (!aiReview.value) return
  const lines = []
  if (aiReview.value.summary) lines.push(aiReview.value.summary)
  reviewSuggestions.value.forEach(item => lines.push(item))
  grading.value.feedback = lines.join('\n')
  if (grading.value.score == null && aiReview.value.aiScore != null) grading.value.score = aiReview.value.aiScore
}

const reloadSubmissions = async () => {
  if (userRole !== 'student') {
    subLoading.value = true
    try {
      const res = await getSubmissionsByTask(taskNo)
      if (res.data.code === 200) submissions.value = res.data.data
      else ElMessage.error(res.data.msg)
    } catch { ElMessage.error('提交记录加载失败') }
    finally { subLoading.value = false }
  }
}

onMounted(async () => {
  await loadTask()
  await loadMySubmissions()
  await reloadSubmissions()
})
</script>

<style scoped>
.task-submit-page {
  min-height: 100vh;
  margin: -20px;
  padding: 28px 28px 56px;
  color: #f8edcf;
  background-position: center;
  background-size: cover;
  background-attachment: fixed;
}
.task-submit-shell { width: min(980px, 100%); margin: 0 auto; }
.task-back {
  margin-bottom: 18px;
  border-color: rgba(238, 181, 91, .36);
  color: #f8ebcb;
  background: rgba(255, 255, 255, .08);
}
.task-submit-page :deep(.el-card) {
  border: 1px solid rgba(232, 184, 91, .3);
  border-radius: 10px;
  color: #f8edcf;
  background: linear-gradient(145deg, rgba(55, 29, 21, .92), rgba(9, 11, 15, .92));
  box-shadow: 0 18px 44px rgba(0, 0, 0, .34);
}
.task-brief :deep(.el-card__header) { border-bottom-color: rgba(232, 184, 91, .22); }
.task-brief :deep(.el-card__header b) { color: #fff1c9; font-family: Georgia, serif; font-size: 25px; }
.task-brief :deep(.el-descriptions__body),
.task-brief :deep(.el-descriptions__table) { background: transparent; }
.task-brief :deep(.el-descriptions__cell) { border-color: rgba(232, 184, 91, .26) !important; }
.task-brief :deep(.el-descriptions__label),
.task-brief :deep(.el-descriptions__label.is-bordered-label) { color: #e0b968; background: rgba(125, 71, 35, .34); }
.task-brief :deep(.el-descriptions__content),
.task-brief :deep(.el-descriptions__content.is-bordered-content) { color: #fff1d0; background: rgba(20, 15, 15, .72); }
.submit-panel { max-width: 760px !important; margin-top: 18px; }
.submit-panel h4 { color: #fff1c9; font-family: Georgia, serif; }
.submit-panel :deep(.el-form-item__label) { color: #d8c39f; }
.submit-panel :deep(.el-textarea__inner),
.submit-panel :deep(.el-input__wrapper) { color: #f8edcf; background: rgba(255, 255, 255, .08); box-shadow: 0 0 0 1px rgba(232, 184, 91, .22) inset; }
.submit-panel :deep(.el-textarea__inner)::placeholder { color: #9d8b70; }
.submit-panel :deep(.el-upload-list__item-name) { color: #e9bd6d; }
.task-submit-page :deep(.el-alert) { border-color: rgba(232, 184, 91, .25); background: rgba(232, 184, 91, .1); }
.task-submit-page :deep(.el-tag--info) { color: #d8c39f; border-color: rgba(189, 168, 131, .38); background: rgba(189, 168, 131, .12); }
.task-submit-page :deep(.el-tag--warning) { color: #ffda85; border-color: rgba(232, 184, 91, .42); background: rgba(232, 184, 91, .14); }
.task-submit-page :deep(.el-tag--success) { color: #a7d7a9; border-color: rgba(118, 191, 122, .42); background: rgba(118, 191, 122, .14); }
.task-submit-page :deep(.el-tag--danger) { color: #ff9d83; border-color: rgba(220, 86, 62, .42); background: rgba(220, 86, 62, .14); }
.task-submit-page :deep(.el-table) { color: #eadfc8; background: rgba(8, 10, 14, .42); }
.task-submit-page :deep(.el-table th.el-table__cell) { color: #d8b779; background: rgba(232, 184, 91, .1); }
.task-submit-page :deep(.el-table tr),
.task-submit-page :deep(.el-table td.el-table__cell) { color: #eadfc8; background: transparent; border-bottom-color: rgba(232, 184, 91, .14); }
.task-submit-page :deep(.el-table::before) { background-color: rgba(232, 184, 91, .2); }
.submission-count { color: #d8b779; }
.submission-record {
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid rgba(232, 184, 91, .3);
  border-radius: 7px;
  background: linear-gradient(90deg, rgba(232, 184, 91, .12), rgba(28, 20, 18, .72));
}
.submission-record.graded { border-color: rgba(118, 191, 122, .42); background: linear-gradient(90deg, rgba(118, 191, 122, .12), rgba(28, 20, 18, .72)); }
.submission-record.superseded { opacity: .58; border-color: rgba(151, 112, 70, .26); background: rgba(20, 16, 16, .55); }
.submission-time { margin-left: 8px; color: #bda883; font-size: 13px; }
.teacher-visible { margin-left: 8px; color: #ffda85; font-size: 12px; }
.attachment-hint { color: #bda883; font-size: 12px; }
.grade-question { margin-bottom: 12px; padding: 12px; border: 1px solid rgba(232, 184, 91, .24); border-radius: 7px; color: #eadfc8; text-align: left; background: rgba(232, 184, 91, .08); }
.question-score { color: #d8b779; font-size: 12px; }
.student-answer { margin-top: 8px; color: #d8c39f; }
.student-answer span { color: #eadfc8; }
.student-answer span.correct { color: #a7d7a9; }
.student-answer span.incorrect { color: #ff9d83; }
.correct-answer { margin-top: 5px; color: #e9bd6d; }
.non-quiz-note { margin-top: 16px; padding: 22px; color: #d8c39f; text-align: center; border: 1px dashed rgba(232, 184, 91, .3); border-radius: 7px; background: rgba(232, 184, 91, .08); }
.ai-review-panel {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid rgba(232, 184, 91, .28);
  border-radius: 6px;
  background: rgba(232, 184, 91, .08);
}
.ai-review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.ai-review-head h4 {
  margin: 0;
  color: #fff1c9;
}
.ai-suggestions {
  margin: 10px 0;
  line-height: 1.8;
  color: #d8c39f;
}
.manual-grade-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(232, 184, 91, .22);
  color: #d8c39f;
}
@media (max-width: 760px) {
  .task-submit-page { margin: -16px; padding: 20px 16px 40px; }
  .task-brief :deep(.el-card__header b) { font-size: 21px; }
}
</style>
