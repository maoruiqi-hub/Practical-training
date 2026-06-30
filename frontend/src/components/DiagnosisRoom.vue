<template>
  <section class="diagnosis-dialogue" aria-label="开局侦察">
    <div v-if="loading" class="scene-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <div class="scene-vignette" aria-hidden="true"></div>

      <div class="scan-meter">
        <span>侦察准确率</span>
        <strong>{{ Math.round(correctRate * 100) }}%</strong>
      </div>

      <div class="analyst-actor" aria-label="侦察终端">
        <div class="analyst-aura"></div>
        <div class="analyst-core">
          <span></span>
        </div>
        <div class="analyst-name">
          <strong>侦察终端</strong>
          <em>正在扫描当前知识点</em>
        </div>
      </div>

      <div v-if="feedback" class="result-float" :class="feedback.type">
        {{ feedback.text }}
      </div>

      <section v-if="activeQuestion.questionId" class="dialogue-layer">
        <div class="speaker-line">
          <strong>侦察终端</strong>
          <span>第 {{ activeIndex + 1 }} / {{ questions.length }} 题 · {{ typeLabel(activeQuestion.type) }}</span>
        </div>

        <div class="dialogue-box">
          <p>{{ activeQuestion.stem }}</p>
          <small>回答会影响本局初始状态和后续战斗风险。</small>
        </div>

        <div class="choice-stack" :class="{ compact: activeQuestion.type !== 'single' }">
          <template v-if="activeQuestion.type === 'single'">
            <button
              v-for="option in displayOptions"
              :key="option"
              type="button"
              class="choice-button"
              :disabled="choiceLocked"
              @click="chooseSingle(option)"
            >
              <span class="choice-icon">···</span>
              <b>{{ option }}</b>
            </button>
          </template>

          <template v-else-if="activeQuestion.type === 'multi'">
            <button
              v-for="option in displayOptions"
              :key="option"
              type="button"
              class="choice-button"
              :class="{ selected: selectedMulti.includes(option) }"
              :disabled="choiceLocked"
              @click="toggleMulti(option)"
            >
              <span class="choice-icon">+</span>
              <b>{{ option }}</b>
            </button>
            <button type="button" class="confirm-choice" :disabled="choiceLocked" @click="confirmMulti">
              确认这些选项
            </button>
          </template>

          <template v-else>
            <el-input
              v-model="freeAnswer"
              :type="activeQuestion.type === 'program' || activeQuestion.type === 'essay' ? 'textarea' : 'text'"
              :rows="activeQuestion.type === 'program' ? 6 : 3"
              class="free-answer"
              placeholder="输入你的答案"
              :disabled="choiceLocked"
            />
            <button type="button" class="confirm-choice" :disabled="choiceLocked" @click="confirmFreeAnswer">
              提交侦察答案
            </button>
          </template>
        </div>
      </section>

      <div class="scene-tools">
        <button type="button" :disabled="choiceLocked" @click="$emit('diagnosed', { status: 'partial', correctRate })">
          直接进入战斗
        </button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionById, getQuestionsByKnowledgePoint, getTaskQuestions } from '../api'

const props = defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' }
})

const emit = defineEmits(['diagnosed'])
const loading = ref(true)
const questions = ref([])
const answers = reactive({})
const activeIndex = ref(0)
const correctRate = ref(0)
const correctCount = ref(0)
const resolvedCount = ref(0)
const choiceLocked = ref(false)
const feedback = ref(null)
const selectedMulti = ref([])
const freeAnswer = ref('')

const activeQuestion = computed(() => questions.value[activeIndex.value] || {})
const displayOptions = computed(() => parseOptions(activeQuestion.value.options))
const typeLabel = type => ({ single: '单选', multi: '多选', fill: '填空', essay: '简答', program: '编程' })[type] || type
const fallbackQuestions = computed(() => [
  {
    questionId: `diagnosis-demo-${props.kpId}-1`,
    knowledgePointId: props.kpId,
    type: 'single',
    stem: 'Python 程序从上到下一行一行执行，这种结构通常称为？',
    options: ['A. 顺序结构', 'B. 分支结构', 'C. 循环结构', 'D. 异常结构'],
    answer: 'A',
    score: 10
  },
  {
    questionId: `diagnosis-demo-${props.kpId}-2`,
    knowledgePointId: props.kpId,
    type: 'single',
    stem: '下面哪个符号用于给变量赋值？',
    options: ['A. ==', 'B. =', 'C. !=', 'D. >='],
    answer: 'B',
    score: 10
  },
  {
    questionId: `diagnosis-demo-${props.kpId}-3`,
    knowledgePointId: props.kpId,
    type: 'fill',
    stem: '把字符串转换为整数可以使用内置函数 ____。',
    answer: 'int',
    score: 10
  }
])

watch(activeIndex, () => {
  const question = activeQuestion.value
  selectedMulti.value = Array.isArray(answers[question.questionId]) ? [...answers[question.questionId]] : []
  freeAnswer.value = typeof answers[question.questionId] === 'string' ? answers[question.questionId] : ''
})

const parseOptions = options => {
  if (!options) return []
  if (Array.isArray(options)) return options
  try {
    const parsed = JSON.parse(options)
    if (Array.isArray(parsed)) return parsed
    if (parsed && typeof parsed === 'object') return Object.values(parsed)
    return []
  } catch {
    return String(options).split(/\r?\n/).map(item => item.trim()).filter(Boolean)
  }
}

const normalize = value => String(value || '').trim().toLowerCase()

const matchesAnswer = (actual, expected) => {
  const actualValue = normalize(actual)
  const expectedValue = normalize(expected)
  return actualValue === expectedValue ||
    actualValue.startsWith(`${expectedValue}.`) ||
    actualValue.startsWith(`${expectedValue}、`) ||
    (expectedValue.length <= 2 && actualValue.startsWith(expectedValue))
}

const isCorrect = question => {
  const answer = answers[question.questionId]
  if (question.type === 'multi') {
    const expected = normalize(question.answer).split(',').map(item => item.trim()).filter(Boolean).sort()
    const actual = Array.isArray(answer) ? answer.map(item => normalize(item)).sort() : normalize(answer).split(',').sort()
    return expected.length > 0 && expected.every((item, index) => matchesAnswer(actual[index], item))
  }
  return matchesAnswer(answer, question.answer)
}

const loadQuestions = async () => {
  loading.value = true
  questions.value = []
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
    if (!questions.value.length) questions.value = fallbackQuestions.value
    questions.value.forEach(question => {
      answers[question.questionId] = question.type === 'multi' ? [] : ''
    })
    loading.value = false
  }
}

const chooseSingle = option => {
  answers[activeQuestion.value.questionId] = option
  resolveCurrentAnswer()
}

const toggleMulti = option => {
  const exists = selectedMulti.value.includes(option)
  selectedMulti.value = exists
    ? selectedMulti.value.filter(item => item !== option)
    : [...selectedMulti.value, option]
  answers[activeQuestion.value.questionId] = selectedMulti.value
}

const confirmMulti = () => {
  if (!selectedMulti.value.length) {
    ElMessage.warning('先选择至少一个答案')
    return
  }
  answers[activeQuestion.value.questionId] = selectedMulti.value
  resolveCurrentAnswer()
}

const confirmFreeAnswer = () => {
  if (!String(freeAnswer.value || '').trim()) {
    ElMessage.warning('先输入答案')
    return
  }
  answers[activeQuestion.value.questionId] = freeAnswer.value
  resolveCurrentAnswer()
}

const resolveCurrentAnswer = () => {
  if (choiceLocked.value) return
  choiceLocked.value = true

  const correct = isCorrect(activeQuestion.value)
  resolvedCount.value += 1
  if (correct) correctCount.value += 1
  correctRate.value = correctCount.value / Math.max(1, resolvedCount.value)
  feedback.value = correct
    ? { type: 'correct', text: '侦察成功，概念稳定' }
    : { type: 'wrong', text: '侦察偏差，后续战斗风险上升' }

  setTimeout(() => {
    if (activeIndex.value >= questions.value.length - 1) {
      finishDiagnosis()
      return
    }
    activeIndex.value += 1
    feedback.value = null
    choiceLocked.value = false
  }, 760)
}

const finishDiagnosis = () => {
  const status = correctRate.value >= 0.85 ? 'mastered' : correctRate.value >= 0.4 ? 'partial' : 'weak'
  emit('diagnosed', { status, correctRate: correctRate.value })
}

onMounted(loadQuestions)
</script>

<style scoped>
.diagnosis-dialogue {
  position: relative;
  overflow: hidden;
  min-height: min(760px, calc(100vh - 96px));
  color: #fff4d8;
}

.scene-loading {
  display: grid;
  min-height: inherit;
  place-items: center;
  padding: 40px;
  background: rgba(6, 8, 12, .48);
}

.scene-vignette {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 42%, rgba(105, 218, 230, .1), transparent 32%),
    linear-gradient(180deg, rgba(6, 8, 12, .06), rgba(6, 8, 12, .68) 78%),
    linear-gradient(90deg, rgba(4, 6, 10, .56), transparent 35%, transparent 66%, rgba(4, 6, 10, .54));
  pointer-events: none;
}

.scan-meter {
  position: absolute;
  z-index: 5;
  top: 18px;
  right: 28px;
  display: grid;
  min-width: 140px;
  gap: 2px;
  padding: 12px 16px;
  border: 1px solid rgba(112, 223, 231, .38);
  border-radius: 8px;
  text-align: center;
  background: rgba(8, 18, 24, .58);
  box-shadow: 0 14px 30px rgba(0, 0, 0, .34);
  backdrop-filter: blur(6px);
}

.scan-meter span {
  color: #b8f2f0;
  font-size: 12px;
  font-weight: 900;
}

.scan-meter strong {
  color: #fff8dc;
  font-size: 30px;
}

.analyst-actor {
  position: absolute;
  z-index: 3;
  left: clamp(60px, 12vw, 220px);
  bottom: 130px;
  display: grid;
  place-items: center;
}

.analyst-aura {
  position: absolute;
  width: 240px;
  height: 100px;
  bottom: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(82, 231, 239, .24), transparent 68%);
}

.analyst-core {
  position: relative;
  display: grid;
  width: 170px;
  height: 210px;
  place-items: center;
  filter: drop-shadow(0 28px 30px rgba(0, 0, 0, .56));
}

.analyst-core span {
  width: 118px;
  height: 118px;
  border: 2px solid rgba(151, 237, 241, .6);
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(228, 255, 250, .96) 0 12px, transparent 13px),
    radial-gradient(circle, rgba(76, 221, 232, .42), rgba(12, 56, 65, .9) 60%, rgba(8, 14, 20, .96));
  box-shadow: 0 0 36px rgba(84, 228, 235, .36);
}

.analyst-name {
  position: relative;
  display: grid;
  min-width: 170px;
  gap: 3px;
  padding: 8px 14px;
  border: 1px solid rgba(151, 237, 241, .32);
  border-radius: 999px;
  text-align: center;
  background: rgba(9, 10, 14, .56);
  backdrop-filter: blur(4px);
}

.analyst-name strong {
  color: #fff3d0;
}

.analyst-name em {
  color: #bce7e2;
  font-size: 12px;
  font-style: normal;
}

.result-float {
  position: absolute;
  z-index: 8;
  top: 42%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 12px 22px;
  border: 1px solid rgba(255, 244, 210, .42);
  border-radius: 999px;
  color: #fff9df;
  font-size: 22px;
  font-weight: 900;
  background: rgba(8, 10, 14, .6);
  box-shadow: 0 18px 44px rgba(0, 0, 0, .4);
  backdrop-filter: blur(8px);
}

.result-float.correct {
  color: #d6ffc0;
}

.result-float.wrong {
  color: #ffb49d;
}

.dialogue-layer {
  position: absolute;
  z-index: 6;
  left: 4vw;
  right: 4vw;
  bottom: 26px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(310px, 430px);
  gap: 18px;
  align-items: end;
}

.speaker-line {
  grid-column: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 22px;
}

.speaker-line strong {
  padding: 6px 18px;
  border-radius: 999px;
  color: #10252c;
  background: linear-gradient(180deg, #c3fff8, #55bfd0);
}

.speaker-line span {
  color: #d6eee8;
  font-size: 13px;
}

.dialogue-box {
  grid-column: 1;
  min-height: 116px;
  padding: 22px 26px;
  border-top: 1px solid rgba(196, 247, 244, .42);
  border-bottom: 1px solid rgba(196, 247, 244, .3);
  background: linear-gradient(90deg, rgba(6, 8, 12, .82), rgba(6, 8, 12, .64), rgba(6, 8, 12, .28));
  box-shadow: 0 -18px 60px rgba(0, 0, 0, .34);
  backdrop-filter: blur(7px);
}

.dialogue-box p {
  margin: 0;
  color: #fff7df;
  font-size: clamp(20px, 2.1vw, 30px);
  line-height: 1.45;
}

.dialogue-box small {
  display: block;
  margin-top: 10px;
  color: #d4c29f;
}

.choice-stack {
  grid-column: 2;
  grid-row: 1 / span 2;
  display: grid;
  gap: 10px;
  align-content: end;
}

.choice-button,
.confirm-choice,
.scene-tools button {
  border: 1px solid rgba(222, 251, 248, .22);
  color: #fff7dd;
  background: linear-gradient(90deg, rgba(18, 22, 28, .78), rgba(18, 22, 28, .46));
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: transform .16s ease, border-color .16s ease, background .16s ease;
}

.choice-button {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 52px;
  padding: 10px 16px;
  border-radius: 999px;
  text-align: left;
}

.choice-button:hover,
.confirm-choice:hover,
.scene-tools button:hover {
  border-color: rgba(197, 255, 250, .64);
  transform: translateX(-4px);
  background: linear-gradient(90deg, rgba(36, 62, 70, .82), rgba(18, 22, 28, .56));
}

.choice-button:disabled,
.confirm-choice:disabled,
.scene-tools button:disabled {
  cursor: default;
  opacity: .62;
  transform: none;
}

.choice-button.selected {
  border-color: rgba(121, 226, 213, .76);
  background: linear-gradient(90deg, rgba(20, 78, 78, .82), rgba(18, 22, 28, .54));
}

.choice-icon {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #10252c;
  background: #c6fff6;
  font-weight: 900;
}

.choice-button b {
  overflow-wrap: anywhere;
  font-size: 15px;
  line-height: 1.35;
}

.confirm-choice {
  min-height: 48px;
  border-radius: 999px;
  font-weight: 900;
  background: linear-gradient(90deg, rgba(28, 112, 121, .9), rgba(25, 56, 69, .72));
}

.free-answer :deep(.el-textarea__inner),
.free-answer :deep(.el-input__inner) {
  border-color: rgba(222, 251, 248, .28);
  color: #fff7dd;
  background: rgba(8, 10, 14, .62);
  box-shadow: none;
}

.scene-tools {
  position: absolute;
  z-index: 7;
  top: 92px;
  left: 28px;
}

.scene-tools button {
  min-width: 108px;
  min-height: 36px;
  border-radius: 999px;
  font-weight: 800;
}

@media (max-width: 980px) {
  .diagnosis-dialogue {
    min-height: 760px;
  }
  .dialogue-layer {
    grid-template-columns: 1fr;
  }
  .choice-stack {
    grid-column: 1;
    grid-row: auto;
  }
}

@media (max-width: 680px) {
  .analyst-actor {
    left: 0;
    transform: scale(.75);
  }
  .dialogue-layer {
    left: 14px;
    right: 14px;
  }
}
</style>
