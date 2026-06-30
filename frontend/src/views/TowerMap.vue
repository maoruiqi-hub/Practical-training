<template>
  <div class="tower-page" v-loading="loading" :style="pageStyle">
    <GameHud :profile="profile" :course-name="courseName" compact :show-hp="false" />

    <main class="tower-shell">
      <section class="map-stage" aria-label="??????">
        <div class="act-header">
          <div>
            <p class="kicker">Act {{ activeAct }} Route Map</p>
            <h1>{{ actTitle }}</h1>
          </div>
          <div class="act-actions">
            <el-button class="ghost-button" aria-label="????" @click="loadData">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button class="danger-button" @click="logout">
              <el-icon><SwitchButton /></el-icon>
              ??
            </el-button>
          </div>
        </div>

        <div class="route-map">
          <div class="route-lantern" aria-hidden="true"></div>
          <div
            v-for="row in visualRows"
            :key="row.level"
            class="route-row"
            :class="{ boss: row.nodes.some(node => node.roomType === 'boss') }"
          >
            <button
              v-for="node in row.nodes"
              :key="node.nodeId"
              type="button"
              class="route-node"
              :class="[node.status, node.roomType, { selected: selectedNode?.nodeId === node.nodeId }]"
              :disabled="node.status === 'locked'"
              :aria-label="`${node.kpName}, ${roomLabel(node.roomType)}, ${statusText(node.status)}`"
              @mouseenter="previewNode(node)"
              @focus="previewNode(node)"
              @click="enterNode(node)"
            >
              <span class="node-glow" aria-hidden="true"></span>
              <img class="node-icon" :src="iconFor(node)" alt="" />
              <strong>{{ node.level }}</strong>
              <small>{{ roomLabel(node.roomType) }}</small>
            </button>
          </div>
        </div>
      </section>

      <aside class="node-panel" aria-label="??????">
        <section class="panel-card primary">
          <p class="kicker">Next Choice</p>
          <h2>{{ previewTitle }}</h2>
          <p class="panel-copy">{{ previewCopy }}</p>
          <div v-if="selectedNode" class="intel-list">
            <span>??? <b>{{ selectedNode.kpName }}</b></span>
            <span>?? <b>{{ roomLabel(selectedNode.roomType) }}</b></span>
            <span>?? <b>{{ riskText(selectedNode.risk) }}</b></span>
            <span>?? <b>{{ rewardText(selectedNode) }}</b></span>
          </div>
          <el-button
            class="spire-button"
            :disabled="!canEnterSelected"
            @click="enterSelected"
          >
            <el-icon><Aim /></el-icon>
            ????
          </el-button>
        </section>

      </aside>
    </main>

    <TreasureRoom
      v-if="activeRoomType === 'treasure'"
      v-model="roomVisible"
      :course-id="courseId"
      :course-name="courseName"
      :selected-node="selectedNode"
      @room-complete="completeRoom"
    />

    <GameRoomModal
      v-else
      v-model="roomVisible"
      :room-type="activeRoomType"
      :profile="profile"
      :user="user"
      :student-id="studentId"
      :course-name="courseName"
      :run-stats="runStats"
      @open-supply="openSupply"
      @course-picked="pickCourse"
      @room-complete="completeRoom"
    />

    <SupplyModal
      v-model="supplyVisible"
      :student-id="studentId"
      :course-id="courseId"
      :profile="profile"
      @used="refreshProfile"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Refresh, SwitchButton } from '@element-plus/icons-vue'
import GameHud from '../components/GameHud.vue'
import GameRoomModal from '../components/GameRoomModal.vue'
import SupplyModal from '../components/SupplyModal.vue'
import TreasureRoom from '../components/TreasureRoom.vue'
import { gameBackgrounds, mapLegendIcons, referenceTokenIcons } from '../data/gameAssetManifest'
import { getLeaderboard, getStudentProfile, getTowerMap, sendGameEvent } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const profile = ref({})
const floors = ref([])
const leaderboard = ref([])
const selectedNode = ref(null)
const roomVisible = ref(false)
const supplyVisible = ref(false)
const activeRoomType = ref('event')

const roomPattern = ['diagnosis', 'battle', 'treasure', 'battle', 'rest', 'elite', 'event', 'shop', 'battle', 'treasure', 'battle', 'rest', 'elite', 'boss']
const fallbackNames = [
  'Python Syntax Basics',
  'Variables and Data Types',
  'Expressions and Operators',
  'Branching Control',
  'Loop Control',
  'Functions and Parameters',
  'Lists and Tuples',
  'Dictionaries and Sets',
  'String Processing',
  'File Processing',
  'Exception Handling',
  'Modules and Packages',
  'Integrated Program Design',
  'Python Basics Boss'
]

const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .28), rgba(6, 8, 12, .68)), url(${gameBackgrounds.mapAct1})`
}))

const studentId = computed(() =>
  user.studentNo || user.student_no || user.no || user.id || user.username || user.name || '1'
)
const courseId = computed(() =>
  route.query.courseId || route.query.course_id || localStorage.getItem('courseId') || '1'
)
const courseName = computed(() => route.query.courseName || localStorage.getItem('courseName') || 'Python Program Design')

const fallbackFloors = fallbackNames.map((name, index) => ({
  kpId: String(index + 1),
  kpName: name,
  level: index + 1,
  masteryRate: index === 0 ? 42 : 0,
  status: index === 0 ? 'available' : 'locked',
  roomType: roomPattern[index]
}))

const normalizeStatus = (floor, index) => {
  const raw = String(floor.floorStatus || floor.floor_status || floor.status || '').toLowerCase()
  const mastery = Number(floor.masteryRate ?? floor.mastery_rate ?? floor.mastery ?? 0)
  const accessible = floor.isAccessible ?? floor.is_accessible
  if (['cleared', 'mastered', 'passed', 'done'].includes(raw) || mastery >= 80) return 'cleared'
  if (['weak', 'review', 'remedial'].includes(raw) || (mastery > 0 && mastery < 60)) return 'weak'
  if (raw === 'locked' || accessible === false) return 'locked'
  if (raw === 'available' || raw === 'open' || accessible === true || index === 0) return 'available'
  return 'locked'
}

const normalizeFloors = list => {
  const source = Array.isArray(list) && list.length ? list : fallbackFloors
  return source.map((item, index) => {
    const level = Number(item.level || item.floorLevel || index + 1)
    const roomType = item.roomType || item.room_type || roomPattern[index % roomPattern.length]
    return {
      nodeId: `${level}-${item.kpId || item.knowledgePointId || index}`,
      kpId: item.kpId || item.knowledgePointId || item.knowledge_point_id || item.id || String(index + 1),
      kpName: item.kpName || item.knowledgePointName || item.name || item.title || `Floor ${index + 1}`,
      level,
      masteryRate: Number(item.masteryRate ?? item.mastery_rate ?? item.mastery ?? 0),
      status: normalizeStatus(item, index),
      roomType: item.boss || item.isBoss || item.is_boss ? 'boss' : roomType
    }
  })
}

const baseNodes = computed(() => normalizeFloors(floors.value).sort((a, b) => a.level - b.level))
const firstOpenIndex = computed(() => {
  const index = baseNodes.value.findIndex(node => ['available', 'weak'].includes(node.status))
  return index >= 0 ? index : 0
})

const routeRows = computed(() => baseNodes.value.map((node, index) => {
  const branchTypes = branchRoomTypes(node, index)
  const nodes = branchTypes.map((roomType, branch) => ({
    ...node,
    nodeId: `${node.nodeId}-branch-${branch}`,
    roomType,
    risk: roomRisk(roomType, node.status),
    status: branchStatus(node, index, branch),
    branch
  }))
  return { level: node.level, nodes }
}))

const visualRows = computed(() => routeRows.value.slice().reverse())

const activeAct = computed(() => Math.min(3, Math.max(1, Math.ceil((baseNodes.value[firstOpenIndex.value]?.level || 1) / 5))))
const actTitle = computed(() => ['Foundation Trail', 'Data Citadel', 'Final Tower'][activeAct.value - 1])

const allVisualNodes = computed(() => routeRows.value.flatMap(row => row.nodes))
const runStats = computed(() => ({
  cleared: baseNodes.value.filter(node => node.status === 'cleared').length,
  weak: baseNodes.value.filter(node => node.status === 'weak').length,
  available: allVisualNodes.value.filter(node => node.status === 'available' || node.status === 'weak').length
}))

const canEnterSelected = computed(() => selectedNode.value && selectedNode.value.status !== 'locked')
const previewTitle = computed(() => selectedNode.value ? roomLabel(selectedNode.value.roomType) : 'Choose Next Node')
const previewCopy = computed(() => {
  if (!selectedNode.value) return 'Click an unlocked map node to view its knowledge point, risk, and reward.'
  const node = selectedNode.value
  return roomDescriptions[node.roomType] || `Enter ${node.kpName}, complete the challenge, then return to unlock the next step.`
})

const legendItems = computed(() => [
  { type: 'battle', label: 'Battle: quiz challenge', icon: mapLegendIcons.enemy },
  { type: 'elite', label: 'Elite: harder practice', icon: mapLegendIcons.elite },
  { type: 'treasure', label: 'Treasure: resource reward', icon: mapLegendIcons.treasure },
  { type: 'rest', label: 'Rest: recover and review', icon: mapLegendIcons.rest },
  { type: 'shop', label: 'Shop: spend coins', icon: mapLegendIcons.merchant },
  { type: 'event', label: 'Event: learning choice', icon: mapLegendIcons.unknown }
])

const roomDescriptions = {
  diagnosis: 'Run a short diagnosis before the challenge.',
  battle: 'Answer questions to damage the enemy and earn rewards.',
  elite: 'A harder mixed challenge with better rewards.',
  boss: 'A chapter boss challenge that closes this run segment.',
  treasure: 'Open a chest to gain resources or a knowledge card.',
  rest: 'Recover, review weak points, and prepare for the next node.',
  shop: 'Spend coins for hints or cleanup actions.',
  event: 'Resolve a learning event with a benefit and a cost.'
}

const branchRoomTypes = (node, index) => {
  if (node.roomType === 'boss') return ['boss']
  if (index === 0) return ['diagnosis', 'battle']
  if (index % 5 === 0) return ['battle', 'elite', 'rest']
  if (index % 3 === 0) return ['battle', 'treasure']
  if (index % 4 === 0) return ['event', 'shop']
  return [node.roomType || 'battle']
}

const branchStatus = (node, index, branch) => {
  if (node.status === 'cleared') return 'cleared'
  if (node.status === 'weak') return branch === 0 ? 'weak' : 'available'
  if (index === firstOpenIndex.value && node.status !== 'locked') return 'available'
  if (index < firstOpenIndex.value) return 'cleared'
  return node.status
}

const roomRisk = (type, status) => {
  if (status === 'weak') return 'high'
  if (['elite', 'boss'].includes(type)) return 'high'
  if (['battle', 'event'].includes(type)) return 'normal'
  return 'low'
}

const iconFor = node => ({
  diagnosis: referenceTokenIcons.magicOrb,
  battle: mapLegendIcons.enemy,
  elite: mapLegendIcons.elite,
  boss: referenceTokenIcons.bossHeartFlame,
  treasure: mapLegendIcons.treasure,
  rest: mapLegendIcons.rest,
  shop: mapLegendIcons.merchant,
  event: mapLegendIcons.unknown
})[node.roomType] || mapLegendIcons.unknown

const roomLabel = type => ({
  diagnosis: 'Diagnosis',
  battle: 'Battle',
  elite: 'Elite',
  boss: 'Boss',
  rest: 'Rest',
  shop: 'Shop',
  treasure: 'Treasure',
  event: 'Event'
})[type] || 'Room'

const statusText = status => ({
  cleared: 'Cleared',
  weak: 'Weak',
  available: 'Available',
  locked: 'Locked'
})[status] || status

const riskText = risk => ({
  low: 'Low',
  normal: 'Normal',
  high: 'High'
})[risk] || 'Normal'

const rewardText = node => ({
  diagnosis: 'Initial state',
  battle: 'Card / coins',
  elite: 'Rare reward',
  boss: 'Chapter clear',
  rest: 'Heal / review',
  shop: 'Hint / cleanup',
  treasure: 'Course resource',
  event: 'Benefit and cost'
})[node.roomType] || 'Reward'

const pickProfile = payload => payload?.profile || payload || {}

const refreshProfile = async () => {
  try {
    const res = await getStudentProfile(studentId.value, courseId.value)
    if (res.data.code === 200) profile.value = pickProfile(res.data.data)
  } catch {
    profile.value = {}
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [profileRes, mapRes, rankRes] = await Promise.allSettled([
      getStudentProfile(studentId.value, courseId.value),
      getTowerMap(studentId.value, courseId.value),
      getLeaderboard(courseId.value, 'progress')
    ])

    if (profileRes.status === 'fulfilled' && profileRes.value.data.code === 200) {
      profile.value = pickProfile(profileRes.value.data.data)
    }
    if (mapRes.status === 'fulfilled' && mapRes.value.data.code === 200) {
      floors.value = mapRes.value.data.data || []
    }
    if (rankRes.status === 'fulfilled' && rankRes.value.data.code === 200) {
      leaderboard.value = rankRes.value.data.data || []
    }

    selectedNode.value =
      allVisualNodes.value.find(node => ['available', 'weak'].includes(node.status)) ||
      allVisualNodes.value.find(node => node.status === 'cleared') ||
      allVisualNodes.value[0] ||
      null
  } finally {
    loading.value = false
  }
}

const previewNode = node => {
  if (node.status !== 'locked') selectedNode.value = node
}

const selectNode = node => {
  if (node.status === 'locked') {
    ElMessage.warning('Node is locked')
    return
  }
  selectedNode.value = node
}

const enterNode = node => {
  selectNode(node)
  if (node.status === 'locked') return
  enterSelected()
}

const enterSelected = () => {
  const node = selectedNode.value
  if (!node || node.status === 'locked') return
  if (['diagnosis', 'battle', 'elite', 'boss'].includes(node.roomType)) {
    router.push({
      path: `/floor/${node.kpId}`,
      query: {
        courseId: courseId.value,
        courseName: courseName.value,
        floorName: node.kpName,
        boss: node.roomType === 'boss' ? '1' : '0',
        roomType: node.roomType
      }
    })
    return
  }
  activeRoomType.value = node.roomType
  roomVisible.value = true
}

const openSupply = () => {
  roomVisible.value = false
  supplyVisible.value = true
}

const roomEventType = roomType => ({
  treasure: 'treasure_opened',
  rest: 'rest_taken',
  shop: 'shop_purchased',
  event: 'event_resolved'
})[roomType] || 'event_resolved'

const sendRoomEvent = async (roomType, payload = {}) => {
  const node = selectedNode.value
  const res = await sendGameEvent(studentId.value, {
    course_id: courseId.value,
    event_type: roomEventType(roomType),
    room_type: roomType,
    reward_name: payload.rewardName || payload.reward_name || '',
    knowledge_point_id: node?.kpId || '',
    source_id: node?.kpId || ''
  })
  if (res.data.code === 200) profile.value = pickProfile(res.data.data)
}

const completeRoom = async payload => {
  roomVisible.value = false
  try {
    await sendRoomEvent(payload?.roomType || activeRoomType.value, payload || {})
  } catch {
    ElMessage.warning('Room event sync failed')
  }
  ElMessage.success('Room completed')
  loadData()
}

const pickCourse = async course => {
  const code = course.courseCode || course.code || course.id
  const name = course.courseName || course.name || courseName.value
  if (code) localStorage.setItem('courseId', code)
  if (name) localStorage.setItem('courseName', name)
  try {
    await sendRoomEvent('treasure', { rewardName: 'course_card' })
  } catch {
    ElMessage.warning('Treasure event sync failed')
  }
  ElMessage.success('Knowledge card added')
  loadData()
}

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}

const openRoomFromQuery = () => {
  const room = route.query.room
  if (!room) return
  activeRoomType.value = String(room)
  roomVisible.value = true
}

watch(() => route.query.room, openRoomFromQuery)
onMounted(async () => {
  await loadData()
  openRoomFromQuery()
})
</script>

<style scoped>
.tower-page {
  min-height: 100vh;
  color: #f8edcf;
  background-position: center;
  background-size: cover;
  background-attachment: fixed;
}

.tower-shell {
  position: relative;
  min-height: calc(100vh - 88px);
  width: 100%;
  margin: 0;
  padding: 0;
}

.panel-card {
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(24, 16, 13, .76), rgba(8, 10, 14, .84));
  box-shadow: 0 24px 70px rgba(0, 0, 0, .42);
  backdrop-filter: blur(2px);
}

.map-stage {
  position: relative;
  overflow: hidden;
  min-height: calc(100vh - 88px);
  padding: 24px 28px 34px;
}

.act-header {
  position: absolute;
  z-index: 5;
  top: 22px;
  left: 30px;
  right: 30px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 0;
  pointer-events: none;
  text-shadow: 0 4px 18px rgba(0, 0, 0, .78);
}

.act-actions {
  display: flex;
  gap: 10px;
  pointer-events: auto;
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
  font-size: 32px;
}

h2 {
  font-size: 22px;
}

.route-map {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  width: min(43vw, 620px);
  min-height: calc(100vh - 126px);
  margin: 62px auto 0;
  padding: 70px 0 20px;
}

.route-lantern {
  position: absolute;
  inset: 30px 34% 16px;
  opacity: .16;
  clip-path: polygon(50% 0, 62% 14%, 56% 28%, 68% 45%, 58% 60%, 66% 82%, 50% 100%, 34% 82%, 42% 60%, 32% 45%, 44% 28%, 38% 14%);
  background: linear-gradient(180deg, #f0b85d, #2b1410 56%, transparent);
  pointer-events: none;
}

.route-row {
  position: relative;
  display: flex;
  justify-content: center;
  gap: clamp(28px, 7vw, 120px);
}

.route-row::before {
  content: '';
  position: absolute;
  top: -18px;
  left: 24%;
  right: 24%;
  height: 34px;
  border-top: 2px dashed rgba(232, 184, 92, .22);
  border-radius: 50%;
}

.route-row:first-child::before {
  display: none;
}

.route-node {
  position: relative;
  display: grid;
  width: 92px;
  height: 108px;
  place-items: center;
  border: 0;
  color: #faedcd;
  background: transparent;
  cursor: pointer;
  transition: transform .18s ease-out, filter .18s ease-out, opacity .18s ease-out;
}

.route-node:disabled {
  cursor: not-allowed;
  filter: grayscale(.9);
  opacity: .38;
}

.route-node:hover:not(:disabled),
.route-node.selected {
  transform: translateY(-6px);
  filter: brightness(1.12);
}

.node-glow {
  position: absolute;
  inset: 8px 10px 20px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(241, 190, 92, .38), transparent 64%);
  opacity: 0;
  transition: opacity .18s ease-out;
}

.route-node.available .node-glow,
.route-node.weak .node-glow,
.route-node.selected .node-glow {
  opacity: 1;
}

.route-node.weak .node-glow {
  background: radial-gradient(circle, rgba(220, 86, 62, .45), transparent 64%);
}

.node-icon {
  position: relative;
  width: 68px;
  height: 68px;
  object-fit: contain;
  filter: drop-shadow(0 12px 16px rgba(0, 0, 0, .44));
}

.route-node.boss .node-icon {
  width: 84px;
  height: 84px;
}

.route-node strong,
.route-node small {
  position: relative;
  text-shadow: 0 2px 8px rgba(0, 0, 0, .78);
}

.route-node strong {
  font-size: 15px;
}

.route-node small {
  color: #e0bb76;
  font-size: 12px;
  font-weight: 800;
}

.node-panel {
  position: absolute;
  z-index: 6;
  top: 112px;
  right: 32px;
  display: grid;
  align-content: start;
  gap: 14px;
  width: min(330px, calc(100vw - 64px));
}

.panel-card {
  padding: 18px;
  background: linear-gradient(180deg, rgba(42, 23, 19, .68), rgba(8, 10, 14, .72));
  box-shadow: 0 20px 46px rgba(0, 0, 0, .34);
  backdrop-filter: blur(8px);
}

.panel-card.primary {
  background:
    linear-gradient(180deg, rgba(82, 35, 24, .74), rgba(14, 12, 13, .78)),
    radial-gradient(circle at 40% 0, rgba(220, 128, 43, .16), transparent 34%);
}

.panel-copy {
  min-height: 72px;
  margin: 12px 0 16px;
  color: #dec8a4;
  line-height: 1.7;
}

.intel-list,
.stat-grid,
.legend-list {
  display: grid;
  gap: 10px;
}

.intel-list {
  margin-bottom: 16px;
}

.intel-list span,
.stat-grid span {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 36px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .08);
  color: #d9c4a2;
}

.intel-list b,
.stat-grid b {
  overflow: hidden;
  max-width: 170px;
  color: #fff4d4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-list span {
  display: grid;
  grid-template-columns: 30px 1fr;
  align-items: center;
  gap: 10px;
  color: #d9c4a2;
}

.legend-list img {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.spire-button,
.danger-button,
.ghost-button {
  min-height: 44px;
  border-radius: 6px;
}

.spire-button {
  width: 100%;
  border-color: #da9a4d;
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #73301f);
}

.danger-button {
  border-color: #bf6d55;
  color: #fff5d6;
  background: linear-gradient(180deg, #8c2d24, #5a201b);
}

.ghost-button {
  min-width: 44px;
  border-color: rgba(238, 181, 91, .36);
  color: #f8ebcb;
  background: rgba(255, 255, 255, .08);
}

@media (max-width: 1160px) {
  .route-map {
    width: min(58vw, 620px);
  }
  .node-panel {
    right: 24px;
  }
}

@media (max-width: 760px) {
  .tower-shell {
    width: 100%;
  }
  .map-stage {
    min-height: 680px;
    padding: 16px;
  }
  .act-header {
    flex-direction: column;
  }
  .route-map {
    width: min(100%, 520px);
    min-height: 580px;
    margin-top: 118px;
    padding-inline: 10px;
  }
  .route-row {
    gap: 18px;
  }
  .route-node {
    width: 74px;
  }
  .node-icon {
    width: 56px;
    height: 56px;
  }
  .node-panel {
    position: static;
    grid-template-columns: 1fr;
    width: min(100% - 24px, 520px);
    margin: 0 auto 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-node,
  .node-glow {
    transition: none;
  }
  .route-node:hover:not(:disabled),
  .route-node.selected {
    transform: none;
  }
}
</style>
