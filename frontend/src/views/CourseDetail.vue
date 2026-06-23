<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>
    <h3>{{ courseName }}</h3>
    <el-tabs v-model="activeTab" style="margin-top:10px">
      <!-- 课时列表 -->
      <el-tab-pane label="课时列表" name="lessons">
        <div v-loading="loading" element-loading-text="正在加载课时..." style="min-height:200px">
          <div v-if="userRole!=='student'" style="margin-bottom:12px">
            <el-button type="success" @click="openLessonAdd">新增课时</el-button>
          </div>
          <el-table :data="lessons" style="width:100%" empty-text="暂无课时">
            <el-table-column prop="lessonNo" label="编号" width="80" />
            <el-table-column label="课时标题">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push('/lesson/' + row.lessonNo)">{{ row.lessonTitle }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="内容简介" show-overflow-tooltip />
            <el-table-column v-if="userRole!=='student'" label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="openLessonEdit(row)">编辑</el-button>
                  <el-popconfirm title="确定删除？" @confirm="deleteLesson(row.lessonNo)">
                    <template #reference><el-button size="small" type="danger">删除</el-button></template>
                  </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 学习任务 -->
      <el-tab-pane label="学习任务" name="tasks">
        <div v-loading="taskLoading" element-loading-text="正在加载任务..." style="min-height:200px">
          <div v-if="userRole!=='student'" style="margin-bottom:12px">
            <el-button type="success" @click="openTaskAdd">发布任务</el-button>
          </div>
          <el-table :data="tasks" style="width:100%" empty-text="暂无任务">
            <el-table-column prop="taskNo" label="编号" width="80" />
            <el-table-column label="任务名称" width="160">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push('/task/detail/' + row.taskNo)">{{ row.description || row.taskType }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="任务说明" />
            <el-table-column prop="deadline" label="截止时间" width="180" />
            <el-table-column prop="submitMethod" label="提交方式" width="100" />
            <el-table-column prop="score" label="分值" width="80" />
            <el-table-column label="操作" :width="userRole==='student' ? 100 : 240">
              <template #default="{ row }">
                <el-button v-if="userRole==='student'" size="small" type="primary" @click="row.taskType==='quiz' ? $router.push('/quiz/take/' + row.taskNo) : $router.push('/task/' + code + '/submit/' + row.taskNo)">{{ row.taskType==='quiz' ? '答题' : '提交' }}</el-button>
                <template v-else>
                  <el-button size="small" type="primary" @click="$router.push('/task/' + code + '/submit/' + row.taskNo)">查看提交</el-button>
                  <el-button size="small" @click="openTaskEdit(row)">编辑</el-button>
                  <el-popconfirm title="确定删除？" @confirm="deleteTask(row.taskNo)">
                    <template #reference><el-button size="small" type="danger">删除</el-button></template>
                  </el-popconfirm>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 任务弹窗 -->
    <el-dialog v-model="taskDialog" :title="isTaskEdit ? '编辑任务' : '发布任务'" width="650px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="类型"><el-select v-model="taskForm.taskType" @change="onTaskTypeChange"><el-option label="编程作业" value="编程作业" /><el-option label="课堂测验" value="课堂测验" /><el-option label="实验报告" value="实验报告" /><el-option label="测验" value="quiz" /><el-option label="其他" value="other" /></el-select></el-form-item>
        <el-form-item :label="taskForm.taskType==='quiz'?'测验名称':'任务说明'"><el-input v-model="taskForm.description" /></el-form-item>
        <el-form-item label="截止时间"><el-input v-model="taskForm.deadline" placeholder="2026-07-15 23:59:59" /></el-form-item>
        <el-form-item v-if="taskForm.taskType!=='quiz'" label="提交方式"><el-input v-model="taskForm.submitMethod" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="taskForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item v-if="taskForm.taskType!=='quiz'" label="附件"><el-upload :auto-upload="false" :limit="1" :on-change="handleTaskFile" accept="*"><el-button type="primary">选择文件</el-button></el-upload></el-form-item>
        <!-- 测验选题 -->
        <div v-if="taskForm.taskType==='quiz'" style="margin-bottom:16px">
          <div style="display:flex;gap:8px;margin-bottom:10px;flex-wrap:wrap">
            <el-input v-model="qKeyword" placeholder="搜索题干/知识点" style="width:180px" size="small" @keyup.enter="loadQuestions" clearable />
            <el-input v-model="qLesson" placeholder="课时编号" style="width:100px" size="small" clearable />
            <el-button size="small" @click="loadQuestions">筛选</el-button>
          </div>
          <div v-loading="qLoading" style="max-height:320px;overflow-y:auto;width:100%;border:1px solid #eee;border-radius:6px;padding:12px">
            <div style="color:#999;font-size:12px;margin-bottom:8px">已选 {{ selectedQuestions.length }} 题，共 {{ filteredQuestions.length }} 题</div>
            <el-checkbox-group v-model="selectedQuestions">
              <div v-for="q in filteredQuestions" :key="q.questionId" style="margin-bottom:8px;padding-bottom:8px;border-bottom:1px dashed #eee;text-align:left">
                <el-checkbox :value="q.questionId">{{ q.stem }}</el-checkbox>
                <span style="margin-left:8px"><el-tag size="small">{{ typeLabel(q.type) }}</el-tag><span style="color:#999;font-size:12px;margin-left:4px">{{ q.score }}分 | 课时{{ q.lessonNo }} | {{ q.knowledgePoint }}</span></span>
              </div>
            </el-checkbox-group>
            <el-empty v-if="!questionBank.length && !qLoading" description="暂无题目" :image-size="40" />
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="taskDialog=false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 课时弹窗 -->
    <el-dialog v-model="lessonDialog" :title="isLessonEdit ? '编辑课时' : '新增课时'">
      <el-form :model="lessonForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="lessonForm.lessonTitle" /></el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="lessonForm.resourceType">
            <el-option label="视频" value="video" /><el-option label="PPT" value="ppt" />
            <el-option label="文档" value="doc" /><el-option label="图片" value="img" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="lessonForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="资源文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleLessonFile" accept="*">
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lessonDialog=false">取消</el-button>
        <el-button type="primary" @click="saveLesson">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getCourseLessons,
  searchCourse,
  getTaskList,
  searchQuestion,
  getQuestionsByCourse,
  updateLesson,
  addLesson,
  deleteLesson as deleteLessonApi,
  addTask,
  updateTask,
  deleteTask as deleteTaskApi,
  addQuestionsToTask
} from '../api'

const route = useRoute()
const code = route.params.code
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const courseName = ref(code)
const lessons = ref([])
const tasks = ref([])
const loading = ref(true)
const taskLoading = ref(true)
const activeTab = ref('lessons')
// 任务弹窗
const taskDialog = ref(false)
const isTaskEdit = ref(false)
const taskForm = reactive({ taskNo: '', taskType: '', description: '', deadline: '', submitMethod: '', score: 0 })

// 课时弹窗
const lessonDialog = ref(false)
const isLessonEdit = ref(false)
const lessonFile = ref(null)
const lessonForm = reactive({ lessonNo: '', lessonTitle: '', resourceType: 'video', description: '' })
const taskFile = ref(null)
const selectedQuestions = ref([])
const questionBank = ref([])
const qLoading = ref(false)
const qKeyword = ref('')
const qLesson = ref('')

const filteredQuestions = computed(() => {
  return questionBank.value.filter(q => {
    if (qKeyword.value && !q.stem.includes(qKeyword.value) && !(q.knowledgePoint||'').includes(qKeyword.value)) return false
    if (qLesson.value && q.lessonNo !== qLesson.value) return false
    return true
  })
})

const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答'}[t]||t)

const loadQuestions = async () => {
  qLoading.value = true
  try {
    const r = qKeyword.value ? await searchQuestion(qKeyword.value) : await getQuestionsByCourse(code)
    if (r.data.code === 200) questionBank.value = r.data.data
    else ElMessage.error(r.data.msg)
  } catch {
    ElMessage.error('题目加载失败')
  } finally { qLoading.value = false }
}

const onTaskTypeChange = async (val) => {
  if (val === 'quiz') { qKeyword.value = ''; qLesson.value = ''; selectedQuestions.value = []; loadQuestions() }
}

onMounted(async () => {
  try {
    const [lRes, cRes] = await Promise.all([getCourseLessons(code), searchCourse(code)])
    if (lRes.data.code === 200) lessons.value = lRes.data.data
    if (cRes.data.code === 200 && cRes.data.data.length > 0) courseName.value = cRes.data.data[0].courseName
  } finally { loading.value = false }
  try {
    const tRes = await getTaskList(code)
    if (tRes.data.code === 200) tasks.value = tRes.data.data
  } finally { taskLoading.value = false }
})

const reloadLessons = async () => { const r = await getCourseLessons(code); if (r.data.code === 200) lessons.value = r.data.data }

const openLessonAdd = () => {
  isLessonEdit.value = false; lessonFile.value = null
  Object.assign(lessonForm, { lessonNo: '', lessonTitle: '', resourceType: 'video', description: '' })
  lessonDialog.value = true
}
const openLessonEdit = (row) => {
  isLessonEdit.value = true; lessonFile.value = null
  Object.assign(lessonForm, row)
  lessonDialog.value = true
}
const handleLessonFile = (f) => { lessonFile.value = f.raw }
const handleTaskFile = (f) => { taskFile.value = f.raw }
const saveLesson = async () => {
  const fd = new FormData(); fd.append('courseCode', code)
  fd.append('lessonTitle', lessonForm.lessonTitle)
  fd.append('resourceType', lessonForm.resourceType)
  fd.append('description', lessonForm.description || '')
  if (lessonFile.value) fd.append('file', lessonFile.value)
  if (isLessonEdit.value) {
    await updateLesson(code, lessonForm.lessonNo, fd)
  } else {
    await addLesson(fd)
  }
  ElMessage.success(isLessonEdit.value ? '已更新' : '已新增')
  lessonDialog.value = false; reloadLessons()
}
const deleteLesson = async (lessonNo) => {
  await deleteLessonApi(code, lessonNo)
  ElMessage.success('已删除'); reloadLessons()
}

// 任务增删改
const reloadTasks = async () => { const r = await getTaskList(code); if (r.data.code === 200) tasks.value = r.data.data }

const openTaskAdd = () => {
  isTaskEdit.value = false; taskFile.value = null
  Object.assign(taskForm, { taskNo: '', taskType: '', description: '', deadline: '', submitMethod: '', score: 0 })
  taskDialog.value = true
}
const openTaskEdit = (row) => { isTaskEdit.value = true; Object.assign(taskForm, row); taskDialog.value = true }
const saveTask = async () => {
  const fd = new FormData()
  fd.append('taskType', taskForm.taskType); fd.append('description', taskForm.description)
  fd.append('deadline', taskForm.deadline || ''); fd.append('submitMethod', taskForm.taskType==='quiz' ? '在线答题' : (taskForm.submitMethod || ''))
  fd.append('score', String(taskForm.score))
  if (taskFile.value) fd.append('file', taskFile.value)

  if (isTaskEdit.value) {
    await updateTask(code, taskForm.taskNo, fd)
    ElMessage.success('已更新')
  } else {
    fd.append('courseCode', code)
    const created = await addTask(fd)
    // 测验：保存选题关联
    if (taskForm.taskType === 'quiz' && selectedQuestions.value.length) {
      if (created.data.code === 200 && created.data.data) {
        await addQuestionsToTask(created.data.data, selectedQuestions.value)
      }
    }
    ElMessage.success('发布成功')
  }
  taskDialog.value = false; taskFile.value = null; selectedQuestions.value = []; reloadTasks()
}
const deleteTask = async (taskNo) => {
  await deleteTaskApi(code, taskNo)
  ElMessage.success('已删除'); reloadTasks()
}
</script>
