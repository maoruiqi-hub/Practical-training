<template>
  <div>
    <h3>学生管理</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索学生姓名" style="width:260px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
      <el-button type="success" @click="openAdd">新增学生</el-button>
      <el-upload :show-file-list="false" :before-upload="handleImport" accept=".csv" style="display:inline-block;margin-right:8px">
        <el-button type="success" size="small">导入学生</el-button>
      </el-upload>
      <el-button type="warning" size="small" @click="handleExport">导出学生</el-button>
    </div>
    <el-table :data="students" style="width:100%" v-loading="loading" element-loading-text="正在加载学生..." empty-text="暂无学生">
      <el-table-column prop="studentNo" label="学号" width="80" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="college" label="学院" />
      <el-table-column prop="className" label="班级" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="phone" label="联系方式" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row.studentNo)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!isEdit" label="学号"><el-input v-model="form.studentNo" placeholder="留空自动生成" /></el-form-item>
        <el-form-item v-if="!isEdit" label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码"><el-input v-model="form.password" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.phone" /></el-form-item>
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
import { getStudentList, searchStudent, updateStudent, deleteStudent, studentRegister } from '../api'
import { importStudents, exportStudents } from '@/api/profile'

const students = ref([])
const loading = ref(true)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ studentNo: '', username: '', password: '', name: '', college: '', className: '', phone: '' })

const doSearch = async () => {
  loading.value = true
  try {
    const res = keyword.value ? await searchStudent(keyword.value) : await getStudentList()
    if (res.data.code === 200) students.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('学生加载失败')
  } finally { loading.value = false }
}

onMounted(doSearch)

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { studentNo: '', username: '', password: '', name: '', college: '', className: '', phone: '' })
  dialogVisible.value = true
}
const openEdit = (row) => { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
const save = async () => {
  if (isEdit.value) { await updateStudent(form.studentNo, form); ElMessage.success('已更新') }
  else { await studentRegister(form); ElMessage.success('已新增') }
  dialogVisible.value = false; doSearch()
}
const del = async (no) => { await deleteStudent(no); ElMessage.success('已删除'); doSearch() }

const handleImport = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const { data } = await importStudents(formData)
    if (data.code === 200) { ElMessage.success(data.msg); doSearch() }
    else ElMessage.error(data.msg)
  } catch (e) { ElMessage.error('导入失败') }
  return false
}

const handleExport = async () => {
  try {
    const { data } = await exportStudents()
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'students.csv'; a.click()
    URL.revokeObjectURL(url)
  } catch (e) { ElMessage.error('导出失败') }
}
</script>

<style scoped>
.toolbar { margin-bottom:16px; display:flex; gap:10px; }
</style>
