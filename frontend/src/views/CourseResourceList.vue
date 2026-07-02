<template>
  <div>
    <el-button @click="$router.back()" style="margin-bottom:12px">← 返回课程</el-button>
    <div class="page-head">
      <h3>课程资源</h3>
      <div v-if="userRole!=='student'" class="head-actions">
        <el-button type="primary" @click="openUploadDialog">上传资源</el-button>
        <el-button type="primary" plain @click="openCandidates">知识点候选</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="resources" empty-text="暂无课程资源">
      <el-table-column prop="title" label="资源名称" min-width="220" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.sourceType === 'lesson' ? 'success' : 'info'">
            {{ resourceTypeLabel(row.resourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.sourceType === 'lesson' ? '' : 'warning'">
            {{ row.sourceType === 'lesson' ? '课时资源' : '上传资源' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chapter" label="章节" min-width="150" />
      <el-table-column prop="uploadedAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="viewResource(row)">查看</el-button>
          <el-button
            v-if="userRole!=='student' && row.sourceType !== 'lesson'"
            size="small"
            :loading="extractingId===row.resourceId"
            @click="extractResource(row)"
          >
            抽取知识点
          </el-button>
          <el-popconfirm
            v-if="userRole!=='student' && row.sourceType !== 'lesson'"
            title="确认删除该上传资源？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="deleteResource(row)"
          >
            <template #reference>
              <el-button size="small" type="danger" plain :loading="deletingId===row.resourceId">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="uploadDialog" title="上传课程资源" width="560px">
      <el-form :model="uploadForm" label-width="96px">
        <el-form-item label="资源名称">
          <el-input v-model="uploadForm.title" placeholder="不填则使用文件名" />
        </el-form-item>
        <el-form-item label="章节">
          <el-input v-model="uploadForm.chapter" placeholder="如：第 1 章 / 课时 3" />
        </el-form-item>
        <el-form-item label="关联知识点">
          <el-select v-model="uploadForm.knowledgePointId" clearable filterable placeholder="可选" style="width:100%">
            <el-option
              v-for="point in knowledgePoints"
              :key="point.knowledgePointId"
              :label="point.name"
              :value="point.knowledgePointId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            :file-list="uploadFileList"
            accept=".ppt,.pptx,.pdf,.doc,.docx,.mp4,.webm,.mov,.png,.jpg,.jpeg,.gif,.webp,.txt,.md"
            :on-change="handleUploadFile"
            :on-remove="removeUploadFile"
            :on-exceed="handleUploadExceed"
          >
            <el-button>选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialog=false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  acceptKnowledgeCandidate,
  deleteCourseResource,
  extractKnowledgeCandidates,
  getCourseLessons,
  getCourseResources,
  getKnowledgePoints,
  getKnowledgeExtractionCandidates,
  rejectKnowledgeCandidate,
  uploadCourseResource
} from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const userRole = user.role
const resources = ref([])
const loading = ref(true)
const candidateDialog = ref(false)
const candidateLoading = ref(false)
const candidates = ref([])
const extractingId = ref('')
const deletingId = ref('')
const uploadDialog = ref(false)
const uploading = ref(false)
const uploadFileList = ref([])
const knowledgePoints = ref([])
const uploadForm = ref({
  title: '',
  chapter: '',
  knowledgePointId: '',
  file: null
})

const resourceTypeLabel = (type) => {
  const labels = {
    video: '视频',
    pdf: 'PDF',
    ppt: 'PPT',
    word: '文档',
    image: '图片',
    text: '文本'
  }
  return labels[type] || type || '-'
}

const normalizeUploadedResources = (items = []) => items.map((item) => ({
  ...item,
  sourceType: 'upload',
  uploadedAt: item.uploadedAt || '-'
}))

const normalizeLessonResources = (lessons = []) => lessons
  .filter((lesson) => lesson.resourceUrl)
  .map((lesson) => ({
    resourceId: `lesson-${lesson.lessonNo}`,
    sourceType: 'lesson',
    lessonNo: lesson.lessonNo,
    title: lesson.lessonTitle,
    resourceType: lesson.resourceType || 'video',
    chapter: `课时 ${lesson.lessonNo}`,
    uploadedAt: '-',
    resourceUrl: lesson.resourceUrl
  }))

const loadResources = async () => {
  loading.value = true
  try {
    const [resourceResult, lessonResult] = await Promise.allSettled([
      getCourseResources(route.params.code),
      getCourseLessons(route.params.code)
    ])

    const uploadedResources = resourceResult.status === 'fulfilled' && resourceResult.value.data.code === 200
      ? normalizeUploadedResources(resourceResult.value.data.data || [])
      : []
    const lessonResources = lessonResult.status === 'fulfilled' && lessonResult.value.data.code === 200
      ? normalizeLessonResources(lessonResult.value.data.data || [])
      : []

    resources.value = [...lessonResources, ...uploadedResources]

    if (resourceResult.status === 'rejected' && lessonResult.status === 'rejected') {
      ElMessage.error('课程资源加载失败')
    } else if (resourceResult.status === 'rejected') {
      ElMessage.warning('上传资源加载失败')
    } else if (lessonResult.status === 'rejected') {
      ElMessage.warning('课时资源加载失败')
    } else if (resourceResult.status === 'fulfilled' && resourceResult.value.data.code !== 200) {
      ElMessage.warning(resourceResult.value.data.msg || '上传资源加载失败')
    } else if (lessonResult.status === 'fulfilled' && lessonResult.value.data.code !== 200) {
      ElMessage.warning(lessonResult.value.data.msg || '课时资源加载失败')
    }
  } catch {
    ElMessage.error('课程资源加载失败')
  } finally {
    loading.value = false
  }
}

const viewResource = (row) => {
  if (row.sourceType === 'lesson') {
    router.push(`/lesson/${row.lessonNo}`)
    return
  }
  router.push(`/course-resource/${row.resourceId}/preview`)
}

const resetUploadForm = () => {
  uploadForm.value = {
    title: '',
    chapter: '',
    knowledgePointId: '',
    file: null
  }
  uploadFileList.value = []
}

const loadKnowledgePoints = async () => {
  try {
    const response = await getKnowledgePoints(route.params.code)
    if (response.data.code === 200) knowledgePoints.value = response.data.data || []
    else ElMessage.warning(response.data.msg || '知识点加载失败')
  } catch {
    ElMessage.warning('知识点加载失败')
  }
}

const openUploadDialog = async () => {
  resetUploadForm()
  uploadDialog.value = true
  if (!knowledgePoints.value.length) await loadKnowledgePoints()
}

const handleUploadFile = (file) => {
  uploadForm.value.file = file.raw
  uploadFileList.value = [file]
}

const removeUploadFile = () => {
  uploadForm.value.file = null
  uploadFileList.value = []
}

const handleUploadExceed = () => {
  ElMessage.warning('一次只能上传一个资源文件')
}

const submitUpload = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择课程资源文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('courseCode', route.params.code)
    formData.append('file', uploadForm.value.file)
    if (uploadForm.value.title) formData.append('title', uploadForm.value.title)
    if (uploadForm.value.chapter) formData.append('chapter', uploadForm.value.chapter)
    if (uploadForm.value.knowledgePointId) formData.append('knowledgePointId', uploadForm.value.knowledgePointId)

    const response = await uploadCourseResource(formData)
    if (response.data.code === 200) {
      ElMessage.success('课程资源上传成功')
      uploadDialog.value = false
      resetUploadForm()
      await loadResources()
    } else {
      ElMessage.error(response.data.msg || '课程资源上传失败')
    }
  } catch {
    ElMessage.error('课程资源上传失败')
  } finally {
    uploading.value = false
  }
}

const deleteResource = async (row) => {
  deletingId.value = row.resourceId
  try {
    const response = await deleteCourseResource(row.resourceId)
    if (response.data.code === 200) {
      ElMessage.success('课程资源已删除')
      await loadResources()
    } else {
      ElMessage.error(response.data.msg || '课程资源删除失败')
    }
  } catch {
    ElMessage.error('课程资源删除失败')
  } finally {
    deletingId.value = ''
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
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
