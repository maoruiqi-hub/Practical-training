<template>
  <div class="wrong-book">
    <div class="page-head">
      <div>
        <h3>错题本</h3>
        <p>汇总在线测验中的客观题错题，按知识点和题型查看薄弱项。</p>
      </div>
      <el-button type="primary" @click="loadWrongBook">刷新</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="filters.taskNo" placeholder="任务编号" clearable style="width:160px" @keyup.enter="loadWrongBook" />
      <el-select v-model="filters.knowledgePointId" placeholder="知识点" clearable filterable style="width:200px">
        <el-option v-for="item in knowledgeOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.type" placeholder="题型" clearable style="width:160px">
        <el-option label="单选题" value="single" />
        <el-option label="多选题" value="multi" />
        <el-option label="填空题" value="fill" />
        <el-option label="简答题" value="essay" />
        <el-option label="编程题" value="program" />
      </el-select>
      <el-button type="primary" @click="loadWrongBook">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div v-loading="loading" element-loading-text="正在加载错题本…" class="content">
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :sm="8">
          <el-statistic title="客观题作答数" :value="summary.totalAnswers" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="错题数" :value="summary.wrongAnswers" />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-statistic title="错误率" :value="wrongRate" suffix="%" :precision="1" />
        </el-col>
      </el-row>

      <el-row :gutter="16" class="analysis-row">
        <el-col :xs="24" :md="12">
          <h4>知识点掌握</h4>
          <el-table :data="summary.mastery" size="small" height="260">
            <el-table-column prop="knowledgePointName" label="知识点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="wrong" label="错题" width="70" />
            <el-table-column prop="total" label="作答" width="70" />
            <el-table-column label="掌握度" width="110">
              <template #default="{ row }">
                <el-progress :percentage="row.masteryRate" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column prop="level" label="状态" width="100" />
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <h4>按知识点统计</h4>
          <el-table :data="summary.byKnowledgePoint" size="small" height="260">
            <el-table-column prop="key" label="知识点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="wrong" label="错题" width="80" />
            <el-table-column prop="total" label="作答" width="80" />
            <el-table-column label="错误率" width="90">
              <template #default="{ row }">{{ row.wrongRate }}%</template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
      <el-row :gutter="16" class="analysis-row">
        <el-col :xs="24" :md="12">
          <h4>按题型统计</h4>
          <el-table :data="typeStats" size="small" height="220">
            <el-table-column prop="label" label="题型" min-width="120" />
            <el-table-column prop="wrong" label="错题" width="80" />
            <el-table-column prop="total" label="作答" width="80" />
            <el-table-column label="错误率" width="90">
              <template #default="{ row }">{{ row.wrongRate }}%</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <h4>复习建议</h4>
          <el-table :data="summary.recommendations" size="small" height="220" empty-text="暂无专项建议">
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

      <h4>错题明细</h4>
      <el-table :data="summary.wrongList" style="width:100%" empty-text="暂无错题">
        <el-table-column label="题干" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.stem || `题目 #${row.questionId}` }}</template>
        </el-table-column>
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="知识点" width="140" show-overflow-tooltip />
        <el-table-column label="我的答案" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ formatAnswer(row.studentAnswer) }}</template>
        </el-table-column>
        <el-table-column label="正确答案" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ formatAnswer(row.correctAnswer) }}</template>
        </el-table-column>
        <el-table-column label="得分" width="90">
          <template #default="{ row }">{{ row.score ?? 0 }}/{{ row.maxScore ?? '-' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentWrongQuestions } from '../api'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const loading = ref(false)
const filters = reactive({ taskNo: '', knowledgePointId: '', type: '' })
const summary = reactive({
  totalAnswers: 0,
  wrongAnswers: 0,
  byKnowledgePoint: [],
  byType: [],
  wrongList: [],
  mastery: [],
  recommendations: []
})

const wrongRate = computed(() => {
  if (!summary.totalAnswers) return 0
  return summary.wrongAnswers * 100 / summary.totalAnswers
})

const knowledgeOptions = computed(() => {
  const map = new Map()
  summary.wrongList.forEach(item => {
    if (item.knowledgePointId) map.set(String(item.knowledgePointId), item.knowledgePointName || String(item.knowledgePointId))
  })
  return [...map.entries()].map(([id, name]) => ({ id, name }))
})

const typeStats = computed(() => summary.byType.map(item => ({
  ...item,
  label: typeLabel(item.key)
})))

function typeLabel(type) {
  const labels = { single: '单选题', multi: '多选题', fill: '填空题', essay: '简答题', program: '编程题', unknown: '未分类', '未分类': '未分类' }
  return labels[type] || type || '未分类'
}

function formatAnswer(value) {
  if (value == null || value === '') return '-'
  return String(value)
}

function applySummary(data = {}) {
  summary.totalAnswers = data.totalAnswers || 0
  summary.wrongAnswers = data.wrongAnswers || 0
  summary.byKnowledgePoint = data.byKnowledgePoint || []
  summary.byType = data.byType || []
  summary.wrongList = data.wrongList || []
  summary.mastery = data.mastery || []
  summary.recommendations = data.recommendations || []
}

async function loadWrongBook() {
  if (!user.studentNo) {
    ElMessage.error('未找到学生信息，请重新登录')
    return
  }
  loading.value = true
  try {
    const res = await getStudentWrongQuestions(user.studentNo, {
      taskNo: filters.taskNo || undefined,
      knowledgePointId: filters.knowledgePointId || undefined,
      type: filters.type || undefined
    })
    if (res.data.code === 200) {
      applySummary(res.data.data)
    } else {
      ElMessage.error(res.data.msg || '错题本加载失败')
    }
  } catch {
    ElMessage.error('错题本加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { taskNo: '', knowledgePointId: '', type: '' })
  loadWrongBook()
}

onMounted(loadWrongBook)
</script>

<style scoped>
.wrong-book { max-width: 1200px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.toolbar { display:flex; flex-wrap:wrap; gap:10px; align-items:center; margin-bottom:16px; }
.content { min-height:360px; }
.stats-row { margin-bottom:18px; }
.analysis-row { margin-bottom:18px; }
h4 { margin:14px 0 10px; }
</style>
