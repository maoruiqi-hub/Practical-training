<template>
  <div>
    <h3>题库管理</h3>
    <div class="toolbar">
      <el-select v-model="filter.courseCode" placeholder="课程" style="width:180px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="String(c.courseCode)" />
      </el-select>
      <el-select v-model="filter.lessonNo" placeholder="章节/课时" style="width:150px" clearable @change="doSearch">
        <el-option v-for="l in lessons" :key="l.lessonNo" :label="l.lessonTitle" :value="String(l.lessonNo)" />
      </el-select>
      <el-select v-model="filter.type" placeholder="题型" style="width:110px" clearable @change="doSearch">
        <el-option label="单选" value="single" />
        <el-option label="多选" value="multi" />
        <el-option label="填空" value="fill" />
        <el-option label="简答" value="essay" />
        <el-option label="编程" value="program" />
      </el-select>
      <el-select v-model="filter.difficulty" placeholder="难度" style="width:100px" clearable @change="doSearch">
        <el-option v-for="d in difficultyOptions" :key="d.value" :label="d.label" :value="d.value" />
      </el-select>
      <el-select v-model="filter.knowledgePointId" placeholder="知识点" style="width:150px" filterable clearable @change="doSearch">
        <el-option v-for="kp in knowledgePoints" :key="kp.knowledgePointId" :label="kp.name" :value="String(kp.knowledgePointId)" />
      </el-select>
      <el-input v-model="filter.keyword" placeholder="搜索题干或知识点" style="width:220px" @keyup.enter="doSearch" clearable />
      <el-button type="primary" @click="doSearch">搜索</el-button>
      <el-button @click="resetFilter">重置</el-button>
      <el-button type="success" @click="openAdd">新增题目</el-button>
    </div>
    <el-table :data="questions" style="width:100%" v-loading="loading" element-loading-text="正在加载题目..." empty-text="暂无题目" size="small">
      <el-table-column prop="questionId" label="ID" width="60" />
      <el-table-column label="题型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" show-overflow-tooltip />
      <el-table-column label="难度" width="80">
        <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column label="知识点" width="120">
        <template #default="{ row }">{{ knowledgeLabel(row.knowledgePointId) || '未关联' }}</template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="60" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row.questionId)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '新增题目'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="题型"><el-select v-model="form.type"><el-option label="单选" value="single" /><el-option label="多选" value="multi" /><el-option label="填空" value="fill" /><el-option label="简答" value="essay" /><el-option label="编程" value="program" /></el-select></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.stem" type="textarea" :rows="2" /></el-form-item>
        <el-form-item v-if="form.type==='single'||form.type==='multi'" label="选项">
          <div v-for="(o,i) in optList" :key="i" style="display:flex;gap:8px;margin-bottom:6px">
            <el-input v-model="optList[i]" :placeholder="'选项'+String.fromCharCode(65+i)" size="small" />
            <el-button size="small" @click="optList.splice(i,1)" :disabled="optList.length<=2">-</el-button>
          </div>
          <el-button size="small" @click="optList.push('')">+ 添加选项</el-button>
        </el-form-item>
        <el-form-item label="答案"><el-input v-model="form.answer" :placeholder="answerPlaceholder" /></el-form-item>
        <el-form-item label="课程"><el-input v-model="form.courseCode" placeholder="课程编号" /></el-form-item>
        <el-form-item label="关联课时"><el-input v-model="form.lessonNo" placeholder="课时编号" /></el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" filterable clearable placeholder="选择知识点">
            <el-option v-for="kp in knowledgePoints" :key="kp.knowledgePointId" :label="kp.name" :value="String(kp.knowledgePointId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty">
            <el-option v-for="d in difficultyOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.score" :min="0" :max="100" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchCourse, getCourseLessons, getKnowledgePoints, filterQuestion, updateQuestion, addQuestion, deleteQuestion } from '../api'

const questions = ref([])
const loading = ref(true)
const courses = ref([])
const lessons = ref([])
const knowledgePoints = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ questionId:'',type:'single',stem:'',answer:'',courseCode:'1',lessonNo:'',knowledgePointId:'',difficulty:3,score:10,options:'' })
const filter = reactive({ courseCode:'', lessonNo:'', knowledgePointId:'', type:'', difficulty:null, keyword:'' })
const optList = ref([])

const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答',program:'编程'}[t]||t)
const difficultyOptions = [
  { label: '基础题', value: 1 },
  { label: '较易题', value: 2 },
  { label: '中等题', value: 3 },
  { label: '提高题', value: 4 },
  { label: '综合题', value: 5 }
]
const difficultyLabel = d => difficultyOptions.find(item => item.value === d)?.label || '未设置'
const knowledgeLabel = id => knowledgePoints.value.find(kp => String(kp.knowledgePointId) === String(id))?.name || ''
const answerPlaceholder = computed(() => {
  if (form.type === 'multi') return '多选用逗号分隔'
  if (form.type === 'essay') return '填写评分要点'
  if (form.type === 'program') return '填写参考代码、测试要点或评分标准'
  return '填写正确答案'
})
const doSearch = async () => {
  loading.value = true
  try {
    const res = await filterQuestion({
      courseCode: filter.courseCode || undefined,
      lessonNo: filter.lessonNo || undefined,
      knowledgePointId: filter.knowledgePointId || undefined,
      type: filter.type || undefined,
      difficulty: filter.difficulty || undefined,
      keyword: filter.keyword || undefined
    })
    if (res.data.code === 200) questions.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('题目加载失败')
  } finally { loading.value = false }
}

const loadCourses = async () => {
  try {
    const res = await searchCourse('')
    if (res.data.code === 200) {
      courses.value = res.data.data
      if (!filter.courseCode && courses.value.length) filter.courseCode = String(courses.value[0].courseCode)
      await loadLessons()
      await loadKnowledgePoints()
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('课程加载失败')
  }
}

const loadLessons = async () => {
  if (!filter.courseCode) { lessons.value = []; return }
  try {
    const res = await getCourseLessons(filter.courseCode)
    if (res.data.code === 200) lessons.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('课时加载失败')
  }
}

const loadKnowledgePoints = async () => {
  if (!filter.courseCode) { knowledgePoints.value = []; return }
  try {
    const res = await getKnowledgePoints(filter.courseCode)
    if (res.data.code === 200) knowledgePoints.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('知识点加载失败')
  }
}

const onCourseChange = async () => {
  filter.lessonNo = ''
  filter.knowledgePointId = ''
  await loadLessons()
  await loadKnowledgePoints()
  await doSearch()
}

const resetFilter = async () => {
  Object.assign(filter, { courseCode: filter.courseCode, lessonNo:'', knowledgePointId:'', type:'', difficulty:null, keyword:'' })
  await doSearch()
}

onMounted(async () => {
  await loadCourses()
  await doSearch()
})

const openAdd = () => {
  isEdit.value = false; optList.value = []
  Object.assign(form, { questionId:'',type:'single',stem:'',answer:'',courseCode:filter.courseCode || '',lessonNo:filter.lessonNo || '',knowledgePointId:'',difficulty:3,score:10 })
  dialogVisible.value = true
}
const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  form.knowledgePointId = row.knowledgePointId ? String(row.knowledgePointId) : ''
  optList.value = parseOptions(row.options)
  dialogVisible.value = true
}
const parseOptions = (options) => {
  if (!options) return []
  try {
    const parsed = JSON.parse(options)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    ElMessage.warning('该题选项格式异常，已先置为空选项，请保存后修正')
    return []
  }
}
const save = async () => {
  try {
    if (['single','multi'].includes(form.type)) form.options = JSON.stringify(optList.value)
    else form.options = null
    if (isEdit.value) {
      const res = await updateQuestion(form.questionId, form)
      if (res.data.code !== 200) { ElMessage.error(res.data.msg); return }
      ElMessage.success('已更新')
    } else {
      const res = await addQuestion(form)
      if (res.data.code !== 200) { ElMessage.error(res.data.msg); return }
      ElMessage.success('已新增')
    }
    dialogVisible.value = false; doSearch()
  } catch {
    ElMessage.error('题目保存失败')
  }
}
const del = async (id) => {
  try {
    const res = await deleteQuestion(id)
    if (res.data.code !== 200) { ElMessage.error(res.data.msg); return }
    ElMessage.success('已删除'); doSearch()
  } catch {
    ElMessage.error('题目删除失败')
  }
}
</script>

<style scoped>
.toolbar { margin-bottom:16px; display:flex; gap:10px; }
</style>
