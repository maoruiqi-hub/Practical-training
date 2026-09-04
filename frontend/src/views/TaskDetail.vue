<template>
  <div class="task-detail-page" :style="pageStyle">
    <div class="task-detail-shell">
    <el-button class="detail-back" @click="$router.back()">← 返回活动挑战</el-button>
    <div v-loading="loading" element-loading-text="正在加载任务详情..." style="min-height:200px">
      <el-card v-if="task" class="detail-card">
        <template #header><h3 style="margin:0">{{ task.description || task.taskType }}</h3></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务编号">{{ task.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ task.taskType }}</el-descriptions-item>
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail, reportBehaviorLog } from '../api'
import { gameBackgrounds } from '../data/gameAssetManifest'
import { getCurrentUser, getStudentId, getTeacherId } from '../utils/authContext'

const route = useRoute()
const task = ref(null)
const loading = ref(true)
const pageStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(6, 8, 12, .2), rgba(6, 8, 12, .82)), url(${gameBackgrounds.mapAct1})`
}))

const user = getCurrentUser()

onMounted(async () => {
  try {
    const res = await getTaskDetail(route.params.taskNo)
    if (res.data.code === 200) {
      task.value = res.data.data
      // 记录查看任务详情的行为日志
      reportBehaviorLog({
        userId: user.role === 'student' ? getStudentId(user) : getTeacherId(user),
        userType: user.role || 'student',
        resourceType: 'task',
        resourceId: route.params.taskNo,
        taskNo: route.params.taskNo,
        actionType: 'task_view'
      }).catch(() => {})
    }
  } finally { loading.value = false }
})
</script>

<style scoped>
.task-detail-page { min-height: 100vh; margin: -20px; padding: 28px; color: #f8edcf; background-position: center; background-size: cover; background-attachment: fixed; }
.task-detail-shell { width: min(980px, 100%); margin: 0 auto; }
.detail-back { margin-bottom: 18px; border-color: rgba(238, 181, 91, .36); color: #f8ebcb; background: rgba(255, 255, 255, .08); }
.detail-card { border: 1px solid rgba(232, 184, 91, .3); border-radius: 10px; background: linear-gradient(145deg, rgba(55, 29, 21, .92), rgba(9, 11, 15, .92)); box-shadow: 0 18px 44px rgba(0, 0, 0, .34); }
.detail-card :deep(.el-card__header) { border-bottom-color: rgba(232, 184, 91, .22); }
.detail-card :deep(h3) { color: #fff1c9; font-family: Georgia, serif; font-size: 25px; }
.detail-card :deep(.el-descriptions__label) { color: #d8b779; background: rgba(232, 184, 91, .08); }
.detail-card :deep(.el-descriptions__content) { color: #eadfc8; background: rgba(8, 10, 14, .3); }
@media (max-width: 760px) { .task-detail-page { margin: -16px; padding: 20px 16px 40px; } }
</style>
