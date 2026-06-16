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
      <el-table :data="submissions" style="width:100%" v-loading="subLoading">
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
            <el-tag :type="row.status==='graded'?'success':'warning'">{{ row.status==='graded'?'已批改':'已提交' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">批改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 批改弹窗 -->
    <el-dialog v-model="dialogVisible" title="批改提交" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="学生">{{ grading.studentName }}</el-descriptions-item>
        <el-descriptions-item label="提交内容">{{ grading.content || '(无)' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px" style="margin-top:20px">
        <el-form-item label="得分"><el-input-number v-model="grading.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="grading.feedback" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="doGrade">确认批改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitTask, getSubmissionsByTask, gradeSubmission } from '../api'

const route = useRoute()
const taskNo = route.params.taskNo
const userRole = JSON.parse(localStorage.getItem('user') || '{}').role
const content = ref('')
const file = ref(null)
const submitting = ref(false)
const submissions = ref([])
const subLoading = ref(true)
const dialogVisible = ref(false)
const grading = ref({ submissionId: '', studentName: '', content: '', score: 0, feedback: '' })

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
  } finally { submitting.value = false }
}

const openGradeDialog = (row) => {
  grading.value = { submissionId: row.submissionId, studentName: row.studentName, content: row.content, score: row.score || 0, feedback: row.feedback || '' }
  dialogVisible.value = true
}

const doGrade = async () => {
  const res = await gradeSubmission(grading.value.submissionId, {
    score: grading.value.score, feedback: grading.value.feedback
  })
  if (res.data.code === 200) { ElMessage.success('已批改'); dialogVisible.value = false; location.reload() }
  else ElMessage.error(res.data.msg)
}

onMounted(async () => {
  if (userRole !== 'student') {
    try {
      const res = await getSubmissionsByTask(taskNo)
      if (res.data.code === 200) submissions.value = res.data.data
    } finally { subLoading.value = false }
  }
})
</script>
