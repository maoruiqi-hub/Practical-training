<template>
  <div class="data-center" :style="pageStyle" v-loading="loading">
    <el-alert v-if="!studentNo || !courseId" title="缺少学习上下文" description="请先登录并选择课程后再查看数据统计。" type="warning" show-icon :closable="false" />
    <el-alert v-else-if="loadError" title="统计加载失败" :description="loadError" type="error" show-icon :closable="false" />
    <div class="data-shell">
      <header class="data-header">
        <div>
          <el-button class="back-button" @click="$router.back()">← 返回登塔</el-button>
          <p class="kicker">成长档案 · 最近战况</p>
          <h1>数据统计中心</h1>
          <p>查看最近学习表现，确认下一段路线应该强化什么能力。</p>
        </div>
        <div class="profile-orb">{{ levelLabel }}</div>
      </header>

      <section class="stat-cards">
        <article><span>最近提交</span><strong>{{ stats.totalSubmissions || 0 }}</strong><small>次</small></article>
        <article><span>平均得分</span><strong>{{ stats.averageScore || 0 }}</strong><small>分</small></article>
        <article><span>完成任务</span><strong>{{ stats.completedCount || 0 }}</strong><small>项</small></article>
        <article><span>学习时长</span><strong>{{ formatDuration(stats.totalStudyDuration) }}</strong><small>累计</small></article>
      </section>

      <main class="data-grid">
        <section class="panel recent-panel">
          <div class="panel-heading">
            <div><p class="kicker">RECENT RECORDS</p><h2>最近统计</h2></div>
            <span class="panel-mark">✦</span>
          </div>
          <div v-if="recentDetails.length" class="recent-list">
            <article v-for="item in recentDetails" :key="item.taskNo || item.submitTime || item.taskName" class="recent-item">
              <div class="recent-icon">{{ item.score == null ? '·' : item.score >= 80 ? '✓' : '!' }}</div>
              <div class="recent-copy">
                <strong>{{ item.taskName || item.taskType || '学习任务' }}</strong>
                <span>{{ item.taskType || '任务' }} · {{ item.submitTime || '最近完成' }}</span>
              </div>
              <b :class="scoreClass(item.score)">{{ item.score == null ? '待批改' : `${item.score}分` }}</b>
            </article>
          </div>
          <el-empty v-else description="暂无最近记录" :image-size="70" />
        </section>

        <section class="panel profile-panel">
          <div class="panel-heading">
            <div><p class="kicker">LEARNING STATUS</p><h2>学习状态</h2></div>
            <span class="level-badge">Lv.{{ profile.level || 1 }}</span>
          </div>
          <div class="attribute-list">
            <div v-for="item in attributes" :key="item.key">
              <span>{{ item.label }}</span><b>{{ item.value }}%</b>
              <i><em :style="{ width: `${item.value}%`, background: item.color }"></em></i>
            </div>
          </div>
          <p class="profile-hint">这里展示游戏化学习状态，不参与课程能力映射计算。</p>
        </section>
      </main>

      <section class="panel ability-panel">
        <div class="panel-heading">
          <div><p class="kicker">ABILITY COMPARISON</p><h2>课程能力对照</h2></div>
          <span class="panel-caption">当前课程 · {{ courseName }}</span>
        </div>
        <StudentAbilityMapPanel :student-no="studentNo" :course-code="courseId" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getStudentCourseStats } from '../api'
import { getProfileSummary } from '../api/profile'
import StudentAbilityMapPanel from '../components/StudentAbilityMapPanel.vue'
import { gameBackgrounds } from '../data/gameAssetManifest'
import { getCurrentUser, getCourseId, getStudentId } from '../utils/authContext'

const route = useRoute()
const user = getCurrentUser()
const studentNo = getStudentId(user)
const courseId = getCourseId(route)
const courseName = route.query.courseName || localStorage.getItem('courseName') || ''
const loading = ref(true)
const loadError = ref('')
const stats = ref({})
const profile = ref({})

const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .2), rgba(6, 8, 12, .84)), url(${gameBackgrounds.mapAct1})`
}))

const recentDetails = computed(() => (stats.value.details || []).slice(-6).reverse())
const levelLabel = computed(() => `Lv.${profile.value.level || 1}`)
const clamp = value => Math.max(0, Math.min(100, Math.round(Number(value) || 0)))
const attributes = computed(() => [
  { key: 'atk', label: '解题力', value: clamp(profile.value.atk), color: '#e9bd6d' },
  { key: 'def', label: '基础度', value: clamp(profile.value.def), color: '#b98c55' },
  { key: 'hp', label: '学习信心', value: clamp(profile.value.hp), color: '#d36b4f' }
])
const scoreClass = score => Number(score) >= 80 ? 'good' : Number(score) > 0 ? 'warn' : 'pending'
const formatDuration = seconds => {
  const value = Number(seconds) || 0
  if (!value) return '0'
  const minutes = Math.floor(value / 60)
  return minutes >= 60 ? `${Math.floor(minutes / 60)}h` : `${minutes}m`
}

onMounted(async () => {
  if (!studentNo || !courseId) {
    loading.value = false
    return
  }
  try {
    const [statsRes, profileRes] = await Promise.all([
      getStudentCourseStats(studentNo, courseId),
      getProfileSummary(studentNo, courseId)
    ])
    if (statsRes.data.code === 200) stats.value = statsRes.data.data || {}
    if (profileRes.data.code === 200) profile.value = profileRes.data.data?.profile || profileRes.data.data || {}
  } catch {
    loadError.value = '当前课程统计暂时不可用，请稍后重试。'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.data-center { min-height: 100vh; margin: -20px; padding: 30px 28px 54px; color: #f8edcf; background-position: center; background-size: cover; background-attachment: fixed; }
.data-shell { width: min(1180px, 100%); margin: 0 auto; }
.data-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 22px 26px; border-bottom: 1px solid rgba(232, 184, 91, .28); }
.back-button { margin-bottom: 22px; border-color: rgba(238, 181, 91, .36); color: #f8ebcb; background: rgba(255, 255, 255, .08); }
.kicker { margin: 0 0 7px; color: #dfa54f; font-size: 11px; font-weight: 800; letter-spacing: .16em; }
h1, h2 { margin: 0; color: #fff5d6; font-family: Georgia, serif; }
h1 { font-size: clamp(32px, 5vw, 54px); letter-spacing: .08em; }
.data-header p:last-child { margin: 10px 0 0; color: #d6c19d; }
.profile-orb, .level-badge { display: grid; place-items: center; border: 1px solid rgba(232, 184, 91, .5); color: #ffda85; background: rgba(72, 42, 22, .8); box-shadow: 0 0 24px rgba(232, 184, 91, .16); }
.profile-orb { width: 104px; height: 104px; border-radius: 50%; font-family: Georgia, serif; font-size: 22px; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 22px 0; }
.stat-cards article, .panel { border: 1px solid rgba(232, 184, 91, .28); border-radius: 9px; background: linear-gradient(145deg, rgba(55, 29, 21, .9), rgba(9, 11, 15, .9)); box-shadow: 0 16px 38px rgba(0, 0, 0, .3); }
.stat-cards article { padding: 16px 18px; }
.stat-cards span, .stat-cards small { color: #cdb58e; font-size: 12px; }
.stat-cards strong { display: inline-block; margin: 12px 7px 0 0; color: #ffda85; font-family: Georgia, serif; font-size: 28px; }
.data-grid { display: grid; grid-template-columns: 1.2fr .8fr; gap: 18px; }
.panel { padding: 20px; }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.panel-heading h2 { font-size: 24px; }
.panel-mark { color: #ffda85; font-size: 24px; }
.panel-caption { color: #cdb58e; font-size: 12px; }
.recent-list { display: grid; gap: 9px; }
.recent-item { display: flex; align-items: center; gap: 12px; padding: 10px; border: 1px solid rgba(232, 184, 91, .18); border-radius: 7px; background: rgba(232, 184, 91, .08); }
.recent-icon { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 50%; color: #ffda85; background: rgba(232, 184, 91, .18); }
.recent-copy { display: grid; flex: 1; min-width: 0; gap: 4px; }
.recent-copy strong { overflow: hidden; color: #fff1c9; text-overflow: ellipsis; white-space: nowrap; }
.recent-copy span { color: #bda883; font-size: 12px; }
.recent-item b { color: #ffda85; }.recent-item b.good { color: #a7d7a9; }.recent-item b.warn { color: #ffb36b; }.recent-item b.pending { color: #bda883; }
.level-badge { width: 50px; height: 30px; border-radius: 5px; font-size: 12px; }
.attribute-list { display: grid; gap: 18px; margin-top: 26px; }
.attribute-list > div { display: grid; grid-template-columns: 1fr auto; gap: 7px; color: #d8c39f; }
.attribute-list b { color: #ffda85; }.attribute-list i { grid-column: 1 / -1; height: 8px; overflow: hidden; border-radius: 999px; background: rgba(255,255,255,.1); }.attribute-list em { display: block; height: 100%; border-radius: inherit; }
.profile-hint { margin: 24px 0 0; color: #bda883; font-size: 12px; line-height: 1.7; }
.ability-panel { margin-top: 18px; }.ability-panel :deep(.student-ability-map) { color: #f8edcf; }.ability-panel :deep(.el-empty__description) { color: #cdb58e; }
@media (max-width: 760px) { .data-center { margin: -16px; padding: 20px 16px 40px; }.data-header { padding-inline: 8px; }.profile-orb { width: 72px; height: 72px; font-size: 16px; }.data-header p:last-child { max-width: 230px; font-size: 13px; }.stat-cards { grid-template-columns: repeat(2, 1fr); }.data-grid { grid-template-columns: 1fr; } }
</style>
