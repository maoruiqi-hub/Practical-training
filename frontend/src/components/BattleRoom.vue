<template>
  <section class="battle-room" :class="{ boss: bossMode }" aria-label="战斗房">
    <div class="battle-stage">
      <aside class="enemy-panel">
        <p class="room-kicker">{{ bossMode ? 'Boss Room' : 'Battle Room' }}</p>
        <h2>{{ bossMode ? '区域首领' : '楼层敌人' }}</h2>
        <div class="enemy-sigil" aria-hidden="true"></div>
        <p class="enemy-name">{{ floorName }}</p>
        <div class="enemy-meta">
          <span>题数 {{ questions.length }}</span>
          <span>难度 {{ difficultyText }}</span>
          <span>奖励由后端结算</span>
        </div>
      </aside>

      <main class="combat-panel">
        <el-skeleton v-if="loading" :rows="6" animated />
        <el-empty v-else-if="!questions.length" description="当前楼层暂无可挑战题目">
          <el-button class="spire-button" @click="$emit('battle-end', { cleared: false, correctRate: 0 })">
            返回塔地图
          </el-button>
        </el-empty>

        <form v-else @submit.prevent="submitBattle">
          <div class="combat-top">
            <div>
              <p class="room-kicker">Card {{ activeIndex + 1 }} / {{ questions.length }}</p>
              <h3>{{ activeQuestion.stem }}</h3>
            </div>
            <el-tag effect="dark" class="difficulty-tag">{{ typeLabel(activeQuestion.type) }}</el-tag>
          </div>

          <div class="answer-board">
            <el-radio-group
              v-if="activeQuestion.type === 'single'"
              v-model="answers[activeQuestion.questionId]"
              class="option-list"
            >
              <el-radio v-for="option in parseOptions(activeQuestion.options)" :key="option" :value="option">
                {{ option }}
              </el-radio>
            </el-radio-group>

            <el-checkbox-group
              v-else-if="activeQuestion.type === 'multi'"
              v-model="answers[activeQuestion.questionId]"
              class="option-list"
            >
              <el-checkbox
                v-for="option in parseOptions(activeQuestion.options)"
                :key="option"
                :value="option"
                :label="option"
              />
            </el-checkbox-group>

            <el-input
              v-else
              v-model="answers[activeQuestion.questionId]"
              :type="activeQuestion.type === 'program' || activeQuestion.type === 'essay' ? 'textarea' : 'text'"
              :rows="activeQuestion.type === 'program' ? 9 : 4"
              placeholder="输入你的答案"
            />
          </div>

          <div class="card-strip" aria-label="题目切换">
            <button
              v-for="(question, index) in questions"
              :key="question.questionId"
              type="button"
              class="mini-card"
              :class="{ active: index === activeIndex, answered: hasAnswer(question) }"
              :aria-label="`切换到第 ${index + 1} 题`"
              @click="activeIndex = index"
            >
              {{ index + 1 }}
            </button>
          </div>

          <div class="battle-actions">
            <el-button class="ghost-button" :disabled="activeIndex === 0" @click="activeIndex--">
              上一题
            </el-button>
            <el-button class="ghost-button" :disabled="activeIndex === questions.length - 1" @click="activeIndex++">
              下一题
            </el-button>
            <el-button class="ghost-button" :loading="hinting" @click="useHint">
              使用提示
            </el-button>
            <el-button class="ghost-button" :loading="skipping" @click="skipQuestion">
              跳过本题
            </el-button>
            <el-button class="spire-button" native-type="submit" :loading="submitting">
              提交挑战
            </el-button>
          </div>
        </form>
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getQuestionById,
  getQuestionsByKnowledgePoint,
  getTaskQuestions,
  sendGameEvent,
  submitTask
} from '../api'

const props = defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  studentId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' },
  floorName: { type: String, default: '未知楼层' },
  bossMode: { type: Boolean, default: false }
})

const emit = defineEmits(['battle-end', 'profile-refresh'])
const loading = ref(true)
const submitting = ref(false)
const hinting = ref(false)
const skipping = ref(false)
const questions = ref([])
const answers = reactive({})
const activeIndex = ref(0)

const activeQuestion = computed(() => questions.value[activeIndex.value] || {})
const typeLabel = type => ({ single: '单选', multi: '多选', fill: '填空', essay: '简答', program: '编程' }[type] || type)
const difficultyText = computed(() => {
  const max = Math.max(...questions.value.map(question => Number(question.difficulty) || 1), 1)
  return `${max}/5`
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

const hasAnswer = question => {
  const value = answers[question.questionId]
  return Array.isArray(value) ? value.length > 0 : Boolean(String(value || '').trim())
}

const matchesAnswer = (actual, expected) => {
  const actualValue = normalize(actual)
  const expectedValue = normalize(expected)
  if (!expectedValue) return false
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
  try {
    if (props.taskNo) {
      const taskRes = await getTaskQuestions(props.taskNo)
      if (taskRes.data.code === 200) {
        const details = await Promise.all(taskRes.data.data.map(item => getQuestionById(item.questionId)))
        questions.value = details
          .map(res => res.data.code === 200 ? res.data.data : null)
          .filter(Boolean)
      }
    }

    if (!questions.value.length) {
      const res = await getQuestionsByKnowledgePoint(props.courseId, props.kpId)
      if (res.data.code === 200) questions.value = res.data.data || []
    }

    if (!props.bossMode) {
      questions.value = questions.value
        .filter(question => !question.knowledgePointId || String(question.knowledgePointId) === String(props.kpId))
        .slice(0, 5)
    } else {
      questions.value = questions.value.slice(0, 6)
    }

    questions.value.forEach(question => {
      answers[question.questionId] = question.type === 'multi' ? [] : ''
    })
  } catch {
    questions.value = []
  } finally {
    loading.value = false
  }
}

const submitBattle = async () => {
  submitting.value = true
  try {
    const gradable = questions.value.filter(question => ['single', 'multi', 'fill'].includes(question.type))
    const correctCount = gradable.filter(isCorrect).length
    const correctRate = gradable.length ? correctCount / gradable.length : 0

    if (props.taskNo) {
      const content = questions.value.map(question => ({
        no: question.questionId,
        response: Array.isArray(answers[question.questionId])
          ? answers[question.questionId].join(',')
          : (answers[question.questionId] || '')
      }))
      const formData = new FormData()
      formData.append('taskNo', props.taskNo)
      formData.append('content', JSON.stringify(content))
      const res = await submitTask(formData)
      if (res.data.code !== 200) {
        ElMessage.error(res.data.msg || '挑战提交失败')
        return
      }
    } else {
      ElMessage.warning('未找到可提交的测验任务，本次仅展示本地结算')
    }

    emit('profile-refresh')
    emit('battle-end', { cleared: correctRate >= 0.7, correctRate })
  } catch {
    ElMessage.error('挑战提交失败')
  } finally {
    submitting.value = false
  }
}

const useHint = async () => {
  hinting.value = true
  try {
    await sendGameEvent(props.studentId, {
      course_id: props.courseId,
      event_type: 'hint_used',
      question_id: activeQuestion.value.questionId
    })
    emit('profile-refresh')
    ElMessage.success('提示已记录')
  } catch {
    ElMessage.error('提示使用失败')
  } finally {
    hinting.value = false
  }
}

const skipQuestion = async () => {
  skipping.value = true
  try {
    await sendGameEvent(props.studentId, {
      course_id: props.courseId,
      event_type: 'answer_skipped',
      question_id: activeQuestion.value.questionId,
      knowledge_point_id: props.kpId
    })
    emit('profile-refresh')
    if (activeIndex.value < questions.value.length - 1) activeIndex.value++
  } catch {
    ElMessage.error('跳过失败')
  } finally {
    skipping.value = false
  }
}

onMounted(loadQuestions)
</script>

<style scoped>
.battle-room {
  color: #f9edce;
}

.battle-stage {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 20px;
}

.enemy-panel,
.combat-panel {
  border: 1px solid rgba(237, 183, 90, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(64, 33, 21, .95), rgba(21, 16, 14, .97));
  box-shadow: 0 18px 40px rgba(0, 0, 0, .36);
}

.enemy-panel {
  min-height: 460px;
  padding: 22px;
  text-align: center;
}

.combat-panel {
  padding: 22px;
}

.room-kicker {
  margin: 0 0 6px;
  color: #d99d4d;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h2,
h3 {
  margin: 0;
}

h3 {
  color: #fff5d6;
  line-height: 1.55;
}

.enemy-sigil {
  width: 150px;
  height: 190px;
  margin: 28px auto 18px;
  border: 3px solid #d29a51;
  border-radius: 18px;
  background:
    radial-gradient(circle at 50% 22%, rgba(255, 226, 151, .88) 0 16px, transparent 17px),
    radial-gradient(circle at 50% 72%, rgba(177, 51, 32, .9) 0 38px, transparent 39px),
    linear-gradient(145deg, #65301f, #150f0e);
  box-shadow: inset 0 0 0 7px rgba(255, 245, 210, .08), 0 22px 34px rgba(0, 0, 0, .38);
}

.boss .enemy-sigil {
  border-color: #f0c66b;
  background:
    radial-gradient(circle at 50% 24%, rgba(255, 238, 173, .95) 0 20px, transparent 21px),
    radial-gradient(circle at 50% 72%, rgba(124, 29, 29, .96) 0 44px, transparent 45px),
    linear-gradient(145deg, #7a2b1e, #180f10);
}

.enemy-name {
  margin: 0 0 16px;
  color: #fff5d6;
  font-size: 18px;
  font-weight: 800;
}

.enemy-meta {
  display: grid;
  gap: 8px;
}

.enemy-meta span {
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .07);
  color: #d8c19b;
}

.combat-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.difficulty-tag {
  background: #6b321f;
  border-color: #c7833d;
}

.answer-board {
  min-height: 210px;
  padding: 18px;
  border: 1px solid rgba(72, 48, 30, .18);
  border-radius: 8px;
  background: linear-gradient(180deg, #efe0bd, #d4b778);
  color: #2e2117;
}

.option-list {
  display: grid;
  gap: 12px;
}

.card-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
}

.mini-card {
  min-width: 46px;
  min-height: 54px;
  border: 1px solid rgba(232, 184, 92, .38);
  border-radius: 7px;
  color: #f8ebcb;
  background: linear-gradient(180deg, #4d271b, #1d1512);
  cursor: pointer;
  transition: transform .18s ease-out, border-color .18s ease-out, background .18s ease-out;
}

.mini-card.active {
  border-color: #f0c66b;
  background: linear-gradient(180deg, #9e4c24, #4a2219);
  transform: translateY(-3px);
}

.mini-card.answered::after {
  content: '';
  display: block;
  width: 16px;
  height: 3px;
  margin: 5px auto 0;
  border-radius: 999px;
  background: #8bd17c;
}

.battle-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
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

@media (max-width: 900px) {
  .battle-stage {
    grid-template-columns: 1fr;
  }
  .enemy-panel {
    min-height: auto;
  }
  .enemy-sigil {
    width: 110px;
    height: 132px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mini-card {
    transition: none;
  }
  .mini-card.active {
    transform: none;
  }
}
</style>
