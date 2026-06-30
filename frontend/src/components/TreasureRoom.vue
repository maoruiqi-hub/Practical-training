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
            <p class="kicker">Treasure</p>
            <h2>知识宝箱</h2>
            <p>从当前课程资源里选择一份补给，带回路线继续推进。</p>
          </div>
        </header>

        <div v-if="loading" class="loading-panel">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else class="choice-grid reward">
          <button
            v-for="resource in shownResources"
            :key="resource.resourceId"
            type="button"
            class="choice-card reward-card"
            @click="claimResource(resource)"
          >
            <small>{{ resource.resourceType || 'Resource' }}</small>
            <strong>{{ resource.title || '课程资源' }}</strong>
            <span>{{ resource.chapter || nodeName || courseName }}</span>
          </button>

          <button
            v-if="!shownResources.length"
            type="button"
            class="choice-card reward-card fallback"
            @click="claimFallback"
          >
            <small>Fallback Reward</small>
            <strong>知识卡碎片</strong>
            <span>当前知识点暂无绑定资源，先获得一份路线奖励。</span>
          </button>
        </div>
      </div>
    </section>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
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

const closeWithReward = rewardName => {
  emit('room-complete', { roomType: 'treasure', rewardName })
  visible.value = false
}

const claimResource = async resource => {
  try {
    await recordCourseResourceView(resource.resourceId, { action: 'start', durationMs: 0 })
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

.fallback {
  border-style: dashed;
}
</style>
