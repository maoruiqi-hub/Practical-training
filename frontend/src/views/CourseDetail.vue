<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">← 返回</el-button>
    <h3>{{ courseName }}</h3>
    <el-tabs v-model="activeTab" style="margin-top:10px">
      <el-tab-pane label="课时列表" name="lessons">
        <div v-loading="loading" style="min-height:200px">
          <el-table :data="lessons" style="width:100%">
            <el-table-column prop="lessonNo" label="编号" width="80" />
            <el-table-column label="课时标题">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push('/lesson/' + row.lessonNo)">{{ row.lessonTitle }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="内容简介" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="$router.push('/lesson/' + row.lessonNo)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学习任务" name="tasks">
        <div v-loading="taskLoading" style="min-height:200px">
          <el-table :data="tasks" style="width:100%">
            <el-table-column prop="taskNo" label="编号" width="80" />
            <el-table-column prop="taskType" label="类型" width="100" />
            <el-table-column prop="description" label="任务说明" />
            <el-table-column prop="deadline" label="截止时间" width="180" />
            <el-table-column prop="submitMethod" label="提交方式" width="100" />
            <el-table-column prop="score" label="分值" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="userRole==='student'" size="small" type="primary" @click="$router.push('/task/' + code + '/submit/' + row.taskNo)">提交</el-button>
                <el-button v-else size="small" type="primary" @click="$router.push('/task/' + code + '/submit/' + row.taskNo)">查看提交</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-divider />
          <el-card v-if="userRole!=='student'" header="发布新任务">
            <el-form :model="newTask" inline>
              <el-form-item label="类型"><el-input v-model="newTask.taskType" /></el-form-item>
              <el-form-item label="说明"><el-input v-model="newTask.description" /></el-form-item>
              <el-form-item label="截止时间"><el-input v-model="newTask.deadline" placeholder="2026-07-15 23:59:59" /></el-form-item>
              <el-form-item label="提交方式"><el-input v-model="newTask.submitMethod" /></el-form-item>
              <el-form-item label="分值"><el-input-number v-model="newTask.score" :min="0" :max="100" /></el-form-item>
              <el-form-item><el-button type="primary" @click="publishTask">发布</el-button></el-form-item>
            </el-form>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseLessons, searchCourse, getTaskList, addTask } from '../api'

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
const newTask = reactive({ taskType: '', description: '', deadline: '', submitMethod: '', score: 0 })

onMounted(async () => {
  try {
    const [lRes, cRes] = await Promise.all([
      getCourseLessons(code),
      searchCourse(code)
    ])
    if (lRes.data.code === 200) lessons.value = lRes.data.data
    if (cRes.data.code === 200 && cRes.data.data.length > 0) {
      courseName.value = cRes.data.data[0].courseName
    }
  } finally { loading.value = false }

  try {
    const tRes = await getTaskList(code)
    if (tRes.data.code === 200) tasks.value = tRes.data.data
  } finally { taskLoading.value = false }
})

const publishTask = async () => {
  const res = await addTask({ ...newTask, courseCode: code })
  if (res.data.code === 200) { ElMessage.success('发布成功'); location.reload() }
  else ElMessage.error(res.data.msg)
}
</script>
