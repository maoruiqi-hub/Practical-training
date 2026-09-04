<template>
  <section class="student-ability-map" :class="{ compact }" v-loading="loading">
    <div v-if="!compact" class="summary-row">
      <div>
        <strong>{{ abilityCards.length }}</strong>
        <span>能力点</span>
      </div>
      <div>
        <strong>{{ linkedKnowledgeCount }}</strong>
        <span>关联知识点</span>
      </div>
      <div>
        <strong>{{ overallMastery === null ? '--' : `${overallMastery}%` }}</strong>
        <span>平均掌握度</span>
      </div>
    </div>

    <el-empty v-if="!abilityCards.length && !loading" description="暂无能力图谱" />

    <section v-if="radarDimensions.length || trueCompetencies.length" class="radar-comparison">
      <article v-if="radarDimensions.length" class="radar-panel">
        <div class="radar-title">
          <strong>课程要点</strong>
          <span>体现本课程各项学习内容的掌握情况</span>
        </div>
        <div v-show="chartRadarDimensions.length" ref="radarRef" class="radar-chart" aria-label="课程要点雷达图"></div>
        <el-empty v-if="!chartRadarDimensions.length" :image-size="72" description="暂无课程要点证据" />
        <p>{{ radarSummary || '展示当前课程各项学习内容的掌握情况。' }}</p>
      </article>

      <article v-if="trueCompetencies.length" class="radar-panel true-radar-panel">
        <div class="radar-title">
          <strong>核心能力</strong>
          <span>体现通过课程学习形成的综合能力</span>
        </div>
        <div v-show="chartTrueCompetencies.length" ref="trueRadarRef" class="radar-chart" aria-label="核心能力雷达图"></div>
        <el-empty v-if="!chartTrueCompetencies.length" :image-size="72" description="暂无核心能力证据" />
        <p>{{ trueCompetencySummary }}</p>
      </article>
    </section>

    <div v-if="abilityCards.length && !compact" class="ability-grid">
      <article
        v-for="ability in abilityCards"
        :key="ability.abilityPointId"
        class="ability-card"
        :class="statusClass(ability.status)"
      >
        <header>
          <div>
            <small>{{ ability.status }}</small>
            <h3>{{ ability.name }}</h3>
          </div>
          <b>{{ ability.masteryRate === null ? '--' : `${ability.masteryRate}%` }}</b>
        </header>
        <div v-if="ability.latestDelta" class="delta-line" :class="{ positive: ability.latestDelta.deltaScore >= 0, negative: ability.latestDelta.deltaScore < 0 }">
          <strong>{{ ability.latestDelta.deltaScore >= 0 ? '+' : '' }}{{ ability.latestDelta.deltaScore }}</strong>
          <span>{{ ability.latestDelta.aiSummary || ability.latestDelta.reason }}</span>
        </div>
        <p>{{ ability.description || '暂无说明' }}</p>
        <div class="knowledge-chips">
          <span
            v-for="point in ability.knowledgePoints"
            :key="point.id"
            :class="chipClass(point.masteryRate)"
          >
            {{ point.name }}
            <b>{{ point.masteryRate === null ? '暂无' : `${point.masteryRate}%` }}</b>
          </span>
          <span v-if="!ability.knowledgePoints.length" class="empty-chip">未绑定知识点</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { getTrueCompetency, getAbilityMap, getAbilityRadar, getKnowledgeGraph, getKnowledgeMastery } from '../api'

const props = defineProps({
  studentNo: { type: [String, Number], required: true },
  courseCode: { type: [String, Number], required: true },
  runId: { type: [String, Number], default: '' },
  nodeId: { type: [String, Number], default: '' },
  compact: { type: Boolean, default: false }
})

const loading = ref(false)
const abilityPoints = ref([])
const mappings = ref([])
const knowledgePoints = ref([])
const masteryRows = ref([])
const abilityRadar = ref(null)
const trueCompetencies = ref([])
const radarRef = ref(null)
const trueRadarRef = ref(null)
let radarChart = null
let trueRadarChart = null

const knowledgeIndex = computed(() => {
  const map = new Map()
  knowledgePoints.value.forEach(point => {
    const id = String(point.knowledgePointId || point.id || point.kpId || '')
    if (id) map.set(id, point)
  })
  return map
})

const masteryIndex = computed(() => {
  const map = new Map()
  masteryRows.value.forEach(item => {
    const id = String(item.knowledgePointId || item.knowledge_point_id || '')
    const score = item.masteryScore ?? item.mastery_score ?? item.score
    if (id && score !== null && score !== undefined) map.set(id, Number(score))
  })
  return map
})

const abilityCards = computed(() => abilityPoints.value.map(ability => {
  const abilityId = ability.abilityPointId || ability.ability_point_id || ability.id
  const radarDimension = radarDimensions.value.find(item => String(item.abilityPointId) === String(abilityId))
  const latestDelta = radarDimension && abilityRadar.value?.mode === 'node'
    ? {
        deltaScore: Number(radarDimension.delta || 0),
        reason: radarDimension.reason || '由本次答题证据聚合得出'
      }
    : null
  const linkedIds = mappings.value
    .filter(item => String(item.abilityPointId || item.ability_point_id) === String(abilityId))
    .map(item => String(item.knowledgePointId || item.knowledge_point_id))
    .filter(Boolean)
  const linkedKnowledgePoints = linkedIds.map(id => {
    const point = knowledgeIndex.value.get(id)
    const rawMastery = masteryIndex.value.get(id)
    const masteryRate = rawMastery === undefined ? null : clamp(rawMastery)
    const importance = Math.max(1, Number(point?.importance ?? 1))
    return {
      id,
      name: point?.name || point?.knowledgePointName || id,
      masteryRate,
      importance
    }
  })
  const evidencedPoints = linkedKnowledgePoints.filter(point => point.masteryRate !== null)
  const projectedScore = radarDimension?.afterScore
  const masteryRate = projectedScore !== null && projectedScore !== undefined
    ? clamp(projectedScore)
    : evidencedPoints.length
    ? Math.round(
        evidencedPoints.reduce((sum, point) => sum + point.masteryRate * point.importance, 0) /
        evidencedPoints.reduce((sum, point) => sum + point.importance, 0)
      )
    : null
  const status = masteryRate === null ? '暂无证据' : masteryRate >= 85 ? '掌握' : masteryRate >= 60 ? '推进中' : '薄弱'
  return {
    ...ability,
    abilityPointId: abilityId,
    name: ability.name || ability.title || abilityId,
    description: ability.description || '',
    knowledgePoints: linkedKnowledgePoints,
    masteryRate,
    status,
    latestDelta
  }
}))

const linkedKnowledgeCount = computed(() => new Set(mappings.value.map(item => item.knowledgePointId || item.knowledge_point_id)).size)
const overallMastery = computed(() => {
  const evidenced = abilityCards.value.filter(ability => ability.masteryRate !== null)
  if (!evidenced.length) return null
  return Math.round(evidenced.reduce((sum, ability) => sum + ability.masteryRate, 0) / evidenced.length)
})
const radarDimensions = computed(() => abilityRadar.value?.dimensions || [])
const chartRadarDimensions = computed(() => radarDimensions.value.filter(item => item.afterScore !== null && item.afterScore !== undefined))
const chartTrueCompetencies = computed(() => trueCompetencies.value.filter(item => item.score !== null && item.score !== undefined))
const radarSummary = computed(() => abilityRadar.value?.summary || '')
const trueCompetencySummary = computed(() => {
  const sourceCount = trueCompetencies.value.reduce((sum, item) => {
    const matched = String(item.coverage || '').match(/^\d+/)
    return sum + Number(matched?.[0] || 0)
  }, 0)
  return sourceCount
    ? `当前 ${trueCompetencies.value.length} 项核心能力，共聚合 ${sourceCount} 个有效能力来源。`
    : `当前 ${trueCompetencies.value.length} 项核心能力，依据已发布映射矩阵计算。`
})

const clamp = value => Math.max(0, Math.min(100, Number(value || 0)))
const statusClass = status => status === '掌握' ? 'mastered' : status === '推进中' ? 'progressing' : status === '暂无证据' ? 'no-evidence' : 'weak'
const chipClass = mastery => mastery === null ? 'no-evidence-chip' : mastery >= 85 ? 'mastered-chip' : mastery >= 60 ? 'progress-chip' : 'weak-chip'
const formatRadarLabel = value => {
  const text = String(value || '')
  if (text.length <= 8) return text
  const secondLine = text.slice(8, 15)
  return `${text.slice(0, 8)}\n${secondLine}${text.length > 15 ? '…' : ''}`
}

const loadData = async () => {
  if (!props.studentNo || !props.courseCode) return
  loading.value = true
  try {
    const [abilityRes, graphRes, masteryRes, trueCompetencyRes, radarRes] = await Promise.allSettled([
      getAbilityMap(props.courseCode),
      getKnowledgeGraph(props.courseCode),
      getKnowledgeMastery(props.studentNo, props.courseCode),
      getTrueCompetency(props.studentNo, props.courseCode),
      getAbilityRadar(props.studentNo, props.courseCode, props.runId, props.nodeId)
    ])
    if (abilityRes.status === 'fulfilled' && abilityRes.value.data.code === 200) {
      abilityPoints.value = abilityRes.value.data.data?.abilityPoints || []
      mappings.value = abilityRes.value.data.data?.mappings || []
    } else {
      abilityPoints.value = []
      mappings.value = []
    }
    if (graphRes.status === 'fulfilled' && graphRes.value.data.code === 200) {
      knowledgePoints.value = graphRes.value.data.data?.nodes || []
    } else {
      knowledgePoints.value = []
    }
    if (masteryRes.status === 'fulfilled' && masteryRes.value.data.code === 200) {
      masteryRows.value = masteryRes.value.data.data || []
    } else {
      masteryRows.value = []
    }
    if (trueCompetencyRes.status === 'fulfilled' && trueCompetencyRes.value.data.code === 200) {
      trueCompetencies.value = trueCompetencyRes.value.data.data || []
    } else {
      trueCompetencies.value = []
    }
    if (radarRes.status === 'fulfilled' && radarRes.value.data.code === 200) {
      abilityRadar.value = radarRes.value.data.data || null
    } else {
      abilityRadar.value = null
    }
    await nextTick()
    renderRadars()
  } finally {
    loading.value = false
  }
}

const radarBase = indicators => ({
  indicator: indicators,
  radius: '62%',
  splitNumber: 5,
  axisName: { color: '#334155', fontSize: 12, lineHeight: 16, formatter: formatRadarLabel },
  splitLine: { lineStyle: { color: '#dbe3ef' } },
  splitArea: { areaStyle: { color: ['#f8fafc', '#ffffff'] } },
  axisLine: { lineStyle: { color: '#cbd5e1' } }
})

const renderOriginalRadar = () => {
  if (!radarRef.value || !chartRadarDimensions.value.length) return
  if (!radarChart) radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    color: ['#2563eb'],
    tooltip: { trigger: 'item' },
    radar: radarBase(chartRadarDimensions.value.map(item => ({ name: item.name || item.abilityPointId, max: 100 }))),
    series: [{
      type: 'radar',
      data: [{
        value: chartRadarDimensions.value.map(item => clamp(item.afterScore)),
        name: '课程要点',
        areaStyle: { opacity: 0.2 }
      }]
    }]
  }, true)
}

const renderTrueRadar = () => {
  if (!trueRadarRef.value || !chartTrueCompetencies.value.length) return
  if (!trueRadarChart) trueRadarChart = echarts.init(trueRadarRef.value)
  trueRadarChart.setOption({
    color: ['#16a34a'],
    tooltip: { trigger: 'item' },
    radar: radarBase(chartTrueCompetencies.value.map(item => ({ name: item.name || item.competencyId, max: 100 }))),
    series: [{
      type: 'radar',
      data: [{
        value: chartTrueCompetencies.value.map(item => clamp(item.score)),
        name: '核心能力',
        areaStyle: { opacity: 0.2 }
      }]
    }]
  }, true)
}

const renderRadars = () => {
  renderOriginalRadar()
  renderTrueRadar()
}

const resizeRadars = () => {
  if (radarChart) radarChart.resize()
  if (trueRadarChart) trueRadarChart.resize()
}

watch(() => [props.studentNo, props.courseCode, props.runId, props.nodeId], loadData)
onMounted(loadData)
onMounted(() => window.addEventListener('resize', resizeRadars))
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeRadars)
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  if (trueRadarChart) {
    trueRadarChart.dispose()
    trueRadarChart = null
  }
})
</script>

<style scoped>
.student-ability-map {
  display: grid;
  gap: 16px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-row div {
  min-height: 72px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.summary-row strong {
  display: block;
  color: #111827;
  font-size: 26px;
  line-height: 1;
}

.summary-row span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.ability-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.radar-comparison {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 360px), 1fr));
  gap: 10px;
}

.radar-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.true-radar-panel {
  border-color: #bbf7d0;
  background: #fbfffc;
}

.radar-title strong,
.radar-title span {
  display: block;
}

.radar-title strong {
  color: #0f172a;
  font-size: 17px;
}

.radar-title span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.radar-chart {
  width: 100%;
  height: 360px;
}

.student-ability-map.compact .radar-chart {
  height: 300px;
}

.radar-panel p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.ability-card {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-left: 5px solid #ef4444;
  border-radius: 8px;
  background: #fff;
}

.ability-card.progressing {
  border-left-color: #ca8a04;
}

.ability-card.no-evidence {
  border-left-color: #94a3b8;
}

.ability-card.mastered {
  border-left-color: #16a34a;
}

.no-evidence-chip {
  color: #64748b;
  background: #f1f5f9;
}

.ability-card header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.ability-card small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.ability-card h3 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 17px;
  line-height: 1.35;
}

.ability-card header b {
  color: #111827;
  font-size: 24px;
}

.ability-card p {
  min-height: 42px;
  margin: 0;
  color: #64748b;
  line-height: 1.5;
}

.delta-line {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f1f5f9;
}

.delta-line strong {
  font-size: 18px;
}

.delta-line.positive strong {
  color: #15803d;
}

.delta-line.negative strong {
  color: #b91c1c;
}

.delta-line span {
  color: #475569;
  line-height: 1.4;
}

.knowledge-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-chips span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 6px 9px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.3;
}

.knowledge-chips b {
  font-weight: 800;
}

.mastered-chip {
  color: #14532d;
  background: #dcfce7;
}

.progress-chip {
  color: #713f12;
  background: #fef3c7;
}

.weak-chip {
  color: #7f1d1d;
  background: #fee2e2;
}

.empty-chip {
  color: #64748b;
  background: #f1f5f9;
}

@media (max-width: 720px) {
  .summary-row {
    grid-template-columns: 1fr;
  }

  .radar-comparison {
    grid-template-columns: 1fr;
  }
}
</style>
