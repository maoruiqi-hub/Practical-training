<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:10px">返回</el-button>
    <el-table :data="tasks" style="width:100%" v-loading="loading">
      <el-table-column prop="taskNo" label="编号" width="80" />
      <el-table-column prop="taskType" label="类型" width="100" />
      <el-table-column prop="description" label="任务说明" />
      <el-table-column prop="deadline" label="截止时间" width="180" />
      <el-table-column prop="submitMethod" label="提交方式" width="100" />
      <el-table-column prop="score" label="分值" width="80" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="userRole==='student'" size="small" type="primary" @click="$router.push(`/task/${route.params.courseCode}/submit/${row.taskNo}`)">提交</el-button>
          <el-button v-else size="small" type="primary" @click="$router.push(`/task/${route.params.courseCode}/submit/${row.taskNo}`)">查看提交</el-button>
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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskList, addTask } from '../api'

const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const tasks = ref([])
const loading = ref(true)
const newTask = reactive({ taskType: '', description: '', deadline: '', submitMethod: '', score: 0 })

onMounted(async () => {
  try {
    const res = await getTaskList(route.params.courseCode)
    if (res.data.code === 200) tasks.value = res.data.data
  } finally { loading.value = false }
})

const publishTask = async () => {
  const res = await addTask({ ...newTask, courseCode: route.params.courseCode })
  if (res.data.code === 200) { ElMessage.success('发布成功'); location.reload() }
  else ElMessage.error(res.data.msg)
}
</script>
