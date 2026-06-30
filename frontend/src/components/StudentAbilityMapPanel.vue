<template>
  <section class="student-ability-map" v-loading="loading">
    <div class="summary-row">
      <div>
        <strong>{{ abilityCards.length }}</strong>
        <span>能力点</span>
      </div>
      <div>
        <strong>{{ linkedKnowledgeCount }}</strong>
        <span>关联知识点</span>
      </div>
      <div>
        <strong>{{ overallMastery }}%</strong>
        <span>平均掌握度</span>
      </div>
    </div>

    <el-empty v-if="!abilityCards.length && !loading" description="暂无能力图谱" />

    <div v-else class="ability-grid">
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
          <b>{{ ability.masteryRate }}%</b>
        </header>
        <p>{{ ability.description || '暂无说明' }}</p>
        <div class="knowledge-chips">
          <span
            v-for="point in ability.knowledgePoints"
            :key="point.id"
            :class="chipClass(point.masteryRate)"
          >
            {{ point.name }}
            <b>{{ point.masteryRate }}%</b>
          </span>
          <span v-if="!ability.knowledgePoints.length" class="empty-chip">未绑定知识点</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getAbilityMap, getKnowledgeGraph, getKnowledgeMastery } from '../api'

const props = defineProps({
  studentNo: { type: [String, Number], required: true },
  courseCode: { type: [String, Number], required: true }
})

const loading = ref(false)
const abilityPoints = ref([])
const mappings = ref([])
const knowledgePoints = ref([])
const masteryRows = ref([])

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
    if (id) map.set(id, Number(item.masteryScore ?? item.mastery_score ?? item.score ?? 0))
  })
  return map
})

const abilityCards = computed(() => abilityPoints.value.map(ability => {
  const abilityId = ability.abilityPointId || ability.ability_point_id || ability.id
  const linkedIds = mappings.value
    .filter(item => String(item.abilityPointId || item.ability_point_id) === String(abilityId))
    .map(item => String(item.knowledgePointId || item.knowledge_point_id))
    .filter(Boolean)
  const linkedKnowledgePoints = linkedIds.map(id => {
    const point = knowledgeIndex.value.get(id)
    const masteryRate = clamp(masteryIndex.value.get(id) ?? 0)
    return {
      id,
      name: point?.name || point?.knowledgePointName || id,
      masteryRate
    }
  })
  const masteryRate = linkedKnowledgePoints.length
    ? Math.round(linkedKnowledgePoints.reduce((sum, point) => sum + point.masteryRate, 0) / linkedKnowledgePoints.length)
    : 0
  const status = masteryRate >= 85 ? '掌握' : masteryRate >= 60 ? '推进中' : '薄弱'
  return {
    ...ability,
    abilityPointId: abilityId,
    name: ability.name || ability.title || abilityId,
    description: ability.description || '',
    knowledgePoints: linkedKnowledgePoints,
    masteryRate,
    status
  }
}))

const linkedKnowledgeCount = computed(() => new Set(mappings.value.map(item => item.knowledgePointId || item.knowledge_point_id)).size)
const overallMastery = computed(() => {
  if (!abilityCards.value.length) return 0
  return Math.round(abilityCards.value.reduce((sum, ability) => sum + ability.masteryRate, 0) / abilityCards.value.length)
})

const clamp = value => Math.max(0, Math.min(100, Number(value || 0)))
const statusClass = status => status === '掌握' ? 'mastered' : status === '推进中' ? 'progressing' : 'weak'
const chipClass = mastery => mastery >= 85 ? 'mastered-chip' : mastery >= 60 ? 'progress-chip' : 'weak-chip'

const loadData = async () => {
  if (!props.studentNo || !props.courseCode) return
  loading.value = true
  try {
    const [abilityRes, graphRes, masteryRes] = await Promise.allSettled([
      getAbilityMap(props.courseCode),
      getKnowledgeGraph(props.courseCode),
      getKnowledgeMastery(props.studentNo, props.courseCode)
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
  } finally {
    loading.value = false
  }
}

watch(() => [props.studentNo, props.courseCode], loadData)
onMounted(loadData)
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

.ability-card.mastered {
  border-left-color: #16a34a;
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
}
</style>
