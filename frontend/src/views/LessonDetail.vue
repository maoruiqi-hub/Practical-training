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
      </el-card>
      <el-empty v-if="!lesson && !loading" description="课时不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getLessonDetail } from '../api'

const route = useRoute()
const lesson = ref(null)
const loading = ref(true)

const resourceTag = (type) => {
  switch (type) {
    case 'video': return 'success'
    case 'ppt': return 'warning'
    case 'doc': return ''
    case 'img': return 'danger'
    default: return 'info'
  }
}

onMounted(async () => {
  try {
    const res = await getLessonDetail(route.params.lessonNo)
    if (res.data.code === 200) lesson.value = res.data.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-card h2 { font-size:22px; }
</style>
