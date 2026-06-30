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
            <p>选择一次主要行动：恢复状态、整理补给，或按错题线索复习。</p>
          </div>
        </header>

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
            <button type="button" class="choice-card" @click="takeRest">
              <small>Rest</small>
              <strong>休整回血</strong>
              <span>后端记录 rest_taken，恢复 HP 并补一点防御和精力。</span>
            </button>
            <button type="button" class="choice-card" @click="$emit('open-supply')">
              <small>Supply</small>
              <strong>整理补给</strong>
              <span>打开补给面板，使用已有 supply_used 能力恢复状态。</span>
            </button>
            <button type="button" class="choice-card" @click="reviewWeakPoint">
              <small>Review</small>
              <strong>复习薄弱点</strong>
              <span>按错题线索完成休息点，把复习收益交给后端结算。</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gameBackgrounds, mapLegendIcons } from '../data/gameAssetManifest'
import { getStudentWrongQuestions } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  studentId: { type: [String, Number], default: '' },
  profile: { type: Object, default: () => ({}) },
  selectedNode: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'open-supply', 'room-complete'])

const loading = ref(false)
const weakCards = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

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

const takeRest = () => {
  ElMessage.success('休整完成')
  complete('rest_recover')
}

const reviewWeakPoint = () => {
  ElMessage.success('薄弱点复习完成')
  complete('weak_point_review')
}

watch(() => [props.modelValue, props.studentId, props.selectedNode?.kpId], loadWeakCards, { immediate: true })
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
  dl {
    grid-template-columns: 1fr;
  }
}
</style>
