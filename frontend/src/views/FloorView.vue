<template>
  <div class="floor-page" v-loading="loading">
    <GameHud :profile="profile" :course-name="courseName" />

    <header class="floor-header">
      <el-button class="ghost-button" @click="router.push('/tower-map')">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div>
        <p class="kicker">{{ isBossFloor ? 'Boss Floor' : 'Knowledge Floor' }}</p>
        <h1>{{ floor.kpName || floorName }}</h1>
      </div>
      <div class="header-actions">
        <el-button class="ghost-button" @click="supplyVisible = true">
          <el-icon><Box /></el-icon>
          补给
        </el-button>
        <el-button class="spire-button" @click="startBattle">
          <el-icon><Aim /></el-icon>
          直接挑战
        </el-button>
      </div>
    </header>

    <section class="floor-layout">
      <aside class="floor-rail">
        <div
          v-for="item in steps"
          :key="item.key"
          class="rail-step"
          :class="{ active: phase === item.key, done: item.done }"
        >
          <span>{{ item.order }}</span>
          <strong>{{ item.label }}</strong>
        </div>
      </aside>

      <main class="room-stage">
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
          @battle-end="handleBattleEnd"
          @profile-refresh="refreshProfile"
        />

        <section v-else class="settlement-room">
          <p class="kicker">Settlement</p>
          <h2>{{ battleResult.cleared ? '楼层已通过' : '本次挑战未通过' }}</h2>
          <el-progress
            :percentage="Math.round((battleResult.correctRate || 0) * 100)"
            :stroke-width="14"
            :color="battleResult.cleared ? '#8bd17c' : '#d95d43'"
          />
          <p class="settlement-copy">
            {{ battleResult.cleared ? '画像与积分以服务端结算为准，返回地图可查看新的楼层状态。' : '建议先补给或回到诊断房确认薄弱题型。' }}
          </p>
          <div class="settlement-actions">
            <el-button class="ghost-button" @click="phase = 'diagnosis'">重新诊断</el-button>
            <el-button class="spire-button" @click="router.push('/tower-map')">返回塔地图</el-button>
          </div>
        </section>
      </main>

      <aside class="floor-side">
        <section class="side-panel">
          <p class="kicker">Floor Intel</p>
          <h2>楼层情报</h2>
          <div class="info-list">
            <span>知识点 <b>{{ kpId }}</b></span>
            <span>模式 <b>{{ isBossFloor ? 'Boss 战' : '普通战斗' }}</b></span>
            <span>任务 <b>{{ taskNo || '本地题库' }}</b></span>
          </div>
        </section>

        <section class="side-panel">
          <p class="kicker">Diagnosis</p>
          <h2>诊断结果</h2>
          <div v-if="diagnosisResult" class="diagnosis-card" :class="diagnosisResult.status">
            <strong>{{ diagnosisText }}</strong>
            <span>{{ Math.round((diagnosisResult.correctRate || 0) * 100) }}%</span>
          </div>
          <el-empty v-else description="尚未诊断" :image-size="82" />
        </section>
      </aside>
    </section>

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
import SupplyModal from '../components/SupplyModal.vue'
import { getStudentProfile, getTaskList, getTowerMap } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const phase = ref('diagnosis')
const profile = ref({})
const floor = ref({})
const taskNo = ref('')
const diagnosisResult = ref(null)
const battleResult = ref({ cleared: false, correctRate: 0 })
const supplyVisible = ref(false)

const kpId = computed(() => route.params.kpId)
const courseId = computed(() => route.query.courseId || route.query.course_id || localStorage.getItem('courseId') || '1')
const courseName = computed(() => route.query.courseName || localStorage.getItem('courseName') || 'Python 程序设计')
const floorName = computed(() => route.query.floorName || floor.value.kpName || `第 ${kpId.value} 层`)
const studentId = computed(() =>
  user.studentNo || user.student_no || user.no || user.id || user.username || user.name || '1'
)
const isBossFloor = computed(() => route.query.boss === '1' || floor.value.boss || floor.value.isBoss)

const steps = computed(() => [
  { key: 'diagnosis', order: '01', label: '诊断房', done: Boolean(diagnosisResult.value) },
  { key: isBossFloor.value ? 'boss' : 'battle', order: '02', label: isBossFloor.value ? 'Boss 战' : '战斗房', done: phase.value === 'settlement' },
  { key: 'settlement', order: '03', label: '结算', done: phase.value === 'settlement' }
])

const diagnosisText = computed(() => ({
  mastered: '可以跳过普通战斗',
  partial: '需要常规挑战',
  weak: '建议优先补给'
}[diagnosisResult.value?.status] || '未判定'))

const pickProfile = payload => payload?.profile || payload || {}

const refreshProfile = async () => {
  try {
    const res = await getStudentProfile(studentId.value, courseId.value)
    if (res.data.code === 200) profile.value = pickProfile(res.data.data)
  } catch {
    profile.value = {}
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
    await Promise.all([refreshProfile(), loadFloor()])
    await findTask()
  } finally {
    loading.value = false
  }
}

const startBattle = () => {
  phase.value = isBossFloor.value ? 'boss' : 'battle'
}

const handleDiagnosed = result => {
  diagnosisResult.value = result
  if (result.status === 'mastered' && !isBossFloor.value) {
    battleResult.value = { cleared: true, correctRate: result.correctRate || 1 }
    phase.value = 'settlement'
    ElMessage.success('诊断通过，普通战斗已跳过')
    return
  }
  phase.value = isBossFloor.value ? 'boss' : 'battle'
}

const handleBattleEnd = result => {
  battleResult.value = result
  phase.value = 'settlement'
  refreshProfile()
}

onMounted(loadData)
</script>

<style scoped>
.floor-page {
  min-height: calc(100vh - 60px);
  padding: 18px;
  color: #f8edcf;
  background:
    linear-gradient(180deg, rgba(31, 17, 13, .9), rgba(9, 11, 16, .96)),
    repeating-linear-gradient(45deg, rgba(235, 163, 72, .07) 0 1px, transparent 1px 16px);
}

.floor-header,
.floor-layout {
  max-width: 1380px;
  margin: 0 auto;
}

.floor-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: end;
  gap: 14px;
  padding: 24px 0 18px;
}

.kicker {
  margin: 0 0 6px;
  color: #d99d4d;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #fff6dc;
}

h1 {
  font-size: 32px;
  line-height: 1.15;
}

h2 {
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.floor-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 300px;
  gap: 18px;
}

.floor-rail,
.floor-side {
  display: grid;
  align-content: start;
  gap: 12px;
}

.rail-step,
.side-panel,
.settlement-room {
  border: 1px solid rgba(232, 184, 92, .24);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(60, 31, 20, .94), rgba(18, 14, 13, .96));
}

.rail-step {
  display: grid;
  gap: 5px;
  padding: 14px;
  color: #bda98b;
}

.rail-step span {
  color: #d99d4d;
  font-size: 12px;
  font-weight: 800;
}

.rail-step strong {
  color: #f8ebcb;
}

.rail-step.active {
  border-color: rgba(240, 198, 107, .78);
  box-shadow: inset 4px 0 0 #d99d4d;
}

.rail-step.done {
  color: #bcdba3;
}

.room-stage {
  min-width: 0;
}

.side-panel {
  padding: 18px;
}

.info-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.info-list span {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  min-height: 34px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .06);
  color: #d7c4a6;
}

.info-list b {
  overflow: hidden;
  max-width: 150px;
  color: #fff3d2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diagnosis-card {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .06);
}

.diagnosis-card strong {
  color: #fff3d2;
}

.diagnosis-card span {
  color: #bcdba3;
  font-size: 24px;
  font-weight: 800;
}

.diagnosis-card.weak span {
  color: #e17a5e;
}

.settlement-room {
  display: grid;
  gap: 18px;
  min-height: 420px;
  padding: 28px;
  align-content: center;
}

.settlement-room h2 {
  font-size: 28px;
}

.settlement-copy {
  margin: 0;
  color: #d7c4a6;
}

.settlement-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.spire-button,
.ghost-button {
  min-height: 42px;
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

@media (max-width: 1180px) {
  .floor-layout {
    grid-template-columns: 150px minmax(0, 1fr);
  }
  .floor-side {
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .floor-page {
    padding: 12px;
  }
  .floor-header {
    grid-template-columns: 1fr;
    align-items: start;
  }
  h1 {
    font-size: 28px;
  }
  .floor-layout {
    grid-template-columns: 1fr;
  }
  .floor-rail,
  .floor-side {
    grid-template-columns: 1fr;
  }
}
</style>
