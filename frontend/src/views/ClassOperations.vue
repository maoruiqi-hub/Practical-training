<template>
  <div class="class-page">
    <div class="page-head">
      <div>
        <h3>班级运营</h3>
        <p>管理教学班级、学生名单、风险预警和 AI 教学建议。</p>
      </div>
      <div class="actions">
        <el-button :loading="loading" @click="loadClasses">刷新</el-button>
        <el-button type="success" @click="openClass()">新建班级</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card shadow="never">
          <template #header>我的班级</template>
          <el-table v-loading="loading" :data="classes" height="620" highlight-current-row empty-text="暂无班级" @current-change="selectClass">
            <el-table-column prop="name" label="班级" min-width="120" show-overflow-tooltip />
            <el-table-column prop="courseId" label="课程" width="100" />
            <el-table-column prop="semester" label="学期" width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button size="small" @click.stop="openClass(row)">编辑</el-button>
                <el-popconfirm title="删除空班级？" @confirm="removeClass(row.id)">
                  <template #reference><el-button size="small" type="danger" @click.stop>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card shadow="never" v-if="currentClass">
          <template #header>
            <div class="card-head">
              <span>{{ currentClass.name }}</span>
              <div>
                <el-button size="small" @click="loadClassDetail">刷新详情</el-button>
                <el-button size="small" type="primary" :loading="detecting" @click="detectRisks">风险检测</el-button>
                <el-button size="small" type="success" :loading="suggesting" @click="generateSuggestions">生成建议</el-button>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="学生名单" name="students">
              <div class="inline-form">
                <el-input v-model="studentId" placeholder="学生编号" style="width:220px" @keyup.enter="enrollStudent" />
                <el-button type="primary" @click="enrollStudent">添加学生</el-button>
              </div>
              <el-table :data="studentRows" empty-text="暂无学生">
                <el-table-column prop="studentId" label="学生编号" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button size="small" type="danger" text @click="removeStudent(row.studentId)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="风险预警" name="risks">
              <el-table :data="risks" empty-text="暂无风险预警">
                <el-table-column prop="studentId" label="学生" width="110" />
                <el-table-column prop="riskType" label="类型" min-width="130" show-overflow-tooltip />
                <el-table-column label="等级" width="90">
                  <template #default="{ row }">
                    <el-tag :type="riskTag(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="170" />
                <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" text @click="resolveRisk(row.id)">处理</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="教学建议" name="suggestions">
              <el-table :data="suggestions" empty-text="暂无教学建议">
                <el-table-column label="建议内容" min-width="220">
                  <template #default="{ row }">{{ row.title || row.content || row.suggestion || row.message || JSON.stringify(row) }}</template>
                </el-table-column>
                <el-table-column label="优先级" width="100">
                  <template #default="{ row }">{{ row.priority || row.level || '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="反馈汇总" name="feedback">
              <el-table :data="feedback" empty-text="暂无反馈">
                <el-table-column label="内容" min-width="220">
                  <template #default="{ row }">{{ row.content || row.question || row.feedback || JSON.stringify(row) }}</template>
                </el-table-column>
                <el-table-column label="来源" width="120">
                  <template #default="{ row }">{{ row.studentName || row.studentId || '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
        <el-empty v-else description="请选择一个班级" />
      </el-col>
    </el-row>

    <el-dialog v-model="classDialog" :title="classForm.id ? '编辑班级' : '新建班级'" width="520px">
      <el-form :model="classForm" label-width="80px">
        <el-form-item label="班级名称"><el-input v-model="classForm.name" /></el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="classForm.courseId" filterable style="width:100%">
            <el-option v-for="course in courses" :key="course.courseCode" :label="course.courseName" :value="String(course.courseCode)" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期"><el-input v-model="classForm.semester" placeholder="2025-2026-1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialog=false">取消</el-button>
        <el-button type="primary" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addClass,
  deleteClass,
  detectClassRisks,
  enrollClassStudent,
  generateTeachingSuggestions,
  getClassDetail,
  getClassFeedbackSummary,
  getClassList,
  getClassRiskAlerts,
  getTeachingSuggestions,
  removeClassStudent,
  resolveRiskAlert,
  searchCourse,
  updateClass
} from '../api'

const courses = ref([])
const classes = ref([])
const currentClass = ref(null)
const detail = ref(null)
const risks = ref([])
const suggestions = ref([])
const feedback = ref([])
const loading = ref(false)
const detecting = ref(false)
const suggesting = ref(false)
const activeTab = ref('students')
const studentId = ref('')
const classDialog = ref(false)
const classForm = reactive({ id: '', name: '', courseId: '', semester: '' })

const studentRows = computed(() => (detail.value?.studentIds || []).map(id => ({ studentId: id })))

async function loadCourses() {
  const res = await searchCourse('')
  if (res.data.code === 200) courses.value = res.data.data || []
}

async function loadClasses() {
  loading.value = true
  try {
    const res = await getClassList()
    if (res.data.code === 200) {
      classes.value = res.data.data || []
      if (!currentClass.value && classes.value.length) selectClass(classes.value[0])
    } else ElMessage.error(res.data.msg || '班级加载失败')
  } catch {
    ElMessage.error('班级加载失败')
  } finally {
    loading.value = false
  }
}

async function selectClass(row) {
  if (!row) return
  currentClass.value = row
  await loadClassDetail()
}

async function loadClassDetail() {
  if (!currentClass.value?.id) return
  const [detailRes, riskRes, suggestionRes, feedbackRes] = await Promise.all([
    getClassDetail(currentClass.value.id),
    getClassRiskAlerts(currentClass.value.id),
    getTeachingSuggestions(currentClass.value.id),
    getClassFeedbackSummary(currentClass.value.id)
  ])
  if (detailRes.data.code === 200) detail.value = detailRes.data.data
  if (riskRes.data.code === 200) risks.value = riskRes.data.data || []
  if (suggestionRes.data.code === 200) suggestions.value = suggestionRes.data.data || []
  if (feedbackRes.data.code === 200) feedback.value = feedbackRes.data.data || []
}

function openClass(row) {
  Object.assign(classForm, row || { id: '', name: '', courseId: courses.value[0]?.courseCode || '', semester: '' })
  classDialog.value = true
}

async function saveClass() {
  if (!classForm.name || !classForm.courseId) return ElMessage.warning('请填写班级名称和课程')
  const payload = { name: classForm.name, courseId: classForm.courseId, semester: classForm.semester }
  const res = classForm.id ? await updateClass(classForm.id, payload) : await addClass(payload)
  if (res.data.code === 200) {
    ElMessage.success('已保存')
    classDialog.value = false
    currentClass.value = res.data.data || currentClass.value
    loadClasses()
  } else ElMessage.error(res.data.msg || '保存失败')
}

async function removeClass(id) {
  const res = await deleteClass(id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    currentClass.value = null
    loadClasses()
  } else ElMessage.error(res.data.msg || '删除失败')
}

async function enrollStudent() {
  if (!studentId.value) return
  const res = await enrollClassStudent(currentClass.value.id, studentId.value)
  if (res.data.code === 200) {
    ElMessage.success('已添加')
    studentId.value = ''
    loadClassDetail()
  } else ElMessage.error(res.data.msg || '添加失败')
}

async function removeStudent(id) {
  const res = await removeClassStudent(currentClass.value.id, id)
  if (res.data.code === 200) {
    ElMessage.success('已移除')
    loadClassDetail()
  } else ElMessage.error(res.data.msg || '移除失败')
}

async function detectRisks() {
  detecting.value = true
  try {
    const res = await detectClassRisks(currentClass.value.id, currentClass.value.courseId)
    if (res.data.code === 200) {
      risks.value = res.data.data || []
      ElMessage.success('风险检测完成')
      activeTab.value = 'risks'
    } else ElMessage.error(res.data.msg || '风险检测失败')
  } finally {
    detecting.value = false
  }
}

async function resolveRisk(id) {
  const res = await resolveRiskAlert(id)
  if (res.data.code === 200) {
    ElMessage.success('已处理')
    loadClassDetail()
  } else ElMessage.error(res.data.msg || '处理失败')
}

async function generateSuggestions() {
  suggesting.value = true
  try {
    const res = await generateTeachingSuggestions(currentClass.value.id, currentClass.value.courseId)
    if (res.data.code === 200) {
      suggestions.value = res.data.data || []
      ElMessage.success('教学建议已生成')
      activeTab.value = 'suggestions'
    } else ElMessage.error(res.data.msg || '生成失败')
  } finally {
    suggesting.value = false
  }
}

const riskLabel = level => ({ high: '高', medium: '中', low: '低' }[level] || level || '-')
const riskTag = level => level === 'high' ? 'danger' : level === 'medium' ? 'warning' : 'info'

onMounted(async () => {
  await loadCourses()
  await loadClasses()
})
</script>

<style scoped>
.class-page { max-width: 1240px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.actions { display:flex; gap:10px; align-items:center; }
.card-head { display:flex; justify-content:space-between; align-items:center; gap:12px; }
.inline-form { display:flex; gap:10px; margin-bottom:12px; }
</style>
