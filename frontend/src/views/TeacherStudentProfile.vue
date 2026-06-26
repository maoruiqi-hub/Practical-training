<template>
  <div class="teacher-profile-page">
    <!-- 顶部：课程选择 -->
    <div class="toolbar">
      <span style="font-size:16px;font-weight:bold">学生画像与个性化学习</span>
      <el-select v-model="selectedCourse" placeholder="选择课程" @change="onCourseChange" style="width:300px;margin-left:16px">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="c.courseCode" />
      </el-select>
    </div>

    <!-- 学生画像列表 -->
    <el-card v-if="selectedCourse" style="margin-top:16px" v-loading="loading">
      <template #header>
        <span>学生画像总览（{{ students.length }}人）</span>
        <el-tag style="margin-left:8px" size="small">按经验值排序</el-tag>
        <el-input v-model="searchKey" placeholder="搜索学生姓名/学号" style="width:220px;margin-left:16px;float:right" clearable />
      </template>
      <el-empty v-if="!filteredStudents.length && !loading" description="暂无学生数据" />
      <el-table v-else :data="filteredStudents" stripe highlight-current-row @row-click="selectStudent" style="cursor:pointer">
        <el-table-column prop="studentNo" label="学号" width="90" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '未激活' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTagType(row.level)">{{ levelNames[row.level] || '入门' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="HP" width="65">
          <template #default="{ row }">
            <span :style="{color: row.hp < 30 ? '#f56c6c' : row.hp < 60 ? '#e6a23c' : '#67c23a', fontWeight:'bold'}">{{ row.hp }}</span>
          </template>
        </el-table-column>
        <el-table-column label="ATK" width="65">
          <template #default="{ row }">
            <el-progress :percentage="row.atk || 0" :stroke-width="6" :show-text="false" :color="'#e6a23c'" />
          </template>
        </el-table-column>
        <el-table-column label="DEF" width="65">
          <template #default="{ row }">
            <el-progress :percentage="row.def || 0" :stroke-width="6" :show-text="false" :color="'#409eff'" />
          </template>
        </el-table-column>
        <el-table-column prop="exp" label="EXP" width="70" />
        <el-table-column prop="coins" label="金币" width="70" />
        <el-table-column label="徽章" width="60">
          <template #default="{ row }">{{ row.badgeCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="最近活动" width="110">
          <template #default="{ row }">
            <span style="font-size:12px;color:#999">{{ row.lastActivityDate ? new Date(row.lastActivityDate).toLocaleDateString() : '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="!selectedCourse" description="请先选择课程查看学生画像" style="margin-top:60px" />

    <!-- 学生画像详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="900px" top="3vh" @opened="onDetailOpened">
      <template v-if="detailStudent">
        <!-- 属性面板 -->
        <el-row :gutter="16">
          <el-col :span="16">
            <el-card shadow="never">
              <template #header>
                <span>属性面板</span>
                <el-tag style="margin-left:8px" :type="detailStatusType">{{ detailProfile.status || '正常学习' }}</el-tag>
                <el-tag style="margin-left:8px">{{ detailTitleText }}</el-tag>
              </template>
              <el-row :gutter="12">
                <el-col :span="6" v-for="attr in detailAttrs" :key="attr.key">
                  <div style="text-align:center">
                    <div style="font-size:24px;font-weight:bold" :style="{color:attr.color}">{{ detailProfile[attr.key] || 0 }}</div>
                    <div style="color:#999;font-size:12px">{{ attr.label }}</div>
                    <el-progress :percentage="attr.percent(detailProfile)" :color="attr.color" :show-text="false" :stroke-width="5" />
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never">
              <template #header><span>成长数据</span></template>
              <div>等级：<el-tag :type="levelTagType(detailProfile.level)">{{ levelNames[detailProfile.level] || '入门' }}</el-tag></div>
              <div style="margin-top:6px">金币：{{ detailProfile.coins || 0 }}</div>
              <div style="margin-top:6px">称号：{{ detailTitleText }}</div>
              <div style="margin-top:6px">徽章：{{ detailBadgeCount }} 个</div>
              <div style="margin-top:6px;font-size:12px;color:#999">最近活动：{{ detailProfile.lastActivityDate ? new Date(detailProfile.lastActivityDate).toLocaleString() : '-' }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 详情Tab -->
        <el-tabs v-model="detailTab" style="margin-top:12px" @tab-change="onDetailTabChange">
          <el-tab-pane label="能力评分" name="competency">
            <div ref="detailRadar" style="width:100%;height:350px"></div>
          </el-tab-pane>
          <el-tab-pane label="个性化推荐" name="recommendations">
            <el-empty v-if="!detailRecs.length" description="暂无推荐" />
            <div v-for="rec in detailRecs" :key="rec.id"
                 style="padding:10px;margin-bottom:6px;background:#f5f7fa;border-radius:6px;display:flex;justify-content:space-between;align-items:center">
              <div>
                <el-tag size="small" :type="recTypeTag(rec.type)">{{ recTypeLabel(rec.type) }}</el-tag>
                <span style="margin-left:6px;font-weight:bold">{{ rec.targetName }}</span>
                <div style="color:#999;font-size:12px;margin-top:2px">{{ rec.reason }}</div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="成就" name="achievements">
            <el-empty v-if="!detailAchievements.length" description="暂无成就" />
            <el-row :gutter="10">
              <el-col :span="8" v-for="a in detailAchievements" :key="a.id" style="margin-bottom:10px">
                <div style="text-align:center;padding:12px;background:#fef0f0;border-radius:8px">
                  <div style="font-size:32px">{{ badgeIcon(a.name) }}</div>
                  <div style="font-weight:bold">{{ a.name }}</div>
                  <div style="font-size:11px;color:#999">{{ a.description }}</div>
                </div>
              </el-col>
            </el-row>
          </el-tab-pane>
          <el-tab-pane label="成长历史" name="growthHistory">
            <el-empty v-if="!detailGrowthHistory.length" description="暂无记录" />
            <el-timeline v-else>
              <el-timeline-item v-for="(g, i) in detailGrowthHistory" :key="i"
                :timestamp="g.createdAt ? new Date(g.createdAt).toLocaleString() : ''"
                :color="g.amount > 0 ? '#67c23a' : '#f56c6c'">
                {{ g.type === 'exp' ? '经验' : '金币' }} {{ g.amount > 0 ? '+' : '' }}{{ g.amount }}
                ({{ sourceLabel(g.source) }})
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
          <el-tab-pane label="能力变化" name="competencyHistory">
            <el-empty v-if="!detailCompHistory.length" description="暂无变化记录" />
            <div v-else ref="detailHistoryChart" style="width:100%;height:350px"></div>
          </el-tab-pane>
          <el-tab-pane label="学习反馈" name="feedback">
            <el-empty v-if="!detailFeedback" description="暂无反馈数据" />
            <div v-else>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="学习状态">{{ detailFeedback.status }}</el-descriptions-item>
                <el-descriptions-item label="HP信心值">{{ detailFeedback.hp }}</el-descriptions-item>
                <el-descriptions-item label="ATK解题力">{{ detailFeedback.atk }}</el-descriptions-item>
                <el-descriptions-item label="DEF基础度">{{ detailFeedback.def }}</el-descriptions-item>
                <el-descriptions-item label="等级">{{ detailFeedback.level }}</el-descriptions-item>
                <el-descriptions-item label="经验值">{{ detailFeedback.exp }}</el-descriptions-item>
              </el-descriptions>
              <div v-if="detailFeedback.weakPoints && detailFeedback.weakPoints.length" style="margin-top:12px">
                <p style="font-weight:bold;color:#f56c6c">薄弱知识点：</p>
                <el-tag v-for="wp in detailFeedback.weakPoints" :key="wp.name" style="margin:2px" size="small" type="danger">
                  {{ wp.name }}({{ wp.score }}分) - {{ wp.suggestion }}
                </el-tag>
              </div>
              <el-alert v-if="detailFeedback.nextAction" :title="detailFeedback.nextAction" type="info" :closable="false" style="margin-top:12px" show-icon />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { searchCourse } from '../api'
import {
  getCourseStudentProfiles, getProfileSummary, getCompetency,
  getRecommendations, getAchievements, getTitle,
  getGrowthHistory, getCompetencyHistory, getTestFeedback
} from '../api/profile'

const courses = ref([])
const selectedCourse = ref('')
const students = ref([])
const loading = ref(false)
const searchKey = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detailStudent = ref(null)
const detailProfile = ref({})
const detailCompetency = ref([])
const detailRecs = ref([])
const detailAchievements = ref([])
const detailTitleText = ref('')
const detailBadgeCount = ref(0)
const detailGrowthHistory = ref([])
const detailCompHistory = ref([])
const detailFeedback = ref(null)
const detailTab = ref('competency')
const detailRadar = ref(null)
const detailHistoryChart = ref(null)

const levelNames = { 1: '入门', 2: '初级', 3: '中级', 4: '熟练', 5: '精通' }

const filteredStudents = computed(() => {
  if (!searchKey.value) return students.value
  const k = searchKey.value.toLowerCase()
  return students.value.filter(s =>
    String(s.studentNo).includes(k) || (s.name || '').toLowerCase().includes(k)
  )
})

const detailAttrs = [
  { key: 'hp', label: 'HP 信心值', color: '#f56c6c', percent: p => (p.hp || 0) },
  { key: 'atk', label: 'ATK 解题力', color: '#e6a23c', percent: p => (p.atk || 0) },
  { key: 'def', label: 'DEF 基础度', color: '#409eff', percent: p => (p.def || 0) },
  { key: 'exp', label: 'EXP 经验值', color: '#67c23a', percent: p => Math.min(100, (p.exp || 0) / 20) }
]

const detailTitle = computed(() => {
  if (!detailStudent.value) return ''
  return `${detailStudent.value.name}（${detailStudent.value.studentNo}）的画像详情`
})

const detailStatusType = computed(() => {
  const s = detailProfile.value.status
  if (s === '存在风险') return 'danger'
  if (s === '进度滞后') return 'warning'
  if (s === '能力提升') return 'success'
  return ''
})

const statusTagType = (s) => {
  if (s === '存在风险') return 'danger'
  if (s === '进度滞后') return 'warning'
  if (s === '能力提升') return 'success'
  if (s === '正常学习') return ''
  return 'info'
}
const levelTagType = (lv) => lv >= 5 ? 'success' : lv >= 3 ? 'warning' : ''
const recTypeTag = (t) => ({ review_material: 'danger', practice: 'warning', extended_material: 'success', knowledge_point: '' }[t] || '')
const recTypeLabel = (t) => ({ review_material: '复习', practice: '练习', extended_material: '拓展', knowledge_point: '学习' }[t] || t)
const badgeIcon = (n) => ({ '连击王': '🔥', '完美主义': '💎', '速通者': '🏃', 'Pythonic': '🐍', 'Debug之眼': '🔍', '夜枭': '🦉', '助人者': '🤝' }[n] || '🏆')
const sourceLabel = (s) => ({ quiz: '测验', boss: '综合测验', default: '答题', task_complete: '任务完成', exam_pass: '考试通过', streak: '连续学习' }[s] || s)

const loadCourses = async () => {
  try {
    const { data } = await searchCourse('')
    if (data.code === 200) {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      if (user.role === 'admin') {
        courses.value = data.data
      } else {
        courses.value = data.data.filter(c => c.teacher === user.name)
      }
      if (courses.value.length) selectedCourse.value = courses.value[0].courseCode
    }
  } catch (e) { /* ignore */ }
}

const loadStudents = async () => {
  if (!selectedCourse.value) return
  loading.value = true
  try {
    const { data } = await getCourseStudentProfiles(selectedCourse.value)
    if (data.code === 200) students.value = data.data
  } catch (e) {
    ElMessage.error('学生画像加载失败')
  }
  loading.value = false
}

const onCourseChange = () => loadStudents()

const selectStudent = async (row) => {
  detailStudent.value = row
  detailVisible.value = true
  detailTab.value = 'competency'
  await loadDetailData()
}

const loadDetailData = async () => {
  if (!detailStudent.value) return
  const sn = parseInt(detailStudent.value.studentNo)
  const cc = parseInt(selectedCourse.value)

  try {
    const [sumRes, recRes, achRes, titleRes] = await Promise.all([
      getProfileSummary(sn, cc),
      getRecommendations(sn, cc),
      getAchievements(sn, cc),
      getTitle(sn, cc)
    ])
    if (sumRes.data.code === 200) {
      detailProfile.value = sumRes.data.data.profile
      detailCompetency.value = sumRes.data.data.competencyScores || []
    }
    if (recRes.data.code === 200) detailRecs.value = recRes.data.data
    if (achRes.data.code === 200) {
      detailAchievements.value = achRes.data.data
      detailBadgeCount.value = detailAchievements.value.filter(a => a.achievementType === 'badge').length
    }
    if (titleRes.data.code === 200) detailTitleText.value = titleRes.data.data
  } catch (e) { /* ignore */ }
}

const onDetailTabChange = (tab) => {
  if (!detailStudent.value) return
  const sn = parseInt(detailStudent.value.studentNo)
  const cc = parseInt(selectedCourse.value)
  if (tab === 'competency') nextTick(() => renderDetailRadar())
  if (tab === 'growthHistory') loadDetailGrowthHistory(sn, cc)
  if (tab === 'competencyHistory') { loadDetailCompHistory(sn, cc); nextTick(() => renderDetailHistoryChart()) }
  if (tab === 'feedback') loadDetailFeedback(sn, cc)
}

const onDetailOpened = () => nextTick(() => renderDetailRadar())

const loadDetailGrowthHistory = async (sn, cc) => {
  try {
    const { data } = await getGrowthHistory(sn, cc)
    if (data.code === 200) detailGrowthHistory.value = data.data
  } catch (e) { /* ignore */ }
}

const loadDetailCompHistory = async (sn, cc) => {
  try {
    const { data } = await getCompetencyHistory(sn, cc, null)
    if (data.code === 200) detailCompHistory.value = data.data
  } catch (e) { /* ignore */ }
}

const loadDetailFeedback = async (sn, cc) => {
  try {
    const { data } = await getTestFeedback(sn, cc)
    if (data.code === 200) detailFeedback.value = data.data
  } catch (e) { /* ignore */ }
}

const renderDetailRadar = () => {
  if (!detailRadar.value || !detailCompetency.value.length) return
  const chart = echarts.init(detailRadar.value)
  chart.setOption({
    radar: { indicator: detailCompetency.value.map(c => ({ name: c.abilityPointName, max: 100 })) },
    series: [{ type: 'radar', data: [{ value: detailCompetency.value.map(c => c.score), name: '能力评分' }],
      areaStyle: { color: 'rgba(64,158,255,0.2)' }, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } }]
  })
}

const renderDetailHistoryChart = () => {
  if (!detailHistoryChart.value || !detailCompHistory.value.length) return
  const chart = echarts.init(detailHistoryChart.value)
  const byPoint = {}
  detailCompHistory.value.forEach(h => {
    if (!byPoint[h.abilityPointId]) byPoint[h.abilityPointId] = []
    byPoint[h.abilityPointId].push(h)
  })
  const series = Object.entries(byPoint).map(([apid, items]) => ({
    name: apid, type: 'line', data: items.reverse().map(h => [h.changedAt, h.newScore]), smooth: true
  }))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(byPoint), type: 'scroll', bottom: 0 },
    xAxis: { type: 'time' }, yAxis: { min: 0, max: 100 }, series
  })
}

onMounted(async () => {
  await loadCourses()
  if (selectedCourse.value) loadStudents()
})
</script>

<style scoped>
.teacher-profile-page { padding: 20px; }
.toolbar { display: flex; align-items: center; }
</style>
