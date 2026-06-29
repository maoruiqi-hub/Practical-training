<template>
  <div class="run-page" v-loading="loading">
    <header class="run-topbar">
      <div class="brand-lockup">
        <span class="spire-mark" aria-hidden="true"></span>
        <div>
          <p class="kicker">Tower Run</p>
          <h1>{{ courseName }}</h1>
        </div>
      </div>
      <div class="top-actions">
        <span class="player-tag">{{ user.name || user.username }}</span>
        <el-button class="icon-button" aria-label="刷新路线" @click="loadData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button class="danger-button" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </header>

    <GameHud :profile="profile" :course-name="courseName" />

    <main class="run-board">
      <section class="route-panel" aria-label="本局爬塔路线">
        <div class="route-backdrop" aria-hidden="true"></div>
        <div class="act-banner">
          <p class="kicker">Act {{ activeAct }}</p>
          <h2>{{ actTitle }}</h2>
          <span>{{ actSubtitle }}</span>
        </div>

        <div class="map-stack">
          <div
            v-for="row in routeRows"
            :key="row.level"
            class="route-row"
            :class="{ boss: row.nodes.some(node => node.roomType === 'boss') }"
          >
            <button
              v-for="node in row.nodes"
              :key="node.nodeId"
              type="button"
              class="route-node"
              :class="[node.status, node.roomType]"
              :disabled="node.status === 'locked'"
              :aria-label="`${node.kpName}，${roomLabel(node.roomType)}，${statusText(node.status)}`"
              @click="enterNode(node)"
            >
              <span class="node-symbol" aria-hidden="true"></span>
              <strong>{{ node.level }}</strong>
              <small>{{ roomLabel(node.roomType) }}</small>
              <em>{{ node.kpName }}</em>
            </button>
          </div>
        </div>
      </section>

      <aside class="run-side">
        <section class="run-panel">
          <p class="kicker">Next Choice</p>
          <h2>下一步</h2>
          <p class="side-copy">{{ nextCopy }}</p>
          <el-button class="spire-button" @click="openFirstAvailable">
            <el-icon><Aim /></el-icon>
            进入可选房间
          </el-button>
        </section>

        <section class="run-panel">
          <p class="kicker">Run Status</p>
          <h2>本局状态</h2>
          <div class="stat-list">
            <span>已通关 <b>{{ runStats.cleared }}</b></span>
            <span>待强化 <b>{{ runStats.weak }}</b></span>
            <span>可进入 <b>{{ runStats.available }}</b></span>
          </div>
        </section>

        <section class="run-panel">
          <p class="kicker">Room Types</p>
          <div class="legend-list">
            <span><i class="legend battle"></i>战斗：测验题卡</span>
            <span><i class="legend elite"></i>精英：综合练习</span>
            <span><i class="legend rest"></i>休息：画像补给</span>
            <span><i class="legend shop"></i>商店：错题强化</span>
            <span><i class="legend treasure"></i>宝箱：课程资源</span>
            <span><i class="legend event"></i>事件：进度学情</span>
          </div>
        </section>
      </aside>
    </main>

    <GameRoomModal
      v-model="roomVisible"
      :room-type="activeRoomType"
      :profile="profile"
      :user="user"
      :student-id="studentId"
      :course-name="courseName"
      :run-stats="runStats"
      @open-supply="openSupply"
      @course-picked="pickCourse"
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
import { getLeaderboard, getStudentProfile, getTowerMap } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const profile = ref({})
const floors = ref([])
const leaderboard = ref([])
const roomVisible = ref(false)
const supplyVisible = ref(false)
const activeRoomType = ref('start')

const roomPattern = ['battle', 'treasure', 'battle', 'rest', 'elite', 'battle', 'shop', 'battle', 'event', 'boss', 'battle', 'treasure', 'elite', 'boss']
const fallbackNames = [
  'Python 基础语法',
  '变量与数据类型',
  '流程控制',
  '函数与模块',
  '基础应用 Boss',
  '列表与元组',
  '字典与集合',
  '文件处理',
  '异常处理',
  '数据处理 Boss',
  '面向对象',
  '常用库实践',
  '综合程序设计',
  '项目实战 Boss'
]

const studentId = computed(() =>
  user.studentNo || user.student_no || user.no || user.id || user.username || user.name || '1'
)
const courseId = computed(() =>
  route.query.courseId || route.query.course_id || localStorage.getItem('courseId') || '1'
)
const courseName = computed(() => route.query.courseName || localStorage.getItem('courseName') || 'Python 程序设计')
const activeAct = computed(() => Math.min(3, Math.max(1, Math.ceil((firstOpenLevel.value || 1) / 5))))
const actTitle = computed(() => ['基础山道', '机械城廊', '终章塔尖'][activeAct.value - 1])
const actSubtitle = computed(() => ['建立语法牌组', '强化数据处理路线', '完成项目 Boss 战'][activeAct.value - 1])

const fallbackFloors = fallbackNames.map((name, index) => ({
  kpId: String(index + 1),
  kpName: name,
  level: index + 1,
  masteryRate: index === 0 ? 45 : 0,
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
      kpName: item.kpName || item.knowledgePointName || item.name || item.title || `第 ${index + 1} 层`,
      level,
      masteryRate: Number(item.masteryRate ?? item.mastery_rate ?? item.mastery ?? 0),
      status: normalizeStatus(item, index),
      roomType: item.boss || item.isBoss || item.is_boss ? 'boss' : roomType
    }
  })
}

const routeNodes = computed(() => normalizeFloors(floors.value))
const firstOpenLevel = computed(() => routeNodes.value.find(node => node.status === 'available' || node.status === 'weak')?.level || 1)
const runStats = computed(() => ({
  cleared: routeNodes.value.filter(node => node.status === 'cleared').length,
  weak: routeNodes.value.filter(node => node.status === 'weak').length,
  available: routeNodes.value.filter(node => node.status === 'available').length
}))
const routeRows = computed(() => routeNodes.value
  .slice()
  .sort((a, b) => b.level - a.level)
  .map(node => ({ level: node.level, nodes: [node] }))
)
const nextCopy = computed(() => {
  const target = routeNodes.value.find(node => node.status === 'available' || node.status === 'weak')
  if (!target) return '本局路线暂时没有可进入节点，刷新后查看后端最新进度。'
  return `下一间房是「${roomLabel(target.roomType)}」，对应 ${target.kpName}。`
})

const roomLabel = type => ({
  battle: '战斗',
  elite: '精英',
  boss: 'Boss',
  rest: '休息点',
  shop: '商店',
  treasure: '宝箱',
  event: '事件'
}[type] || '房间')

const statusText = status => ({
  cleared: '已通过',
  weak: '薄弱待强化',
  available: '可进入',
  locked: '未解锁'
}[status] || status)

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
  } finally {
    loading.value = false
  }
}

const enterNode = node => {
  if (node.status === 'locked') {
    ElMessage.warning('这条路线还没有解锁')
    return
  }
  if (['battle', 'elite', 'boss'].includes(node.roomType)) {
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

const openFirstAvailable = () => {
  const target = routeNodes.value.find(node => node.status === 'available' || node.status === 'weak') ||
    routeNodes.value.find(node => node.status === 'cleared')
  if (target) enterNode(target)
}

const openSupply = () => {
  roomVisible.value = false
  supplyVisible.value = true
}

const pickCourse = course => {
  const code = course.courseCode || course.code || course.id
  const name = course.courseName || course.name || courseName.value
  if (code) localStorage.setItem('courseId', code)
  if (name) localStorage.setItem('courseName', name)
  ElMessage.success('知识卡已加入本局路线')
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
.run-page {
  min-height: 100vh;
  padding: 18px;
  color: #f8edcf;
  background:
    linear-gradient(180deg, rgba(20, 12, 10, .92), rgba(8, 10, 14, .98)),
    repeating-linear-gradient(135deg, rgba(214, 139, 52, .08) 0 1px, transparent 1px 18px);
  overscroll-behavior: contain;
}

.run-topbar,
.run-board {
  max-width: 1440px;
  margin: 0 auto;
}

.run-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 76px;
}

.brand-lockup {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 14px;
  align-items: center;
}

.spire-mark {
  width: 50px;
  height: 58px;
  border: 3px solid #e5ad57;
  border-radius: 10px;
  background:
    radial-gradient(circle at 50% 22%, rgba(255, 236, 168, .9) 0 8px, transparent 9px),
    linear-gradient(180deg, #6d2b1d, #1b1110);
  box-shadow: inset 0 0 0 4px rgba(255, 240, 196, .12), 0 16px 28px rgba(0, 0, 0, .35);
}

.kicker {
  margin: 0 0 5px;
  color: #dfa54f;
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
}

h2 {
  font-size: 22px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.player-tag {
  min-height: 36px;
  padding: 8px 12px;
  border: 1px solid rgba(232, 184, 92, .3);
  border-radius: 6px;
  color: #fff4d4;
  background: rgba(255, 255, 255, .07);
}

.run-board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  margin-top: 18px;
}

.route-panel {
  position: relative;
  overflow: hidden;
  min-height: 760px;
  padding: 28px;
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(73, 32, 20, .86), rgba(17, 12, 13, .96)),
    linear-gradient(90deg, rgba(0, 0, 0, .34), rgba(244, 181, 78, .05), rgba(0, 0, 0, .4));
  box-shadow: 0 22px 60px rgba(0, 0, 0, .4);
}

.route-backdrop {
  position: absolute;
  inset: 70px 24% 42px 24%;
  opacity: .2;
  clip-path: polygon(50% 0, 62% 18%, 57% 32%, 72% 50%, 62% 65%, 70% 85%, 50% 100%, 30% 85%, 38% 65%, 28% 50%, 43% 32%, 38% 18%);
  background: linear-gradient(180deg, #9b3e24, #0d0e13);
}

.act-banner {
  position: relative;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(232, 184, 92, .18);
}

.act-banner span {
  color: #d8c3a1;
}

.map-stack {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 28px 0 4px;
}

.route-row {
  position: relative;
  display: flex;
  justify-content: center;
}

.route-row::before {
  content: '';
  position: absolute;
  top: -22px;
  width: 3px;
  height: 26px;
  border-radius: 999px;
  background: rgba(223, 165, 79, .38);
}

.route-row:first-child::before {
  display: none;
}

.route-node {
  position: relative;
  display: grid;
  place-items: center;
  width: 132px;
  min-height: 132px;
  padding: 12px 10px;
  border: 1px solid rgba(232, 184, 92, .42);
  border-radius: 8px;
  color: #faedcd;
  background: linear-gradient(180deg, #4d2619, #171110);
  box-shadow: 0 16px 22px rgba(0, 0, 0, .34);
  cursor: pointer;
  transition: transform .18s ease-out, border-color .18s ease-out, filter .18s ease-out;
}

.route-node:hover:not(:disabled) {
  transform: translateY(-5px);
  border-color: #f0c66b;
  filter: brightness(1.08);
}

.route-node:disabled {
  cursor: not-allowed;
  opacity: .5;
  filter: grayscale(.8);
}

.node-symbol {
  width: 28px;
  height: 28px;
  border: 2px solid currentColor;
  border-radius: 50%;
  background: rgba(255, 255, 255, .08);
}

.route-node.treasure .node-symbol {
  border-radius: 6px;
}

.route-node.shop .node-symbol {
  clip-path: polygon(50% 0, 100% 40%, 82% 100%, 18% 100%, 0 40%);
}

.route-node.rest .node-symbol {
  border-radius: 8px 8px 20px 20px;
}

.route-node.event .node-symbol {
  transform: rotate(45deg);
}

.route-node.elite .node-symbol,
.route-node.boss .node-symbol {
  border-radius: 4px;
  transform: rotate(45deg);
}

.route-node strong {
  color: #fff7dc;
  font-size: 18px;
}

.route-node small {
  color: #dfa54f;
  font-weight: 800;
}

.route-node em {
  display: -webkit-box;
  overflow: hidden;
  min-height: 36px;
  color: #ead8b5;
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
  text-align: center;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.route-node.cleared {
  border-color: rgba(137, 203, 119, .72);
}

.route-node.available,
.route-node.weak {
  border-color: rgba(240, 198, 107, .84);
}

.route-node.weak {
  box-shadow: 0 0 0 2px rgba(220, 92, 67, .2), 0 16px 22px rgba(0, 0, 0, .34);
}

.run-side {
  display: grid;
  align-content: start;
  gap: 14px;
}

.run-panel {
  padding: 18px;
  border: 1px solid rgba(232, 184, 92, .24);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(60, 31, 20, .94), rgba(18, 14, 13, .96));
}

.side-copy {
  margin: 10px 0 16px;
  color: #d8c3a1;
  line-height: 1.7;
}

.stat-list,
.legend-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.stat-list span,
.legend-list span {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 36px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .07);
  color: #d7c4a6;
}

.stat-list b {
  color: #fff4d4;
}

.legend {
  width: 15px;
  height: 15px;
  border: 2px solid #dfa54f;
  border-radius: 50%;
}

.legend.elite,
.legend.boss,
.legend.event {
  border-radius: 4px;
  transform: rotate(45deg);
}

.legend.treasure {
  border-radius: 4px;
}

.legend.shop {
  clip-path: polygon(50% 0, 100% 40%, 82% 100%, 18% 100%, 0 40%);
}

.legend.rest {
  border-radius: 5px 5px 12px 12px;
}

.spire-button,
.danger-button,
.icon-button {
  min-height: 44px;
  border-radius: 6px;
}

.spire-button {
  width: 100%;
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #73301f);
  border-color: #da9a4d;
}

.danger-button {
  color: #fff5d6;
  background: linear-gradient(180deg, #8c2d24, #5a201b);
  border-color: #bf6d55;
}

.icon-button {
  min-width: 44px;
  color: #f8ebcb;
  background: rgba(255, 255, 255, .06);
  border-color: rgba(238, 181, 91, .3);
}

@media (max-width: 1100px) {
  .run-board {
    grid-template-columns: 1fr;
  }
  .run-side {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .run-page {
    padding: 12px;
  }
  .run-topbar,
  .act-banner {
    align-items: flex-start;
    flex-direction: column;
  }
  h1 {
    font-size: 28px;
  }
  .route-panel {
    min-height: 680px;
    padding: 18px 12px;
  }
  .route-node {
    width: 112px;
    min-height: 118px;
  }
  .run-side {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-node {
    transition: none;
  }
  .route-node:hover:not(:disabled) {
    transform: none;
  }
}
</style>
