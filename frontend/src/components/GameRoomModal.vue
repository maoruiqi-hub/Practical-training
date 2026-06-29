<template>
  <el-dialog
    v-model="visible"
    width="760px"
    class="run-room-dialog"
    append-to-body
    :show-close="true"
  >
    <template #header>
      <div class="room-head">
        <span class="room-symbol" :class="roomType" aria-hidden="true"></span>
        <div>
          <p class="kicker">{{ roomCopy.kicker }}</p>
          <h2>{{ roomCopy.title }}</h2>
        </div>
      </div>
    </template>

    <section class="room-body" :class="roomType">
      <div v-if="roomType === 'rest' || roomType === 'start'" class="room-grid">
        <article class="room-card">
          <p class="kicker">Profile</p>
          <h3>{{ user.name || user.username || '学习者' }}</h3>
          <div class="stat-stack">
            <span>HP <b>{{ profile.hp ?? 100 }}</b></span>
            <span>ATK <b>{{ profile.atk ?? 50 }}</b></span>
            <span>DEF <b>{{ profile.def ?? 50 }}</b></span>
          </div>
        </article>
        <article class="room-card">
          <p class="kicker">Campfire</p>
          <h3>休整选择</h3>
          <p>这里对应原来的学生画像与学习建议。进入休息点时再查看状态、恢复生命或整理牌组。</p>
          <el-button class="spire-button" @click="$emit('open-supply')">打开补给</el-button>
        </article>
      </div>

      <div v-else-if="roomType === 'treasure'" class="reward-grid">
        <button
          v-for="course in courseCards"
          :key="course.courseCode || course.code || course.courseName"
          type="button"
          class="reward-card"
          @click="chooseCourse(course)"
        >
          <span>Knowledge Card</span>
          <strong>{{ course.courseName || course.name || course.title || courseName }}</strong>
          <small>{{ course.teacher || course.teacherName || '课程资源' }}</small>
        </button>
      </div>

      <div v-else-if="roomType === 'shop'" class="room-grid">
        <article class="room-card full">
          <p class="kicker">Shop</p>
          <h3>错题商店</h3>
          <p>错题本在这里变成“待重铸的卡牌”。你只在需要强化牌组时进入，不再作为常驻菜单。</p>
          <ol v-if="mistakes.length" class="compact-list">
            <li v-for="(item, index) in mistakes.slice(0, 5)" :key="item.questionId || index">
              <span>{{ index + 1 }}</span>
              <strong>{{ item.questionStem || item.stem || item.knowledgePointName || '错题卡' }}</strong>
            </li>
          </ol>
          <el-empty v-else description="暂无错题卡" :image-size="82" />
        </article>
      </div>

      <div v-else class="room-grid">
        <article class="room-card">
          <p class="kicker">Event</p>
          <h3>路径事件</h3>
          <p>学习进度、成绩统计和学情分析统一变成路线事件。系统会在合适节点给出状态、风险和下一步建议。</p>
        </article>
        <article class="room-card">
          <p class="kicker">Progress</p>
          <h3>本轮推进</h3>
          <div class="stat-stack">
            <span>课程 <b>{{ courseName }}</b></span>
            <span>已通关 <b>{{ runStats.cleared || 0 }}</b></span>
            <span>待强化 <b>{{ runStats.weak || 0 }}</b></span>
          </div>
        </article>
      </div>
    </section>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
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

const emit = defineEmits(['update:modelValue', 'open-supply', 'course-picked'])
const courseCards = ref([])
const mistakes = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const roomCopy = computed(() => ({
  start: { kicker: 'Neow Choice', title: '开局准备' },
  rest: { kicker: 'Rest Site', title: '休息点' },
  treasure: { kicker: 'Treasure', title: '知识卡奖励' },
  shop: { kicker: 'Shop', title: '错题商店' },
  progress: { kicker: 'Run Log', title: '路线事件' },
  event: { kicker: 'Unknown Event', title: '学习事件' }
}[props.roomType] || { kicker: 'Room', title: '学习房间' }))

const loadRoomData = async () => {
  if (!visible.value) return
  if (props.roomType === 'treasure') {
    try {
      const res = await getCourseList()
      courseCards.value = res.data.code === 200 ? (res.data.data || []) : []
    } catch {
      courseCards.value = []
    }
  }
  if (props.roomType === 'shop' && props.studentId) {
    try {
      const res = await getStudentWrongQuestions(props.studentId)
      mistakes.value = res.data.code === 200 ? (res.data.data || []) : []
    } catch {
      mistakes.value = []
    }
  }
}

const chooseCourse = course => {
  emit('course-picked', course)
  visible.value = false
}

watch(() => [props.modelValue, props.roomType], loadRoomData, { immediate: true })
</script>

<style scoped>
:deep(.run-room-dialog) {
  border: 1px solid rgba(232, 184, 92, .35);
  border-radius: 8px;
  background: linear-gradient(180deg, #3a2018, #15100f);
  box-shadow: 0 28px 70px rgba(0, 0, 0, .52);
}

:deep(.run-room-dialog .el-dialog__header),
:deep(.run-room-dialog .el-dialog__body) {
  color: #f8edcf;
}

.room-head {
  display: grid;
  grid-template-columns: 54px 1fr;
  gap: 14px;
  align-items: center;
}

.room-symbol {
  width: 48px;
  height: 48px;
  border: 2px solid #e1a64f;
  border-radius: 50%;
  background: linear-gradient(180deg, #5c2b1d, #1a1210);
}

.room-symbol.treasure {
  border-radius: 8px;
}

.room-symbol.shop {
  clip-path: polygon(50% 0, 100% 40%, 82% 100%, 18% 100%, 0 40%);
}

.room-symbol.rest {
  border-radius: 8px 8px 24px 24px;
}

.kicker {
  margin: 0 0 5px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h2,
h3,
p {
  margin: 0;
}

h2,
h3 {
  color: #fff6dc;
}

.room-body {
  min-height: 260px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.room-card,
.reward-card {
  min-height: 210px;
  padding: 18px;
  border: 1px solid rgba(232, 184, 92, .24);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 246, 220, .08), rgba(0, 0, 0, .16));
}

.room-card.full {
  grid-column: 1 / -1;
}

.room-card p {
  margin-top: 10px;
  color: #d9c7a8;
  line-height: 1.7;
}

.stat-stack {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.stat-stack span {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .07);
  color: #d9c7a8;
}

.stat-stack b {
  color: #fff6dc;
}

.reward-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.reward-card {
  display: grid;
  align-content: space-between;
  color: #f8edcf;
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease-out, border-color .18s ease-out;
}

.reward-card:hover {
  transform: translateY(-4px);
  border-color: #e1a64f;
}

.reward-card span,
.reward-card small {
  color: #c9ad7e;
}

.reward-card strong {
  color: #fff6dc;
  font-size: 20px;
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
  background: rgba(255, 255, 255, .07);
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

.spire-button {
  min-height: 44px;
  margin-top: 16px;
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #74311f);
  border-color: #d49b51;
  border-radius: 6px;
}

@media (max-width: 760px) {
  .room-grid,
  .reward-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reward-card {
    transition: none;
  }
  .reward-card:hover {
    transform: none;
  }
}
</style>
