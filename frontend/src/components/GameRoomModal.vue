<template>
  <el-dialog
    v-model="visible"
    width="860px"
    class="run-room-dialog"
    append-to-body
    :show-close="true"
  >
    <section class="room-scene" :class="roomType" :style="roomStyle">
      <div class="room-scrim">
        <header class="room-head">
          <img class="room-icon" :src="roomIcon" alt="" />
          <div>
            <p class="kicker">{{ roomCopy.kicker }}</p>
            <h2>{{ roomCopy.title }}</h2>
            <p>{{ roomCopy.description }}</p>
          </div>
        </header>

        <div v-if="roomType === 'rest' || roomType === 'start'" class="choice-grid">
          <button type="button" class="choice-card" @click="$emit('open-supply')">
            <strong>整理补给</strong>
            <span>打开补给面板，查看画像建议和恢复选项。</span>
          </button>
          <button type="button" class="choice-card" @click="complete('恢复 20 HP')">
            <strong>休整回血</strong>
            <span>本节点视为完成，返回地图继续推进。</span>
          </button>
          <button type="button" class="choice-card" @click="complete('复习薄弱概念')">
            <strong>复习薄弱点</strong>
            <span>把休息点用于巩固当前知识路线。</span>
          </button>
        </div>

        <div v-else-if="roomType === 'treasure'" class="choice-grid reward">
          <button
            v-for="course in treasureCards"
            :key="course.courseCode || course.code || course.courseName"
            type="button"
            class="choice-card reward-card"
            @click="chooseCourse(course)"
          >
            <small>Knowledge Card</small>
            <strong>{{ course.courseName || course.name || course.title || courseName }}</strong>
            <span>{{ course.teacher || course.teacherName || '课程资源' }}</span>
          </button>
          <button v-if="!treasureCards.length" type="button" class="choice-card reward-card" @click="complete('金币袋')">
            <small>Fallback Reward</small>
            <strong>金币袋</strong>
            <span>获得一份临时资源奖励。</span>
          </button>
        </div>

        <div v-else-if="roomType === 'shop'" class="shop-layout">
          <article class="shop-board">
            <p class="kicker">Wrong Cards</p>
            <h3>错题商店</h3>
            <ol v-if="mistakes.length" class="compact-list">
              <li v-for="(item, index) in mistakes.slice(0, 5)" :key="item.questionId || index">
                <span>{{ index + 1 }}</span>
                <strong>{{ item.questionStem || item.stem || item.knowledgePointName || '错题卡' }}</strong>
              </li>
            </ol>
            <el-empty v-else description="暂无错题卡" :image-size="82" />
          </article>
          <div class="choice-grid shop-actions">
            <button type="button" class="choice-card" @click="complete('购买提示卡')">
              <strong>购买提示卡</strong>
              <span>花费金币换取下一场战斗的提示。</span>
            </button>
            <button type="button" class="choice-card" @click="complete('净化错题卡')">
              <strong>净化错题卡</strong>
              <span>把一次错题转化为复习收益。</span>
            </button>
          </div>
        </div>

        <div v-else class="choice-grid">
          <button type="button" class="choice-card" @click="complete('代码迷雾')">
            <strong>代码迷雾</strong>
            <span>承担一点风险，获得一张临时提示卡。</span>
          </button>
          <button type="button" class="choice-card" @click="complete('复习岔路')">
            <strong>复习岔路</strong>
            <span>立即复习薄弱点，获得少量经验。</span>
          </button>
          <button type="button" class="choice-card" @click="complete('调试祭坛')">
            <strong>调试祭坛</strong>
            <span>处理一个错误概念，但下一场战斗更难。</span>
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
import { getCourseList, getStudentWrongQuestions } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  roomType: { type: String, default: 'event' },
  profile: { type: Object, default: () => ({}) },
  user: { type: Object, default: () => ({}) },
  studentId: { type: [String, Number], default: '' },
  courseName: { type: String, default: 'Python 程序设计' },
  runStats: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'open-supply', 'course-picked', 'room-complete'])
const treasureCards = ref([])
const mistakes = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const roomCopy = computed(() => ({
  start: { kicker: 'Neow Choice', title: '开局准备', description: '确认状态，整理补给，然后进入第一条路线。' },
  rest: { kicker: 'Rest Site', title: '休息点', description: '恢复、复习或整理卡组，只能选择一个主要行动。' },
  treasure: { kicker: 'Treasure', title: '知识宝箱', description: '打开宝箱，选择一份课程资源或奖励卡。' },
  shop: { kicker: 'Shop', title: '错题商店', description: '用本局资源购买提示，或处理错题卡。' },
  progress: { kicker: 'Run Log', title: '路线事件', description: '根据当前学情触发一次路线事件。' },
  event: { kicker: 'Unknown Event', title: '学习事件', description: '做一个有收益和代价的选择。' }
})[props.roomType] || { kicker: 'Room', title: '学习房间', description: '完成房间事件后返回地图。' })

const backgroundForRoom = computed(() => ({
  start: gameBackgrounds.runEntry,
  rest: gameBackgrounds.rest,
  treasure: gameBackgrounds.treasure,
  shop: gameBackgrounds.shop,
  event: gameBackgrounds.diagnosis,
  progress: gameBackgrounds.mapAct1
})[props.roomType] || gameBackgrounds.diagnosis)

const roomStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(8, 10, 14, .16), rgba(8, 10, 14, .82)), url(${backgroundForRoom.value})`
}))

const roomIcon = computed(() => ({
  start: mapLegendIcons.unknown,
  rest: mapLegendIcons.rest,
  treasure: mapLegendIcons.treasure,
  shop: mapLegendIcons.merchant,
  event: mapLegendIcons.unknown,
  progress: mapLegendIcons.unknown
})[props.roomType] || mapLegendIcons.unknown)

const loadRoomData = async () => {
  if (!visible.value) return
  if (props.roomType === 'treasure') {
    try {
      const res = await getCourseList()
      treasureCards.value = res.data.code === 200 ? (res.data.data || []).slice(0, 3) : []
    } catch {
      treasureCards.value = []
    }
  }
  if (props.roomType === 'shop' && props.studentId) {
    try {
      const res = await getStudentWrongQuestions(props.studentId)
      mistakes.value = res.data.code === 200 ? (res.data.data?.wrongList || []) : []
    } catch {
      mistakes.value = []
    }
  }
}

const chooseCourse = course => {
  emit('course-picked', course)
  visible.value = false
}

const complete = rewardName => {
  ElMessage.success(`房间完成：${rewardName}`)
  emit('room-complete', { roomType: props.roomType, rewardName })
  visible.value = false
}

watch(() => [props.modelValue, props.roomType], loadRoomData, { immediate: true })
</script>

<style scoped>
:deep(.run-room-dialog) {
  overflow: hidden;
  padding: 0;
  border: 1px solid rgba(232, 184, 92, .35);
  border-radius: 8px;
  background: #17100f;
  box-shadow: 0 28px 70px rgba(0, 0, 0, .52);
}

:deep(.run-room-dialog .el-dialog__header) {
  display: none;
}

:deep(.run-room-dialog .el-dialog__body) {
  padding: 0;
  color: #f8edcf;
}

.room-scene {
  min-height: 560px;
  background-position: center;
  background-size: cover;
}

.room-scrim {
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

h2,
h3,
p {
  margin: 0;
}

h2 {
  color: #fff6dc;
  font-size: 30px;
}

h3 {
  color: #fff6dc;
  font-size: 22px;
}

.room-head p {
  margin-top: 8px;
  color: #e1cba5;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.choice-card {
  min-height: 180px;
  padding: 18px;
  border: 1px solid rgba(232, 184, 92, .32);
  border-radius: 8px;
  color: #f8edcf;
  text-align: left;
  background: linear-gradient(180deg, rgba(255, 246, 220, .12), rgba(24, 14, 12, .84));
  box-shadow: 0 18px 34px rgba(0, 0, 0, .32);
  cursor: pointer;
  transition: transform .18s ease-out, border-color .18s ease-out, filter .18s ease-out;
}

.choice-card:hover {
  transform: translateY(-5px);
  border-color: #e1a64f;
  filter: brightness(1.08);
}

.choice-card strong {
  display: block;
  color: #fff6dc;
  font-size: 20px;
}

.choice-card small {
  display: block;
  margin-bottom: 10px;
  color: #dfa54f;
  font-weight: 800;
}

.choice-card span {
  display: block;
  margin-top: 12px;
  color: #e1cba5;
  line-height: 1.7;
}

.shop-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, .8fr);
  gap: 14px;
}

.shop-board {
  min-height: 300px;
  padding: 18px;
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 246, 220, .12), rgba(24, 14, 12, .84));
}

.shop-actions {
  grid-template-columns: 1fr;
}

.compact-list {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 16px 0 0;
  list-style: none;
}

.compact-list li {
  display: grid;
  grid-template-columns: 30px 1fr;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .08);
}

.compact-list span {
  color: #dfa54f;
  font-weight: 800;
}

.compact-list strong {
  overflow: hidden;
  color: #fff6dc;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  :deep(.run-room-dialog) {
    width: calc(100% - 24px) !important;
  }
  .room-scrim {
    padding: 18px;
  }
  .choice-grid,
  .shop-layout {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .choice-card {
    transition: none;
  }
  .choice-card:hover {
    transform: none;
  }
}
</style>
