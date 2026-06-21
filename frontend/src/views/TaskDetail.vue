<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:20px">← 返回</el-button>
    <el-card v-if="task" v-loading="loading">
      <template #header><h3 style="margin:0">{{ task.taskType }}</h3></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务编号">{{ task.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ task.deadline || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="提交方式">{{ task.submitMethod }}</el-descriptions-item>
        <el-descriptions-item label="分值">{{ task.score }}分</el-descriptions-item>
        <el-descriptions-item label="任务说明" :span="2">{{ task.description || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="附件资源" :span="2">
          <el-link v-if="task.resourceUrl" :href="'/practical-training/' + task.resourceUrl" target="_blank" type="primary">
            📎 {{ task.resourceUrl.split('/').pop() }}
          </el-link>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-empty v-if="!task && !loading" description="任务不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail } from '../api'

const route = useRoute()
const task = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getTaskDetail(route.params.taskNo)
    if (res.data.code === 200) task.value = res.data.data
  } finally { loading.value = false }
})
</script>
