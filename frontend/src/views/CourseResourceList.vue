<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:12px">← 返回课程</el-button>
    <h3>课程资源</h3>
    <el-table v-loading="loading" :data="resources" empty-text="暂无课程资源">
      <el-table-column prop="title" label="资源名称" min-width="220" />
      <el-table-column prop="resourceType" label="类型" width="100" />
      <el-table-column prop="chapter" label="章节" min-width="150" />
      <el-table-column prop="uploadedAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="$router.push('/course-resource/' + row.resourceId + '/preview')">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseResources } from '../api'

const route = useRoute()
const resources = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await getCourseResources(route.params.code)
    if (response.data.code === 200) resources.value = response.data.data
    else ElMessage.error(response.data.msg)
  } catch {
    ElMessage.error('课程资源加载失败')
  } finally {
    loading.value = false
  }
})
</script>
