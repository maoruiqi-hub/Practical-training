<template>
  <div>
    <h3>课程管理</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程" style="width:300px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
      <el-button type="success" @click="openAdd">新增课程</el-button>
    </div>
    <div v-loading="loading" element-loading-text="正在加载课程..." style="min-height:200px">
      <el-table :data="courses" style="width:100%" empty-text="暂无课程">
        <el-table-column prop="courseCode" label="编号" width="100" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacher" label="授课教师" width="120" />
        <el-table-column prop="credits" label="学分" width="80" />
        <el-table-column prop="hours" label="学时" width="80" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="del(row.courseCode)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '新增课程'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="课程名称"><el-input v-model="form.courseName" /></el-form-item>
        <el-form-item label="授课教师"><el-input v-model="form.teacher" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="form.credits" :min="0" :max="10" /></el-form-item>
        <el-form-item label="总学时"><el-input-number v-model="form.hours" :min="0" :max="200" /></el-form-item>
        <el-form-item v-if="!isEdit" label="课程封面">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleCover" accept="image/*">
            <el-button type="primary">选择封面图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="isEdit" label="封面URL"><el-input v-model="form.coverUrl" placeholder="图片链接" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchCourse, addCourse, updateCourse, deleteCourse } from '../api'

const keyword = ref('')
const courses = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ courseCode: '', courseName: '', teacher: '', credits: 0, hours: 0, coverUrl: '' })
const coverFile = ref(null)

const handleCover = (f) => { coverFile.value = f.raw }

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

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { courseCode: '', courseName: '', teacher: '', credits: 0, hours: 0, coverUrl: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  const fd = new FormData()
  fd.append('courseName', form.courseName)
  fd.append('teacher', form.teacher)
  fd.append('credits', String(form.credits))
  fd.append('hours', String(form.hours))
  if (coverFile.value) fd.append('file', coverFile.value)
  if (isEdit.value) {
    await updateCourse(form.courseCode, fd)
    ElMessage.success('已更新')
  } else {
    await addCourse(fd)
    ElMessage.success('已新增')
  }
  dialogVisible.value = false
  coverFile.value = null
  doSearch()
}

const del = async (code) => {
  await deleteCourse(code)
  ElMessage.success('已删除')
  doSearch()
}

onMounted(doSearch)
</script>

<style scoped>
.toolbar { margin-bottom:20px; display:flex; gap:10px; }
</style>
