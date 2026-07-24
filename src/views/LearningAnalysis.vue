<template>
  <div class="learning-analysis">
    <div class="page-head">
      <div>
        <h3>学情分析</h3>
        <p>基于在线测验逐题作答，查看班级共性错题、知识点掌握和推荐干预方向。</p>
      </div>
      <div class="actions">
        <el-select v-model="selectedCourse" placeholder="选择课程" filterable style="width:220px" @change="onCourseChange">
          <el-option v-for="course in courses" :key="course.courseCode" :label="course.courseName" :value="String(course.courseCode)" />
        </el-select>
        <el-select v-model="selectedClass" placeholder="选择班级" filterable clearable style="width:220px" @change="loadAiAnalysis">
          <el-option v-for="item in courseClasses" :key="item.id" :label="item.name" :value="String(item.id)" />
        </el-select>
        <el-button type="warning" :loading="clusterLoading" :disabled="!selectedClass" @click="generateCluster">问题聚类</el-button>
        <el-button type="success" :loading="suggestionLoading" :disabled="!selectedClass" @click="generateSuggestions">生成建议</el-button>
        <el-button type="primary" :loading="loading" @click="loadAnalysis">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" element-loading-text="正在加载学情分析…" class="content">
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :sm="8">
          <el-statistic title="客观题作答数" :value="summary.totalAnswers" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="错误数" :value="summary.wrongAnswers" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="班级错误率" :value="wrongRate" suffix="%" :precision="1" />
        </el-col>
      </el-row>

      <el-row :gutter="16" class="analysis-row">
        <el-col :xs="24" :md="12">
          <h4>知识点掌握</h4>
          <el-table :data="summary.mastery" size="small" height="260" empty-text="暂无测验数据">
            <el-table-column prop="knowledgePointName" label="知识点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="wrong" label="错误" width="70" />
            <el-table-column prop="total" label="作答" width="70" />
            <el-table-column label="掌握度" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.masteryRate" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column prop="level" label="状态" width="100" />
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <h4>教学干预建议</h4>
          <el-table :data="summary.recommendations" size="small" height="260" empty-text="暂无建议">
            <el-table-column prop="knowledgePointName" label="知识点" width="120" show-overflow-tooltip />
            <el-table-column label="优先级" width="90">
              <template #default="{ row }">
                <el-tag :type="row.priority === 'high' ? 'danger' : 'warning'" size="small">
                  {{ row.priority === 'high' ? '高' : '中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="建议" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="analysis-row">
        <el-col :xs="24" :md="12">
          <h4>共性错题</h4>
          <el-table :data="summary.byQuestion" size="small" height="260" empty-text="暂无错题">
            <el-table-column prop="key" label="题目编号" width="100" />
            <el-table-column prop="wrong" label="错误" width="80" />
            <el-table-column prop="total" label="作答" width="80" />
            <el-table-column label="错误率" width="90">
              <template #default="{ row }">{{ row.wrongRate }}%</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <h4>题型表现</h4>
          <el-table :data="typeStats" size="small" height="260" empty-text="暂无题型数据">
            <el-table-column prop="label" label="题型" min-width="120" />
            <el-table-column prop="wrong" label="错误" width="80" />
            <el-table-column prop="total" label="作答" width="80" />
            <el-table-column label="错误率" width="90">
              <template #default="{ row }">{{ row.wrongRate }}%</template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="analysis-row">
        <el-col :xs="24" :md="12">
          <h4>共性问题聚类</h4>
          <el-table :data="clusters" size="small" height="260" empty-text="请选择班级并生成问题聚类">
            <el-table-column label="主题" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.topic || row.title || row.cluster || row.name || '共性问题' }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.note || row.summary || row.reason || row.raw_response || row.raw || JSON.stringify(row) }}</template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ shortTime(row.generated_at || row.generatedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <h4>AI 教学建议</h4>
          <el-table :data="aiSuggestions" size="small" height="260" empty-text="请选择班级并生成教学建议">
            <el-table-column label="建议" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.title || row.content || row.suggestion || row.message || row.raw_response || JSON.stringify(row) }}</template>
            </el-table-column>
            <el-table-column label="优先级" width="90">
              <template #default="{ row }">
                <el-tag :type="priorityTag(row.urgency || row.priority || row.level)" size="small">
                  {{ priorityLabel(row.urgency || row.priority || row.level) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  generateProblemCluster,
  generateTeachingSuggestions,
  getClassList,
  getCourseWrongQuestions,
  getProblemCluster,
  getTeachingSuggestions,
  searchCourse
} from '../api'

const courses = ref([])
const classes = ref([])
const selectedCourse = ref('')
const selectedClass = ref('')
const loading = ref(false)
const clusterLoading = ref(false)
const suggestionLoading = ref(false)
const clusters = ref([])
const aiSuggestions = ref([])
const summary = reactive({
  totalAnswers: 0,
  wrongAnswers: 0,
  byQuestion: [],
  byType: [],
  mastery: [],
  recommendations: []
})

const wrongRate = computed(() => {
  if (!summary.totalAnswers) return 0
  return summary.wrongAnswers * 100 / summary.totalAnswers
})

const typeStats = computed(() => summary.byType.map(item => ({
  ...item,
  label: typeLabel(item.key)
})))
const courseClasses = computed(() => classes.value.filter(item => String(item.courseId) === String(selectedCourse.value)))

function typeLabel(type) {
  const labels = { single: '单选题', multi: '多选题', fill: '填空题', essay: '简答题', program: '编程题', unknown: '未分类', '未分类': '未分类' }
  return labels[type] || type || '未分类'
}

function priorityTag(value) {
  return ({ high: 'danger', medium: 'warning', low: 'info', 高: 'danger', 中: 'warning', 低: 'info' }[value] || 'info')
}

function priorityLabel(value) {
  return ({ high: '高', medium: '中', low: '低' }[value] || value || '-')
}

function shortTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function applySummary(data = {}) {
  summary.totalAnswers = data.totalAnswers || 0
  summary.wrongAnswers = data.wrongAnswers || 0
  summary.byQuestion = data.byQuestion || []
  summary.byType = data.byType || []
  summary.mastery = data.mastery || []
  summary.recommendations = data.recommendations || []
}

async function loadCourses() {
  try {
    const res = await searchCourse('')
    if (res.data.code === 200) {
      courses.value = res.data.data || []
      if (!selectedCourse.value && courses.value.length) selectedCourse.value = String(courses.value[0].courseCode)
    } else {
      ElMessage.error(res.data.msg || '课程加载失败')
    }
  } catch {
    ElMessage.error('课程加载失败')
  }
}

async function loadClasses() {
  try {
    const res = await getClassList()
    if (res.data.code === 200) classes.value = res.data.data || []
    else ElMessage.error(res.data.msg || '班级加载失败')
  } catch {
    ElMessage.error('班级加载失败')
  }
}

function syncSelectedClass() {
  if (courseClasses.value.some(item => String(item.id) === String(selectedClass.value))) return
  selectedClass.value = courseClasses.value.length ? String(courseClasses.value[0].id) : ''
}

async function onCourseChange() {
  syncSelectedClass()
  await loadAnalysis()
}

async function loadAnalysis() {
  if (!selectedCourse.value) return
  loading.value = true
  try {
    const res = await getCourseWrongQuestions(selectedCourse.value)
    if (res.data.code === 200) applySummary(res.data.data)
    else ElMessage.error(res.data.msg || '学情分析加载失败')
    await loadAiAnalysis()
  } catch {
    ElMessage.error('学情分析加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAiAnalysis() {
  clusters.value = []
  aiSuggestions.value = []
  if (!selectedClass.value) return
  try {
    const [clusterRes, suggestionRes] = await Promise.all([
      getProblemCluster(selectedClass.value),
      getTeachingSuggestions(selectedClass.value)
    ])
    if (clusterRes.data.code === 200) clusters.value = clusterRes.data.data || []
    if (suggestionRes.data.code === 200) aiSuggestions.value = suggestionRes.data.data || []
  } catch { /* AI 历史结果为空时不影响基础学情分析 */ }
}

async function generateCluster() {
  if (!selectedClass.value) return ElMessage.warning('请先选择班级')
  clusterLoading.value = true
  try {
    const res = await generateProblemCluster(selectedClass.value, selectedCourse.value)
    if (res.data.code === 200) {
      clusters.value = res.data.data || []
      ElMessage.success('共性问题聚类已生成')
    } else ElMessage.error(res.data.msg || '问题聚类失败')
  } finally {
    clusterLoading.value = false
  }
}

async function generateSuggestions() {
  if (!selectedClass.value) return ElMessage.warning('请先选择班级')
  suggestionLoading.value = true
  try {
    const res = await generateTeachingSuggestions(selectedClass.value, selectedCourse.value)
    if (res.data.code === 200) {
      aiSuggestions.value = res.data.data || []
      ElMessage.success('教学建议已生成')
    } else ElMessage.error(res.data.msg || '教学建议生成失败')
  } finally {
    suggestionLoading.value = false
  }
}

onMounted(async () => {
  await loadCourses()
  await loadClasses()
  syncSelectedClass()
  await loadAnalysis()
})
</script>

<style scoped>
.learning-analysis { max-width: 1200px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.actions { display:flex; gap:10px; align-items:center; flex-wrap:wrap; justify-content:flex-end; }
.content { min-height:360px; }
.stats-row { margin-bottom:18px; }
.analysis-row { margin-bottom:18px; }
h4 { margin:14px 0 10px; }
</style>
