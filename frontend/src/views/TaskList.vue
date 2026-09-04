<template>
  <div>
    <section v-if="userRole==='student'" class="challenge-page" :style="challengePageStyle">
      <div class="challenge-shell">
        <header class="challenge-header">
          <div class="challenge-heading">
            <el-button class="challenge-back" @click="$router.back()">← 返回地图</el-button>
            <p class="challenge-kicker">活动挑战 · 任务大厅</p>
            <h1>今日挑战</h1>
            <p>完成老师布置的任务，收集经验，继续你的爬塔路线。</p>
          </div>
          <img class="challenge-emblem" :src="referenceTokenIcons.magicOrb" alt="" />
        </header>

        <div class="challenge-stats">
          <div><span>全部任务</span><strong>{{ challengeStats.total }}</strong></div>
          <div><span>待完成</span><strong>{{ challengeStats.pending }}</strong></div>
          <div><span>已完成</span><strong>{{ challengeStats.completed }}</strong></div>
        </div>

        <section class="challenge-toolbar">
          <div>
            <p class="challenge-section-kicker">QUEST BOARD</p>
            <h2>老师的任务</h2>
          </div>
          <el-select v-model="filters.taskType" clearable placeholder="全部类型" class="challenge-filter" @change="loadTasks">
            <el-option label="全部类型" value="" />
            <el-option label="编程作业" value="编程作业" />
            <el-option label="实验报告" value="实验报告" />
            <el-option label="课堂测验" value="课堂测验" />
            <el-option label="章节作业" value="章节作业" />
            <el-option label="测验套题" value="quiz" />
          </el-select>
        </section>

        <div v-loading="loading" class="challenge-content" element-loading-text="正在召集任务...">
          <div v-if="!loading && !tasks.length" class="challenge-empty">
            <img :src="referenceTokenIcons.magicOrb" alt="" />
            <h3>暂时没有活动挑战</h3>
            <p>老师发布新任务后，它会出现在这里。</p>
          </div>
          <div v-else class="challenge-grid">
            <article v-for="task in tasks" :key="task.taskNo" class="challenge-card" :class="challengeStatus(task)">
              <div class="challenge-card-top">
                <span class="challenge-type">{{ taskTypeLabel(task.taskType) }}</span>
                <span class="challenge-status">{{ challengeStatusLabel(task) }}</span>
              </div>
              <div class="challenge-card-icon"><img :src="referenceTokenIcons.magicOrb" alt="" /></div>
              <h3>{{ task.taskName || task.description || '未命名任务' }}</h3>
              <p class="challenge-description">{{ task.description || '老师暂未填写任务说明。' }}</p>
              <div class="challenge-meta">
                <span>⌛ {{ task.deadline || '不限时' }}</span>
                <span>✦ {{ task.score || 0 }} XP</span>
              </div>
              <el-button class="challenge-action" :disabled="task.status==='closed'" @click="openChallenge(task)">
                {{ challengeActionLabel(task) }} <span>→</span>
              </el-button>
            </article>
          </div>
        </div>
      </div>
    </section>

    <!-- 筛选栏 -->
    <el-card v-if="userRole!=='student'" style="margin-bottom:16px">
      <el-form :inline="true">
        <el-form-item label="任务类型">
          <el-select v-model="filters.taskType" clearable placeholder="全部类型" style="width:140px" @change="loadTasks">
            <el-option label="编程作业" value="编程作业" />
            <el-option label="实验报告" value="实验报告" />
            <el-option label="课堂测验" value="课堂测验" />
            <el-option label="章节作业" value="章节作业" />
            <el-option label="视频观看" value="video" />
            <el-option label="PPT阅读" value="reading" />
            <el-option label="课程报告" value="课程报告" />
            <el-option label="实践项目" value="实践项目" />
            <el-option label="测验套题" value="quiz" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userRole!=='student'" label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width:120px" @change="loadTasks">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTasks">筛选</el-button>
          <el-button @click="filters={}; loadTasks()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 学生端：按学生视角展示任务状态 -->
    <el-table v-if="false" :data="tasks" style="width:100%" v-loading="loading" element-loading-text="正在加载任务..." empty-text="暂无任务">
      <el-table-column prop="taskName" label="任务名称" min-width="140" />
      <el-table-column prop="taskType" label="类型" width="100" />
      <el-table-column prop="description" label="任务说明" min-width="200" show-overflow-tooltip />
      <el-table-column prop="deadline" label="截止时间" width="170" />
      <el-table-column prop="submitMethod" label="提交方式" width="100" />
      <el-table-column prop="score" label="分值" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status==='closed'" type="info">已关闭</el-tag>
          <el-tag v-else-if="row.studentStatus==='completed'" type="success">已完成</el-tag>
          <el-tag v-else-if="row.studentStatus==='submitted'" type="warning">待批改</el-tag>
          <el-tag v-else-if="row.studentStatus==='overdue'" type="danger">已逾期</el-tag>
          <el-tag v-else type="">待完成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.taskType==='quiz' || row.taskType==='课堂测验'" size="small" type="primary"
            @click="$router.push(`/quiz/take/${row.taskNo}`)" :disabled="row.status==='closed'">
            {{ row.studentStatus==='completed'||row.studentStatus==='submitted' ? '查看' : '答题' }}
          </el-button>
          <el-button v-else-if="row.taskType==='video'||row.taskType==='reading'" size="small" type="info" disabled>
            自动记录
          </el-button>
          <el-button v-else size="small" type="primary"
            @click="$router.push(`/task/${route.params.courseCode}/submit/${row.taskNo}`)" :disabled="row.status==='closed'">
            {{ row.studentStatus==='completed'||row.studentStatus==='submitted' ? '查看' : '提交' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 教师端：含编辑删除 -->
    <el-table v-else-if="userRole!=='student'" :data="tasks" style="width:100%" v-loading="loading" element-loading-text="正在加载任务..." empty-text="暂无任务">
      <el-table-column prop="taskNo" label="编号" width="70" />
      <el-table-column prop="taskName" label="任务名称" min-width="140" />
      <el-table-column prop="taskType" label="类型" width="100" />
      <el-table-column prop="description" label="任务说明" min-width="200" show-overflow-tooltip />
      <el-table-column prop="deadline" label="截止时间" width="170" />
      <el-table-column prop="submitMethod" label="提交方式" width="100" />
      <el-table-column prop="score" label="分值" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status==='draft'" type="info">草稿</el-tag>
          <el-tag v-else-if="row.status==='published'" type="success">已发布</el-tag>
          <el-tag v-else-if="row.status==='closed'" type="danger">已关闭</el-tag>
          <el-tag v-else type="info">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="$router.push(`/task/${route.params.courseCode}/submit/${row.taskNo}`)">查看提交</el-button>
          <el-button size="small" type="warning" @click="openEditDialog(row)">编辑</el-button>
          <el-dropdown @command="(cmd) => handleStatus(row, cmd)" style="margin-left:4px">
            <el-button size="small">状态 ▾</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="draft" :disabled="row.status==='draft'">设为草稿</el-dropdown-item>
                <el-dropdown-item command="published" :disabled="row.status==='published'">设为发布</el-dropdown-item>
                <el-dropdown-item command="closed" :disabled="row.status==='closed'">设为关闭</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="small" type="danger" @click="doDeleteTask(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider />

    <!-- 教师发布新任务（增强版） -->
    <el-card v-if="userRole!=='student'" header="发布新任务">
      <el-form :model="newTask" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" required><el-input v-model="newTask.taskName" placeholder="如：第一章Python基础作业" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务类型" required>
              <el-select v-model="newTask.taskType" style="width:100%">
                <el-option label="章节作业" value="章节作业" />
                <el-option label="编程作业" value="编程作业" />
                <el-option label="课堂测验" value="课堂测验" />
                <el-option label="测验套题" value="quiz" />
                <el-option label="实验报告" value="实验报告" />
                <el-option label="课程报告" value="课程报告" />
                <el-option label="视频观看" value="video" />
                <el-option label="PPT阅读" value="reading" />
                <el-option label="实践项目" value="实践项目" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务说明" required><el-input v-model="newTask.description" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="截止时间"><el-date-picker v-model="newTask.deadline" type="datetime" placeholder="选择截止时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="提交方式" required><el-input v-model="newTask.submitMethod" placeholder="在线提交/文档上传/在线答题" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分值" required><el-input-number v-model="newTask.score" :min="0" :max="100" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="允许逾期"><el-switch v-model="newTask.allowLateBool" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大提交次数"><el-input-number v-model="newTask.maxAttempts" :min="1" :max="10" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="附件格式"><el-input v-model="newTask.attachmentFormats" placeholder=".pdf,.doc,.zip" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="关联章节"><el-input v-model="newTask.lessonNo" placeholder="章节编号" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联知识点"><el-input v-model="newTask.knowledgePoints" placeholder='如["函数","循环"]' /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分规则"><el-input v-model="newTask.gradingRule" placeholder="评分细则" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="初始状态">
              <el-radio-group v-model="newTask.status">
                <el-radio value="draft">草稿</el-radio>
                <el-radio value="published">发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="附件"><el-upload :auto-upload="false" :limit="1" :on-change="handleFile">
              <el-button size="small">选择文件</el-button>
            </el-upload></el-form-item>
          </el-col>
        </el-row>
        <el-form-item><el-button type="primary" @click="publishTask" size="large">发布任务</el-button></el-form-item>
      </el-form>
    </el-card>

    <!-- 编辑任务弹窗 -->
    <el-dialog v-model="editVisible" title="编辑任务" width="700px">
      <el-form v-if="editTask" :model="editTask" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="任务名称"><el-input v-model="editTask.taskName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="任务类型"><el-input v-model="editTask.taskType" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="任务说明"><el-input v-model="editTask.description" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="截止时间"><el-date-picker v-model="editTask.deadline" type="datetime" placeholder="选择截止时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="提交方式"><el-input v-model="editTask.submitMethod" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="分值"><el-input-number v-model="editTask.score" :min="0" :max="100" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="允许逾期"><el-switch v-model="editTask.allowLateBool" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最大提交次数"><el-input-number v-model="editTask.maxAttempts" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="附件格式"><el-input v-model="editTask.attachmentFormats" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="评分规则"><el-input v-model="editTask.gradingRule" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="doEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentUser, getStudentId } from '../utils/authContext'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, addTask, updateTask, deleteTask as apiDeleteTask, toggleTaskStatus } from '../api'
import { gameBackgrounds, referenceTokenIcons } from '../data/gameAssetManifest'

const route = useRoute()
const router = useRouter()
const user = getCurrentUser()
const userRole = user.role
const tasks = ref([])
const loading = ref(true)
const filters = reactive({ taskType: '', status: '' })
const file = ref(null)

const newTask = reactive({
  taskName: '', taskType: '', description: '', deadline: '', submitMethod: '', score: 0,
  lessonNo: '', knowledgePoints: '', gradingRule: '', status: 'published',
  allowLateBool: false, maxAttempts: 1, attachmentFormats: ''
})

const editVisible = ref(false)
const editTask = ref(null)

const challengePageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .22), rgba(6, 8, 12, .78)), url(${gameBackgrounds.mapAct1})`
}))

const challengeStats = computed(() => ({
  total: tasks.value.length,
  pending: tasks.value.filter(task => !['completed', 'submitted'].includes(task.studentStatus) && task.status !== 'closed').length,
  completed: tasks.value.filter(task => ['completed', 'submitted'].includes(task.studentStatus)).length
}))

const taskTypeLabel = type => ({
  quiz: '测验套题',
  课堂测验: '课堂测验',
  编程作业: '编程作业',
  实验报告: '实验报告',
  章节作业: '章节作业',
  video: '视频学习',
  reading: '资料阅读'
})[type] || type || '学习任务'

const challengeStatus = task => {
  if (task.status === 'closed') return 'closed'
  if (task.studentStatus === 'completed') return 'completed'
  if (task.studentStatus === 'submitted') return 'submitted'
  if (task.studentStatus === 'overdue') return 'overdue'
  return 'pending'
}

const challengeStatusLabel = task => ({
  closed: '已关闭',
  completed: '已完成',
  submitted: '待批改',
  overdue: '已逾期',
  pending: '待挑战'
})[challengeStatus(task)]

const challengeActionLabel = task => {
  if (task.status === 'closed') return '挑战已关闭'
  if (['completed', 'submitted'].includes(task.studentStatus)) return '查看记录'
  if (task.taskType === 'quiz' || task.taskType === '课堂测验') return '开始答题'
  return '完成作业'
}

const openChallenge = task => {
  if (task.taskType === 'quiz' || task.taskType === '课堂测验') {
    router.push(`/quiz/take/${task.taskNo}`)
    return
  }
  router.push(`/task/${route.params.courseCode}/submit/${task.taskNo}`)
}

const handleFile = (f) => { file.value = f.raw }

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getTaskList(route.params.courseCode, {
      taskType: filters.taskType,
      status: userRole === 'student' ? '' : filters.status,
      studentId: userRole === 'student' ? getStudentId(user) : ''
    })
    if (res.data.code === 200) {
      tasks.value = res.data.data
      // 学生端加载每个任务的状态
      if (userRole === 'student') {
        // 简单标注状态（后端可以在list接口中直接提供）
        tasks.value.forEach(t => {
          if (t.status === 'closed') t.studentStatus = 'closed'
          // studentStatus 由后端在列表返回中包含
        })
      }
    } else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('任务加载失败')
  } finally { loading.value = false }
}

onMounted(loadTasks)

const publishTask = async () => {
  if (!newTask.taskName || !newTask.taskType || !newTask.submitMethod) {
    ElMessage.warning('请填写任务名称、类型和提交方式')
    return
  }
  const fd = new FormData()
  fd.append('courseCode', route.params.courseCode)
  fd.append('taskName', newTask.taskName)
  fd.append('taskType', newTask.taskType)
  fd.append('description', newTask.description)
  fd.append('deadline', newTask.deadline)
  fd.append('submitMethod', newTask.submitMethod)
  fd.append('score', newTask.score)
  fd.append('lessonNo', newTask.lessonNo || '')
  fd.append('knowledgePoints', newTask.knowledgePoints || '')
  fd.append('gradingRule', newTask.gradingRule || '')
  fd.append('status', newTask.status)
  fd.append('allowLate', newTask.allowLateBool ? '1' : '0')
  fd.append('maxAttempts', newTask.maxAttempts)
  fd.append('attachmentFormats', newTask.attachmentFormats || '')
  if (file.value) fd.append('file', file.value)

  try {
    const res = await addTask(fd)
    if (res.data.code === 200) { ElMessage.success('发布成功'); loadTasks(); resetForm() }
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('发布失败')
  }
}

const resetForm = () => {
  Object.assign(newTask, { taskName: '', taskType: '', description: '', deadline: '', submitMethod: '', score: 0,
    lessonNo: '', knowledgePoints: '', gradingRule: '', status: 'published', allowLateBool: false, maxAttempts: 1, attachmentFormats: '' })
  file.value = null
}

const openEditDialog = (row) => {
  editTask.value = {
    ...row,
    allowLateBool: row.allowLate === 1
  }
  editVisible.value = true
}

const doEdit = async () => {
  const t = editTask.value
  const body = {
    taskName: t.taskName || t.description,
    taskType: t.taskType,
    description: t.description,
    deadline: t.deadline,
    submitMethod: t.submitMethod,
    score: t.score,
    lessonNo: t.lessonNo || null,
    knowledgePoints: t.knowledgePoints || null,
    gradingRule: t.gradingRule || null,
    status: t.status,
    allowLate: t.allowLateBool ? 1 : 0,
    maxAttempts: t.maxAttempts || 1,
    attachmentFormats: t.attachmentFormats || null
  }

  try {
    const res = await updateTask(t.courseCode || route.params.courseCode, t.taskNo, body)
    if (res.data.code === 200) { ElMessage.success('修改成功'); editVisible.value = false; loadTasks() }
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('修改失败')
  }
}

const handleStatus = async (row, newStatus) => {
  try {
    const res = await toggleTaskStatus(row.courseCode || route.params.courseCode, row.taskNo, newStatus)
    if (res.data.code === 200) { ElMessage.success(res.data.data || '状态已更新'); loadTasks() }
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('状态更新失败')
  }
}

const doDeleteTask = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除任务「${row.taskName || row.description}」吗？`,
      '删除任务',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await apiDeleteTask(row.courseCode || route.params.courseCode, row.taskNo, true)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      await loadTasks()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('Delete error:', e)
      ElMessage.error('删除失败，请重试')
    }
  }
}
</script>

<style scoped>
.challenge-page {
  min-height: 100vh;
  margin: -20px;
  padding: 32px 28px 48px;
  color: #f8edcf;
  background-position: center;
  background-size: cover;
  background-attachment: fixed;
}

.challenge-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.challenge-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 190px;
  padding: 12px 44px 26px;
  border-bottom: 1px solid rgba(232, 184, 91, .3);
}

.challenge-back {
  margin-bottom: 24px;
  border-color: rgba(238, 181, 91, .36);
  color: #f8ebcb;
  background: rgba(255, 255, 255, .08);
}

.challenge-kicker,
.challenge-section-kicker {
  margin: 0 0 8px;
  color: #dfa54f;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .16em;
}

.challenge-heading h1 {
  margin: 0;
  color: #fff5d6;
  font-family: Georgia, serif;
  font-size: clamp(34px, 5vw, 56px);
  letter-spacing: .08em;
  text-shadow: 0 4px 18px rgba(0, 0, 0, .76);
}

.challenge-heading > p:last-child {
  margin: 10px 0 0;
  color: #dec8a4;
  font-size: 15px;
}

.challenge-emblem {
  width: 150px;
  height: 150px;
  object-fit: contain;
  filter: drop-shadow(0 16px 20px rgba(0, 0, 0, .5));
  opacity: .92;
}

.challenge-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin: 22px 0 30px;
}

.challenge-stats > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 16px 20px;
  border: 1px solid rgba(232, 184, 91, .26);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(42, 23, 19, .75), rgba(8, 10, 14, .78));
  box-shadow: 0 12px 30px rgba(0, 0, 0, .28);
}

.challenge-stats span {
  color: #cdb58e;
  font-size: 13px;
}

.challenge-stats strong {
  color: #ffda85;
  font-family: Georgia, serif;
  font-size: 28px;
}

.challenge-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.challenge-toolbar h2 {
  margin: 0;
  color: #fff5d6;
  font-family: Georgia, serif;
  font-size: 26px;
}

.challenge-filter {
  width: 160px;
}

.challenge-content {
  min-height: 300px;
}

.challenge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.challenge-card {
  position: relative;
  min-height: 280px;
  padding: 20px 22px 18px;
  overflow: hidden;
  border: 1px solid rgba(232, 184, 91, .28);
  border-radius: 10px;
  background: linear-gradient(145deg, rgba(55, 29, 21, .9), rgba(9, 11, 15, .9));
  box-shadow: 0 18px 44px rgba(0, 0, 0, .34);
}

.challenge-card::after {
  position: absolute;
  right: -40px;
  bottom: -50px;
  width: 170px;
  height: 170px;
  border: 1px solid rgba(232, 184, 91, .12);
  border-radius: 50%;
  content: '';
  box-shadow: 0 0 0 18px rgba(232, 184, 91, .04), 0 0 0 36px rgba(232, 184, 91, .03);
}

.challenge-card.completed { border-color: rgba(118, 191, 122, .48); }
.challenge-card.submitted { border-color: rgba(232, 184, 91, .62); }
.challenge-card.overdue { border-color: rgba(220, 86, 62, .58); }
.challenge-card.closed { opacity: .62; }

.challenge-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.challenge-type,
.challenge-status {
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.challenge-type {
  color: #e9bd6d;
  background: rgba(232, 184, 91, .12);
}

.challenge-status {
  color: #d9c4a2;
  background: rgba(255, 255, 255, .08);
}

.completed .challenge-status { color: #a7d7a9; background: rgba(118, 191, 122, .14); }
.submitted .challenge-status { color: #ffda85; background: rgba(232, 184, 91, .14); }
.overdue .challenge-status { color: #ff9d83; background: rgba(220, 86, 62, .14); }

.challenge-card-icon {
  position: absolute;
  top: 42px;
  right: 18px;
  width: 68px;
  height: 68px;
  opacity: .46;
}

.challenge-card-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.challenge-card h3 {
  position: relative;
  z-index: 1;
  max-width: calc(100% - 68px);
  margin: 28px 0 8px;
  color: #fff1c9;
  font-family: Georgia, serif;
  font-size: 23px;
}

.challenge-description {
  position: relative;
  z-index: 1;
  min-height: 44px;
  margin: 0;
  color: #cdbb9d;
  line-height: 1.7;
}

.challenge-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 16px 0;
  color: #d9b875;
  font-size: 12px;
}

.challenge-action {
  position: relative;
  z-index: 1;
  width: 100%;
  border-color: #da9a4d;
  color: #fff5d6;
  background: linear-gradient(180deg, #b75b28, #73301f);
}

.challenge-action span {
  margin-left: 8px;
  font-size: 18px;
}

.challenge-empty {
  padding: 70px 20px;
  text-align: center;
  border: 1px dashed rgba(232, 184, 91, .34);
  border-radius: 10px;
  background: rgba(12, 12, 15, .58);
}

.challenge-empty img {
  width: 88px;
  height: 88px;
  object-fit: contain;
  opacity: .7;
}

.challenge-empty h3 {
  margin: 12px 0 8px;
  color: #fff1c9;
  font-family: Georgia, serif;
}

.challenge-empty p { margin: 0; color: #cdbb9d; }

@media (max-width: 760px) {
  .challenge-page { margin: -16px; padding: 20px 16px 36px; }
  .challenge-header { min-height: 150px; padding: 8px 12px 20px; }
  .challenge-emblem { width: 92px; height: 92px; }
  .challenge-heading > p:last-child { max-width: 250px; font-size: 13px; }
  .challenge-stats { gap: 8px; }
  .challenge-stats > div { display: block; padding: 12px; }
  .challenge-stats strong { display: block; margin-top: 4px; }
  .challenge-toolbar { align-items: start; flex-direction: column; }
  .challenge-filter { width: 100%; }
  .challenge-grid { grid-template-columns: 1fr; }
}
</style>
