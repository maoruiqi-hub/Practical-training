<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:12px">← 返回课程</el-button>
    <div class="page-head">
      <h3>课程资源</h3>
      <el-button v-if="userRole!=='student'" type="primary" plain @click="openCandidates">知识点候选</el-button>
    </div>
    <el-table v-loading="loading" :data="resources" empty-text="暂无课程资源">
      <el-table-column prop="title" label="资源名称" min-width="220" />
      <el-table-column prop="resourceType" label="类型" width="100" />
      <el-table-column prop="chapter" label="章节" min-width="150" />
      <el-table-column prop="uploadedAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="$router.push('/course-resource/' + row.resourceId + '/preview')">查看</el-button>
          <el-button v-if="userRole!=='student'" size="small" :loading="extractingId===row.resourceId" @click="extractResource(row)">抽取知识点</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="candidateDialog" title="知识点候选审核" width="760px">
      <el-table v-loading="candidateLoading" :data="candidates" empty-text="暂无待审核候选">
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="chapter" label="章节" width="120" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="importance" label="重要度" width="80" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="acceptCandidate(row)">采纳</el-button>
            <el-button size="small" type="danger" text @click="rejectCandidate(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { acceptKnowledgeCandidate, extractKnowledgeCandidates, getCourseResources, getKnowledgeExtractionCandidates, rejectKnowledgeCandidate } from '../api'

const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const resources = ref([])
const loading = ref(true)
const candidateDialog = ref(false)
const candidateLoading = ref(false)
const candidates = ref([])
const extractingId = ref('')

const loadResources = async () => {
  loading.value = true
  try {
    const response = await getCourseResources(route.params.code)
    if (response.data.code === 200) resources.value = response.data.data
    else ElMessage.error(response.data.msg)
  } catch {
    ElMessage.error('课程资源加载失败')
  } finally {
    loading.value = false
  }
}

const loadCandidates = async () => {
  candidateLoading.value = true
  try {
    const response = await getKnowledgeExtractionCandidates(route.params.code)
    if (response.data.code === 200) candidates.value = response.data.data || []
    else ElMessage.error(response.data.msg || '候选知识点加载失败')
  } catch {
    ElMessage.error('候选知识点加载失败')
  } finally {
    candidateLoading.value = false
  }
}

const openCandidates = async () => {
  candidateDialog.value = true
  await loadCandidates()
}

const extractResource = async (row) => {
  extractingId.value = row.resourceId
  try {
    const response = await extractKnowledgeCandidates(route.params.code, row.resourceId)
    if (response.data.code === 200) {
      candidates.value = response.data.data || []
      candidateDialog.value = true
      ElMessage.success('知识点抽取完成')
    } else ElMessage.error(response.data.msg || '知识点抽取失败')
  } catch {
    ElMessage.error('知识点抽取失败')
  } finally {
    extractingId.value = ''
  }
}

const acceptCandidate = async (row) => {
  const response = await acceptKnowledgeCandidate(route.params.code, row.candidateId, row)
  if (response.data.code === 200) {
    ElMessage.success('已采纳')
    loadCandidates()
  } else ElMessage.error(response.data.msg || '采纳失败')
}

const rejectCandidate = async (row) => {
  const response = await rejectKnowledgeCandidate(route.params.code, row.candidateId)
  if (response.data.code === 200) {
    ElMessage.success('已拒绝')
    loadCandidates()
  } else ElMessage.error(response.data.msg || '拒绝失败')
}

onMounted(loadResources)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.page-head h3 {
  margin: 0;
}
</style>
