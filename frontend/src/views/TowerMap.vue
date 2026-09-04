<template>
  <div class="tower-page" v-loading="loading" :style="pageStyle">
    <GameHud :profile="profile" :course-name="courseName" compact :show-hp="false" />

    <main class="tower-shell">
      <section class="map-stage" aria-label="路线地图">
        <div class="act-header">
          <div>
            <p class="kicker">第 {{ activeAct }} 幕 · 路线图</p>
            <h1>{{ actTitle }}</h1>
          </div>
          <div class="act-actions">
            <el-button class="ghost-button" aria-label="刷新路线" @click="loadData">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button class="danger-button" @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出
            </el-button>
          </div>
        </div>

        <div class="route-map" :style="routeMapStyle">
          <el-empty
            v-if="mapError"
            class="route-empty"
            :description="mapError"
            :image-size="120"
          />
          <div class="route-lantern" aria-hidden="true"></div>
          <svg v-if="!mapError" class="route-paths" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
            <path
              v-for="edge in routeEdges"
              :key="edge.key"
              class="route-path"
              :class="{ cleared: edge.cleared, active: edge.active }"
              :d="edge.path"
            />
          </svg>
          <div v-if="!mapError" class="route-nodes">
            <button
              v-for="node in visualNodes"
              :key="node.nodeId"
              type="button"
              class="route-node"
              :class="[node.status, node.roomType, { selected: selectedNode?.nodeId === node.nodeId }]"
              :disabled="node.status === 'locked'"
              :aria-label="`${node.kpName}, ${roomLabel(node.roomType)}, ${statusText(node.status)}`"
              :style="node.positionStyle"
              @focus="selectNode(node)"
              @click="handleNodeClick(node)"
            >
              <span class="node-glow" aria-hidden="true"></span>
              <img class="node-icon" :src="iconFor(node)" alt="" />
              <strong>{{ node.level }}</strong>
              <small>{{ roomLabel(node.roomType) }}</small>
            </button>
          </div>
        </div>
      </section>

      <aside class="node-panel" aria-label="节点详情">
        <button class="activity-challenge" type="button" aria-label="打开活动挑战，完成老师布置的作业" @click="openActivityChallenges">
          <span class="activity-challenge-icon">
            <img :src="referenceTokenIcons.magicOrb" alt="" />
          </span>
          <strong>活动挑战</strong>
          <small>完成作业</small>
        </button>
        <section class="panel-card primary">
          <p class="kicker">下一步选择</p>
          <h2>{{ previewTitle }}</h2>
          <p class="panel-copy">{{ previewCopy }}</p>
          <div v-if="selectedNode" class="intel-list">
            <span>知识点 <b>{{ selectedNode.kpName }}</b></span>
            <span>掌握度 <b>{{ selectedNode.masteryRate }}%</b></span>
            <span v-if="selectedNode.masterySource">来源 <b>{{ masterySourceLabel(selectedNode.masterySource) }}</b></span>
            <span v-if="selectedNode.abilityPointName">关联能力 <b>{{ selectedNode.abilityPointName }}</b></span>
            <span>房间 <b>{{ roomLabel(selectedNode.roomType) }}</b></span>
            <span>风险 <b>{{ riskText(selectedNode.risk) }}</b></span>
            <span>奖励 <b>{{ rewardText(selectedNode) }}</b></span>
          </div>
          <el-button
            class="spire-button"
            :disabled="!canEnterSelected"
            @click="enterSelected"
          >
            <el-icon><Aim /></el-icon>
            {{ enterButtonText }}
          </el-button>
        </section>

      </aside>
    </main>

    <el-dialog
      v-model="optionDialogVisible"
      :title="optionDialogTitle"
      width="min(680px, 92vw)"
      append-to-body
      :close-on-click-modal="!optionSubmitting"
    >
      <div v-loading="optionLoading" class="server-option-panel">
        <p class="server-option-hint">选项和数值由服务端生成，选择后会同步更新库存、成长记录和路线状态。</p>
        <div class="server-option-grid">
          <button
            v-for="option in nodeOptions"
            :key="option.optionId"
            type="button"
            class="server-option-card"
            :disabled="optionSubmitting || optionEnvelope.resolved"
            @click="chooseOption(option)"
          >
            <strong>{{ option.title }}</strong>
            <span>{{ option.description }}</span>
            <small v-if="option.selected">已选择</small>
          </button>
        </div>
        <div v-if="nodeInventory.length" class="inventory-strip">
          <span v-for="item in nodeInventory" :key="item.itemCode">
            {{ item.name }} × {{ item.quantity }}
            <el-button
              v-if="item.itemCode === 'healing_supply'"
              size="small"
              :disabled="optionSubmitting"
              @click="useSupply(item)"
            >使用</el-button>
          </span>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { getCurrentUser, getCourseId, getStudentId } from '../utils/authContext'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Refresh, SwitchButton } from '@element-plus/icons-vue'
import GameHud from '../components/GameHud.vue'
import { gameBackgrounds, mapLegendIcons, referenceTokenIcons } from '../data/gameAssetManifest'
import { chooseTowerNodeOption, enterTowerNode, getLeaderboard, getStudentProfile, getTowerNodeOptions, getTowerRun, useTowerInventory } from '../api'

const route = useRoute()
const router = useRouter()
const user = getCurrentUser()

const loading = ref(false)
const mapError = ref('')
const profile = ref({})
const floors = ref([])
const towerRun = ref(null)
const leaderboard = ref([])
const selectedNode = ref(null)
const optionDialogVisible = ref(false)
const optionLoading = ref(false)
const optionSubmitting = ref(false)
const optionEnvelope = ref({ options: [], inventory: [], resolved: false })
const optionNode = ref(null)

const roomPattern = ['diagnosis', 'battle', 'treasure', 'battle', 'rest', 'elite', 'event', 'shop', 'battle', 'treasure', 'battle', 'rest', 'elite', 'boss']
const normalizeCourseName = name => String(name || '').trim() === 'Python Program Design' ? 'Python 程序设计' : name

const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .28), rgba(6, 8, 12, .68)), url(${gameBackgrounds.mapAct1})`
}))

const studentId = computed(() =>
  getStudentId(user)
)
const courseId = computed(() =>
  getCourseId(route)
)
const courseName = computed(() => normalizeCourseName(route.query.courseName || localStorage.getItem('courseName') || 'Python 程序设计'))

const normalizeStatus = (floor, index) => {
  const raw = String(floor.floorStatus || floor.floor_status || floor.status || '').toLowerCase()
  const mastery = Number(floor.masteryRate ?? floor.mastery_rate ?? floor.mastery ?? 0)
  const accessible = floor.isAccessible ?? floor.is_accessible
  if (['cleared', 'cleared_by_diagnosis', 'mastered', 'passed', 'done'].includes(raw) || mastery >= 80) return 'cleared'
  if (['weak', 'review', 'remedial'].includes(raw) || (mastery > 0 && mastery < 60)) return 'weak'
  if (raw === 'locked' || accessible === false) return 'locked'
  if (raw === 'available' || raw === 'open' || accessible === true || index === 0) return 'available'
  return 'locked'
}

const normalizeFloors = list => {
  const source = Array.isArray(list) ? list : []
  return source.map((item, index) => {
    const level = Number(item.level || item.nodeOrder || item.node_order || item.floorLevel || index + 1)
    const roomType = item.roomType || item.room_type || roomPattern[index % roomPattern.length]
    return {
      runId: item.runId || item.run_id || towerRun.value?.runId || '',
      nodeId: item.nodeId || item.node_id || `${level}-${item.kpId || item.knowledgePointId || index}`,
      nodeOrder: Number(item.nodeOrder || item.node_order || level),
      row: Number(item.row || item.rowNo || item.row_no || level),
      col: Number(item.col || item.colNo || item.col_no || 1),
      kpId: item.kpId || item.knowledgePointId || item.knowledge_point_id || item.id || String(index + 1),
      kpName: item.kpName || item.knowledgePointName || item.name || item.title || `Floor ${index + 1}`,
      level,
      masteryRate: Number(item.masteryRate ?? item.mastery_rate ?? item.mastery ?? 0),
      masterySource: item.masterySource || item.mastery_source || '',
      abilityPointId: item.abilityPointId || item.ability_point_id || '',
      abilityPointName: item.abilityPointName || item.ability_point_name || '',
      statusReason: item.statusReason || item.status_reason || '',
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

const routeRows = computed(() => {
  const groups = new Map()
  baseNodes.value.forEach((node, index) => {
    const row = Number(node.row || node.level || index + 1)
    if (!groups.has(row)) groups.set(row, [])
    groups.get(row).push({
      ...node,
      risk: roomRisk(node.roomType, node.status),
      branch: Number(node.col || 1) - 1
    })
  })
  return Array.from(groups.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([level, nodes]) => ({
      level,
      nodes: nodes.sort((a, b) => Number(a.col || 1) - Number(b.col || 1))
    }))
})

const visualRows = computed(() => routeRows.value.slice().reverse())

const mapHeight = computed(() => Math.max(720, visualRows.value.length * 86 + 80))

const xPositions = count => {
  if (count <= 1) return [50]
  const padding = count >= 4 ? 18 : 28
  return Array.from({ length: count }, (_, index) => padding + ((100 - padding * 2) * index) / (count - 1))
}

const visualNodes = computed(() => {
  const rows = visualRows.value
  const rowStep = rows.length > 1 ? 86 / (rows.length - 1) : 0
    return rows.flatMap((row, rowIndex) => {
      const positions = xPositions(row.nodes.length)
      return row.nodes.map((node, nodeIndex) => ({
        ...node,
        mapRow: rowIndex,
        mapX: positions[nodeIndex],
      mapY: 7 + rowIndex * rowStep,
      positionStyle: {
        left: `${positions[nodeIndex]}%`,
        top: `${7 + rowIndex * rowStep}%`
      }
    }))
  })
})

const routeEdges = computed(() => {
  const nodesByRow = visualRows.value.map((row, rowIndex) =>
    row.nodes
      .map(node => visualNodes.value.find(item => item.nodeId === node.nodeId && item.mapRow === rowIndex))
      .filter(Boolean)
  )
  const populatedRows = nodesByRow.filter(row => row.length)
  const edges = []

  for (let rowIndex = 0; rowIndex < populatedRows.length - 1; rowIndex += 1) {
    const fromNodes = populatedRows[rowIndex]
    const toNodes = populatedRows[rowIndex + 1]
    fromNodes.forEach(from => {
      const nearest = toNodes
        .slice()
        .sort((a, b) => Math.abs(a.mapX - from.mapX) - Math.abs(b.mapX - from.mapX))
      const targets = nearest.length > 1 && Math.abs(nearest[0].mapX - from.mapX) > 22
        ? nearest.slice(0, 2)
        : nearest.slice(0, 1)
      targets.forEach(to => {
        const midY = (from.mapY + to.mapY) / 2
        const isCleared = from.status === 'cleared' && to.status === 'cleared'
        const isActive = selectedNode.value && [from.nodeId, to.nodeId].includes(selectedNode.value.nodeId)
        edges.push({
          key: `${from.nodeId}-${to.nodeId}`,
          cleared: isCleared,
          active: isActive,
          path: `M ${from.mapX} ${from.mapY} C ${from.mapX} ${midY}, ${to.mapX} ${midY}, ${to.mapX} ${to.mapY}`
        })
      })
    })
  }
  return edges
})

const routeMapStyle = computed(() => ({ minHeight: `${mapHeight.value}px` }))

const activeAct = computed(() => Math.min(3, Math.max(1, Math.ceil((baseNodes.value[firstOpenIndex.value]?.level || 1) / 5))))
const actTitle = computed(() => ['基础路线', '数据城堡', '最终高塔'][activeAct.value - 1])

const allVisualNodes = computed(() => routeRows.value.flatMap(row => row.nodes))
const backendReadyRoomTypes = new Set(['diagnosis', 'battle', 'elite', 'boss', 'treasure', 'rest', 'shop', 'event'])
const canEnterSelected = computed(() => selectedNode.value && selectedNode.value.status !== 'locked'
  && backendReadyRoomTypes.has(selectedNode.value.roomType))
const enterButtonText = computed(() => '进入房间')
const nodeOptions = computed(() => optionEnvelope.value.options || [])
const nodeInventory = computed(() => optionEnvelope.value.inventory || [])
const optionDialogTitle = computed(() => optionNode.value ? `${roomLabel(optionNode.value.roomType)}·${optionNode.value.kpName}` : '节点选择')
const previewTitle = computed(() => selectedNode.value ? roomLabel(selectedNode.value.roomType) : '选择下一节点')
const previewCopy = computed(() => {
  if (!selectedNode.value) return '点击已解锁的地图节点，查看知识点、风险和奖励。'
  const node = selectedNode.value
  return roomDescriptions[node.roomType] || `进入 ${node.kpName}，完成挑战后返回地图解锁下一步。`
})

const legendItems = computed(() => [
  { type: 'battle', label: '战斗：答题挑战', icon: mapLegendIcons.enemy },
  { type: 'elite', label: '精英：高难练习', icon: mapLegendIcons.elite },
  { type: 'treasure', label: '宝箱：资源奖励', icon: mapLegendIcons.treasure },
  { type: 'rest', label: '休息：恢复复盘', icon: mapLegendIcons.rest },
  { type: 'shop', label: '商店：购买补给', icon: mapLegendIcons.merchant },
  { type: 'event', label: '事件：学习选择', icon: mapLegendIcons.unknown }
])

const roomDescriptions = {
  diagnosis: '先完成短诊断，确认当前知识点状态。',
  battle: '通过答题攻击敌人，并获得通关奖励。',
  elite: '更难的综合挑战，奖励也更稀有。',
  boss: '章节首领战，完成后结束本段路线。',
  treasure: '打开宝箱，获得课程资源或知识卡。',
  rest: '恢复状态、复盘薄弱点，为下一节点做准备。',
  shop: '花费金币购买提示或清理错题卡。',
  event: '处理一次带有收益和代价的学习事件。'
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
  diagnosis: '诊断',
  battle: '战斗',
  elite: '精英',
  boss: '首领',
  rest: '休息',
  shop: '商店',
  treasure: '宝箱',
  event: '事件'
})[type] || '房间'

const statusText = status => ({
  cleared: '已通过',
  cleared_by_diagnosis: '诊断跳过',
  weak: '薄弱',
  available: '可进入',
  locked: '未解锁'
})[status] || status

const riskText = risk => ({
  low: '低',
  normal: '中',
  high: '高'
})[risk] || '中'

const masterySourceLabel = source => ({
  knowledge_mastery: '来自知识点掌握度',
  competency_score: '来自能力评分',
  none: '暂无掌握度'
})[source] || source

const rewardText = node => ({
  diagnosis: '初始状态',
  battle: '卡牌 / 金币',
  elite: '稀有奖励',
  boss: '章节通关',
  rest: '恢复 / 复盘',
  shop: '提示 / 清理',
  treasure: '课程资源',
  event: '收益与代价'
})[node.roomType] || '奖励'

const pickProfile = payload => payload?.profile || payload || {}

const refreshProfile = async () => {
  if (!studentId.value || !courseId.value) return
  try {
    const res = await getStudentProfile(studentId.value, courseId.value)
    if (res.data.code === 200) profile.value = pickProfile(res.data.data)
  } catch {
    profile.value = {}
  }
}

const loadData = async () => {
  if (!studentId.value || !courseId.value) {
    mapError.value = '缺少学生或课程信息，请先选择课程。'
    return
  }
  loading.value = true
  mapError.value = ''
  try {
    const [profileRes, mapRes, rankRes] = await Promise.allSettled([
      getStudentProfile(studentId.value, courseId.value),
      getTowerRun(studentId.value, courseId.value),
      getLeaderboard(courseId.value, 'progress')
    ])

    if (profileRes.status === 'fulfilled' && profileRes.value.data.code === 200) {
      profile.value = pickProfile(profileRes.value.data.data)
    }
    if (mapRes.status === 'fulfilled' && mapRes.value.data.code === 200) {
      towerRun.value = mapRes.value.data.data || null
      floors.value = towerRun.value?.nodes || []
      if (!floors.value.length) mapError.value = '当前课程尚未生成可用路线'
    } else {
      towerRun.value = null
      floors.value = []
      mapError.value = mapRes.status === 'fulfilled'
        ? (mapRes.value.data.msg || '路线数据加载失败')
        : '路线数据加载失败，请稍后重试'
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

const selectNode = node => {
  if (node.status === 'locked') {
    ElMessage.warning('节点尚未解锁')
    return
  }
  selectedNode.value = node
}

const handleNodeClick = node => {
  if (node.status === 'locked') {
    selectNode(node)
    return
  }
  selectedNode.value = node
  enterSelected()
}

const openActivityChallenges = () => {
  router.push({
    path: `/task/${courseId.value}`,
    query: { courseName: courseName.value }
  })
}

const enterSelected = async () => {
  const node = selectedNode.value
  if (!node || node.status === 'locked') return
  if (['diagnosis', 'battle', 'elite', 'boss'].includes(node.roomType)) {
    let evaluationId = ''
    if (node.runId && node.nodeId) {
      try {
        const enterRes = await enterTowerNode(studentId.value, node.runId, node.nodeId)
        if (enterRes.data.code !== 200) throw new Error(enterRes.data.msg || '进入节点失败')
        evaluationId = enterRes.data.data?.evaluationId || ''
      } catch (error) {
        ElMessage.error(error?.message || '进入节点失败')
        return
      }
    }
    router.push({
      path: `/floor/${node.kpId}`,
      query: {
        courseId: courseId.value,
        courseName: courseName.value,
        floorName: node.kpName,
        runId: node.runId,
        nodeId: node.nodeId,
        evaluationId,
        boss: node.roomType === 'boss' ? '1' : '0',
        roomType: node.roomType
      }
    })
    return
  }
  optionNode.value = node
  optionDialogVisible.value = true
  optionLoading.value = true
  try {
    const response = await getTowerNodeOptions(studentId.value, node.runId, node.nodeId)
    if (response.data.code !== 200) throw new Error(response.data.msg || '节点选项加载失败')
    optionEnvelope.value = response.data.data || { options: [], inventory: [] }
  } catch (error) {
    optionDialogVisible.value = false
    ElMessage.error(error?.message || '节点选项加载失败')
  } finally {
    optionLoading.value = false
  }
}

const newActionId = () => window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`

const chooseOption = async option => {
  const node = optionNode.value
  if (!node || !option?.optionId) return
  optionSubmitting.value = true
  try {
    const response = await chooseTowerNodeOption(studentId.value, node.runId, node.nodeId, option.optionId, newActionId())
    if (response.data.code !== 200) throw new Error(response.data.msg || '节点结算失败')
    ElMessage.success(response.data.data?.title || '节点结算完成')
    optionDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '节点结算失败')
  } finally {
    optionSubmitting.value = false
  }
}

const useSupply = async item => {
  const node = optionNode.value
  if (!node) return
  optionSubmitting.value = true
  try {
    const response = await useTowerInventory(studentId.value, node.runId, node.nodeId, item.itemCode, newActionId())
    if (response.data.code !== 200) throw new Error(response.data.msg || '补给使用失败')
    optionEnvelope.value = { ...optionEnvelope.value, inventory: response.data.data?.inventory || [] }
    profile.value = response.data.data?.profile || profile.value
    ElMessage.success('恢复补给已使用')
  } catch (error) {
    ElMessage.error(error?.message || '补给使用失败')
  } finally {
    optionSubmitting.value = false
  }
}

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(async () => {
  await loadData()
})
</script>

<style scoped>
.server-option-panel {
  min-height: 180px;
}

.server-option-hint {
  margin: 0 0 16px;
  color: #7a684f;
  line-height: 1.65;
}

.server-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.server-option-card {
  display: grid;
  gap: 8px;
  min-height: 132px;
  padding: 16px;
  border: 1px solid #d3a958;
  border-radius: 8px;
  color: #3c2a19;
  text-align: left;
  background: linear-gradient(160deg, #fff9e9, #f0dfba);
  cursor: pointer;
}

.server-option-card:disabled {
  cursor: default;
  opacity: .62;
}

.server-option-card strong { font-size: 17px; }
.server-option-card span { line-height: 1.5; }
.server-option-card small { color: #8a5c17; font-weight: 700; }

.inventory-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #ead8b3;
}

.inventory-strip > span {
  display: flex;
  gap: 8px;
  align-items: center;
}

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

.activity-challenge {
  position: absolute;
  z-index: 4;
  top: 50%;
  right: calc(50vw + min(21.5vw, 310px) - 12px);
  display: grid;
  width: 122px;
  padding: 10px 8px 12px;
  place-items: center;
  border: 1px solid rgba(232, 184, 91, .52);
  border-radius: 12px;
  color: #fff1c9;
  background: linear-gradient(180deg, rgba(72, 42, 22, .9), rgba(17, 15, 17, .9));
  box-shadow: 0 12px 30px rgba(0, 0, 0, .42), 0 0 18px rgba(232, 184, 91, .14);
  cursor: pointer;
  transform: translateY(-50%);
  transition: transform .18s ease, border-color .18s ease, filter .18s ease;
}

.activity-challenge:hover,
.activity-challenge:focus-visible {
  border-color: #ffda85;
  filter: brightness(1.12);
  outline: none;
  transform: translateY(calc(-50% - 5px));
}

.activity-challenge-icon {
  display: grid;
  width: 84px;
  height: 84px;
  place-items: center;
  margin-bottom: 4px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(241, 190, 92, .34), transparent 68%);
}

.activity-challenge-icon img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  filter: drop-shadow(0 8px 10px rgba(0, 0, 0, .5));
}

.activity-challenge strong {
  font-size: 16px;
  letter-spacing: .08em;
}

.activity-challenge small {
  margin-top: 4px;
  color: #d8b779;
  font-size: 12px;
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
  width: min(43vw, 620px);
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

.route-paths,
.route-nodes {
  position: absolute;
  inset: 0;
}

.route-paths {
  z-index: 1;
  width: 100%;
  height: 100%;
  overflow: visible;
  pointer-events: none;
}

.route-path {
  fill: none;
  stroke: rgba(232, 184, 91, .62);
  stroke-width: 1.25;
  stroke-dasharray: 2.8 2.4;
  vector-effect: non-scaling-stroke;
  filter: drop-shadow(0 0 2px rgba(255, 215, 128, .3));
  transition: stroke .2s ease, stroke-width .2s ease, opacity .2s ease;
}

.route-path.cleared {
  stroke: #e8b85b;
  stroke-dasharray: none;
}

.route-path.active {
  stroke: #ffda85;
  stroke-width: 2.2;
  filter: drop-shadow(0 0 5px rgba(232, 184, 91, .8));
}

.route-path.active:not(.cleared) {
  stroke-dasharray: 2.8 2.4;
}

.route-nodes {
  z-index: 2;
}

.route-node {
  position: absolute;
  display: grid;
  width: 92px;
  height: 108px;
  place-items: center;
  border: 0;
  color: #faedcd;
  background: transparent;
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition: transform .18s ease-out, filter .18s ease-out, opacity .18s ease-out;
}

.route-node:disabled {
  cursor: not-allowed;
  filter: grayscale(.9);
  opacity: .38;
}

.route-node:hover:not(:disabled),
.route-node.selected {
  transform: translate(-50%, calc(-50% - 6px));
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
  .activity-challenge {
    right: calc(50vw + min(29vw, 310px) - 12px);
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
    margin-top: 118px;
    padding-inline: 10px;
  }
  .route-node {
    width: 74px;
  }
  .activity-challenge {
    top: 104px;
    left: 16px;
    right: auto;
    width: 104px;
    transform: none;
  }
  .activity-challenge:hover,
  .activity-challenge:focus-visible {
    transform: translateY(-5px);
  }
  .activity-challenge-icon {
    width: 64px;
    height: 64px;
  }
  .activity-challenge-icon img {
    width: 56px;
    height: 56px;
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
    transform: translate(-50%, -50%);
  }
}
</style>
