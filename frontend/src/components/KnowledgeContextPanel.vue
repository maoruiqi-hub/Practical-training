<template>
  <section class="context-panel">
    <p class="kicker">Knowledge Context</p>
    <h2>图谱上下文</h2>

    <div v-if="loading" class="context-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <template v-else>
      <div v-if="currentNode" class="current-node">
        <span>当前知识点</span>
        <strong>{{ currentNode.name || currentNode.knowledgePointId }}</strong>
      </div>

      <div v-if="hasContext" class="context-groups">
        <div v-for="group in groups" :key="group.key" class="context-group">
          <span>{{ group.label }}</span>
          <button
            v-for="node in group.nodes"
            :key="node.id"
            type="button"
            @click="goToNode(node.id)"
          >
            {{ node.name }}
          </button>
          <small v-if="!group.nodes.length">暂无</small>
        </div>
      </div>

      <el-empty v-else description="暂无图谱上下文" :image-size="72" />
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeGraph } from '../api'

const props = defineProps({
  courseId: { type: [String, Number], required: true },
  knowledgePointId: { type: [String, Number], required: true }
})

const router = useRouter()
const loading = ref(false)
const nodes = ref([])
const edges = ref([])

const nodeId = node => String(node.knowledgePointId || node.id || node.kpId || '')
const edgeFrom = edge => String(edge.fromKnowledgePointId || edge.from || edge.source || '')
const edgeTo = edge => String(edge.toKnowledgePointId || edge.to || edge.target || '')
const relationType = edge => String(edge.relationType || edge.type || edge.relation || '').toLowerCase()

const currentNode = computed(() =>
  nodes.value.find(node => nodeId(node) === String(props.knowledgePointId))
)

const nodeIndex = computed(() => {
  const map = new Map()
  nodes.value.forEach(node => map.set(nodeId(node), node))
  return map
})

const related = computed(() => {
  const current = String(props.knowledgePointId)
  const prerequisites = []
  const next = []
  const peers = []

  edges.value.forEach(edge => {
    const from = edgeFrom(edge)
    const to = edgeTo(edge)
    const type = relationType(edge)
    if (type === 'related' && (from === current || to === current)) {
      peers.push(from === current ? to : from)
      return
    }
    if (to === current) prerequisites.push(from)
    if (from === current) next.push(to)
  })

  return {
    prerequisites: toNodeItems(prerequisites),
    next: toNodeItems(next),
    peers: toNodeItems(peers)
  }
})

const groups = computed(() => [
  { key: 'prerequisites', label: '前置', nodes: related.value.prerequisites },
  { key: 'next', label: '后继', nodes: related.value.next },
  { key: 'peers', label: '相关', nodes: related.value.peers }
])

const hasContext = computed(() => groups.value.some(group => group.nodes.length))

const toNodeItems = ids => [...new Set(ids)]
  .filter(Boolean)
  .map(id => {
    const node = nodeIndex.value.get(id)
    return {
      id,
      name: node?.name || node?.knowledgePointName || id
    }
  })

const loadGraph = async () => {
  if (!props.courseId || !props.knowledgePointId) return
  loading.value = true
  try {
    const res = await getKnowledgeGraph(props.courseId)
    if (res.data.code === 200) {
      nodes.value = res.data.data?.nodes || []
      edges.value = res.data.data?.edges || []
    } else {
      nodes.value = []
      edges.value = []
    }
  } catch {
    nodes.value = []
    edges.value = []
  } finally {
    loading.value = false
  }
}

const goToNode = targetId => {
  router.push({
    path: `/floor/${targetId}`,
    query: { courseId: props.courseId, roomType: 'diagnosis' }
  })
}

watch(() => [props.courseId, props.knowledgePointId], loadGraph)
onMounted(loadGraph)
</script>

<style scoped>
.context-panel {
  padding: 18px;
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(60, 31, 20, .9), rgba(14, 12, 13, .94));
  box-shadow: 0 18px 44px rgba(0, 0, 0, .36);
}

.kicker {
  margin: 0 0 6px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  color: #fff6dc;
  font-size: 20px;
}

.context-loading {
  margin-top: 14px;
}

.current-node {
  display: grid;
  gap: 5px;
  margin-top: 14px;
  padding: 10px;
  border-radius: 6px;
  color: #d9c4a2;
  background: rgba(255, 255, 255, .07);
}

.current-node span {
  font-size: 12px;
}

.current-node strong {
  color: #fff4d4;
}

.context-groups {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.context-group {
  display: grid;
  gap: 8px;
}

.context-group span {
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
}

.context-group button {
  min-height: 34px;
  padding: 7px 10px;
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 6px;
  color: #f8edcf;
  text-align: left;
  background: rgba(255, 255, 255, .07);
  cursor: pointer;
}

.context-group button:hover {
  border-color: rgba(237, 185, 90, .75);
  background: rgba(237, 185, 90, .14);
}

.context-group small {
  color: #bda983;
}
</style>
