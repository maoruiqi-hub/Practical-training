<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:20px">← 返回</el-button>
    <div v-loading="loading" style="min-height:200px">
      <el-card v-if="lesson" class="detail-card">
        <template #header>
          <h2 style="margin:0">{{ lesson.lessonTitle }}</h2>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程名称">{{ lesson.courseName }}</el-descriptions-item>
          <el-descriptions-item label="授课教师">{{ lesson.teacherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资源类型">
            <el-tag :type="resourceTag(lesson.resourceType)">{{ lesson.resourceType }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="课时编号">{{ lesson.lessonNo }}</el-descriptions-item>
          <el-descriptions-item label="内容简介" :span="2">{{ lesson.description || '暂无简介' }}</el-descriptions-item>
          <el-descriptions-item label="资源文件" :span="2">
            <el-link v-if="lesson.resourceUrl" :href="'/practical-training/' + lesson.resourceUrl" target="_blank" type="primary">
              📎 {{ lesson.resourceUrl.split('/').pop() }}
            </el-link>
            <span v-else>无附件</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 视频播放区 -->
        <div v-if="lesson.resourceType === 'video' && lesson.resourceUrl" style="margin-top:20px">
          <h4>视频播放</h4>
          <video ref="videoRef" :src="'/practical-training/' + lesson.resourceUrl" controls
            style="width:100%;max-width:800px;border-radius:8px"
            @play="onVideoPlay" @pause="onVideoPause" @seeked="onVideoSeek" @ended="onVideoEnd" />
        </div>

        <!-- 文档类型：记录浏览 -->
        <div v-if="isDocType && lesson.resourceUrl" style="margin-top:20px">
          <el-alert title="资源已打开" type="info" :closable="false" show-icon>
            <template #default>
              正在浏览{{ lesson.resourceType?.toUpperCase() }}资源：
              <el-link :href="'/practical-training/' + lesson.resourceUrl" target="_blank" type="primary" @click="onResourceOpen">
                {{ lesson.resourceUrl.split('/').pop() }}
              </el-link>
            </template>
          </el-alert>
        </div>
      </el-card>
      <el-empty v-if="!lesson && !loading" description="课时不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getLessonDetail, reportBehaviorLog } from '../api'
import { getCurrentUser, getStudentId, getTeacherId } from '../utils/authContext'

const route = useRoute()
const lesson = ref(null)
const loading = ref(true)
const videoRef = ref(null)
const videoStartTime = ref(null)
const progressTimer = ref(null)
const logged = ref(false)

const user = getCurrentUser()
const isDocType = computed(() => ['ppt', 'doc', 'pdf'].includes(lesson.value?.resourceType))

const resourceTag = (type) => {
  switch (type) { case 'video': return 'success'; case 'ppt': return 'warning'; case 'doc': return ''; case 'img': return 'danger'; default: return 'info' }
}

const logAction = (actionType, duration) => {
  if (!lesson.value) return
  reportBehaviorLog({
    userId: user.role === 'student' ? getStudentId(user) : getTeacherId(user),
    userType: user.role || 'student',
    resourceType: lesson.value.resourceType || 'unknown',
    resourceId: lesson.value.lessonNo,
    actionType: actionType,
    duration: duration || 0,
    completionStatus: actionType === 'video_end' || actionType === 'doc_close' ? 'completed' : 'partial'
  }).catch(() => { /* 静默失败，不影响主流程 */ })
}

// 打开资源时记录一次日志
const logInitialView = () => {
  if (logged.value) return
  logged.value = true
  const type = lesson.value?.resourceType
  if (type === 'video') logAction('video_view')
  else if (type === 'ppt') logAction('ppt_view')
  else if (type === 'doc' || type === 'pdf') logAction('doc_view')
}

// 视频事件
const onVideoPlay = () => {
  videoStartTime.value = Date.now()
  logAction('video_play')
  // 每30秒上报一次播放进度
  progressTimer.value = setInterval(() => {
    if (videoRef.value && !videoRef.value.paused) {
      logAction('video_progress', 30)
    }
  }, 30000)
}

const onVideoPause = () => {
  clearInterval(progressTimer.value)
  const duration = videoStartTime.value ? Math.round((Date.now() - videoStartTime.value) / 1000) : 0
  logAction('video_pause', duration)
  videoStartTime.value = null
}

const onVideoSeek = () => {
  logAction('video_seek')
}

const onVideoEnd = () => {
  clearInterval(progressTimer.value)
  const duration = videoStartTime.value ? Math.round((Date.now() - videoStartTime.value) / 1000) : 0
  logAction('video_end', duration)
  videoStartTime.value = null
}

const onResourceOpen = () => {
  const type = lesson.value?.resourceType
  if (type === 'ppt') logAction('ppt_view')
  else if (type === 'doc' || type === 'pdf') logAction('doc_view')
}

onMounted(async () => {
  try {
    const res = await getLessonDetail(route.params.lessonNo)
    if (res.data.code === 200) {
      lesson.value = res.data.data
      logInitialView()
    }
  } finally { loading.value = false }
})

onBeforeUnmount(() => {
  clearInterval(progressTimer.value)
  if (videoRef.value && !videoRef.value.paused) {
    const duration = videoStartTime.value ? Math.round((Date.now() - videoStartTime.value) / 1000) : 0
    logAction('video_pause', duration)
  }
})
</script>

<style scoped>
.detail-card h2 { font-size:22px; }
</style>
