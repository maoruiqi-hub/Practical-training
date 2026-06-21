<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>
    <h3>{{ courseName }}</h3>
    <el-tabs v-model="activeTab" style="margin-top:10px">
      <!-- 课时列表 -->
      <el-tab-pane label="课时列表" name="lessons">
        <div v-loading="loading" style="min-height:200px">
          <div v-if="userRole!=='student'" style="margin-bottom:12px">
            <el-button type="success" @click="openLessonAdd">新增课时</el-button>
          </div>
          <el-table :data="lessons" style="width:100%">
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
        <div v-loading="taskLoading" style="min-height:200px">
          <div v-if="userRole!=='student'" style="margin-bottom:12px">
            <el-button type="success" @click="openTaskAdd">发布任务</el-button>
          </div>
          <el-table :data="tasks" style="width:100%">
            <el-table-column prop="taskNo" label="编号" width="80" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push('/task/detail/' + row.taskNo)">{{ row.taskType }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="任务说明" />
            <el-table-column prop="deadline" label="截止时间" width="180" />
            <el-table-column prop="submitMethod" label="提交方式" width="100" />
            <el-table-column prop="score" label="分值" width="80" />
            <el-table-column label="操作" :width="userRole==='student' ? 100 : 240">
              <template #default="{ row }">
                <el-button v-if="userRole==='student'" size="small" type="primary" @click="$router.push('/task/' + code + '/submit/' + row.taskNo)">提交</el-button>
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
    <el-dialog v-model="taskDialog" :title="isTaskEdit ? '编辑任务' : '发布任务'">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="类型"><el-input v-model="taskForm.taskType" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="taskForm.description" /></el-form-item>
        <el-form-item label="截止时间"><el-input v-model="taskForm.deadline" placeholder="2026-07-15 23:59:59" /></el-form-item>
        <el-form-item label="提交方式"><el-input v-model="taskForm.submitMethod" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="taskForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="附件"><el-upload :auto-upload="false" :limit="1" :on-change="handleTaskFile" accept="*"><el-button type="primary">选择文件</el-button></el-upload></el-form-item>
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseLessons, searchCourse, getTaskList, addTask } from '../api'
import axios from 'axios'

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
    await axios.put(`/practical-training/lesson/${code}/${lessonForm.lessonNo}`, fd)
  } else {
    await axios.post('/practical-training/lesson', fd)
  }
  ElMessage.success(isLessonEdit.value ? '已更新' : '已新增')
  lessonDialog.value = false; reloadLessons()
}
const deleteLesson = async (lessonNo) => {
  await axios.delete(`/practical-training/lesson/${code}/${lessonNo}`)
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
  fd.append('deadline', taskForm.deadline || ''); fd.append('submitMethod', taskForm.submitMethod)
  fd.append('score', String(taskForm.score))
  if (taskFile.value) fd.append('file', taskFile.value)

  if (isTaskEdit.value) {
    await axios.put(`/practical-training/task/${code}/${taskForm.taskNo}`, fd)
    ElMessage.success('已更新')
  } else {
    fd.append('courseCode', code)
    await axios.post('/practical-training/task', fd)
    ElMessage.success('发布成功')
  }
  taskDialog.value = false; taskFile.value = null; reloadTasks()
}
const deleteTask = async (taskNo) => {
  await axios.delete(`/practical-training/task/${code}/${taskNo}`)
  ElMessage.success('已删除'); reloadTasks()
}
</script>
