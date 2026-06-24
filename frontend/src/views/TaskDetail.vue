<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:20px">← 返回</el-button>
    <div v-loading="loading" element-loading-text="正在加载任务详情..." style="min-height:200px">
      <el-card v-if="task">
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
      <el-card v-if="userRole === 'student' && mySubmission" class="submission-card">
        <template #header><h4 style="margin:0">我的提交与反馈</h4></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提交状态">
            <el-tag :type="mySubmission.status === 'graded' ? 'success' : 'warning'">{{ mySubmission.status === 'graded' ? '已完成' : '待复核' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">{{ mySubmission.score ?? '暂未评分' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ mySubmission.submitTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交内容">{{ mySubmission.content || '附件提交' }}</el-descriptions-item>
          <el-descriptions-item label="教师反馈" :span="2">{{ mySubmission.feedback || '暂无反馈' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="aiReview" class="ai-review-box">
          <div class="ai-title">
            <b>智能修改建议</b>
            <el-tag size="small" effect="plain">{{ aiReview.aiScore ?? '-' }} 分建议</el-tag>
          </div>
          <p>{{ aiReview.summary || '暂无评价摘要' }}</p>
          <div v-for="(item, index) in aiSuggestions" :key="index" class="suggestion-item">- {{ item }}</div>
        </div>
        <el-empty v-else description="暂无智能修改建议" :image-size="48" />
      </el-card>
      <el-empty v-if="userRole === 'student' && task && !mySubmission && !submissionLoading" description="你还没有提交该任务" />
      <el-empty v-if="!task && !loading" description="任务不存在" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail, getMySubmissions, getAiReview } from '../api'

const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const task = ref(null)
const loading = ref(true)
const submissionLoading = ref(false)
const mySubmission = ref(null)
const aiReview = ref(null)

const aiSuggestions = computed(() => parseJson(aiReview.value?.suggestions, []))

onMounted(async () => {
  try {
    const res = await getTaskDetail(route.params.taskNo)
    if (res.data.code === 200) task.value = res.data.data
  } finally { loading.value = false }
  if (userRole === 'student') loadMySubmission()
})

const loadMySubmission = async () => {
  submissionLoading.value = true
  try {
    const res = await getMySubmissions()
    if (res.data.code === 200) {
      mySubmission.value = (res.data.data || []).find(item => String(item.taskNo) === String(route.params.taskNo)) || null
      if (mySubmission.value) loadAiReview(mySubmission.value.submissionId)
    }
  } finally {
    submissionLoading.value = false
  }
}

const loadAiReview = async (submissionId) => {
  try {
    const res = await getAiReview(submissionId)
    if (res.data.code === 200) aiReview.value = res.data.data
  } catch {
    aiReview.value = null
  }
}

const parseJson = (value, fallback) => {
  if (!value) return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}
</script>

<style scoped>
.submission-card {
  margin-top: 16px;
}
.ai-review-box {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
.ai-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.ai-review-box p {
  margin: 6px 0;
  color: #606266;
}
.suggestion-item {
  line-height: 1.7;
  color: #303133;
}
</style>
