<template>
  <div class="learning-analysis">
    <div class="page-head">
      <div>
        <h3>学情分析</h3>
        <p>基于在线测验逐题作答，查看班级共性错题、知识点掌握和推荐干预方向。</p>
      </div>
      <div class="actions">
        <el-select v-model="selectedCourse" placeholder="选择课程" filterable style="width:220px" @change="loadAnalysis">
          <el-option v-for="course in courses" :key="course.courseCode" :label="course.courseName" :value="String(course.courseCode)" />
        </el-select>
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
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCourseWrongQuestions, searchCourse } from '../api'

const courses = ref([])
const selectedCourse = ref('')
const loading = ref(false)
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

function typeLabel(type) {
  const labels = { single: '单选题', multi: '多选题', fill: '填空题', essay: '简答题', program: '编程题', unknown: '未分类', '未分类': '未分类' }
  return labels[type] || type || '未分类'
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

async function loadAnalysis() {
  if (!selectedCourse.value) return
  loading.value = true
  try {
    const res = await getCourseWrongQuestions(selectedCourse.value)
    if (res.data.code === 200) applySummary(res.data.data)
    else ElMessage.error(res.data.msg || '学情分析加载失败')
  } catch {
    ElMessage.error('学情分析加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCourses()
  await loadAnalysis()
})
</script>

<style scoped>
.learning-analysis { max-width: 1200px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.actions { display:flex; gap:10px; align-items:center; }
.content { min-height:360px; }
.stats-row { margin-bottom:18px; }
.analysis-row { margin-bottom:18px; }
h4 { margin:14px 0 10px; }
</style>
