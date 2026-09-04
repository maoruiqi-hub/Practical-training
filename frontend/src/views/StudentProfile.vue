<template>
  <div class="profile-page">
    <el-alert v-if="!studentNo || !courseCode" title="缺少学习上下文" description="请先登录并选择课程后查看个人画像。" type="warning" show-icon :closable="false" style="margin-bottom: 20px" />
    <!-- 画像总览 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>属性面板</span>
            <el-tag style="margin-left:10px" :type="statusType">{{ profile.status || '正常学习' }}</el-tag>
            <el-tag style="margin-left:8px">{{ title }}</el-tag>
          </template>
          <el-row :gutter="16">
            <el-col :span="6" v-for="attr in attributes" :key="attr.key">
              <div style="text-align:center">
                <div style="font-size:28px;font-weight:bold" :style="{color:attr.color}">{{ profile[attr.key] || 0 }}</div>
                <div style="color:#999;font-size:12px">{{ attr.label }}</div>
                <el-progress :percentage="attr.percent(profile)" :color="attr.color" :show-text="false" />
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>成长数据</span></template>
          <div>等级：<el-tag>{{ levelName }}</el-tag></div>
          <div style="margin-top:8px">金币：{{ profile.coins || 0 }}</div>
          <div style="margin-top:8px">称号：{{ title }}</div>
          <div style="margin-top:8px">徽章：{{ badgeCount }} 个</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab区域 -->
    <el-tabs v-model="activeTab" style="margin-top:20px">
      <el-tab-pane label="能力评分" name="competency">
        <el-card>
          <div ref="radarChart" style="width:100%;height:400px"></div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="能力图谱" name="abilityMap">
        <el-card>
          <StudentAbilityMapPanel :student-no="studentNo" :course-code="courseCode" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="个性化推荐" name="recommendations">
        <el-card>
          <div style="margin-bottom:12px">
            <el-button type="primary" @click="refreshRecs" :loading="recLoading">刷新推荐</el-button>
          </div>
          <el-empty v-if="!recommendations.length" description="暂无推荐" />
          <div v-for="rec in recommendations" :key="rec.id"
               style="padding:12px;margin-bottom:8px;background:#f5f7fa;border-radius:6px;display:flex;justify-content:space-between;align-items:center">
            <div>
              <el-tag size="small" :type="recTypeTag(rec.type)">{{ recTypeLabel(rec.type) }}</el-tag>
              <span style="margin-left:8px;font-weight:bold">{{ rec.targetName }}</span>
              <div style="color:#999;font-size:13px;margin-top:4px">{{ rec.reason }}</div>
            </div>
            <div>
              <el-button size="small" @click="feedbackRec(rec.id, 'useful')">有用</el-button>
              <el-button size="small" @click="feedbackRec(rec.id, 'skip')">跳过</el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的成就" name="achievements">
        <el-card>
          <el-empty v-if="!achievements.length" description="暂无成就" />
          <el-row :gutter="12">
            <el-col :span="8" v-for="a in achievements" :key="a.id" style="margin-bottom:12px">
              <div style="text-align:center;padding:16px;background:#fef0f0;border-radius:8px">
                <div style="font-size:36px">{{ badgeIcon(a.name) }}</div>
                <div style="font-weight:bold">{{ a.name }}</div>
                <div style="font-size:12px;color:#999">{{ a.description }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="排行榜" name="leaderboard">
        <el-card>
          <el-radio-group v-model="rankType" @change="loadLeaderboard" style="margin-bottom:12px">
            <el-radio-button value="exp">经验排行</el-radio-button>
            <el-radio-button value="coins">金币排行</el-radio-button>
          </el-radio-group>
          <el-table :data="leaderboard" stripe>
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="studentNo" label="学号" width="100" />
            <el-table-column prop="level" label="等级" width="80" />
            <el-table-column prop="exp" label="经验值" />
            <el-table-column prop="coins" label="金币" />
            <el-table-column prop="badgeCount" label="徽章数" width="80" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="成长历史" name="growthHistory">
        <el-card>
          <el-empty v-if="!growthHistory.length" description="暂无成长记录" />
          <el-timeline v-else>
            <el-timeline-item v-for="(gh, i) in growthHistory" :key="i"
              :timestamp="gh.createdAt ? new Date(gh.createdAt).toLocaleString() : ''"
              :color="gh.amount > 0 ? '#67c23a' : '#f56c6c'">
              {{ gh.type === 'exp' ? '经验' : '金币' }}
              {{ gh.amount > 0 ? '+' : '' }}{{ gh.amount }}
              ({{ sourceLabel(gh.source) }})
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="能力变化" name="competencyHistory">
        <el-card>
          <el-empty v-if="!competencyHistory.length" description="暂无能力变化记录" />
          <div v-else ref="historyChart" style="width:100%;height:400px"></div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import StudentAbilityMapPanel from '../components/StudentAbilityMapPanel.vue'
import { getProfileSummary, getCompetency, getRecommendations, generateRecommendations,
         feedbackRecommendation, getAchievements, getTitle, getLeaderboard,
         getGrowthHistory, getCompetencyHistory } from '@/api/profile'
import { getCurrentUser, getCourseId, getStudentId } from '../utils/authContext'

const user = getCurrentUser()
const studentNo = ref(getStudentId(user))
const courseCode = ref(getCourseId(null))
const activeTab = ref('competency')
const profile = ref({})
const competencyScores = ref([])
const recommendations = ref([])
const achievements = ref([])
const title = ref('')
const leaderboard = ref([])
const rankType = ref('exp')
const recLoading = ref(false)
const growthHistory = ref([])
const competencyHistory = ref([])
const prevProfile = ref({})
const newBadgeAlert = ref(null)
const historyChart = ref(null)

const attributes = [
  { key: 'hp', label: 'HP 信心值', color: '#f56c6c',
    percent: p => (p.hp || 0) },
  { key: 'atk', label: 'ATK 解题力', color: '#e6a23c',
    percent: p => (p.atk || 0) },
  { key: 'def', label: 'DEF 基础度', color: '#409eff',
    percent: p => (p.def || 0) },
  { key: 'exp', label: 'EXP 经验值', color: '#67c23a',
    percent: p => Math.min(100, (p.exp || 0) / 20) }
]

const statusType = computed(() => {
  const s = profile.value.status
  if (s === '存在风险') return 'danger'
  if (s === '进度滞后') return 'warning'
  if (s === '能力提升') return 'success'
  return ''
})

const levelName = computed(() => {
  const lv = profile.value.level || 1
  const names = { 1: '入门', 2: '初级', 3: '中级', 4: '熟练', 5: '精通' }
  return names[lv] || '入门'
})

const badgeCount = computed(() =>
  achievements.value.filter(a => a.achievementType === 'badge').length
)

const recTypeTag = (type) => {
  const map = { review_material: 'danger', practice: 'warning',
                extended_material: 'success', knowledge_point: '' }
  return map[type] || ''
}
const recTypeLabel = (type) => {
  const map = { review_material: '复习', practice: '练习',
                extended_material: '拓展', knowledge_point: '学习' }
  return map[type] || type
}

const badgeIcon = (name) => {
  const icons = { '连击王': '🔥', '完美主义': '💎', '速通者': '🏃',
                  'Pythonic': '🐍', 'Debug之眼': '🔍', '夜枭': '🦉', '助人者': '🤝' }
  return icons[name] || '🏆'
}

const sourceLabel = (src) => {
  const map = { quiz: '测验', boss: '综合测验', default: '答题', task_complete: '任务完成', exam_pass: '考试通过', streak: '连续学习' }
  return map[src] || src
}

const loadProfile = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getProfileSummary(studentNo.value, courseCode.value)
    if (data.code === 200) {
      const old = profile.value
      profile.value = data.data.profile
      competencyScores.value = data.data.competencyScores || []
      // R7.2: detect attribute changes for animation
      if (old && old.hp !== undefined && profile.value.hp !== old.hp) {
        ElMessage({ message: `HP ${profile.value.hp > old.hp ? '+' + (profile.value.hp - old.hp) : profile.value.hp - old.hp}`, type: profile.value.hp > old.hp ? 'success' : 'warning', duration: 2000 })
      }
      await nextTick()
      if (activeTab.value === 'competency') renderRadar()
      if (activeTab.value === 'competencyHistory') renderHistoryChart()
    }
  } catch (e) { /* ignore */ }
}

const loadRecs = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getRecommendations(studentNo.value, courseCode.value)
    if (data.code === 200) recommendations.value = data.data
  } catch (e) { /* ignore */ }
}

const loadAchievements = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const oldCount = achievements.value.length
    const { data } = await getAchievements(studentNo.value, courseCode.value)
    if (data.code === 200) {
      achievements.value = data.data
      // R6.4 & R7.3: badge notification
      if (oldCount > 0 && data.data.length > oldCount) {
        const newBadges = data.data.slice(0, data.data.length - oldCount)
        newBadges.forEach(b => {
          ElMessage({ message: `获得新徽章: ${badgeIcon(b.name)} ${b.name}`, type: 'success', duration: 4000 })
        })
      }
    }
  } catch (e) { /* ignore */ }
}

const loadTitle = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getTitle(studentNo.value, courseCode.value)
    if (data.code === 200) title.value = data.data
  } catch (e) { /* ignore */ }
}

const loadLeaderboard = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getLeaderboard(courseCode.value, rankType.value)
    if (data.code === 200) leaderboard.value = data.data
  } catch (e) { /* ignore */ }
}

const loadGrowthHistory = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getGrowthHistory(studentNo.value, courseCode.value)
    if (data.code === 200) growthHistory.value = data.data
  } catch (e) { /* ignore */ }
}

const loadCompetencyHistory = async () => {
  if (!studentNo.value || !courseCode.value) return
  try {
    const { data } = await getCompetencyHistory(studentNo.value, courseCode.value, null)
    if (data.code === 200) {
      competencyHistory.value = data.data
      await nextTick()
      if (activeTab.value === 'competencyHistory') renderHistoryChart()
    }
  } catch (e) { /* ignore */ }
}

const renderHistoryChart = () => {
  if (!historyChart.value || !competencyHistory.value.length) return
  const chart = echarts.init(historyChart.value)
  const byPoint = {}
  competencyHistory.value.forEach(h => {
    if (!byPoint[h.abilityPointId]) byPoint[h.abilityPointId] = []
    byPoint[h.abilityPointId].push(h)
  })
  const series = Object.entries(byPoint).map(([apid, items]) => ({
    name: apid,
    type: 'line',
    data: items.reverse().map(h => [h.changedAt, h.newScore]),
    smooth: true
  }))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(byPoint), type: 'scroll', bottom: 0 },
    xAxis: { type: 'time' },
    yAxis: { min: 0, max: 100 },
    series
  })
  window.addEventListener('resize', () => chart.resize())
}

const refreshRecs = async () => {
  recLoading.value = true
  try {
    const { data } = await generateRecommendations(studentNo.value, courseCode.value)
    if (data.code === 200) {
      recommendations.value = data.data
      ElMessage.success('推荐已刷新')
    }
  } catch (e) { /* ignore */ }
  recLoading.value = false
}

const feedbackRec = async (id, feedback) => {
  try {
    await feedbackRecommendation(id, feedback)
    ElMessage.success(feedback === 'useful' ? '感谢反馈！' : '已跳过')
    loadRecs()
  } catch (e) { /* ignore */ }
}

const radarChart = ref(null)
const renderRadar = () => {
  if (!radarChart.value || !competencyScores.value.length) return
  const chart = echarts.init(radarChart.value)
  chart.setOption({
    radar: {
      indicator: competencyScores.value.map(c => ({ name: c.abilityPointName, max: 100 }))
    },
    series: [{
      type: 'radar',
      data: [{ value: competencyScores.value.map(c => c.score), name: '能力评分' }],
      areaStyle: { color: 'rgba(64,158,255,0.2)' },
      lineStyle: { color: '#409eff' },
      itemStyle: { color: '#409eff' }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

watch(activeTab, (tab) => {
  if (tab === 'competency') nextTick(() => renderRadar())
  if (tab === 'leaderboard') loadLeaderboard()
  if (tab === 'growthHistory') loadGrowthHistory()
  if (tab === 'competencyHistory') { loadCompetencyHistory(); nextTick(() => renderHistoryChart()) }
})

onMounted(() => {
  loadProfile()
  loadRecs()
  loadAchievements()
  loadTitle()
})
</script>

<style scoped>
.profile-page { padding: 20px; }
</style>
