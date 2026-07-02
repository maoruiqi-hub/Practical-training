<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程" style="width:300px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
    </div>
    <div v-loading="loading" element-loading-text="正在加载课程..." style="min-height:300px">
      <el-row :gutter="20">
        <el-col
          v-for="c in courses"
          :key="c.courseCode"
          :xs="24"
          :sm="courses.length === 1 ? 18 : 12"
          :md="courses.length === 1 ? 14 : 8"
          :lg="courses.length === 1 ? 10 : 6"
          :xl="courses.length === 1 ? 9 : 6"
          style="margin-bottom:20px"
        >
          <el-card
            shadow="hover"
            :body-style="{ padding: '0' }"
            :class="['course-card', { 'single-course-card': courses.length === 1 }]"
            @click="$router.push('/course/' + c.courseCode)"
          >
            <img v-if="c.coverUrl" :src="resolveCoverUrl(c.coverUrl)" class="cover-img" />
            <div v-else class="cover-placeholder"><el-icon :size="40"><Reading /></el-icon></div>
            <div class="card-body">
              <h4>{{ c.courseName }}</h4>
              <p v-if="courses.length === 1 && c.description" class="course-desc">{{ c.description }}</p>
              <div class="card-info">
                <el-tag size="small" type="info">授课 {{ c.teacher }}</el-tag>
                <el-tag size="small">{{ c.credits }}学分</el-tag>
                <el-tag size="small">{{ c.lessonCount }}课时</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && courses.length===0" description="暂无课程" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchCourse } from '../api'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const courses = ref([])
const loading = ref(true)

const resolveCoverUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const normalized = url.startsWith('/') ? url : `/${url}`
  if (normalized.startsWith('/practical-training/')) return normalized
  return `/practical-training${normalized}`
}

const doSearch = async () => {
  loading.value = true
  try {
    const res = await searchCourse(keyword.value || '')
    if (res.data.code === 200) courses.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('课程加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(doSearch)
</script>

<style scoped>
.toolbar { margin-bottom:20px; display:flex; gap:10px; }
.course-card { cursor: pointer; border-radius: 12px; overflow: hidden; transition: transform .2s; }
.course-card:hover { transform: translateY(-6px); }
.cover-img { width:100%; height:180px; object-fit: cover; }
.cover-placeholder { width:100%; height:180px; background:#f5f7fa; display:flex; align-items:center; justify-content:center; color:#c0c4cc; }
.card-body { padding: 14px 16px; }
.card-body h4 { margin: 0 0 10px 0; font-size: 15px; }
.card-info { display: flex; gap: 6px; flex-wrap: wrap; }
.single-course-card .cover-img,
.single-course-card .cover-placeholder { height:260px; }
.single-course-card .card-body { padding: 18px 20px 20px; }
.single-course-card .card-body h4 { font-size: 20px; margin-bottom: 8px; }
.course-desc {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}
</style>
