<template>
  <section
    class="dialogue-combat"
    :class="{ boss: bossMode, 'hurt-flash': hurtFlash }"
    aria-label="知识对话战斗"
  >
    <div v-if="loading" class="scene-loading">
      <div class="game-loading-indicator" role="status">
        <span aria-hidden="true"></span>
        <p>正在加载题目</p>
      </div>
    </div>

    <div v-else-if="packError" class="scene-loading">
      <el-empty :description="packError" :image-size="90" />
      <button type="button" class="confirm-choice" @click="loadQuestions">重新加载题包</button>
    </div>

    <template v-else>
      <div class="scene-vignette" aria-hidden="true"></div>

      <div class="combat-mini-hud">
        <div class="hud-pill player-pill">
          <span>学习者 HP</span>
          <b>{{ playerHp }}/{{ playerMaxHp }}</b>
          <i><em :style="{ width: playerHpPercent + '%' }"></em></i>
        </div>
      </div>

      <div class="scene-actor player-actor" :class="{ damaged: hurtFlash }" aria-label="学习者">
        <div class="actor-aura"></div>
        <div class="player-sprite">
          <img :src="characterSprites.playerKnightGuard" alt="" />
        </div>
        <div class="actor-nameplate">
          <strong>学习者</strong>
          <span>护盾 {{ playerBlock }}</span>
        </div>
      </div>

      <div class="scene-actor enemy-actor" :class="{ hit: hitFlash }" aria-label="知识敌人">
        <div class="enemy-overhead-hp">
          <span>{{ enemyName }} HP</span>
          <b>{{ enemyHp }}/{{ enemyMaxHp }}</b>
          <i><em :style="{ width: enemyHpPercent + '%' }"></em></i>
        </div>
        <div class="actor-aura"></div>
        <div class="enemy-sprite">
          <img :src="enemyToken" :alt="enemyName" />
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
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionById, getQuestionsByKnowledgePoint, getTaskQuestions, getTowerQuestionPack, sendGameEvent, submitTask } from '../api'
import { characterSprites, enemySprites } from '../data/gameAssetManifest'
import { isQuestionAnswerCorrect, parseQuestionOptions } from '../utils/answerMatcher'

const props = defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  studentId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' },
  floorName: { type: String, default: '未知楼层' },
  bossMode: { type: Boolean, default: false },
  roomType: { type: String, default: 'battle' },
  initialHp: { type: Number, default: 100 },
  maxHp: { type: Number, default: 100 },
  runId: { type: [String, Number], default: '' },
  nodeId: { type: [String, Number], default: '' }
})

const emit = defineEmits(['battle-end', 'profile-refresh', 'ai-help'])

const loading = ref(true)
const submitting = ref(false)
const questions = ref([])
const answers = reactive({})
const answerRecords = ref([])
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
const selectedMulti = ref([])
const freeAnswer = ref('')
const packId = ref('')
const packError = ref('')

const playerMaxHp = computed(() => Math.max(1, props.maxHp || props.initialHp || 100))
const activeQuestion = computed(() => questions.value[activeIndex.value] || {})
const displayOptions = computed(() => parseQuestionOptions(activeQuestion.value.options))
const enemyName = computed(() => props.bossMode ? '章节首领' : props.roomType === 'elite' ? '精英知识敌人' : '知识敌人')
const answerSource = computed(() => props.bossMode ? 'boss_room' : props.roomType === 'elite' ? 'elite_room' : 'battle_room')
const enemyToken = computed(() => {
  if (props.bossMode) return enemySprites.bossEnemy
  if (props.roomType === 'elite') return enemySprites.eliteEnemy
  return enemySprites.knowledgeEnemy
})
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

watch(activeIndex, () => {
  const question = activeQuestion.value
  selectedMulti.value = Array.isArray(answers[question.questionId]) ? [...answers[question.questionId]] : []
  freeAnswer.value = typeof answers[question.questionId] === 'string' ? answers[question.questionId] : ''
})

const isCorrect = question => {
  return isQuestionAnswerCorrect(question, answers[question.questionId])
}

const setupBattleState = () => {
  const baseHp = props.bossMode ? 120 : props.roomType === 'elite' ? 92 : 74
  enemyMaxHp.value = Math.max(baseHp, questions.value.length * (props.bossMode ? 18 : 14))
  enemyHp.value = enemyMaxHp.value
  playerHp.value = Math.min(playerMaxHp.value, Math.max(1, props.initialHp || playerMaxHp.value))
  playerBlock.value = 0
  correctCount.value = 0
  resolvedCount.value = 0
  answerRecords.value = []
  activeIndex.value = 0
  choiceLocked.value = false
  finished.value = false
  feedback.value = null
}

const loadQuestions = async () => {
  loading.value = true
  questions.value = []
  packId.value = ''
  packError.value = ''
  let loadedFromTowerPack = false
  try {
    if (props.runId && props.nodeId) {
      const mode = props.bossMode ? 'boss' : props.roomType === 'elite' ? 'elite' : 'battle'
      const packRes = await getTowerQuestionPack(props.studentId, props.runId, props.nodeId, mode)
      if (packRes.data.code === 200) {
        packId.value = packRes.data.data?.packId || ''
        questions.value = packRes.data.data?.questions || []
        loadedFromTowerPack = questions.value.length > 0
      }
      if (!questions.value.length) {
        throw new Error(packRes.data.msg || '当前节点题包为空')
      }
    }

    if (!questions.value.length && props.taskNo) {
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

    if (!props.bossMode && !loadedFromTowerPack) {
      questions.value = questions.value
        .filter(question => !question.knowledgePointId || String(question.knowledgePointId) === String(props.kpId))
        .slice(0, roomQuestionLimit.value)
    } else {
      questions.value = questions.value.slice(0, roomQuestionLimit.value)
    }
  } catch (error) {
    packError.value = error?.message || '题目加载失败，请重试'
    questions.value = []
  } finally {
    if (!questions.value.length && !packError.value) packError.value = '当前节点暂无可用题目，请联系教师补充题库'
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
  const autoGradable = isAutoGradable(question)
  const correct = autoGradable && !skipped && isCorrect(question)
  resolvedCount.value += 1
  answerRecords.value = [
    ...answerRecords.value,
    answerRecord(question, correct, skipped, autoGradable)
  ]

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

  await recordAnswerEvent(correct, skipped)

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
  const gradableRecords = answerRecords.value.filter(record => record.source === answerSource.value && record.autoGradable)
  if (!gradableRecords.length) return 0
  return gradableRecords.filter(record => record.correct).length / gradableRecords.length
}

const isAutoGradable = question => ['single', 'multi', 'fill'].includes(question?.type)

const answerRecord = (question, correct, skipped, autoGradable = isAutoGradable(question)) => {
  const rawAnswer = answers[question.questionId]
  return {
    questionId: question.questionId,
    stem: question.stem,
    studentAnswer: Array.isArray(rawAnswer) ? rawAnswer.join(',') : (rawAnswer || ''),
    correctAnswer: question.answer,
    correct,
    autoGradable,
    answered: !skipped && (Array.isArray(rawAnswer) ? rawAnswer.length > 0 : String(rawAnswer || '').trim().length > 0),
    skipped,
    knowledgePointId: question.knowledgePointId || props.kpId,
    abilityPointId: question.abilityPointId || props.kpId,
    type: question.type,
    source: answerSource.value
  }
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
        finished.value = false
        choiceLocked.value = false
        return
      }
    } else {
      ElMessage.error('当前挑战缺少真实节点信息，无法结算')
      finished.value = false
      choiceLocked.value = false
      return
    }

    emit('profile-refresh')
    emit('battle-end', {
      cleared: Boolean(forcedCleared) && playerHp.value > 0,
      correctRate,
      battleCorrectRate: correctRate,
      hpLeft: playerHp.value,
      packId: packId.value,
      answerSummary: answerRecords.value
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
      ability_point_id: props.kpId,
      task_type: props.roomType,
      source_id: activeQuestion.value.questionId,
      hp_left: playerHp.value
    })
  } catch {
    // 行为记录失败不阻断战斗流程。
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
  top: 76px;
  left: clamp(22px, 2.4vw, 46px);
  display: block;
  pointer-events: none;
}

.hud-pill {
  width: min(340px, 32vw);
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

.scene-actor {
  position: absolute;
  z-index: 3;
  bottom: 150px;
  display: grid;
  place-items: center;
  transition: transform .22s ease, filter .22s ease;
}

.player-actor {
  left: clamp(50px, 11vw, 190px);
}

.enemy-actor {
  right: clamp(340px, 31vw, 660px);
  bottom: 190px;
}

.enemy-overhead-hp {
  position: absolute;
  z-index: 4;
  bottom: calc(100% + 10px);
  left: 50%;
  width: min(330px, 34vw);
  min-width: 240px;
  padding: 10px 12px;
  border: 1px solid rgba(245, 203, 118, .42);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(30, 19, 17, .78), rgba(9, 10, 14, .6));
  box-shadow: 0 14px 32px rgba(0, 0, 0, .34);
  transform: translateX(-50%);
  backdrop-filter: blur(5px);
  pointer-events: none;
}

.enemy-overhead-hp span,
.enemy-overhead-hp b {
  display: inline-block;
  margin-bottom: 6px;
}

.enemy-overhead-hp span {
  color: #e8c884;
  font-size: 12px;
  font-weight: 900;
}

.enemy-overhead-hp b {
  float: right;
  color: #fff4d6;
}

.enemy-overhead-hp i {
  display: block;
  clear: both;
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(25, 10, 9, .74);
}

.enemy-overhead-hp em {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #9d2326, #e16a3f);
  transition: width .24s ease;
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
  width: 220px;
  height: 268px;
  place-items: center;
  filter: drop-shadow(0 28px 30px rgba(0, 0, 0, .56));
}

.player-sprite img {
  max-width: 226px;
  max-height: 268px;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 0 18px rgba(242, 194, 93, .22));
}

.enemy-sprite {
  width: 300px;
  height: 260px;
}

.enemy-sprite img {
  width: 270px;
  height: 240px;
  object-fit: contain;
  object-position: center bottom;
  filter:
    drop-shadow(0 0 22px rgba(224, 84, 48, .34))
    drop-shadow(0 20px 24px rgba(0, 0, 0, .48));
}

.boss .enemy-sprite img {
  width: 300px;
  height: 300px;
}

.actor-nameplate {
  position: relative;
  display: grid;
  gap: 3px;
  min-width: 150px;
  max-width: 260px;
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
  overflow: hidden;
  color: #d7c09a;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  top: 180px;
  right: clamp(220px, 19vw, 430px);
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
.confirm-choice {
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
.confirm-choice:hover {
  border-color: rgba(255, 229, 159, .62);
  transform: translateX(-4px);
  background: linear-gradient(90deg, rgba(43, 48, 58, .86), rgba(18, 22, 28, .56));
}

.choice-button:disabled,
.confirm-choice:disabled {
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
    bottom: 160px;
  }
  .enemy-overhead-hp {
    width: min(300px, 46vw);
    min-width: 220px;
  }
  .intent-bubble {
    top: 170px;
    right: 22px;
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
  .enemy-overhead-hp {
    width: 260px;
    min-width: 0;
  }
  .intent-bubble {
    top: 188px;
    right: 16px;
    min-width: 146px;
  }
  .dialogue-layer {
    left: 14px;
    right: 14px;
  }
}
</style>
