<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>

    <!-- 任务信息卡片 -->
    <el-card v-if="task" style="margin-bottom:20px">
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
          <span v-if="mySubmissions.length" style="color:#909399">（已提交{{ mySubmissions.length }}次）</span>
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
      <el-card v-else v-loading="submitting" style="max-width:700px">
        <!-- 提交历史 -->
        <div v-if="mySubmissions.length" style="margin-bottom:16px">
          <h4 style="margin-bottom:8px">提交记录</h4>
          <div v-for="(s, idx) in mySubmissions" :key="s.submissionId"
            :style="{ opacity: s.status === 'superseded' ? 0.5 : 1, padding: '8px 12px', marginBottom: '6px', borderRadius: '6px', border: '1px solid #ebeef5', background: s.status === 'superseded' ? '#fafafa' : '#f0f9eb' }">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>
                <el-tag :type="s.status === 'superseded' ? 'info' : s.status === 'graded' ? 'success' : 'warning'" size="small">
                  {{ s.status === 'superseded' ? '已覆盖' : s.status === 'graded' ? '已批改' : '待批改' }}
                </el-tag>
                <span style="margin-left:8px;font-weight:500">第{{ s.attemptNumber || idx + 1 }}次提交</span>
                <span style="margin-left:8px;color:#909399;font-size:13px">{{ s.submitTime }}</span>
              </span>
              <span>
                <el-tag v-if="s.status === 'graded' && s.score != null" type="success" size="small">{{ s.score }}分</el-tag>
                <el-link v-if="s.filePath" :href="'/practical-training/' + s.filePath" target="_blank" type="primary" :underline="false" style="margin-left:6px">📎</el-link>
                <span v-if="s.status !== 'superseded' && idx === 0" style="color:#e6a23c;font-size:12px;margin-left:6px">← 教师可见</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 逾期警告 -->
        <el-alert v-if="isOverdue && !task?.allowLate" title="任务已截止，不允许逾期提交" type="error" :closable="false" style="margin-bottom:16px" />

        <!-- 提交次数达上限 -->
        <el-alert v-if="!canResubmit" title="已达最大提交次数（{{ task?.maxAttempts || 3 }}次），如需修改请联系教师" type="warning" :closable="false" style="margin-bottom:16px" />

        <!-- 重新提交提示 -->
        <el-alert v-if="canResubmit && mySubmissions.length > 0" title="您可以重新提交（还有 {{ (task?.maxAttempts || 3) - mySubmissions.length }} 次机会），新提交将覆盖旧提交" type="info" :closable="false" style="margin-bottom:16px" />

        <!-- 提交表单 -->
        <el-form v-if="canSubmit" label-width="100px">
          <el-form-item label="文字内容">
            <el-input v-model="content" type="textarea" :rows="5" placeholder="请输入提交内容或说明" />
          </el-form-item>
          <el-form-item :label="'上传文件'">
            <div style="display:flex;flex-direction:column;gap:8px">
              <el-upload :auto-upload="false" :limit="1" :on-change="handleFile" :accept="task?.attachmentFormats || ''">
                <el-button type="primary">选择文件</el-button>
              </el-upload>
              <span v-if="task?.attachmentFormats" style="color:#909399;font-size:12px">仅支持：{{ task.attachmentFormats }}</span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="submit" :disabled="!canSubmit" style="width:100%" size="large">
              {{ mySubmissions.length > 0 ? '重新提交（第' + (mySubmissions.length + 1) + '次）' : isOverdue && task?.allowLate ? '逾期提交' : '提交作业' }}
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
        <div v-for="(d,i) in gradeDetails" :key="i" style="margin-bottom:12px;padding:10px;background:#f9f9f9;border-radius:6px;text-align:left">
          <div><b>{{ i + 1 }}.</b> {{ d.stem }} <el-tag size="small" style="margin-left:6px">{{ typeLabel(d.type) }}</el-tag> <span style="color:#999;font-size:12px">{{ d.score }}分</span></div>
          <div style="margin-top:6px">学生答案：<span :style="{ color: d.autoGradable ? (d.correct ? '#67c23a' : '#f56c6c') : '#606266' }">{{ d.studentAnswer || '(空)' }}</span></div>
          <div style="margin-top:4px;color:#67c23a">{{ d.autoGradable ? '正确答案' : '参考答案' }}：{{ d.correctAnswer || '(无)' }}</div>
        </div>
      </div>
      <div v-else-if="!gradeLoading" style="margin-top:16px;text-align:center;color:#999;padding:20px">非在线测验提交：{{ grading.content || '附件提交' }}</div>
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitTask, getSubmissionsByTask, getGradeDetail, gradeSubmission, getTaskDetail, getMySubmissions } from '../api'

const route = useRoute()
const taskNo = route.params.taskNo
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const task = ref(null)
const content = ref('')
const file = ref(null)
const submitting = ref(false)
const submissions = ref([])
const subLoading = ref(true)
const mySubmissions = ref([])
const dialogVisible = ref(false)
const grading = ref({ submissionId: '', studentName: '', content: '', score: null, feedback: '' })
const gradeLoading = ref(false)
const gradeDetails = ref([])
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
    }
  } catch { /* ignore */ }
}

const openGradeDialog = async (row) => {
  grading.value = { ...row, score: row.score ?? null, feedback: row.feedback || '' }
  gradeDetails.value = []
  dialogVisible.value = true
  gradeLoading.value = true
  try {
    const res = await getGradeDetail(row.submissionId)
    if (res.data.code === 200) {
      gradeDetails.value = res.data.data.details || []
      grading.value.score = res.data.data.score ?? row.score ?? null
      grading.value.feedback = res.data.data.feedback || row.feedback || ''
      grading.value.status = res.data.data.status || row.status
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('评阅详情加载失败')
  } finally { gradeLoading.value = false }
}

const doGrade = async () => {
  try {
    const res = await gradeSubmission(grading.value.submissionId, {
      score: grading.value.score, feedback: grading.value.feedback
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
