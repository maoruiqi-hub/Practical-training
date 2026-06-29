<template>
  <section class="room-card" aria-label="诊断房">
    <div class="room-head">
      <div>
        <p class="room-kicker">Diagnosis</p>
        <h2>诊断房</h2>
      </div>
      <el-tag class="room-tag" effect="dark">正确率 {{ Math.round(correctRate * 100) }}%</el-tag>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-empty v-else-if="!questions.length" description="当前楼层暂无诊断题">
      <el-button class="spire-button" @click="$emit('diagnosed', { status: 'partial', correctRate: 0 })">
        进入战斗房
      </el-button>
    </el-empty>

    <form v-else class="question-stack" @submit.prevent="submitDiagnosis">
      <article v-for="(question, index) in questions" :key="question.questionId" class="question-card">
        <div class="question-top">
          <span class="number">{{ index + 1 }}</span>
          <strong>{{ typeLabel(question.type) }}</strong>
          <small>{{ question.score || 0 }} 分</small>
        </div>
        <p class="stem">{{ question.stem }}</p>

        <el-radio-group
          v-if="question.type === 'single'"
          v-model="answers[question.questionId]"
          class="option-list"
        >
          <el-radio v-for="option in parseOptions(question.options)" :key="option" :value="option">
            {{ option }}
          </el-radio>
        </el-radio-group>

        <el-checkbox-group
          v-else-if="question.type === 'multi'"
          v-model="answers[question.questionId]"
          class="option-list"
        >
          <el-checkbox v-for="option in parseOptions(question.options)" :key="option" :value="option" :label="option" />
        </el-checkbox-group>

        <el-input
          v-else
          v-model="answers[question.questionId]"
          :type="question.type === 'program' || question.type === 'essay' ? 'textarea' : 'text'"
          :rows="question.type === 'program' ? 6 : 3"
          placeholder="输入你的答案"
        />
      </article>

      <div class="room-actions">
        <el-button class="spire-button" native-type="submit" :loading="submitting">
          完成诊断
        </el-button>
        <el-button class="ghost-button" @click="$emit('diagnosed', { status: 'partial', correctRate: 0 })">
          直接战斗
        </el-button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionById, getQuestionsByKnowledgePoint, getTaskQuestions } from '../api'

const props = defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' }
})

const emit = defineEmits(['diagnosed'])
const loading = ref(true)
const submitting = ref(false)
const questions = ref([])
const answers = reactive({})
const correctRate = ref(0)

const typeLabel = type => ({ single: '单选', multi: '多选', fill: '填空', essay: '简答', program: '编程' }[type] || type)

const parseOptions = options => {
  if (!options) return []
  try {
    const parsed = JSON.parse(options)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const autoGradableQuestions = computed(() =>
  questions.value.filter(question => ['single', 'multi', 'fill'].includes(question.type))
)

const normalize = value => String(value || '').trim().toLowerCase()

const isCorrect = question => {
  const answer = answers[question.questionId]
  if (question.type === 'multi') {
    const expected = normalize(question.answer).split(',').map(item => item.trim()).filter(Boolean).sort().join(',')
    const actual = Array.isArray(answer) ? answer.map(item => normalize(item)).sort().join(',') : normalize(answer)
    return actual === expected
  }
  return normalize(answer) === normalize(question.answer)
}

const loadQuestions = async () => {
  loading.value = true
  try {
    if (props.taskNo) {
      const taskRes = await getTaskQuestions(props.taskNo)
      if (taskRes.data.code === 200) {
        const details = await Promise.all(
          taskRes.data.data.map(item => getQuestionById(item.questionId))
        )
        questions.value = details
          .map(res => res.data.code === 200 ? res.data.data : null)
          .filter(Boolean)
          .filter(question => !question.knowledgePointId || String(question.knowledgePointId) === String(props.kpId))
          .slice(0, 3)
      }
    }

    if (!questions.value.length) {
      const res = await getQuestionsByKnowledgePoint(props.courseId, props.kpId)
      if (res.data.code === 200) questions.value = (res.data.data || []).slice(0, 3)
    }
  } catch {
    questions.value = []
  } finally {
    questions.value.forEach(question => {
      answers[question.questionId] = question.type === 'multi' ? [] : ''
    })
    loading.value = false
  }
}

const submitDiagnosis = async () => {
  submitting.value = true
  try {
    const gradable = autoGradableQuestions.value
    const correctCount = gradable.filter(isCorrect).length
    correctRate.value = gradable.length ? correctCount / gradable.length : 0

    const status = correctRate.value >= 0.85 ? 'mastered' : correctRate.value >= 0.4 ? 'partial' : 'weak'
    emit('diagnosed', { status, correctRate: correctRate.value })
  } catch {
    ElMessage.error('诊断提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadQuestions)
</script>

<style scoped>
.room-card {
  border: 1px solid rgba(238, 181, 91, .28);
  border-radius: 8px;
  padding: 22px;
  color: #f8ebcb;
  background:
    linear-gradient(180deg, rgba(65, 36, 22, .94), rgba(25, 17, 14, .96)),
    radial-gradient(circle at 100% 0, rgba(218, 82, 35, .2), transparent 32%);
  box-shadow: 0 18px 40px rgba(0, 0, 0, .36);
}

.room-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.room-kicker {
  margin: 0 0 4px;
  color: #d89b4a;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-size: 24px;
}

.room-tag {
  background: #6b321f;
  border-color: #c7833d;
}

.question-stack {
  display: grid;
  gap: 16px;
}

.question-card {
  padding: 16px;
  border: 1px solid rgba(236, 201, 137, .24);
  border-radius: 8px;
  background: linear-gradient(180deg, #efe0bd, #d6bb84);
  color: #2d2119;
}

.question-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.number {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #fff4d8;
  background: #6b321f;
}

.stem {
  margin: 0 0 12px;
  line-height: 1.7;
}

.option-list {
  display: grid;
  gap: 10px;
  align-items: flex-start;
}

.room-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.spire-button,
.ghost-button {
  min-height: 44px;
  border-radius: 6px;
}

.spire-button {
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #73301f);
  border-color: #da9a4d;
}

.ghost-button {
  color: #f8ebcb;
  background: rgba(255, 255, 255, .06);
  border-color: rgba(238, 181, 91, .3);
}
</style>
