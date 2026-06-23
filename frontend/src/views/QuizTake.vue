<template>
  <div v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom:20px">← 返回</el-button>
    <h3>{{ task?.description || '测验' }}</h3>
    <el-divider />
    <el-form v-if="questions.length" label-width="60px">
      <el-card v-for="(q, i) in questions" :key="i" style="margin-bottom:16px">
        <template #header>
          <span><b>{{ i + 1 }}.</b> {{ q.stem }}</span>
          <span style="float:right">
            <el-tag size="small">{{ typeLabel(q.type) }}</el-tag>
            <span style="color:#999;font-size:12px;margin-left:6px">{{ q.score }}分</span>
          </span>
        </template>
        <template v-if="q.type==='single'">
          <el-radio-group v-model="getAns[q.questionId]" class="option-list">
            <el-radio v-for="(o, j) in JSON.parse(q.options||'[]')" :key="j" :value="o" class="option-item">{{ o }}</el-radio>
          </el-radio-group>
        </template>
        <template v-else-if="q.type==='multi'">
          <el-checkbox-group v-model="getAns[q.questionId]" class="option-list">
            <el-checkbox v-for="(o, j) in JSON.parse(q.options||'[]')" :key="j" :value="o" :label="o" class="option-item" />
          </el-checkbox-group>
        </template>
        <template v-else-if="q.type==='fill'">
          <el-input v-model="getAns[q.questionId]" placeholder="请输入答案" />
        </template>
        <template v-else-if="q.type==='essay'">
          <el-input v-model="getAns[q.questionId]" type="textarea" :rows="4" placeholder="请输入答案" />
        </template>
      </el-card>
      <el-button type="success" @click="submitQuiz" :disabled="submitted" style="width:100%" size="large">{{ submitted ? '已提交' : '提交答卷' }}</el-button>
    </el-form>
    <el-empty v-if="!questions.length && !loading" description="暂无题目" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskDetail, getTaskQuestions, getQuestionById, submitTask } from '../api'

const route = useRoute()
const taskNo = route.params.taskNo
const task = ref(null)
const questions = ref([])
const loading = ref(true)
const submitted = ref(false)
const getAns = reactive({})
const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答'}[t]||t)

onMounted(async () => {
  try {
    const [tRes, tqRes] = await Promise.all([
      getTaskDetail(taskNo),
      getTaskQuestions(taskNo)
    ])
    if (tRes.data.code === 200) task.value = tRes.data.data
    if (tqRes.data.code === 200) {
      const qRes = await Promise.all(tqRes.data.data.map(tq => getQuestionById(tq.questionId)))
      questions.value = qRes.map(r => r.data.code === 200 ? r.data.data : null).filter(Boolean)
    }
  } finally { loading.value = false }
})

const submitQuiz = async () => {
  const content = questions.value.map(q => {
    const ans = getAns[q.questionId]
    return { no: q.questionId, response: Array.isArray(ans) ? ans.join(',') : (ans || '') }
  })
  const fd = new FormData(); fd.append('taskNo', taskNo); fd.append('content', JSON.stringify(content))
  const res = await submitTask(fd)
  if (res.data.code === 200) { ElMessage.success('提交成功'); submitted.value = true }
  else ElMessage.error(res.data.msg)
}
</script>

<style scoped>
.option-list { display:flex; flex-direction:column; gap:10px; align-items:flex-start; }
.option-item { margin:0; text-align:left; }
</style>
