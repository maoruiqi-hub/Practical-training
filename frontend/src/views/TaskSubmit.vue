<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>
    <h3>提交任务 #{{ taskNo }}</h3>

    <!-- 学生提交表单 -->
    <el-card v-if="userRole==='student'" v-loading="submitting" style="max-width:600px;margin-bottom:20px">
      <el-form label-width="100px">
        <el-form-item label="文字内容"><el-input v-model="content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="上传附件"><el-upload :auto-upload="false" :limit="1" :on-change="handleFile">
          <el-button type="primary">选择文件</el-button>
        </el-upload></el-form-item>
        <el-form-item><el-button type="success" @click="submit" style="width:100%">提交</el-button></el-form-item>
      </el-form>
    </el-card>

    <!-- 教师查看提交列表 -->
    <template v-if="userRole!=='student'">
      <h4>学生提交列表</h4>
      <el-table :data="submissions" style="width:100%" v-loading="subLoading" element-loading-text="正在加载提交记录..." empty-text="暂无提交记录">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="taskType" label="任务类型" width="100" />
        <el-table-column label="提交内容" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.content">{{ row.content }}</span>
            <el-link v-if="row.filePath" :href="'/practical-training/' + row.filePath" target="_blank" type="primary" style="margin-left:5px">📎文件</el-link>
            <span v-if="!row.content && !row.filePath">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column prop="score" label="得分" width="80">
          <template #default="{ row }">{{ row.score ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status==='graded'?'success':'warning'">{{ row.status==='graded'?'已完成':'待复核' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
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
        <el-descriptions-item label="任务">{{ grading.taskName || grading.taskType }}</el-descriptions-item>
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
import { submitTask, getSubmissionsByTask, getGradeDetail, gradeSubmission } from '../api'

const route = useRoute()
const taskNo = route.params.taskNo
const userRole = JSON.parse(localStorage.getItem('user') || '{}').role
const content = ref('')
const file = ref(null)
const submitting = ref(false)
const submissions = ref([])
const subLoading = ref(true)
const dialogVisible = ref(false)
const grading = ref({ submissionId: '', studentName: '', content: '', score: null, feedback: '' })
const gradeLoading = ref(false)
const gradeDetails = ref([])
const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答',program:'编程'}[t]||t)

const handleFile = (f) => { file.value = f.raw }

const submit = async () => {
  submitting.value = true
  try {
    const fd = new FormData(); fd.append('taskNo', taskNo)
    if (content.value) fd.append('content', content.value)
    if (file.value) fd.append('file', file.value)
    const res = await submitTask(fd)
    if (res.data.code === 200) ElMessage.success('提交成功')
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('提交失败')
  } finally { submitting.value = false }
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
  } finally {
    gradeLoading.value = false
  }
}

const doGrade = async () => {
  try {
    const res = await gradeSubmission(grading.value.submissionId, {
      score: grading.value.score, feedback: grading.value.feedback
    })
    if (res.data.code === 200) { ElMessage.success('已完成复核'); dialogVisible.value = false; reloadSubmissions() }
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('复核失败')
  }
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
    } catch {
      ElMessage.error('提交记录加载失败')
    } finally { subLoading.value = false }
  }
}

onMounted(reloadSubmissions)
</script>
