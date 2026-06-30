<template>
  <div class="floor-page" v-loading="loading" :style="pageStyle">
    <GameHud :profile="profile" :course-name="courseName" compact />

    <main class="floor-shell" :class="{ 'scene-shell': scenePhase }">
      <header class="floor-header" :class="{ 'scene-header': scenePhase }">
        <el-button class="ghost-button icon-only" aria-label="返回地图" @click="backToMap">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="floor-title">
          <p class="kicker">{{ roomKicker }}</p>
          <h1>{{ floor.kpName || floorName }}</h1>
        </div>
        <div class="header-actions">
          <el-button class="ghost-button" @click="supplyVisible = true">
            <el-icon><Box /></el-icon>
            补给
          </el-button>
          <el-button
            v-if="phase === 'diagnosis'"
            class="spire-button"
            @click="startBattle"
          >
            <el-icon><Aim /></el-icon>
            直接挑战
          </el-button>
        </div>
      </header>

      <section class="room-frame" :class="{ 'scene-frame': scenePhase }" :style="roomFrameStyle">
        <div v-if="!scenePhase" class="room-progress" aria-label="房间进度">
          <span
            v-for="item in progressItems"
            :key="item.key"
            :class="{ active: phase === item.key, done: item.done }"
          >
            {{ item.label }}
          </span>
        </div>

        <DiagnosisRoom
          v-if="phase === 'diagnosis'"
          :kp-id="kpId"
          :course-id="courseId"
          :task-no="taskNo"
          @diagnosed="handleDiagnosed"
        />

        <BossRoom
          v-else-if="phase === 'boss'"
          :kp-id="kpId"
          :course-id="courseId"
          :student-id="studentId"
          :task-no="taskNo"
          :floor-name="floor.kpName || floorName"
          :initial-hp="playerInitialHp"
          :max-hp="playerMaxHp"
          @boss-end="handleBattleEnd"
          @profile-refresh="refreshProfile"
        />

        <BattleRoom
          v-else-if="phase === 'battle'"
          :kp-id="kpId"
          :course-id="courseId"
          :student-id="studentId"
          :task-no="taskNo"
          :floor-name="floor.kpName || floorName"
          :room-type="roomType"
          :initial-hp="playerInitialHp"
          :max-hp="playerMaxHp"
          @battle-end="handleBattleEnd"
          @profile-refresh="refreshProfile"
        />

        <RewardDraft
          v-else-if="phase === 'reward'"
          :battle-result="battleResult"
          :floor-name="floor.kpName || floorName"
          :room-type="roomType"
          @reward-picked="handleRewardPicked"
        />

        <section v-else class="settlement-room">
          <div class="settlement-panel">
            <p class="kicker">Run Report</p>
            <h2>{{ settlementTitle }}</h2>
            <p class="settlement-copy">{{ settlementCopy }}</p>

            <div class="report-grid">
              <span>正确率 <b>{{ Math.round((battleResult.correctRate || 0) * 100) }}%</b></span>
              <span>节点结果 <b>{{ battleResult.cleared ? '已通过' : '待强化' }}</b></span>
              <span>获得奖励 <b>{{ pickedReward?.name || '无' }}</b></span>
            </div>

            <div class="settlement-actions">
              <el-button class="ghost-button" @click="phase = 'diagnosis'">重新诊断</el-button>
              <el-button class="spire-button" @click="backToMap">返回地图</el-button>
            </div>
          </div>
        </section>
      </section>

      <aside v-if="!scenePhase" class="intel-dock" aria-label="楼层情报">
        <section>
          <p class="kicker">Floor Intel</p>
          <h2>节点情报</h2>
          <div class="info-list">
            <span>知识点 <b>{{ kpId }}</b></span>
            <span>房间 <b>{{ roomLabel }}</b></span>
            <span>任务 <b>{{ taskNo || '本地题库' }}</b></span>
          </div>
        </section>

        <section>
          <p class="kicker">Diagnosis</p>
          <h2>诊断结果</h2>
          <div v-if="diagnosisResult" class="diagnosis-card" :class="diagnosisResult.status">
            <strong>{{ diagnosisText }}</strong>
            <span>{{ Math.round((diagnosisResult.correctRate || 0) * 100) }}%</span>
          </div>
          <el-empty v-else description="尚未诊断" :image-size="80" />
        </section>
      </aside>
    </main>

    <SupplyModal
      v-model="supplyVisible"
      :student-id="studentId"
      :course-id="courseId"
      :current-kp-id="kpId"
      :profile="profile"
      @used="refreshProfile"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, ArrowLeft, Box } from '@element-plus/icons-vue'
import BattleRoom from '../components/BattleRoom.vue'
import BossRoom from '../components/BossRoom.vue'
import DiagnosisRoom from '../components/DiagnosisRoom.vue'
import GameHud from '../components/GameHud.vue'
import RewardDraft from '../components/RewardDraft.vue'
import SupplyModal from '../components/SupplyModal.vue'
import { gameBackgrounds } from '../data/gameAssetManifest'
import { getStudentProfile, getTaskList, getTowerMap, sendGameEvent } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const profile = ref({})
const floor = ref({})
const taskNo = ref('')
const diagnosisResult = ref(null)
const battleResult = ref({ cleared: false, correctRate: 0 })
const pickedReward = ref(null)
const supplyVisible = ref(false)

const kpId = computed(() => route.params.kpId)
const roomType = computed(() => String(route.query.roomType || (route.query.boss === '1' ? 'boss' : 'battle')))
const courseId = computed(() => route.query.courseId || route.query.course_id || localStorage.getItem('courseId') || '1')
const courseName = computed(() => route.query.courseName || localStorage.getItem('courseName') || 'Python 程序设计')
const floorName = computed(() => route.query.floorName || floor.value.kpName || `第 ${kpId.value} 层`)
const studentId = computed(() =>
  user.studentNo || user.student_no || user.no || user.id || user.username || user.name || '1'
)
const isBossFloor = computed(() => roomType.value === 'boss' || route.query.boss === '1' || floor.value.boss || floor.value.isBoss)
const initialPhase = computed(() => {
  if (isBossFloor.value) return 'boss'
  if (roomType.value === 'diagnosis') return 'diagnosis'
  return 'battle'
})
const phase = ref('battle')
const scenePhase = computed(() => ['diagnosis', 'battle', 'boss'].includes(phase.value))
const playerInitialHp = computed(() => Number(profile.value.hp || profile.value.currentHp || profile.value.current_hp || 100))
const playerMaxHp = computed(() => Number(profile.value.maxHp || profile.value.max_hp || 100))

const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .56), rgba(6, 8, 12, .9)), url(${backgroundForPhase.value})`
}))

const backgroundForPhase = computed(() => {
  if (phase.value === 'diagnosis') return gameBackgrounds.diagnosis
  if (phase.value === 'boss') return gameBackgrounds.boss
  if (phase.value === 'reward') return gameBackgrounds.reward
  if (phase.value === 'settlement') return gameBackgrounds.mapAct1
  return isBossFloor.value ? gameBackgrounds.boss : gameBackgrounds.combat
})

const roomFrameStyle = computed(() => ({
  backgroundImage: scenePhase.value
    ? `linear-gradient(180deg, rgba(8, 10, 14, .08), rgba(8, 10, 14, .28)), url(${backgroundForPhase.value})`
    : `linear-gradient(180deg, rgba(8, 10, 14, .18), rgba(8, 10, 14, .72)), url(${backgroundForPhase.value})`
}))

const roomKicker = computed(() => {
  if (phase.value === 'diagnosis') return 'Diagnosis Room'
  if (phase.value === 'reward') return 'Reward Room'
  if (phase.value === 'settlement') return 'Settlement'
  return isBossFloor.value ? 'Boss Floor' : 'Battle Room'
})

const roomLabel = computed(() => ({
  diagnosis: '诊断房',
  battle: '普通战斗',
  elite: '精英战斗',
  boss: 'Boss 战'
})[roomType.value] || '普通战斗')

const progressItems = computed(() => [
  { key: 'diagnosis', label: '侦察', done: Boolean(diagnosisResult.value) },
  { key: isBossFloor.value ? 'boss' : 'battle', label: isBossFloor.value ? 'Boss' : '战斗', done: ['reward', 'settlement'].includes(phase.value) },
  { key: 'reward', label: '奖励', done: Boolean(pickedReward.value) },
  { key: 'settlement', label: '回报', done: phase.value === 'settlement' }
])

const diagnosisText = computed(() => ({
  mastered: '掌握良好，可快速推进',
  partial: '基础可用，建议常规挑战',
  weak: '薄弱节点，建议谨慎作战'
})[diagnosisResult.value?.status] || '未判定')

const settlementTitle = computed(() => battleResult.value.cleared ? '节点已完成' : '本次挑战未通过')
const settlementCopy = computed(() => {
  if (battleResult.value.cleared) return '本节点状态已经记录。回到地图后继续选择下一条路线。'
  return '建议先补给或重新诊断，确认薄弱题型后再挑战。'
})

const pickProfile = payload => payload?.profile || payload || {}

const refreshProfile = async () => {
  try {
    const res = await getStudentProfile(studentId.value, courseId.value)
    if (res.data.code === 200) profile.value = pickProfile(res.data.data)
  } catch {
    if (!Object.keys(profile.value || {}).length) profile.value = {}
  }
}

const loadFloor = async () => {
  try {
    const res = await getTowerMap(studentId.value, courseId.value)
    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      const match = res.data.data.find(item =>
        String(item.kpId || item.knowledgePointId || item.id) === String(kpId.value)
      )
      floor.value = match || { kpId: kpId.value, kpName: floorName.value }
    }
  } catch {
    floor.value = { kpId: kpId.value, kpName: floorName.value }
  }
}

const taskMatchesFloor = task => {
  const haystack = [
    task.taskName,
    task.title,
    task.description,
    task.knowledgePoints,
    task.knowledgePointName,
    task.taskType,
    task.type
  ].filter(Boolean).join(' ').toLowerCase()
  const name = String(floorName.value || '').toLowerCase()
  return haystack.includes(String(kpId.value).toLowerCase()) || (name && haystack.includes(name))
}

const findTask = async () => {
  try {
    const res = await getTaskList(courseId.value)
    if (res.data.code !== 200 || !Array.isArray(res.data.data)) return
    const tasks = res.data.data
    const quizTasks = tasks.filter(task => {
      const type = String(task.taskType || task.type || task.description || '').toLowerCase()
      return type.includes('quiz') || type.includes('测验') || type.includes('考试') || type.includes('练习')
    })
    const bossTasks = quizTasks.filter(task => String(task.description || task.taskName || '').includes('Boss') || String(task.description || '').includes('综合'))
    const matched = quizTasks.find(taskMatchesFloor)
    const target = matched || (isBossFloor.value ? bossTasks[0] : null) || quizTasks[0] || tasks[0]
    taskNo.value = target?.taskNo || target?.task_no || target?.no || ''
  } catch {
    taskNo.value = ''
  }
}

const loadData = async () => {
  loading.value = true
  try {
    phase.value = initialPhase.value
    await Promise.all([refreshProfile(), loadFloor()])
    await findTask()
  } finally {
    loading.value = false
  }
}

const startBattle = () => {
  phase.value = isBossFloor.value ? 'boss' : 'battle'
}

const handleDiagnosed = async result => {
  diagnosisResult.value = result
  await sendEvent('diagnosis_finished', {
    correct_rate: result.correctRate,
    status: result.status
  })
  if (result.status === 'mastered' && !isBossFloor.value) {
    battleResult.value = { cleared: true, correctRate: result.correctRate || 1 }
    phase.value = 'reward'
    ElMessage.success('诊断表现优秀，普通战斗已跳过，进入奖励选择')
    return
  }
  phase.value = isBossFloor.value ? 'boss' : 'battle'
}

const handleBattleEnd = async result => {
  battleResult.value = result
  if (Number.isFinite(Number(result.hpLeft))) {
    profile.value = { ...profile.value, hp: Number(result.hpLeft), maxHp: playerMaxHp.value }
  }
  await sendEvent('battle_finished', {
    correct_rate: result.correctRate,
    cleared: result.cleared,
    room_type: roomType.value
  })
  await refreshProfile()
  phase.value = result.cleared ? 'reward' : 'settlement'
}

const handleRewardPicked = async payload => {
  pickedReward.value = payload.reward
  applyProfileDelta(payload.profileDelta || {})
  await sendEvent('reward_picked', {
    reward_id: payload.reward?.id,
    reward_name: payload.reward?.name
  })
  ElMessage.success(`获得奖励：${payload.reward?.name || '金币'}`)
  phase.value = 'settlement'
}

const applyProfileDelta = delta => {
  if (!delta || !Object.keys(delta).length) return
  const next = { ...profile.value }
  if (delta.hp) next.hp = Math.min(Number(next.maxHp || next.max_hp || 100), Number(next.hp || 0) + Number(delta.hp))
  if (delta.coins) next.coins = Number(next.coins || 0) + Number(delta.coins)
  profile.value = next
}

const sendEvent = async (eventType, extra = {}) => {
  try {
    await sendGameEvent(studentId.value, {
      course_id: courseId.value,
      knowledge_point_id: kpId.value,
      event_type: eventType,
      ...extra
    })
  } catch {
    // 行为记录失败不阻断学习流程。
  }
}

const backToMap = () => {
  router.push({
    path: '/tower-map',
    query: { courseId: courseId.value, courseName: courseName.value }
  })
}

onMounted(loadData)
</script>

<style scoped>
.floor-page {
  min-height: 100vh;
  color: #f8edcf;
  background-position: center;
  background-size: cover;
  background-attachment: fixed;
}

.floor-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  width: min(1460px, calc(100% - 32px));
  margin: 0 auto;
  padding: 18px 0 34px;
}

.floor-shell.scene-shell {
  grid-template-columns: 1fr;
  gap: 0;
  width: 100%;
  max-width: none;
  padding: 0;
}

.floor-header {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 8px 0;
}

.scene-header {
  position: absolute;
  z-index: 20;
  top: 18px;
  left: 24px;
  right: 24px;
  padding: 0;
  pointer-events: none;
}

.scene-header .ghost-button,
.scene-header .spire-button {
  pointer-events: auto;
  box-shadow: 0 12px 28px rgba(0, 0, 0, .34);
  backdrop-filter: blur(6px);
}

.scene-header .floor-title {
  align-self: start;
  padding-top: 2px;
  text-shadow: 0 4px 18px rgba(0, 0, 0, .68);
}

.scene-header h1 {
  font-size: clamp(24px, 2.3vw, 34px);
}

.floor-title {
  min-width: 0;
}

.kicker {
  margin: 0 0 6px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #fff6dc;
}

h1 {
  overflow: hidden;
  font-size: 34px;
  line-height: 1.12;
  text-overflow: ellipsis;
  white-space: nowrap;
}

h2 {
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.room-frame {
  position: relative;
  overflow: hidden;
  min-height: 690px;
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background-position: center;
  background-size: cover;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .48);
}

.room-frame.scene-frame {
  min-height: calc(100vh - 88px);
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.room-progress {
  position: absolute;
  z-index: 4;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.room-progress span {
  min-height: 30px;
  padding: 6px 10px;
  border: 1px solid rgba(238, 181, 91, .22);
  border-radius: 999px;
  color: #cdb894;
  background: rgba(8, 10, 14, .62);
  backdrop-filter: blur(4px);
}

.room-progress span.active {
  border-color: #e4ad58;
  color: #fff4d6;
}

.room-progress span.done {
  color: #bfe4b0;
}

.intel-dock {
  display: grid;
  align-content: start;
  gap: 14px;
}

.intel-dock section,
.settlement-panel {
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(60, 31, 20, .9), rgba(14, 12, 13, .94));
  box-shadow: 0 18px 44px rgba(0, 0, 0, .36);
}

.intel-dock section {
  padding: 18px;
}

.info-list,
.report-grid {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.info-list span,
.report-grid span {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 36px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #d9c4a2;
  background: rgba(255, 255, 255, .07);
}

.info-list b,
.report-grid b {
  overflow: hidden;
  max-width: 160px;
  color: #fff4d4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diagnosis-card {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .07);
}

.diagnosis-card strong {
  color: #fff3d2;
}

.diagnosis-card span {
  color: #bcdba3;
  font-size: 28px;
  font-weight: 900;
}

.diagnosis-card.weak span {
  color: #e17a5e;
}

.settlement-room {
  display: grid;
  min-height: 690px;
  place-items: center;
  padding: 28px;
  background:
    radial-gradient(circle at 50% 40%, rgba(232, 184, 92, .14), transparent 32%),
    linear-gradient(180deg, rgba(8, 10, 14, .12), rgba(8, 10, 14, .78));
}

.settlement-panel {
  width: min(720px, 100%);
  padding: 28px;
}

.settlement-panel h2 {
  font-size: 32px;
}

.settlement-copy {
  color: #dec8a4;
  line-height: 1.7;
}

.settlement-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.spire-button,
.ghost-button {
  min-height: 42px;
  border-radius: 6px;
}

.spire-button {
  border-color: #da9a4d;
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #73301f);
}

.ghost-button {
  border-color: rgba(238, 181, 91, .36);
  color: #f8ebcb;
  background: rgba(255, 255, 255, .08);
}

.icon-only {
  min-width: 44px;
  padding-inline: 0;
}

@media (max-width: 1120px) {
  .floor-shell {
    grid-template-columns: 1fr;
  }
  .intel-dock {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .floor-shell {
    width: min(100% - 24px, 720px);
  }
  .floor-header {
    grid-template-columns: 1fr;
  }
  h1 {
    white-space: normal;
  }
  .room-frame,
  .settlement-room {
    min-height: 620px;
  }
  .intel-dock {
    grid-template-columns: 1fr;
  }
}
</style>
