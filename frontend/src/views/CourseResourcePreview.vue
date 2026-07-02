<template>
  <div v-loading="loading" style="min-height:300px">
    <el-button @click="$router.back()" style="margin-bottom:12px">← 返回资源列表</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-else-if="preview">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <h3 style="margin:0">资源预览</h3>
        <el-link :href="downloadUrl" type="primary">下载原文件</el-link>
      </div>
      <iframe v-if="preview.previewType === 'pdf'" :src="contentUrl" title="PDF 预览" style="width:100%;height:70vh;border:1px solid #dcdfe6" />
      <video v-else-if="preview.previewType === 'video'" :src="contentUrl" controls style="width:100%;max-height:70vh" />
      <img v-else-if="preview.previewType === 'image'" :src="contentUrl" alt="课程资源" style="max-width:100%;max-height:70vh" />
      <iframe v-else-if="preview.previewType === 'text'" :src="contentUrl" title="文本预览" style="width:100%;height:70vh;border:1px solid #dcdfe6" />
      <el-empty v-else description="该格式暂不支持浏览器直接预览，请下载原文件查看" />
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCourseResourcePreview, recordCourseResourceView } from '../api'

const route = useRoute()
const preview = ref(null)
const loading = ref(true)
const error = ref('')
const startedAt = ref(0)
const contentUrl = computed(() => preview.value ? '/practical-training' + preview.value.previewUrl : '')
const downloadUrl = computed(() => preview.value ? '/practical-training' + preview.value.downloadUrl : '')

onMounted(async () => {
  try {
    const response = await getCourseResourcePreview(route.params.resourceId)
    if (response.data.code !== 200) {
      error.value = response.data.msg || '资源预览加载失败'
      return
    }
    preview.value = response.data.data
    startedAt.value = Date.now()
    await recordCourseResourceView(route.params.resourceId, { action: 'start' })
  } catch {
    error.value = '资源预览加载失败'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (!startedAt.value) return
  recordCourseResourceView(route.params.resourceId, { action: 'end', durationMs: Date.now() - startedAt.value }).catch(() => {})
})
</script>
