<template>
  <div>
    <h3>教师管理</h3>
    <el-table :data="teachers" style="width:100%" v-loading="loading">
      <el-table-column prop="teacherNo" label="工号" width="80" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="college" label="学院" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="role" label="角色" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row.teacherNo)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="编辑教师">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="editForm.college" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="editForm.major" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="editForm.phone" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherList, updateTeacher, deleteTeacher } from '../api'

const teachers = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const editForm = reactive({ teacherNo: '', name: '', college: '', major: '', phone: '' })

onMounted(async () => {
  try {
    const res = await getTeacherList()
    if (res.data.code === 200) teachers.value = res.data.data
  } finally { loading.value = false }
})

const edit = (row) => { Object.assign(editForm, row); dialogVisible.value = true }
const save = async () => {
  await updateTeacher(editForm.teacherNo, editForm)
  ElMessage.success('已更新'); dialogVisible.value = false
}
const del = async (no) => { await deleteTeacher(no); ElMessage.success('已删除'); location.reload() }
</script>
