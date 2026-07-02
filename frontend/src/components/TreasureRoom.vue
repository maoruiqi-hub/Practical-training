<template>
  <el-dialog
    v-model="visible"
    width="860px"
    class="treasure-room-dialog"
    append-to-body
    :show-close="true"
  >
    <section class="treasure-scene" :style="roomStyle">
      <div class="treasure-scrim">
        <header class="room-head">
          <img class="room-icon" :src="mapLegendIcons.treasure" alt="" />
          <div>
            <p class="kicker">课程宝箱</p>
            <h2>知识宝箱</h2>
            <p>从当前课程资源里选择一份补给，带回路线继续推进。</p>
          </div>
        </header>

        <div v-if="loading" class="loading-panel">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else class="choice-grid reward">
          <article
            v-for="resource in shownResources"
            :key="resource.resourceId"
            class="choice-card reward-card"
            role="button"
            tabindex="0"
            @click="claimResource(resource)"
            @keydown.enter.prevent="claimResource(resource)"
          >
            <small>{{ resourceTypeLabel(resource) }}</small>
            <strong>{{ resource.title || '课程资源' }}</strong>
            <span>{{ resource.chapter || nodeName || courseName }}</span>
            <button
              v-if="isPptResource(resource)"
              type="button"
              class="mini-action"
              @click.stop="openLecture(resource)"
            >
              AI 讲解
            </button>
          </article>

          <button
            v-if="!shownResources.length"
            type="button"
            class="choice-card reward-card fallback"
            @click="claimFallback"
          >
            <small>兜底奖励</small>
            <strong>知识卡碎片</strong>
            <span>课程资源待配置，先获得一份路线奖励。</span>
          </button>
        </div>
      </div>
    </section>

    <AiTutorPanel
      v-model="lectureVisible"
      :knowledge-point-id="lectureKnowledgePointId"
      :knowledge-point-name="nodeName"
      :course-id="courseId"
      :resource-id="lectureResourceId"
      mode="lecture"
    />
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AiTutorPanel from './AiTutorPanel.vue'
import { gameBackgrounds, mapLegendIcons } from '../data/gameAssetManifest'
import { getCourseResources, recordCourseResourceView } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [String, Number], default: '' },
  courseName: { type: String, default: 'Python 程序设计' },
  selectedNode: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'room-complete'])

const loading = ref(false)
const resources = ref([])
const lectureVisible = ref(false)
const lectureResourceId = ref('')
const lectureKnowledgePointId = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const nodeName = computed(() => props.selectedNode?.kpName || '')
const shownResources = computed(() => resources.value.slice(0, 3))
const roomStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(8, 10, 14, .14), rgba(8, 10, 14, .82)), url(${gameBackgrounds.treasure})`
}))

const loadResources = async () => {
  if (!visible.value || !props.courseId) return
  loading.value = true
  try {
    const params = props.selectedNode?.kpId ? { knowledgePointId: props.selectedNode.kpId } : {}
    const res = await getCourseResources(props.courseId, params)
    resources.value = res.data.code === 200 ? (res.data.data || []) : []
  } catch {
    resources.value = []
    ElMessage.warning('课程资源加载失败，已保留宝箱奖励')
  } finally {
    loading.value = false
  }
}

const isPptResource = resource => {
  const type = String(resource.resourceType || resource.resource_type || resource.type || '').toLowerCase()
  return type.includes('ppt') || type.includes('powerpoint')
}

const resourceTypeLabel = resource => {
  const type = String(resource.resourceType || resource.resource_type || resource.type || '').toLowerCase()
  if (type.includes('video') || type.includes('mp4')) return '视频'
  if (type.includes('ppt') || type.includes('powerpoint')) return '课件'
  if (type.includes('pdf') || type.includes('doc') || type.includes('document')) return '文档'
  if (type.includes('link') || type.includes('url')) return '链接'
  if (type.includes('exercise') || type.includes('practice')) return '练习'
  return '课程资源'
}

const openLecture = resource => {
  const kpId = props.selectedNode?.kpId || resource.knowledgePointId || resource.knowledge_point_id
  if (!kpId) {
    ElMessage.warning('当前资源缺少知识点')
    return
  }
  lectureKnowledgePointId.value = kpId
  lectureResourceId.value = resource.resourceId || resource.resource_id || resource.id
  lectureVisible.value = true
}

const closeWithReward = rewardName => {
  emit('room-complete', { roomType: 'treasure', rewardName })
  visible.value = false
}

const claimResource = async resource => {
  try {
    const resourceId = resource.resourceId || resource.resource_id || resource.id
    if (resourceId) await recordCourseResourceView(resourceId, { action: 'start', durationMs: 0 })
  } catch {
    ElMessage.warning('资源学习行为记录失败')
  }
  ElMessage.success('课程资源已收入宝箱')
  closeWithReward(resource.title || '课程资源')
}

const claimFallback = () => {
  ElMessage.success('知识卡碎片已收入宝箱')
  closeWithReward('知识卡碎片')
}

watch(() => [props.modelValue, props.courseId, props.selectedNode?.kpId], loadResources, { immediate: true })
</script>

<style scoped>
:deep(.treasure-room-dialog) {
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(232, 184, 92, .35);
  border-radius: 8px;
  background: #17100f;
  box-shadow: 0 28px 70px rgba(0, 0, 0, .52);
}

:deep(.treasure-room-dialog .el-dialog__header) {
  display: none;
}

:deep(.treasure-room-dialog .el-dialog__body) {
  padding: 0;
  color: #f8edcf;
}

.treasure-scene {
  min-height: 560px;
  background-position: center;
  background-size: cover;
}

.treasure-scrim {
  min-height: inherit;
  padding: 28px;
  background:
    radial-gradient(circle at 50% 38%, rgba(241, 190, 92, .12), transparent 32%),
    linear-gradient(90deg, rgba(8, 10, 14, .68), rgba(8, 10, 14, .18), rgba(8, 10, 14, .72));
}

.room-head {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
}

.room-icon {
  width: 72px;
  height: 72px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(0, 0, 0, .42));
}

.kicker {
  margin: 0 0 5px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.room-head h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
}

.room-head p:last-child {
  margin: 8px 0 0;
  color: #f2dba9;
}

.loading-panel {
  max-width: 620px;
  padding: 18px;
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 8px;
  background: rgba(12, 12, 16, .62);
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
  max-width: 760px;
}

.choice-card {
  min-height: 156px;
  padding: 18px;
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 8px;
  color: #f8edcf;
  text-align: left;
  background: rgba(12, 12, 16, .68);
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, background .18s ease;
}

.choice-card:hover {
  transform: translateY(-2px);
  border-color: rgba(237, 185, 90, .75);
  background: rgba(32, 22, 17, .86);
}

.choice-card small,
.choice-card strong,
.choice-card span {
  display: block;
}

.choice-card small {
  margin-bottom: 10px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.choice-card strong {
  min-height: 48px;
  font-size: 18px;
  line-height: 1.35;
}

.choice-card span {
  margin-top: 12px;
  color: #e7d1a2;
  line-height: 1.5;
}

.mini-action {
  min-height: 32px;
  margin-top: 14px;
  padding: 6px 10px;
  border: 1px solid rgba(237, 185, 90, .58);
  border-radius: 6px;
  color: #fff5d6;
  background: rgba(183, 91, 40, .58);
  cursor: pointer;
}

.mini-action:hover {
  border-color: rgba(255, 226, 163, .86);
  background: rgba(183, 91, 40, .78);
}

.fallback {
  border-style: dashed;
}
</style>
