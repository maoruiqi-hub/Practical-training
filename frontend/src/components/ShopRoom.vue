<template>
  <el-dialog
    v-model="visible"
    width="900px"
    class="shop-room-dialog"
    append-to-body
    :show-close="true"
  >
    <section class="shop-scene" :style="roomStyle">
      <div class="shop-scrim">
        <header class="room-head">
          <img class="room-icon" :src="mapLegendIcons.merchant" alt="" />
          <div>
            <p class="kicker">Shop</p>
            <h2>错题商店</h2>
            <p>用本局金币换取下一段路线的提示，或把错题转化为防御经验。</p>
          </div>
        </header>

        <div class="shop-layout">
          <article class="shop-board">
            <div class="board-head">
              <div>
                <p class="kicker">Wrong Cards</p>
                <h3>待处理错题</h3>
              </div>
              <span class="coin-chip">金币 {{ coins }}</span>
            </div>

            <div v-if="loading" class="loading-box">
              <el-skeleton :rows="5" animated />
            </div>

            <ol v-else-if="mistakes.length" class="compact-list">
              <li v-for="(item, index) in mistakes.slice(0, 5)" :key="item.questionId || index">
                <span>{{ index + 1 }}</span>
                <strong>{{ item.questionStem || item.stem || item.knowledgePointName || '错题卡' }}</strong>
              </li>
            </ol>

            <el-empty v-else description="暂无错题卡" :image-size="82" />
          </article>

          <div class="choice-grid shop-actions">
            <button type="button" class="choice-card" :disabled="coins < 10" @click="buyHint">
              <small>Cost 10</small>
              <strong>购买提示卡</strong>
              <span>后端记录 shop_purchased，换取下一场战斗的攻击准备。</span>
            </button>
            <button type="button" class="choice-card" :disabled="coins < 8 || !mistakes.length" @click="cleanseWrongCard">
              <small>Cost 8</small>
              <strong>净化错题卡</strong>
              <span>选择一张错题作为来源，转化为防御和经验收益。</span>
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
  courseId: { type: [String, Number], default: '' },
  profile: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'room-complete'])

const loading = ref(false)
const mistakes = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const coins = computed(() => Number(props.profile?.coins || 0))
const roomStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(8, 10, 14, .16), rgba(8, 10, 14, .84)), url(${gameBackgrounds.shop})`
}))

const loadMistakes = async () => {
  if (!visible.value || !props.studentId) return
  loading.value = true
  try {
    const res = await getStudentWrongQuestions(props.studentId, {
      courseCode: props.courseId || undefined
    })
    mistakes.value = res.data.code === 200 ? (res.data.data?.wrongList || []) : []
  } catch {
    mistakes.value = []
    ElMessage.warning('错题卡加载失败')
  } finally {
    loading.value = false
  }
}

const complete = rewardName => {
  emit('room-complete', { roomType: 'shop', rewardName })
  visible.value = false
}

const buyHint = () => {
  if (coins.value < 10) {
    ElMessage.warning('金币不足')
    return
  }
  ElMessage.success('提示卡已购买')
  complete('hint_card')
}

const cleanseWrongCard = () => {
  if (!mistakes.value.length) {
    ElMessage.warning('暂无可净化错题')
    return
  }
  if (coins.value < 8) {
    ElMessage.warning('金币不足')
    return
  }
  ElMessage.success('错题卡已净化为复习收益')
  complete('clean_wrong_card')
}

watch(() => [props.modelValue, props.studentId, props.courseId], loadMistakes, { immediate: true })
</script>

<style scoped>
:deep(.shop-room-dialog) {
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(232, 184, 92, .35);
  border-radius: 8px;
  background: #17100f;
  box-shadow: 0 28px 70px rgba(0, 0, 0, .52);
}

:deep(.shop-room-dialog .el-dialog__header) {
  display: none;
}

:deep(.shop-room-dialog .el-dialog__body) {
  padding: 0;
  color: #f8edcf;
}

.shop-scene {
  min-height: 560px;
  background-position: center;
  background-size: cover;
}

.shop-scrim {
  min-height: inherit;
  padding: 28px;
  background:
    radial-gradient(circle at 50% 38%, rgba(241, 190, 92, .12), transparent 32%),
    linear-gradient(90deg, rgba(8, 10, 14, .70), rgba(8, 10, 14, .22), rgba(8, 10, 14, .76));
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

.shop-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, .9fr);
  gap: 18px;
  max-width: 820px;
}

.shop-board,
.choice-card {
  border: 1px solid rgba(255, 235, 194, .16);
  border-radius: 8px;
  background: rgba(12, 12, 16, .70);
}

.shop-board {
  min-height: 312px;
  padding: 18px;
}

.board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.board-head h3 {
  margin: 0;
  font-size: 22px;
}

.coin-chip {
  flex: 0 0 auto;
  padding: 6px 10px;
  border: 1px solid rgba(237, 185, 90, .55);
  border-radius: 999px;
  color: #ffe2a3;
  background: rgba(237, 185, 90, .12);
}

.loading-box {
  padding-top: 10px;
}

.compact-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.compact-list li {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .06);
}

.compact-list span {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #19130d;
  font-weight: 800;
  background: #dfa54f;
}

.compact-list strong {
  color: #f8edcf;
  line-height: 1.45;
}

.choice-grid {
  display: grid;
  gap: 14px;
}

.choice-card {
  min-height: 148px;
  padding: 18px;
  color: #f8edcf;
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, background .18s ease;
}

.choice-card:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(237, 185, 90, .75);
  background: rgba(32, 22, 17, .86);
}

.choice-card:disabled {
  opacity: .52;
  cursor: not-allowed;
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
  margin-top: 12px;
  color: #e7d1a2;
  line-height: 1.5;
}

@media (max-width: 820px) {
  .shop-layout {
    grid-template-columns: 1fr;
  }
}
</style>
