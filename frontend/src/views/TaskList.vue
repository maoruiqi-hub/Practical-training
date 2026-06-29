<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">返回</el-button>

    <!-- 筛选栏 -->
    <el-card style="margin-bottom:16px">
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
    <el-table v-if="userRole==='student'" :data="tasks" style="width:100%" v-loading="loading" element-loading-text="正在加载任务..." empty-text="暂无任务">
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
    <el-table v-else :data="tasks" style="width:100%" v-loading="loading" element-loading-text="正在加载任务..." empty-text="暂无任务">
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, addTask, updateTask, deleteTask as apiDeleteTask, toggleTaskStatus } from '../api'

const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
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

const handleFile = (f) => { file.value = f.raw }

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getTaskList(route.params.courseCode)
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
