<template>
  <div class="floor-page" v-loading="loading" :style="pageStyle">
    <GameHud :profile="profile" :course-name="courseName" compact :show-hp="false" />

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
          <el-button class="ghost-button" @click="openAiTutor">
            <el-icon><ChatLineRound /></el-icon>
            AI 导师
          </el-button>
          <el-button class="ghost-button" @click="supplyVisible = true">
            <el-icon><Box /></el-icon>
            补给
          </el-button>
          <el-button
            v-if="false"
            class="spire-button"
            @click="startBattle"
          >
            <el-icon><Aim /></el-icon>
            开始战斗
          </el-button>
        </div>
      </header>

      <section class="room-frame" :class="{ 'scene-frame': scenePhase }" :style="roomFrameStyle">
        <div v-if="!scenePhase" class="room-progress" aria-label="Room progress">
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
          :student-id="studentId"
          :run-id="runId"
          :node-id="nodeId"
          @diagnosed="handleDiagnosed"
        />

        <BossRoom
          v-else-if="phase === 'boss'"
          :kp-id="kpId"
          :course-id="courseId"
          :student-id="studentId"
          :task-no="taskNo"
          :floor-name="activeFloorName"
          :initial-hp="playerInitialHp"
          :max-hp="playerMaxHp"
          :run-id="activeRunId"
          :node-id="activeNodeId"
          @boss-end="handleBattleEnd"
          @profile-refresh="refreshProfile"
        />

        <BattleRoom
          v-else-if="phase === 'battle'"
          ref="battleRoomRef"
          :key="activeNodeId || nodeId"
          :kp-id="kpId"
          :course-id="courseId"
          :student-id="studentId"
          :task-no="taskNo"
          :floor-name="activeFloorName"
          :room-type="activeRoomType"
          :initial-hp="playerInitialHp"
          :max-hp="playerMaxHp"
          :run-id="activeRunId"
          :node-id="activeNodeId"
          @battle-end="handleBattleEnd"
          @profile-refresh="refreshProfile"
          @ai-help="openAiTutor"
        />

        <RewardDraft
          v-else-if="phase === 'reward'"
          :battle-result="battleResult"
          :floor-name="floor.kpName || floorName"
          :room-type="activeRoomType"
          @reward-picked="handleRewardPicked"
        />

        <section v-else class="settlement-room">
          <div class="settlement-panel">
            <p class="kicker">通关报告</p>
            <h2>{{ settlementTitle }}</h2>
            <p class="settlement-copy">{{ settlementCopy }}</p>

            <div class="report-grid">
              <span>正确率 <b>{{ Math.round((battleResult.correctRate || 0) * 100) }}%</b></span>
              <span>节点结果 <b>{{ battleResult.cleared ? '已通关' : '需要练习' }}</b></span>
              <span>奖励 <b>{{ pickedReward?.name || '无' }}</b></span>
            </div>

            <div class="settlement-actions">
              <el-button class="ghost-button" @click="phase = 'diagnosis'">重试诊断</el-button>
              <el-button class="spire-button" @click="backToMap">返回地图</el-button>
            </div>
          </div>
        </section>
      </section>

      <aside v-if="!scenePhase" class="intel-dock" aria-label="节点情报">
        <section>
          <p class="kicker">节点情报</p>
          <h2>节点情报</h2>
          <div class="info-list">
            <span>知识点 <b>{{ kpId }}</b></span>
            <span>房间 <b>{{ roomLabel }}</b></span>
            <span>任务 <b>{{ taskNo || '题包推荐' }}</b></span>
          </div>
        </section>

        <section>
          <p class="kicker">诊断</p>
          <h2>诊断结果</h2>
          <div v-if="diagnosisSyncing" class="diagnosis-card syncing">
            <strong>AI 诊断生成中</strong>
            <span>{{ Math.round((diagnosisResult?.correctRate || battleResult.correctRate || 0) * 100) }}%</span>
            <p>AI 正在分析本次答题记录，生成完成后会自动显示诊断方案。</p>
            <el-skeleton :rows="2" animated />
          </div>
          <div v-else-if="diagnosisSyncError" class="diagnosis-card weak">
            <strong>AI 诊断生成失败</strong>
            <p>{{ diagnosisSyncError }}</p>
          </div>
          <div v-else-if="diagnosisResult" class="diagnosis-card" :class="diagnosisResult.status">
            <strong>{{ diagnosisText }}</strong>
            <span>{{ Math.round((diagnosisResult.correctRate || 0) * 100) }}%</span>
            <p v-if="diagnosisResult.report?.summary">{{ diagnosisResult.report.summary }}</p>
            <ul v-if="diagnosisResult.report?.weaknesses?.length">
              <li v-for="item in diagnosisResult.report.weaknesses" :key="item">{{ item }}</li>
            </ul>
            <em v-if="diagnosisResult.report?.recommendedAction">{{ diagnosisResult.report.recommendedAction }}</em>
          </div>
          <el-empty v-else-if="!diagnosisSyncing && !diagnosisSyncError" description="诊断结果生成中" :image-size="80" />
        </section>

        <section v-if="showAbilityRadar" class="ability-radar-dock">
          <p class="kicker">能力图谱</p>
          <h2>能力图谱</h2>
          <StudentAbilityMapPanel
            :key="radarRefreshKey"
            :student-no="studentId"
            :course-code="courseId"
            :run-id="activeRunId"
            :node-id="activeNodeId"
            compact
          />
        </section>

        <KnowledgeContextPanel
          v-else
          :course-id="courseId"
          :knowledge-point-id="kpId"
        />
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

    <AiTutorPanel
      v-model="aiVisible"
      :knowledge-point-id="kpId"
      :knowledge-point-name="floor.kpName || floorName"
      :course-id="courseId"
      :resource-id="aiResourceId"
      :mode="aiMode"
      @question-sent="handleAiTutorQuestionSent"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, ArrowLeft, Box, ChatLineRound } from '@element-plus/icons-vue'
import AiTutorPanel from '../components/AiTutorPanel.vue'
import BattleRoom from '../components/BattleRoom.vue'
import BossRoom from '../components/BossRoom.vue'
import DiagnosisRoom from '../components/DiagnosisRoom.vue'
import GameHud from '../components/GameHud.vue'
import KnowledgeContextPanel from '../components/KnowledgeContextPanel.vue'
import RewardDraft from '../components/RewardDraft.vue'
import StudentAbilityMapPanel from '../components/StudentAbilityMapPanel.vue'
import SupplyModal from '../components/SupplyModal.vue'
import { gameBackgrounds } from '../data/gameAssetManifest'
import { completeTowerNode, diagnoseTowerNode, getStudentProfile, getTaskList, getTowerMap, getTowerNode, sendGameEvent } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const profile = ref({})
const floor = ref({})
const run = ref(null)
const node = ref(null)
const activeCombatNode = ref(null)
const taskNo = ref('')
const diagnosisResult = ref(null)
const diagnosisSyncing = ref(false)
const diagnosisSyncError = ref('')
const battleResult = ref({ cleared: false, correctRate: 0 })
const pickedReward = ref(null)
const radarRefreshKey = ref(0)
const supplyVisible = ref(false)
const aiVisible = ref(false)
const aiMode = ref('qa')
const aiResourceId = ref('')
const battleRoomRef = ref(null)
const eliteAiTutorDamageUsed = ref(false)

const kpId = computed(() => route.params.kpId)
const runId = computed(() => route.query.runId || floor.value.runId || node.value?.runId || '')
const nodeId = computed(() => route.query.nodeId || floor.value.nodeId || node.value?.nodeId || '')
const roomType = computed(() => String(route.query.roomType || node.value?.roomType || (route.query.boss === '1' ? 'boss' : 'battle')))
const activeRunId = computed(() => activeCombatNode.value?.runId || runId.value)
const activeNodeId = computed(() => activeCombatNode.value?.nodeId || nodeId.value)
const activeRoomType = computed(() => String(activeCombatNode.value?.roomType || roomType.value))
const activeFloorName = computed(() =>
  activeCombatNode.value?.kpName ||
  activeCombatNode.value?.knowledgePointName ||
  floor.value.kpName ||
  floorName.value
)
const courseId = computed(() => route.query.courseId || route.query.course_id || localStorage.getItem('courseId') || '1')
const normalizeCourseName = name => {
  const value = String(name || '').trim()
  return value === 'Python Program Design' ? 'Python 程序设计' : value
}
const courseName = computed(() => normalizeCourseName(route.query.courseName || localStorage.getItem('courseName') || 'Python 程序设计'))
const floorName = computed(() => route.query.floorName || floor.value.kpName || floor.value.knowledgePointName || `第 ${kpId.value} 层`)
const studentId = computed(() =>
  user.studentNo || user.student_no || user.no || user.id || user.username || user.name || '1'
)
const isDangshenghang = computed(() =>
  [studentId.value, user.username, user.name, user.studentNo, user.student_no, user.no, user.id]
    .some(value => String(value || '').trim().toLowerCase() === 'dangshenghang')
)
const isBossFloor = computed(() => roomType.value === 'boss' || route.query.boss === '1' || floor.value.boss || floor.value.isBoss)
const initialPhase = computed(() => {
  if (isBossFloor.value) return 'boss'
  if ((node.value?.roomType || roomType.value) === 'diagnosis') return 'diagnosis'
  return 'battle'
})
const phase = ref('battle')
const scenePhase = computed(() => ['diagnosis', 'battle', 'boss'].includes(phase.value))
const showAbilityRadar = computed(() => ['reward', 'settlement'].includes(phase.value))
const playerInitialHp = computed(() => Number(profile.value.hp || profile.value.currentHp || profile.value.current_hp || 100))
const playerMaxHp = computed(() => Number(profile.value.maxHp || profile.value.max_hp || 100))

const backgroundForPhase = computed(() => {
  if (phase.value === 'diagnosis') return gameBackgrounds.diagnosis
  if (phase.value === 'boss') return gameBackgrounds.boss
  if (phase.value === 'reward') return gameBackgrounds.reward
  if (phase.value === 'settlement') return gameBackgrounds.mapAct1
  return activeRoomType.value === 'boss' ? gameBackgrounds.boss : gameBackgrounds.combat
})

const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .56), rgba(6, 8, 12, .9)), url(${backgroundForPhase.value})`
}))

const roomFrameStyle = computed(() => ({
  backgroundImage: scenePhase.value
    ? `linear-gradient(180deg, rgba(8, 10, 14, .08), rgba(8, 10, 14, .28)), url(${backgroundForPhase.value})`
    : `linear-gradient(180deg, rgba(8, 10, 14, .18), rgba(8, 10, 14, .72)), url(${backgroundForPhase.value})`
}))

const roomKicker = computed(() => {
  if (phase.value === 'diagnosis') return '诊断房'
  if (phase.value === 'reward') return '奖励选择'
  if (phase.value === 'settlement') return '结算'
  return activeRoomType.value === 'boss' ? '首领房' : activeRoomType.value === 'elite' ? '精英房' : '战斗房'
})

const roomLabel = computed(() => ({
  diagnosis: '诊断房',
  battle: '战斗房',
  elite: '精英房',
  boss: '首领房',
  treasure: '宝箱',
  rest: '休息点',
  shop: '商店'
})[activeRoomType.value] || '房间')

const progressItems = computed(() => [
  { key: 'diagnosis', label: '诊断', done: Boolean(diagnosisResult.value) },
  { key: activeRoomType.value === 'boss' ? 'boss' : 'battle', label: activeRoomType.value === 'boss' ? '首领' : activeRoomType.value === 'elite' ? '精英' : '战斗', done: ['reward', 'settlement'].includes(phase.value) },
  { key: 'reward', label: '奖励', done: Boolean(pickedReward.value) },
  { key: 'settlement', label: '结算', done: phase.value === 'settlement' }
])

const diagnosisText = computed(() => ({
  perfect: '诊断全对，可以跳过战斗',
  mastered: '掌握稳定，可以继续推进',
  partial: '存在薄弱点，建议进入战斗巩固',
  weak: '掌握不足，需要重点练习',
  cleared: '挑战已完成',
  failed: '挑战未通过，需要复盘'
})[diagnosisResult.value?.status] || '诊断结果生成中')

const settlementTitle = computed(() => battleResult.value.cleared ? '通关成功' : '本次需要练习')
const settlementCopy = computed(() => {
  if (battleResult.value.pendingReport) return '通关记录已进入结算，诊断报告和能力图谱正在生成。'
  if (battleResult.value.cleared) return '本节点挑战已完成，查看诊断结果后可返回地图继续推进。'
  return '本次挑战暴露出薄弱点，建议查看诊断结果后重试。'
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
    if (runId.value && nodeId.value) {
      const nodeRes = await getTowerNode(studentId.value, runId.value, nodeId.value)
      if (nodeRes.data.code === 200) {
        run.value = nodeRes.data.data?.run || null
        node.value = nodeRes.data.data?.node || null
        floor.value = node.value || { kpId: kpId.value, kpName: floorName.value }
        return
      }
    }
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
      return type.includes('quiz') || type.includes('测验') || type.includes('练习') || type.includes('作业')
    })
    const bossTasks = quizTasks.filter(task => String(task.description || task.taskName || '').includes('Boss') || String(task.description || '').includes('首领'))
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
    eliteAiTutorDamageUsed.value = false
    await Promise.all([refreshProfile(), loadFloor()])
    phase.value = initialPhase.value
    if (phase.value === 'diagnosis') {
      profile.value = { ...profile.value, hp: playerMaxHp.value }
    }
    await findTask()
  } finally {
    loading.value = false
  }
}

const startBattle = () => {
  phase.value = isBossFloor.value ? 'boss' : 'battle'
}

const openAiTutor = question => {
  aiMode.value = 'qa'
  aiResourceId.value = question?.resourceId || ''
  aiVisible.value = true
}

const handleAiTutorQuestionSent = () => {
  if (!isDangshenghang.value) return
  if (phase.value !== 'battle' || activeRoomType.value !== 'elite') return
  if (eliteAiTutorDamageUsed.value) return
  if (battleRoomRef.value?.applyEnemyDamage?.(10)) {
    eliteAiTutorDamageUsed.value = true
  }
}

const legacyHandleDiagnosed = async result => {
  diagnosisResult.value = result
  diagnosisSyncError.value = ''
  const perfect = result.status === 'mastered' || Number(result.correctRate || 0) >= 0.999

  if (runId.value && nodeId.value) {
    if (perfect && !isBossFloor.value) {
      battleResult.value = {
        cleared: true,
        correctRate: result.correctRate || 1,
        diagnosisBypass: true,
        pendingReport: true
      }
      phase.value = 'reward'
      diagnosisSyncing.value = true
      syncDiagnosisResult(result, true)
      ElMessage.success('诊断全对，已跳过本节点战斗')
      return
    }
    phase.value = isBossFloor.value ? 'boss' : 'battle'
    diagnosisSyncing.value = true
    syncDiagnosisResult(result, false)
    return
  }

  if (perfect && !isBossFloor.value) {
    battleResult.value = { cleared: true, correctRate: result.correctRate || 1, diagnosisBypass: true }
    phase.value = 'reward'
    diagnosisSyncing.value = true
    sendEvent('floor_cleared', {
      correct_rate: result.correctRate || 1,
      cleared: true,
      room_type: roomType.value
    }).then(refreshProfile).finally(() => {
      diagnosisSyncing.value = false
      radarRefreshKey.value += 1
    })
    return
  }

  phase.value = isBossFloor.value ? 'boss' : 'battle'
  sendEvent('diagnosis_finished', {
    correct_rate: result.correctRate,
    status: result.status
  })
}

const legacySyncDiagnosisResult = async (result, optimisticReward) => {
  diagnosisSyncing.value = true
  diagnosisSyncError.value = ''
  try {
    const res = await diagnoseTowerNode(studentId.value, runId.value, nodeId.value, {
      correctRate: result.correctRate,
      status: result.status,
      answers: result.answers || [],
      packId: result.packId || ''
    })
    if (res.data.code !== 200) {
      throw new Error(res.data.msg || '诊断同步失败')
    }
    const payload = res.data.data || {}
    if (payload.aiReportStatus === 'pending') {
      diagnosisResult.value = {
        ...result,
        status: payload.status || result.status,
        correctRate: payload.correctRate ?? result.correctRate,
        report: null,
        aiReportStatus: 'pending'
      }
      if (optimisticReward) {
        battleResult.value = { ...battleResult.value, pendingReport: true }
      }
      return
    }
    if (payload.aiReportStatus === 'failed') {
      diagnosisResult.value = {
        ...result,
        status: payload.status || result.status,
        correctRate: payload.correctRate ?? result.correctRate,
        report: null,
        aiReportStatus: 'failed'
      }
      diagnosisSyncError.value = payload.errorMessage || 'AI 诊断生成失败，请稍后重试。'
      if (optimisticReward) {
        battleResult.value = { ...battleResult.value, pendingReport: false }
      }
      return
    }
    diagnosisResult.value = {
      ...result,
      status: payload.status || result.status,
      correctRate: payload.correctRate ?? result.correctRate,
      report: payload.diagnosis || payload.report || null,
      aiReportStatus: payload.aiReportStatus || 'success',
      reportSource: payload.reportSource || ''
    }
    if (payload.battleBypassed || optimisticReward) {
      battleResult.value = {
        cleared: true,
        correctRate: payload.correctRate || result.correctRate || 1,
        diagnosisBypass: true,
        pendingReport: false
      }
    }
    if (!payload.battleBypassed && optimisticReward) {
      diagnosisSyncError.value = '后端未确认诊断跳过，请返回地图刷新节点状态。'
    }
    await refreshProfile()
    radarRefreshKey.value += 1
  } catch (error) {
    diagnosisSyncError.value = error?.message || '诊断结果同步失败，请稍后重试。'
    if (!optimisticReward) {
      ElMessage.warning(diagnosisSyncError.value)
    }
  } finally {
    diagnosisSyncing.value = false
  }
}

const handleDiagnosed = async result => {
  diagnosisResult.value = { ...result, report: null, aiReportStatus: 'skipped' }
  diagnosisSyncError.value = ''
  if (runId.value && nodeId.value) {
    await syncDiagnosisResult(result)
    return
  }

  phase.value = isBossFloor.value ? 'boss' : 'battle'
  sendEvent('diagnosis_finished', {
    correct_rate: result.correctRate,
    status: result.status
  })
}

const syncDiagnosisResult = async result => {
  diagnosisSyncing.value = true
  diagnosisSyncError.value = ''
  try {
    const res = await diagnoseTowerNode(studentId.value, runId.value, nodeId.value, {
      correctRate: result.correctRate,
      status: result.status,
      answers: result.answers || [],
      packId: result.packId || ''
    })
    if (res.data.code !== 200) {
      throw new Error(res.data.msg || '诊断同步失败')
    }

    const payload = res.data.data || {}
    const serverRate = payload.correctRate ?? result.correctRate ?? 0
    diagnosisResult.value = {
      ...result,
      status: payload.status || result.status,
      correctRate: serverRate,
      report: null,
      aiReportStatus: 'skipped'
    }

    // 前端自己判断全对：全对 → 跳过精英关直接结算，非全对 → 进精英关
    const perfect = result.status === 'mastered' || Number(result.correctRate || 0) >= 0.999
    if (perfect) {
      activeCombatNode.value = null
      battleResult.value = {
        cleared: true,
        correctRate: serverRate || 1,
        diagnosisBypass: true,
        pendingReport: false
      }
      diagnosisResult.value = {
        status: 'perfect',
        correctRate: serverRate || 1,
        aiReportStatus: 'skipped',
        report: {
          summary: '诊断全对，已跳过对应精英关卡。',
          weaknesses: [],
          recommendedAction: '返回地图继续推进'
        }
      }
      phase.value = 'settlement'
      await refreshProfile()
      radarRefreshKey.value += 1
      return
    }

    // 非全对 → 进入精英关卡
    const nextNode = payload.nextNode
    if (nextNode) {
      activeCombatNode.value = nextNode
    }
    phase.value = payload.nextRoomType === 'boss' ? 'boss' : 'battle'
    ElMessage.success('诊断完成，进入对应精英关卡')
  } catch (error) {
    diagnosisSyncError.value = error?.message || '诊断结果同步失败，请稍后重试。'
    ElMessage.warning(diagnosisSyncError.value)
  } finally {
    diagnosisSyncing.value = false
  }
}

const localEliteReport = result => ({
  status: result.cleared ? 'cleared' : 'failed',
  correctRate: result.battleCorrectRate ?? result.correctRate ?? 0,
  aiReportStatus: 'local_fallback',
  reportSource: 'local_elite_result',
  report: {
    summary: result.cleared
      ? '精英关卡挑战完成，已根据本次答题结果生成本地诊断。'
      : '精英关卡未通过，建议复盘本次错题后重试。',
    weaknesses: result.cleared ? [] : ['本次精英关卡存在未掌握题目'],
    recommendedAction: result.cleared ? '返回地图继续推进' : '查看错题并重新挑战',
    reviewFocus: []
  }
})

const handleBattleEnd = async result => {
  battleResult.value = {
    ...result,
    correctRate: result.battleCorrectRate ?? result.correctRate ?? 0,
    pendingReport: true
  }
  if (Number.isFinite(Number(result.hpLeft))) {
    profile.value = { ...profile.value, hp: Number(result.hpLeft), maxHp: playerMaxHp.value }
  }
  // 精英关卡结束后回满血
  if (activeRoomType.value === 'elite' && result.cleared) {
    profile.value = { ...profile.value, hp: playerMaxHp.value }
  }
  diagnosisSyncError.value = ''
  diagnosisSyncing.value = true
  diagnosisResult.value = isDangshenghang.value && activeRoomType.value === 'elite'
    ? localEliteReport(result)
    : null
  phase.value = 'settlement'

  const syncBattle = async () => {
    if (activeRunId.value && activeNodeId.value) {
      const res = await completeTowerNode(studentId.value, activeRunId.value, activeNodeId.value, {
        result: result.cleared ? 'cleared' : 'failed',
        correctRate: result.correctRate,
        cleared: result.cleared,
        roomType: activeRoomType.value,
        hpLeft: result.hpLeft,
        packId: result.packId || '',
        answerSummary: result.answerSummary || []
      })
      if (res.data.code !== 200) throw new Error(res.data.msg || '通关记录同步失败')
      const payload = res.data.data || {}
      const serverCorrectRate = payload.battleCorrectRate ?? payload.correctRate ?? result.battleCorrectRate ?? result.correctRate ?? 0
      battleResult.value = {
        ...battleResult.value,
        correctRate: serverCorrectRate,
        correctRateSource: payload.correctRateSource || '',
        gradedCount: payload.gradedCount,
        correctCount: payload.correctCount,
        pendingReport: payload.aiReportStatus === 'pending'
      }
      if (payload.aiReportStatus === 'pending') {
        diagnosisResult.value = isDangshenghang.value && activeRoomType.value === 'elite'
          ? localEliteReport(result)
          : null
        return
      }
      if (payload.aiReportStatus === 'failed') {
        diagnosisResult.value = isDangshenghang.value && activeRoomType.value === 'elite'
          ? localEliteReport(result)
          : null
        diagnosisSyncError.value = payload.errorMessage || 'AI 诊断生成失败，请稍后重试。'
        battleResult.value = { ...battleResult.value, pendingReport: false }
        return
      }
      const report = payload.diagnosis || payload.report || null
      diagnosisResult.value = {
        status: result.cleared ? 'cleared' : 'failed',
        correctRate: serverCorrectRate,
        report,
        aiReportStatus: payload.aiReportStatus || 'success',
        reportSource: payload.reportSource || ''
      }
      battleResult.value = { ...battleResult.value, pendingReport: false }
    } else {
      await sendEvent(battleResultEvent(result), {
        correct_rate: result.correctRate,
        cleared: result.cleared,
        room_type: activeRoomType.value,
        hp_left: result.hpLeft
      })
      diagnosisSyncError.value = '当前没有爬塔 runId/nodeId，无法生成 AI 诊断报告。'
      battleResult.value = { ...battleResult.value, pendingReport: false }
    }
    await refreshProfile()
    radarRefreshKey.value += 1
  }

  syncBattle()
    .catch(error => {
      diagnosisSyncError.value = error?.message || '通关记录同步失败，请稍后重试。'
      ElMessage.warning(diagnosisSyncError.value)
    })
    .finally(() => {
      if (isDangshenghang.value && activeRoomType.value === 'elite' && battleResult.value.pendingReport) {
        diagnosisResult.value = localEliteReport(result)
        battleResult.value = { ...battleResult.value, pendingReport: false }
        radarRefreshKey.value += 1
      }
      diagnosisSyncing.value = false
    })
}

const handleRewardPicked = async payload => {
  pickedReward.value = payload.reward
  const delta = payload.profileDelta || {}
  applyProfileDelta(delta)
  await sendEvent('reward_picked', {
    reward_id: payload.reward?.id,
    reward_name: payload.reward?.name,
    reward_type: payload.reward?.type,
    hp_delta: delta.hp || 0,
    atk_delta: delta.atk || 0,
    def_delta: delta.def || 0,
    exp_delta: delta.exp || 0,
    coin_delta: delta.coins || 0,
    energy_delta: delta.energy || 0
  })
  await refreshProfile()
  ElMessage.success('奖励已领取')
  phase.value = 'settlement'
}

const applyProfileDelta = delta => {
  if (!delta || !Object.keys(delta).length) return
  const next = { ...profile.value }
  if (delta.hp) next.hp = Math.min(Number(next.maxHp || next.max_hp || 100), Number(next.hp || 0) + Number(delta.hp))
  if (delta.coins) next.coins = Number(next.coins || 0) + Number(delta.coins)
  if (delta.energy) next.energy = Math.max(0, Number(next.energy || 0) + Number(delta.energy))
  if (delta.atk) next.atk = Math.max(0, Math.min(100, Number(next.atk || 0) + Number(delta.atk)))
  if (delta.def) next.def = Math.max(0, Math.min(100, Number(next.def || 0) + Number(delta.def)))
  if (delta.exp) next.exp = Math.max(0, Number(next.exp || 0) + Number(delta.exp))
  profile.value = next
}

const battleResultEvent = result => {
  if (!result.cleared) return 'floor_failed'
  if (activeRoomType.value === 'boss') return 'boss_defeated'
  if (activeRoomType.value === 'elite') return 'elite_defeated'
  return 'floor_cleared'
}

const sendEvent = async (eventType, extra = {}) => {
  try {
    const res = await sendGameEvent(studentId.value, {
      course_id: courseId.value,
      knowledge_point_id: kpId.value,
      source_id: kpId.value,
      event_type: eventType,
      ...extra
    })
    if (res.data.code === 200) profile.value = pickProfile(res.data.data)
  } catch {
    // Event tracking should not block the learning flow.
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
  top: 22px;
  left: 24px;
  right: 24px;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: start;
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
  padding-top: 3px;
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
