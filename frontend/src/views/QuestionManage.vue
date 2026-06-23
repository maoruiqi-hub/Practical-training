<template>
  <div>
    <h3>题库管理</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索题干或知识点" style="width:260px" @keyup.enter="doSearch" />
      <el-button type="primary" @click="doSearch">搜索</el-button>
      <el-button type="success" @click="openAdd">新增题目</el-button>
    </div>
    <el-table :data="questions" style="width:100%" v-loading="loading" element-loading-text="正在加载题目..." empty-text="暂无题目" size="small">
      <el-table-column prop="questionId" label="ID" width="60" />
      <el-table-column label="题型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" show-overflow-tooltip />
      <el-table-column label="难度" width="80">
        <template #default="{ row }">{{ '⭐'.repeat(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="knowledgePoint" label="知识点" width="120" />
      <el-table-column prop="score" label="分值" width="60" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row.questionId)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '新增题目'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="题型"><el-select v-model="form.type"><el-option label="单选" value="single" /><el-option label="多选" value="multi" /><el-option label="填空" value="fill" /><el-option label="简答" value="essay" /></el-select></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.stem" type="textarea" :rows="2" /></el-form-item>
        <el-form-item v-if="form.type==='single'||form.type==='multi'" label="选项">
          <div v-for="(o,i) in optList" :key="i" style="display:flex;gap:8px;margin-bottom:6px">
            <el-input v-model="optList[i]" :placeholder="'选项'+String.fromCharCode(65+i)" size="small" />
            <el-button size="small" @click="optList.splice(i,1)" :disabled="optList.length<=2">-</el-button>
          </div>
          <el-button size="small" @click="optList.push('')">+ 添加选项</el-button>
        </el-form-item>
        <el-form-item label="答案"><el-input v-model="form.answer" placeholder="单选填选项文字，多选用逗号分隔，填空填关键词，简答填评分要点" /></el-form-item>
        <el-form-item label="课程"><el-input v-model="form.courseCode" placeholder="课程编号" /></el-form-item>
        <el-form-item label="关联课时"><el-input v-model="form.lessonNo" placeholder="课时编号" /></el-form-item>
        <el-form-item label="知识点"><el-input v-model="form.knowledgePoint" /></el-form-item>
        <el-form-item label="难度"><el-rate v-model="form.difficulty" :max="5" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.score" :min="0" :max="100" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { searchQuestion, getQuestionsByCourse, updateQuestion, addQuestion, deleteQuestion } from '../api'

const questions = ref([])
const loading = ref(true)
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ questionId:'',type:'single',stem:'',answer:'',courseCode:'1',lessonNo:'',knowledgePoint:'',difficulty:3,score:10,options:'' })
const optList = ref([])

const typeLabel = t => ({single:'单选',multi:'多选',fill:'填空',essay:'简答'}[t]||t)

const doSearch = async () => {
  loading.value = true
  try {
    const res = keyword.value ? await searchQuestion(keyword.value) : await getQuestionsByCourse('1')
    if (res.data.code === 200) questions.value = res.data.data
    else ElMessage.error(res.data.msg)
  } catch {
    ElMessage.error('题目加载失败')
  } finally { loading.value = false }
}

onMounted(doSearch)

const openAdd = () => {
  isEdit.value = false; optList.value = []
  Object.assign(form, { questionId:'',type:'single',stem:'',answer:'',courseCode:'1',lessonNo:'',knowledgePoint:'',difficulty:3,score:10 })
  dialogVisible.value = true
}
const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  optList.value = row.options ? JSON.parse(row.options) : []
  dialogVisible.value = true
}
const save = async () => {
  if (['single','multi'].includes(form.type)) form.options = JSON.stringify(optList.value)
  else form.options = null
  if (isEdit.value) {
    await updateQuestion(form.questionId, form)
    ElMessage.success('已更新')
  } else {
    await addQuestion(form)
    ElMessage.success('已新增')
  }
  dialogVisible.value = false; doSearch()
}
const del = async (id) => { await deleteQuestion(id); ElMessage.success('已删除'); doSearch() }
</script>

<style scoped>
.toolbar { margin-bottom:16px; display:flex; gap:10px; }
</style>
