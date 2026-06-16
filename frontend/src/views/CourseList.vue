<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程" style="width:300px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
    </div>
    <div v-loading="loading" style="min-height:300px">
      <el-row :gutter="20">
        <el-col v-for="c in courses" :key="c.courseCode" :span="6" style="margin-bottom:20px">
          <el-card shadow="hover" :body-style="{ padding: '0' }" class="course-card" @click="$router.push('/course/' + c.courseCode)">
            <img v-if="c.coverUrl" :src="'/practical-training/' + c.coverUrl" class="cover-img" />
            <div v-else class="cover-placeholder"><el-icon :size="40"><Reading /></el-icon></div>
            <div class="card-body">
              <h4>{{ c.courseName }}</h4>
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

const keyword = ref('')
const courses = ref([])
const loading = ref(true)

const doSearch = async () => {
  loading.value = true
  const res = await searchCourse(keyword.value || 'Python')
  if (res.data.code === 200) courses.value = res.data.data
  loading.value = false
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
</style>
