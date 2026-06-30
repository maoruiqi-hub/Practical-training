<template>
  <section
    class="dialogue-combat"
    :class="{ boss: bossMode, 'hurt-flash': hurtFlash }"
    aria-label="知识对话战斗"
  >
    <div v-if="loading" class="scene-loading">
      <el-skeleton :rows="7" animated />
    </div>

    <template v-else>
      <div class="scene-vignette" aria-hidden="true"></div>

      <div class="combat-mini-hud">
        <div class="hud-pill player-pill">
          <span>学习者 HP</span>
          <b>{{ playerHp }}/{{ playerMaxHp }}</b>
          <i><em :style="{ width: playerHpPercent + '%' }"></em></i>
        </div>
        <div class="hud-pill enemy-pill">
          <span>{{ enemyName }} HP</span>
          <b>{{ enemyHp }}/{{ enemyMaxHp }}</b>
          <i><em :style="{ width: enemyHpPercent + '%' }"></em></i>
        </div>
      </div>

      <div class="scene-actor player-actor" :class="{ damaged: hurtFlash }" aria-label="学习者">
        <div class="actor-aura"></div>
        <div class="player-sprite">
          <span></span>
        </div>
        <div class="actor-nameplate">
          <strong>学习者</strong>
          <span>护盾 {{ playerBlock }}</span>
        </div>
      </div>

      <div class="scene-actor enemy-actor" :class="{ hit: hitFlash }" aria-label="知识敌人">
        <div class="actor-aura"></div>
        <div class="enemy-sprite">
          <img :src="enemyToken" alt="" />
        </div>
        <div class="actor-nameplate">
          <strong>{{ enemyName }}</strong>
          <span>{{ floorName }}</span>
        </div>
      </div>

      <div class="intent-bubble" :class="enemyIntent.type">
        <span>{{ enemyIntent.label }}</span>
        <b>答错 -{{ incomingDamage }} HP</b>
      </div>

      <div v-if="feedback" class="result-float" :class="feedback.type">
        {{ feedback.text }}
      </div>

      <section v-if="activeQuestion.questionId" class="dialogue-layer">
        <div class="speaker-line">
          <strong>{{ enemyName }}</strong>
          <span>第 {{ activeIndex + 1 }} / {{ questions.length }} 题 · {{ typeLabel(activeQuestion.type) }}</span>
        </div>

        <div class="dialogue-box">
          <p>{{ activeQuestion.stem }}</p>
          <small>选择答案推进对话。答对不会扣血，答错会触发敌人意图。</small>
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
              说出答案
            </button>
          </template>
        </div>
      </section>

      <div class="scene-tools">
        <button type="button" :disabled="hinting || choiceLocked" @click="useHint">提示</button>
        <button type="button" :disabled="choiceLocked" @click="gainBlock">护盾</button>
        <button type="button" :disabled="skipping || choiceLocked" @click="skipQuestion">跳过</button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionById, getQuestionsByKnowledgePoint, getTaskQuestions, sendGameEvent, submitTask } from '../api'
import { mapLegendIcons, referenceTokenIcons } from '../data/gameAssetManifest'

const props = defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  studentId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' },
  floorName: { type: String, default: '未知楼层' },
  bossMode: { type: Boolean, default: false },
  roomType: { type: String, default: 'battle' },
  initialHp: { type: Number, default: 100 },
  maxHp: { type: Number, default: 100 }
})

const emit = defineEmits(['battle-end', 'profile-refresh'])

const loading = ref(true)
const submitting = ref(false)
const hinting = ref(false)
const skipping = ref(false)
const questions = ref([])
const answers = reactive({})
const activeIndex = ref(0)
const playerBlock = ref(0)
const playerHp = ref(Math.max(1, props.initialHp || props.maxHp || 100))
const enemyHp = ref(0)
const enemyMaxHp = ref(70)
const correctCount = ref(0)
const resolvedCount = ref(0)
const choiceLocked = ref(false)
const finished = ref(false)
const feedback = ref(null)
const hurtFlash = ref(false)
const hitFlash = ref(false)
const usingFallbackQuestions = ref(false)
const selectedMulti = ref([])
const freeAnswer = ref('')

const playerMaxHp = computed(() => Math.max(1, props.maxHp || props.initialHp || 100))
const activeQuestion = computed(() => questions.value[activeIndex.value] || {})
const displayOptions = computed(() => parseOptions(activeQuestion.value.options))
const enemyName = computed(() => props.bossMode ? '章节首领' : props.roomType === 'elite' ? '精英知识敌人' : '知识敌人')
const enemyToken = computed(() => props.bossMode ? referenceTokenIcons.bossHeartFlame : mapLegendIcons.enemy)
const typeLabel = type => ({ single: '单选', multi: '多选', fill: '填空', essay: '简答', program: '编程' })[type] || type
const roomQuestionLimit = computed(() => props.bossMode ? 8 : props.roomType === 'elite' ? 6 : 5)
const playerHpPercent = computed(() => Math.round((playerHp.value / playerMaxHp.value) * 100))
const enemyHpPercent = computed(() => Math.round((enemyHp.value / Math.max(1, enemyMaxHp.value)) * 100))

const enemyIntent = computed(() => {
  const question = activeQuestion.value
  if (!question.questionId) return { type: 'wait', label: '等待', value: 0 }
  if (question.type === 'program') return { type: 'code', label: '代码压迫', value: 18 }
  if (question.type === 'multi') return { type: 'debuff', label: '干扰判断', value: 12 }
  if (props.bossMode) return { type: 'attack', label: '综合考察', value: 16 }
  return { type: 'quiz', label: '概念攻击', value: 10 }
})
const incomingDamage = computed(() => Math.max(0, Number(enemyIntent.value.value || 0) - playerBlock.value))

const fallbackQuestions = computed(() => {
  const base = [
    {
      type: 'single',
      stem: '在 Python 中，下面哪个名称最适合作为变量名？',
      options: ['A. 2score', 'B. score_total', 'C. class', 'D. total-score'],
      answer: 'B',
      score: 10
    },
    {
      type: 'single',
      stem: '表达式 3 + 4 * 2 的计算结果是？',
      options: ['A. 14', 'B. 11', 'C. 10', 'D. 16'],
      answer: 'B',
      score: 10
    },
    {
      type: 'multi',
      stem: '下面哪些属于 Python 的基础数据类型？',
      options: ['A. int', 'B. list', 'C. heading', 'D. str'],
      answer: 'A,B,D',
      score: 15
    },
    {
      type: 'fill',
      stem: '用于向控制台输出内容的内置函数是 ____。',
      answer: 'print',
      score: 10
    },
    {
      type: 'single',
      stem: 'if 语句判断条件为 False 时，程序会优先执行哪个分支？',
      options: ['A. if', 'B. elif 或 else', 'C. import', 'D. def'],
      answer: 'B',
      score: 10
    },
    {
      type: 'single',
      stem: 'for i in range(3) 会依次得到哪些值？',
      options: ['A. 1,2,3', 'B. 0,1,2', 'C. 0,1,2,3', 'D. 3,2,1'],
      answer: 'B',
      score: 10
    },
    {
      type: 'fill',
      stem: '定义函数时使用的关键字是 ____。',
      answer: 'def',
      score: 10
    },
    {
      type: 'program',
      stem: '写一行代码：把变量 name 的值输出到控制台。',
      answer: 'print(name)',
      score: 20
    }
  ]

  return base.slice(0, roomQuestionLimit.value).map((question, index) => ({
    ...question,
    questionId: `demo-${props.roomType}-${props.kpId}-${index + 1}`,
    knowledgePointId: props.kpId
  }))
})

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

const setupBattleState = () => {
  const baseHp = props.bossMode ? 120 : props.roomType === 'elite' ? 92 : 74
  enemyMaxHp.value = Math.max(baseHp, questions.value.length * (props.bossMode ? 18 : 14))
  enemyHp.value = enemyMaxHp.value
  playerHp.value = Math.min(playerMaxHp.value, Math.max(1, props.initialHp || playerMaxHp.value))
  playerBlock.value = 0
  correctCount.value = 0
  resolvedCount.value = 0
  activeIndex.value = 0
  choiceLocked.value = false
  finished.value = false
  feedback.value = null
}

const loadQuestions = async () => {
  loading.value = true
  usingFallbackQuestions.value = false
  questions.value = []
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
        .slice(0, roomQuestionLimit.value)
    } else {
      questions.value = questions.value.slice(0, roomQuestionLimit.value)
    }
  } catch {
    questions.value = []
  } finally {
    if (!questions.value.length) {
      questions.value = fallbackQuestions.value
      usingFallbackQuestions.value = true
    }
    questions.value.forEach(question => {
      answers[question.questionId] = question.type === 'multi' ? [] : ''
    })
    setupBattleState()
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

const resolveCurrentAnswer = async ({ skipped = false } = {}) => {
  if (choiceLocked.value || finished.value) return
  choiceLocked.value = true

  const question = activeQuestion.value
  const correct = !skipped && isCorrect(question)
  resolvedCount.value += 1

  if (correct) {
    correctCount.value += 1
    const damage = props.bossMode ? 24 : props.roomType === 'elite' ? 22 : 20
    enemyHp.value = Math.max(0, enemyHp.value - damage)
    hitFlash.value = true
    feedback.value = { type: 'correct', text: '回答正确，敌人受击' }
    setTimeout(() => { hitFlash.value = false }, 420)
  } else if (skipped) {
    feedback.value = { type: 'wrong', text: '跳过本题，没有造成伤害' }
  } else {
    const damage = applyDamage()
    feedback.value = { type: 'wrong', text: `回答错误，HP -${damage}` }
  }

  if (!usingFallbackQuestions.value) {
    await recordAnswerEvent(correct, skipped)
  }

  setTimeout(() => {
    if (playerHp.value <= 0) {
      finishBattle(false)
      return
    }
    if (enemyHp.value <= 0 || activeIndex.value >= questions.value.length - 1) {
      finishBattle(enemyHp.value <= 0 || currentCorrectRate() >= (props.bossMode ? 0.75 : 0.7))
      return
    }
    activeIndex.value += 1
    feedback.value = null
    choiceLocked.value = false
  }, 760)
}

const applyDamage = () => {
  const rawDamage = Number(enemyIntent.value.value || 0)
  const blocked = Math.min(playerBlock.value, rawDamage)
  playerBlock.value = Math.max(0, playerBlock.value - rawDamage)
  const damage = Math.max(0, rawDamage - blocked)
  playerHp.value = Math.max(0, playerHp.value - damage)
  hurtFlash.value = true
  setTimeout(() => { hurtFlash.value = false }, 420)
  return damage
}

const currentCorrectRate = () => {
  const gradable = questions.value.filter(question => ['single', 'multi', 'fill'].includes(question.type))
  const denominator = Math.max(1, Math.min(gradable.length || questions.value.length, resolvedCount.value || questions.value.length))
  return correctCount.value / denominator
}

const finishBattle = async forcedCleared => {
  if (finished.value) return
  finished.value = true
  choiceLocked.value = true
  await submitBattle(forcedCleared)
}

const submitBattle = async forcedCleared => {
  submitting.value = true
  try {
    const correctRate = currentCorrectRate()

    if (props.taskNo && !usingFallbackQuestions.value) {
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
        finished.value = false
        choiceLocked.value = false
        return
      }
    } else {
      ElMessage.warning(usingFallbackQuestions.value ? '演示题卡已本地结算' : '未找到可提交任务，本次使用本地结算')
    }

    emit('profile-refresh')
    emit('battle-end', {
      cleared: Boolean(forcedCleared) && playerHp.value > 0,
      correctRate,
      hpLeft: playerHp.value
    })
  } catch {
    ElMessage.error('挑战提交失败')
    finished.value = false
    choiceLocked.value = false
  } finally {
    submitting.value = false
  }
}

const recordAnswerEvent = async (correct, skipped) => {
  try {
    await sendGameEvent(props.studentId, {
      course_id: props.courseId,
      event_type: skipped ? 'answer_skipped' : correct ? 'answer_correct' : 'answer_wrong',
      question_id: activeQuestion.value.questionId,
      knowledge_point_id: props.kpId,
      hp_left: playerHp.value
    })
  } catch {
    // 行为记录失败不阻断战斗流程。
  }
}

const useHint = async () => {
  hinting.value = true
  try {
    if (usingFallbackQuestions.value) {
      ElMessage.success('提示：先定位题干里的核心概念，再排除明显不符合语法的选项')
      return
    }
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

const gainBlock = () => {
  playerBlock.value += 8
  ElMessage.success('获得 8 点护盾，本题答错可抵消伤害')
}

const skipQuestion = async () => {
  skipping.value = true
  try {
    await resolveCurrentAnswer({ skipped: true })
  } finally {
    skipping.value = false
  }
}

onMounted(loadQuestions)
</script>

<style scoped>
.dialogue-combat {
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
  background: rgba(6, 8, 12, .54);
}

.scene-vignette {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 46%, rgba(255, 230, 170, .08), transparent 34%),
    linear-gradient(180deg, rgba(6, 8, 12, .08), rgba(6, 8, 12, .74) 78%),
    linear-gradient(90deg, rgba(4, 6, 10, .62), transparent 32%, transparent 68%, rgba(4, 6, 10, .62));
  pointer-events: none;
}

.combat-mini-hud {
  position: absolute;
  z-index: 5;
  top: 18px;
  left: 24px;
  right: 24px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  pointer-events: none;
}

.hud-pill {
  width: min(360px, 42vw);
  padding: 10px 12px;
  border: 1px solid rgba(245, 203, 118, .38);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(30, 19, 17, .72), rgba(9, 10, 14, .54));
  box-shadow: 0 14px 32px rgba(0, 0, 0, .34);
  backdrop-filter: blur(5px);
}

.hud-pill span,
.hud-pill b {
  display: inline-block;
  margin-bottom: 6px;
}

.hud-pill span {
  color: #e8c884;
  font-size: 12px;
  font-weight: 900;
}

.hud-pill b {
  float: right;
  color: #fff4d6;
}

.hud-pill i {
  display: block;
  clear: both;
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(25, 10, 9, .74);
}

.hud-pill em {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #cf322f, #f3bf66);
  transition: width .24s ease;
}

.enemy-pill em {
  background: linear-gradient(90deg, #9d2326, #e16a3f);
}

.scene-actor {
  position: absolute;
  z-index: 3;
  bottom: 132px;
  display: grid;
  place-items: center;
  transition: transform .22s ease, filter .22s ease;
}

.player-actor {
  left: clamp(50px, 11vw, 190px);
}

.enemy-actor {
  right: clamp(60px, 13vw, 220px);
}

.actor-aura {
  position: absolute;
  width: 210px;
  height: 94px;
  bottom: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(238, 185, 92, .28), transparent 68%);
  filter: blur(1px);
}

.player-sprite,
.enemy-sprite {
  position: relative;
  display: grid;
  width: 156px;
  height: 218px;
  place-items: center;
  filter: drop-shadow(0 28px 30px rgba(0, 0, 0, .56));
}

.player-sprite span {
  width: 86px;
  height: 138px;
  border-radius: 46% 46% 30% 30%;
  background:
    radial-gradient(circle at 50% 12%, #ffe2a1 0 23px, transparent 24px),
    linear-gradient(180deg, #c78a43 0 42px, #31b3b4 43px 138px);
  box-shadow:
    inset 0 -18px 24px rgba(15, 66, 70, .48),
    0 0 34px rgba(70, 216, 218, .24);
}

.enemy-sprite {
  width: 176px;
  height: 218px;
}

.enemy-sprite img {
  width: 142px;
  height: 142px;
  object-fit: contain;
  filter: drop-shadow(0 0 28px rgba(224, 84, 48, .34));
}

.boss .enemy-sprite img {
  width: 168px;
  height: 168px;
}

.actor-nameplate {
  position: relative;
  display: grid;
  gap: 3px;
  min-width: 150px;
  padding: 8px 14px;
  border: 1px solid rgba(244, 202, 118, .36);
  border-radius: 999px;
  text-align: center;
  background: rgba(9, 10, 14, .56);
  backdrop-filter: blur(4px);
}

.actor-nameplate strong {
  color: #fff3d0;
}

.actor-nameplate span {
  color: #d7c09a;
  font-size: 12px;
}

.enemy-actor.hit {
  transform: translateX(12px) scale(.98);
  filter: brightness(1.35);
}

.player-actor.damaged {
  transform: translateX(-10px);
  filter: brightness(1.25) saturate(1.2);
}

.intent-bubble {
  position: absolute;
  z-index: 4;
  top: 96px;
  right: clamp(220px, 22vw, 340px);
  display: grid;
  gap: 4px;
  min-width: 170px;
  padding: 12px 16px;
  border: 1px solid rgba(246, 205, 126, .34);
  border-radius: 18px 18px 4px 18px;
  background: rgba(28, 18, 17, .68);
  box-shadow: 0 16px 32px rgba(0, 0, 0, .32);
  backdrop-filter: blur(5px);
}

.intent-bubble span {
  color: #ffe2a0;
  font-size: 13px;
  font-weight: 900;
}

.intent-bubble b {
  color: #ff856c;
  font-size: 18px;
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
  color: #442814;
  background: linear-gradient(180deg, #f2d688, #bd7d34);
}

.speaker-line span {
  color: #e6d1a9;
  font-size: 13px;
}

.dialogue-box {
  grid-column: 1;
  min-height: 116px;
  padding: 22px 26px;
  border-top: 1px solid rgba(255, 238, 183, .42);
  border-bottom: 1px solid rgba(255, 238, 183, .3);
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
  border: 1px solid rgba(255, 239, 198, .22);
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
  border-color: rgba(255, 229, 159, .62);
  transform: translateX(-4px);
  background: linear-gradient(90deg, rgba(43, 48, 58, .86), rgba(18, 22, 28, .56));
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
  color: #2c2218;
  background: #f4d78e;
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
  background: linear-gradient(90deg, rgba(151, 74, 37, .9), rgba(97, 42, 28, .72));
}

.free-answer :deep(.el-textarea__inner),
.free-answer :deep(.el-input__inner) {
  border-color: rgba(255, 239, 198, .26);
  color: #fff7dd;
  background: rgba(8, 10, 14, .62);
  box-shadow: none;
}

.scene-tools {
  position: absolute;
  z-index: 7;
  top: 92px;
  left: 28px;
  display: flex;
  gap: 10px;
}

.scene-tools button {
  min-width: 64px;
  min-height: 36px;
  border-radius: 999px;
  font-weight: 800;
}

.hurt-flash::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 9;
  background: rgba(178, 37, 31, .18);
  pointer-events: none;
  animation: hurtPulse .42s ease both;
}

@keyframes hurtPulse {
  from { opacity: 0; }
  35% { opacity: 1; }
  to { opacity: 0; }
}

@media (max-width: 980px) {
  .dialogue-combat {
    min-height: 760px;
  }
  .dialogue-layer {
    grid-template-columns: 1fr;
  }
  .choice-stack {
    grid-column: 1;
    grid-row: auto;
  }
  .enemy-actor {
    right: 8vw;
  }
  .player-actor {
    left: 8vw;
  }
}

@media (max-width: 680px) {
  .combat-mini-hud {
    flex-direction: column;
    right: auto;
  }
  .hud-pill {
    width: min(330px, calc(100vw - 48px));
  }
  .scene-actor {
    transform: scale(.72);
  }
  .player-actor {
    left: -10px;
  }
  .enemy-actor {
    right: -10px;
  }
  .dialogue-layer {
    left: 14px;
    right: 14px;
  }
}
</style>
