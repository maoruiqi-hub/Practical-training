<template>
  <div v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom:12px">← 返回课程</el-button>
    <h3>课程知识图谱</h3>
    <el-empty v-if="!nodes.length && !loading" description="暂无知识点" />
    <div v-show="nodes.length" ref="chartRef" style="height:70vh;min-height:460px;border:1px solid #dcdfe6;border-radius:4px" />
    <el-alert v-if="edges.length" style="margin-top:12px" type="info" :closable="false" title="可拖拽节点；单击节点进入知识点详情。" />
  </div>
</template>
<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getKnowledgeGraph } from '../api'
const route = useRoute(); const router = useRouter(); const nodes = ref([]); const edges = ref([]); const loading = ref(true); const chartRef = ref(null)
let chart
const relationLabel = { hierarchy: '层级', prerequisite: '前置', related: '关联' }
const drawGraph = () => {
  chart = echarts.init(chartRef.value)
  chart.setOption({ tooltip: { formatter: p => p.dataType === 'edge' ? relationLabel[p.data.relationType] : p.data.name }, series: [{ type: 'graph', layout: 'force', roam: true, draggable: true, label: { show: true, position: 'right' }, force: { repulsion: 320, edgeLength: 120 }, data: nodes.value.map(n => ({ id: n.knowledgePointId, name: n.name, value: n.chapter, symbolSize: 42 + (n.importance || 1) * 5 })), links: edges.value.map(e => ({ source: e.fromKnowledgePointId, target: e.toKnowledgePointId, relationType: e.relationType, label: { show: true, formatter: relationLabel[e.relationType] || e.relationType } })), lineStyle: { curveness: 0.15, color: '#909399' } }] })
  chart.on('click', params => { if (params.dataType === 'node') router.push(`/knowledge-point/${params.data.id}`) })
}
onMounted(async () => { try { const r = await getKnowledgeGraph(route.params.code); if (r.data.code === 200) { nodes.value = r.data.data.nodes; edges.value = r.data.data.edges; await nextTick(); if (nodes.value.length) drawGraph() } else ElMessage.error(r.data.msg) } catch { ElMessage.error('知识图谱加载失败') } finally { loading.value = false } })
onBeforeUnmount(() => chart?.dispose())
</script>
