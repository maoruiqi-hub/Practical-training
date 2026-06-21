<template>
  <div>
    <h3>教师管理</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索教师姓名" style="width:260px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
      <el-button type="success" @click="openAdd">新增教师</el-button>
    </div>
    <el-table :data="teachers" style="width:100%" v-loading="loading">
      <el-table-column prop="teacherNo" label="工号" width="80" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="college" label="学院" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="role" label="角色" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row.teacherNo)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教师' : '新增教师'">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!isEdit" label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码"><el-input v-model="form.password" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
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
import { getTeacherList, searchTeacher, updateTeacher, deleteTeacher, teacherRegister } from '../api'

const teachers = ref([])
const loading = ref(true)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ teacherNo: '', username: '', password: '', name: '', college: '', major: '', phone: '' })

const doSearch = async () => {
  loading.value = true
  try {
    const res = keyword.value ? await searchTeacher(keyword.value) : await getTeacherList()
    if (res.data.code === 200) teachers.value = res.data.data
  } finally { loading.value = false }
}

onMounted(doSearch)

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { teacherNo: '', username: '', password: '', name: '', college: '', major: '', phone: '' })
  dialogVisible.value = true
}
const openEdit = (row) => { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
const save = async () => {
  if (isEdit.value) { await updateTeacher(form.teacherNo, form); ElMessage.success('已更新') }
  else { await teacherRegister(form); ElMessage.success('已新增') }
  dialogVisible.value = false; doSearch()
}
const del = async (no) => { await deleteTeacher(no); ElMessage.success('已删除'); doSearch() }
</script>

<style scoped>
.toolbar { margin-bottom:16px; display:flex; gap:10px; }
</style>
