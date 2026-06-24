<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>
    <div style="display:flex;align-items:center;justify-content:space-between">
      <h3>{{ courseName }}</h3>
      <div style="display:flex;gap:8px">
        <el-button type="primary" plain @click="$router.push('/course/' + code + '/resources')">课程资源</el-button>
        <el-button type="success" plain @click="$router.push('/course/' + code + '/knowledge-graph')">知识图谱</el-button>
      </div>
    </div>
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
        <el-form-item label="类型"><el-select v-model="taskForm.taskType" @change="onTaskTypeChange"><el-option label="编程作业" value="编程作业" /><el-option label="实验报告" value="实验报告" /><el-option label="在线测验" value="quiz" /><el-option label="其他" value="other" /></el-select></el-form-item>
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
          <div class="paper-panel">
            <el-select v-model="paperForm.strategy" size="small" style="width:130px">
              <el-option label="随机组卷" value="random" />
              <el-option label="按知识点组卷" value="knowledge" />
              <el-option label="难度平衡" value="difficulty" />
            </el-select>
            <el-input-number v-model="paperForm.count" :min="1" :max="100" size="small" controls-position="right" style="width:110px" />
            <el-select v-model="paperForm.types" multiple collapse-tags collapse-tags-tooltip placeholder="题型" size="small" style="width:180px">
              <el-option label="单选" value="single" />
              <el-option label="多选" value="multi" />
              <el-option label="填空" value="fill" />
              <el-option label="简答" value="essay" />
              <el-option label="编程" value="program" />
            </el-select>
            <el-select v-model="paperForm.knowledgePoints" multiple allow-create filterable default-first-option collapse-tags collapse-tags-tooltip placeholder="知识点" size="small" style="width:200px">
              <el-option v-for="kp in knowledgeOptions" :key="kp" :label="kp" :value="kp" />
            </el-select>
            <el-select v-model="paperForm.difficultyRange" placeholder="难度" size="small" style="width:120px">
              <el-option label="全部难度" :value="[1,5]" />
              <el-option label="基础 1-2" :value="[1,2]" />
              <el-option label="中等 3" :value="[3,3]" />
              <el-option label="提高 4-5" :value="[4,5]" />
            </el-select>
            <el-button size="small" type="primary" :loading="paperLoading" @click="generatePaperQuestions">生成试卷</el-button>
          </div>
          <div v-loading="qLoading" style="max-height:320px;overflow-y:auto;width:100%;border:1px solid #eee;border-radius:6px;padding:12px">
            <div style="color:#999;font-size:12px;margin-bottom:8px">已选 {{ selectedQuestions.length }} 题，共 {{ filteredQuestions.length }} 题</div>
            <el-checkbox-group v-model="selectedQuestions">
              <div v-for="q in filteredQuestions" :key="q.questionId" style="margin-bottom:8px;padding-bottom:8px;border-bottom:1px dashed #eee;text-align:left">
                <el-checkbox :value="q.questionId">{{ q.stem }}</el-checkbox>
                <span style="margin-left:8px"><el-tag size="small">{{ typeLabel(q.type) }}</el-tag><span style="color:#999;font-size:12px;margin-left:4px">{{ q.score }}分 | 难度{{ q.difficulty || '-' }} | 课时{{ q.lessonNo || '-' }} | {{ q.knowledgePoint || '未关联知识点' }}</span></span>
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
  generatePaper,
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
const paperLoading = ref(false)
const paperForm = reactive({ strategy: 'random', count: 10, types: [], knowledgePoints: [], difficultyRange: [1, 5] })

const filteredQuestions = computed(() => {
  return questionBank.value.filter(q => {
    if (qKeyword.value && !q.stem.includes(qKeyword.value) && !(q.knowledgePoint||'').includes(qKeyword.value)) return false
    if (qLesson.value && q.lessonNo !== qLesson.value) return false
    return true
  })
})

const knowledgeOptions = computed(() => {
  return [...new Set(questionBank.value.map(q => q.knowledgePoint).filter(Boolean))]
})

const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答',program:'编程'}[t]||t)

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

const generatePaperQuestions = async () => {
  paperLoading.value = true
  try {
    const [difficultyMin, difficultyMax] = paperForm.difficultyRange || [1, 5]
    const res = await generatePaper(code, {
      strategy: paperForm.strategy,
      count: paperForm.count,
      types: paperForm.types,
      knowledgePoints: paperForm.knowledgePoints,
      difficultyMin,
      difficultyMax
    })
    if (res.data.code === 200) {
      const generated = res.data.data || []
      questionBank.value = mergeQuestions(questionBank.value, generated)
      selectedQuestions.value = generated.map(q => q.questionId)
      ElMessage.success(`已生成 ${generated.length} 道题`)
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('组卷失败')
  } finally {
    paperLoading.value = false
  }
}

const mergeQuestions = (base, added) => {
  const map = new Map(base.map(q => [q.questionId, q]))
  added.forEach(q => map.set(q.questionId, q))
  return [...map.values()]
}

onMounted(async () => {
  try {
    const [lRes, cRes] = await Promise.all([getCourseLessons(code), searchCourse(code)])
    if (lRes.data.code === 200) lessons.value = lRes.data.data
    if (cRes.data.code === 200 && cRes.data.data.length > 0) courseName.value = cRes.data.data[0].courseName
  } catch {
    ElMessage.error('课时加载失败')
  } finally { loading.value = false }
  try {
    const tRes = await getTaskList(code)
    if (tRes.data.code === 200) tasks.value = tRes.data.data
  } catch {
    ElMessage.error('任务加载失败')
  } finally { taskLoading.value = false }
})

const reloadLessons = async () => {
  try {
    const r = await getCourseLessons(code)
    if (r.data.code === 200) lessons.value = r.data.data
    else ElMessage.error(r.data.msg)
  } catch {
    ElMessage.error('课时加载失败')
  }
}

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
  try {
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
  } catch {
    ElMessage.error('课时保存失败')
  }
}
const deleteLesson = async (lessonNo) => {
  try {
    await deleteLessonApi(code, lessonNo)
    ElMessage.success('已删除'); reloadLessons()
  } catch {
    ElMessage.error('课时删除失败')
  }
}

// 任务增删改
const reloadTasks = async () => {
  try {
    const r = await getTaskList(code)
    if (r.data.code === 200) tasks.value = r.data.data
    else ElMessage.error(r.data.msg)
  } catch {
    ElMessage.error('任务加载失败')
  }
}

const openTaskAdd = () => {
  isTaskEdit.value = false; taskFile.value = null
  Object.assign(taskForm, { taskNo: '', taskType: '', description: '', deadline: '', submitMethod: '', score: 0 })
  Object.assign(paperForm, { strategy: 'random', count: 10, types: [], knowledgePoints: [], difficultyRange: [1, 5] })
  taskDialog.value = true
}
const openTaskEdit = (row) => { isTaskEdit.value = true; Object.assign(taskForm, row); taskDialog.value = true }
const saveTask = async () => {
  if (taskForm.taskType === 'quiz' && !selectedQuestions.value.length) {
    ElMessage.error('请先选择题目或生成试卷')
    return
  }
  try {
    const fd = new FormData()
    fd.append('taskType', taskForm.taskType); fd.append('description', taskForm.description)
    fd.append('deadline', taskForm.deadline || ''); fd.append('submitMethod', taskForm.taskType==='quiz' ? '在线答题' : (taskForm.submitMethod || ''))
    fd.append('score', String(taskForm.score))
    if (taskFile.value) fd.append('file', taskFile.value)

    if (isTaskEdit.value) {
      const res = await updateTask(code, taskForm.taskNo, fd)
      if (res.data.code !== 200) {
        ElMessage.error(res.data.msg || '任务更新失败')
        return
      }
      ElMessage.success('已更新')
    } else {
      fd.append('courseCode', code)
      const created = await addTask(fd)
      if (created.data.code !== 200 || !created.data.data) {
        ElMessage.error(created.data.msg || '任务发布失败')
        return
      }
      // 测验：保存选题关联
      if (taskForm.taskType === 'quiz' && selectedQuestions.value.length) {
        const bindRes = await addQuestionsToTask(created.data.data, selectedQuestions.value)
        if (bindRes.data.code !== 200) {
          ElMessage.error(bindRes.data.msg || '试卷题目绑定失败')
          return
        }
      }
      ElMessage.success(taskForm.taskType === 'quiz' ? `发布成功，已绑定 ${selectedQuestions.value.length} 道题` : '发布成功')
    }
    taskDialog.value = false; taskFile.value = null; selectedQuestions.value = []; reloadTasks()
  } catch {
    ElMessage.error(isTaskEdit.value ? '任务更新失败，请稍后重试' : '任务发布失败，请稍后重试')
  }
}
const deleteTask = async (taskNo) => {
  try {
    await deleteTaskApi(code, taskNo)
    ElMessage.success('已删除'); reloadTasks()
  } catch {
    ElMessage.error('任务删除失败')
  }
}
</script>

<style scoped>
.paper-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
</style>
