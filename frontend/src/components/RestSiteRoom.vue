<template>
  <el-dialog
    v-model="visible"
    width="860px"
    class="rest-room-dialog"
    append-to-body
    :show-close="true"
  >
    <section class="rest-scene" :style="roomStyle">
      <div class="rest-scrim">
        <header class="room-head">
          <img class="room-icon" :src="mapLegendIcons.rest" alt="" />
          <div>
            <p class="kicker">Rest Site</p>
            <h2>休息点</h2>
            <p>选择一次主要行动：传承知识、整理补给，或按错题线索复习。</p>
          </div>
        </header>

        <!-- 传承知识 - 课程资源视图 -->
        <template v-if="showResources">
          <button type="button" class="back-link" @click="showResources = false; playingResource = null">
            ← 返回休息点
          </button>

          <!-- 视频播放器 -->
          <div v-if="playingResource" class="video-player-panel">
            <button type="button" class="back-link" @click="playingResource = null">
              ← 返回资源列表
            </button>
            <h3 style="color:#f8edcf;margin:0 0 10px">{{ playingResource.lessonTitle || '课程资源' }}</h3>
            <video
              :src="getVideoUrl(playingResource)"
              controls
              autoplay
              muted
              playsinline
              style="width:100%;max-height:56vh;border-radius:6px;background:#000"
            >
              您的浏览器不支持视频播放
            </video>
          </div>

          <div v-else-if="resourceLoading" class="loading-panel">
            <el-skeleton :rows="4" animated />
          </div>
          <div v-else class="resource-grid">
            <article
              v-for="resource in shownResources"
              :key="resource.lessonNo"
              class="resource-card"
              role="button"
              tabindex="0"
              @click="playResource(resource)"
              @keydown.enter.prevent="playResource(resource)"
            >
              <small>{{ resourceTypeLabel(resource) }}</small>
              <strong>{{ resource.lessonTitle || '课时资源' }}</strong>
              <span>{{ resource.description || nodeName || courseName }}</span>
            </article>
            <div v-if="!resourceLoading && !shownResources.length" class="empty-hint">
              <p>暂无课程资源，请在教师端上传视频资源。</p>
            </div>
          </div>
        </template>

        <!-- 主视图 -->
        <template v-else>
          <div class="rest-layout">
            <article class="status-board">
              <p class="kicker">Current State</p>
              <h3>路线状态</h3>
              <dl>
                <div>
                  <dt>HP</dt>
                  <dd>{{ profile.hp ?? '-' }}</dd>
                </div>
                <div>
                  <dt>防御</dt>
                  <dd>{{ profile.defense ?? profile.def ?? '-' }}</dd>
                </div>
                <div>
                  <dt>精力</dt>
                  <dd>{{ profile.energy ?? '-' }}</dd>
                </div>
              </dl>

              <div class="weak-panel">
                <p class="kicker">Review Cue</p>
                <strong>{{ weakPointTitle }}</strong>
                <span>{{ weakPointHint }}</span>
              </div>
            </article>

            <div class="choice-grid">
              <button type="button" class="choice-card" @click="openResources">
                <small>Knowledge Legacy</small>
                <strong>传承知识</strong>
                <span>查看当前知识点对应的课程视频资源，巩固学习成果。</span>
              </button>
              <button type="button" class="choice-card" @click="$emit('open-supply')">
                <small>Supply</small>
                <strong>整理补给</strong>
                <span>打开补给面板，使用已有 supply_used 能力恢复状态。</span>
              </button>
              <button type="button" class="choice-card" @click="reviewWeakPoint">
                <small>Review</small>
                <strong>{{ reviewStarted ? '完成薄弱点复习' : '复习薄弱点' }}</strong>
                <span>{{ reviewStarted ? '导师复习已打开，可以把休息点复习收益交给后端结算。' : '按错题线索打开 AI 导师，先把薄弱知识点讲清楚。' }}</span>
              </button>
            </div>
          </div>
        </template>
      </div>
    </section>

    <AiTutorPanel
      v-model="aiVisible"
      :knowledge-point-id="aiKnowledgePointId"
      :knowledge-point-name="weakPointTitle"
      :course-id="courseId"
      mode="qa"
      :initial-question="aiInitialQuestion"
    />
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AiTutorPanel from './AiTutorPanel.vue'
import { gameBackgrounds, mapLegendIcons } from '../data/gameAssetManifest'
import { getStudentWrongQuestions, getLessonList } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  studentId: { type: [String, Number], default: '' },
  courseId: { type: [String, Number], default: '' },
  courseName: { type: String, default: 'Python 程序设计' },
  profile: { type: Object, default: () => ({}) },
  selectedNode: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'open-supply', 'room-complete'])

const loading = ref(false)
const weakCards = ref([])
const aiVisible = ref(false)
const aiKnowledgePointId = ref('')
const reviewStarted = ref(false)

// 传承知识 - 课程资源
const showResources = ref(false)
const resourceLoading = ref(false)
const resources = ref([])
const playingResource = ref(null)

const getVideoUrl = resource => {
  return `/practical-training/${resource.resourceUrl || resource.resource_url || ''}`
}

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const nodeName = computed(() => props.selectedNode?.kpName || '')
const weakPoint = computed(() => weakCards.value[0] || null)
const weakPointTitle = computed(() => weakPoint.value?.knowledgePointName || props.selectedNode?.kpName || '当前知识点')
const weakPointHint = computed(() => {
  if (loading.value) return '正在读取错题线索...'
  if (weakPoint.value?.questionStem || weakPoint.value?.stem) return weakPoint.value.questionStem || weakPoint.value.stem
  return '暂无错题线索，可以把休息点用于当前路线复盘。'
})
const roomStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(8, 10, 14, .12), rgba(8, 10, 14, .82)), url(${gameBackgrounds.rest})`
}))
const shownResources = computed(() => resources.value.slice(0, 4))

const loadWeakCards = async () => {
  if (!visible.value || !props.studentId) return
  loading.value = true
  try {
    const res = await getStudentWrongQuestions(props.studentId, {
      knowledgePointId: props.selectedNode?.kpId || undefined
    })
    weakCards.value = res.data.code === 200 ? (res.data.data?.wrongList || []) : []
  } catch {
    weakCards.value = []
  } finally {
    loading.value = false
  }
}

const complete = rewardName => {
  emit('room-complete', { roomType: 'rest', rewardName })
  visible.value = false
}

const openResources = async () => {
  showResources.value = true
  resourceLoading.value = true
  try {
    const res = await getLessonList(props.courseId)
    const lessons = res.data.code === 200 ? (res.data.data || []) : []
    resources.value = lessons.filter(l => l.resourceType === 'video').slice(0, 4)
  } catch (err) {
    console.error('RestSiteRoom: getLessonList error', err)
    resources.value = []
  } finally {
    resourceLoading.value = false
  }
}

const resourceTypeLabel = resource => {
  const type = String(resource.resourceType || resource.resource_type || resource.type || '').toLowerCase()
  if (type.includes('video')) return '视频'
  if (type.includes('ppt')) return '课件'
  if (type.includes('doc') || type.includes('pdf')) return '文档'
  return '课时资源'
}

const playResource = resource => {
  playingResource.value = resource
}

const aiInitialQuestion = ref('')

const reviewWeakPoint = async () => {
  if (reviewStarted.value) {
    ElMessage.success('薄弱点复习完成')
    complete('weak_point_review')
    return
  }
  const targetId = weakPoint.value?.knowledgePointId ||
    weakPoint.value?.knowledge_point_id ||
    props.selectedNode?.kpId ||
    props.selectedNode?.knowledgePointId
  if (!targetId) {
    ElMessage.success('薄弱点复习完成')
    complete('weak_point_review')
    return
  }
  // 加载最近的错题，自动发送给 AI 导师
  aiInitialQuestion.value = ''
  if (weakCards.value.length) {
    const questions = weakCards.value.slice(0, 3).map((card, i) =>
      `${i + 1}. ${card.questionStem || card.stem || card.title || ''}`
    ).filter(Boolean).join('\n')
    if (questions) {
      aiInitialQuestion.value = `我最近做错了以下几道题，请帮我逐一分析错误原因并给出正确的解题思路：\n${questions}`
    }
  }
  if (!aiInitialQuestion.value) {
    aiInitialQuestion.value = `请帮我复习「${weakPointTitle.value}」这个知识点，讲解核心概念和常见易错点。`
  }
  aiKnowledgePointId.value = targetId
  reviewStarted.value = true
  aiVisible.value = true
}

watch(() => [props.modelValue, props.studentId, props.selectedNode?.kpId], () => {
  if (props.modelValue) {
    reviewStarted.value = false
    showResources.value = false
    aiInitialQuestion.value = ''
  }
  loadWeakCards()
}, { immediate: true })
</script>

<style scoped>
:deep(.rest-room-dialog) {
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(232, 184, 92, .35);
  border-radius: 8px;
  background: #17100f;
  box-shadow: 0 28px 70px rgba(0, 0, 0, .52);
}

:deep(.rest-room-dialog .el-dialog__header) {
  display: none;
}

:deep(.rest-room-dialog .el-dialog__body) {
  padding: 0;
  color: #f8edcf;
}

.rest-scene {
  min-height: 560px;
  background-position: center;
  background-size: cover;
}

.rest-scrim {
  min-height: inherit;
  padding: 28px;
  background:
    radial-gradient(circle at 50% 38%, rgba(241, 190, 92, .12), transparent 32%),
    linear-gradient(90deg, rgba(8, 10, 14, .68), rgba(8, 10, 14, .18), rgba(8, 10, 14, .74));
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

.back-link {
  display: inline-block;
  margin-bottom: 16px;
  padding: 0;
  border: none;
  color: #dfa54f;
  background: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}

.back-link:hover {
  color: #f2dba9;
}

.loading-panel {
  padding: 20px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.resource-card {
  min-height: 100px;
  padding: 18px;
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 8px;
  background: rgba(12, 12, 16, .70);
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease;
}

.resource-card:hover {
  transform: translateY(-2px);
  border-color: rgba(237, 185, 90, .75);
}

.resource-card small {
  display: block;
  margin-bottom: 8px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.resource-card strong {
  display: block;
  color: #f8edcf;
  font-size: 16px;
  line-height: 1.35;
}

.resource-card span {
  display: block;
  margin-top: 6px;
  color: #e7d1a2;
  font-size: 13px;
}

.empty-hint {
  grid-column: 1 / -1;
  padding: 30px;
  text-align: center;
  color: #e7d1a2;
}

.video-player-panel {
  padding: 10px 0;
}

.rest-layout {
  display: grid;
  grid-template-columns: minmax(260px, .9fr) minmax(0, 1.1fr);
  gap: 18px;
  max-width: 820px;
}

.status-board,
.choice-card {
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 8px;
  background: rgba(12, 12, 16, .70);
}

.status-board {
  padding: 18px;
}

.status-board h3 {
  margin: 0 0 16px;
  font-size: 22px;
}

dl {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 0 0 18px;
}

dl div {
  padding: 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .06);
}

dt {
  color: #d9bf8f;
  font-size: 12px;
}

dd {
  margin: 6px 0 0;
  color: #ffe2a3;
  font-size: 22px;
  font-weight: 800;
}

.weak-panel {
  padding: 14px;
  border-radius: 8px;
  background: rgba(237, 185, 90, .12);
}

.weak-panel strong,
.weak-panel span {
  display: block;
}

.weak-panel strong {
  color: #f8edcf;
  line-height: 1.4;
}

.weak-panel span {
  margin-top: 8px;
  color: #e7d1a2;
  line-height: 1.5;
}

.choice-grid {
  display: grid;
  gap: 14px;
}

.choice-card {
  min-height: 132px;
  padding: 18px;
  color: #f8edcf;
  text-align: left;
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
  font-size: 18px;
  line-height: 1.35;
}

.choice-card span {
  margin-top: 10px;
  color: #e7d1a2;
  line-height: 1.5;
}

@media (max-width: 820px) {
  .rest-layout,
  dl,
  .resource-grid {
    grid-template-columns: 1fr;
  }
}
</style>
