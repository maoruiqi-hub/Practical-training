<template>
  <div class="ability-page">
    <div class="page-head">
      <div>
        <h3>能力图谱</h3>
        <p>维护课程能力点，并把能力点映射到知识点。</p>
      </div>
      <div class="actions">
        <el-select v-model="selectedCourse" placeholder="选择课程" filterable style="width:220px" @change="loadAll">
          <el-option v-for="course in courses" :key="course.courseCode" :label="course.courseName" :value="String(course.courseCode)" />
        </el-select>
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" :loading="generating" @click="generateMap">AI生成</el-button>
        <el-button type="success" @click="openAbility()">新增能力点</el-button>
      </div>
    </div>

    <el-row :gutter="16" v-loading="loading" element-loading-text="正在加载能力图谱...">
      <el-col :xs="24" :lg="11">
        <el-card shadow="never">
          <template #header>能力点</template>
          <el-table :data="abilityPoints" height="520" empty-text="暂无能力点">
            <el-table-column prop="name" label="能力点" min-width="140" show-overflow-tooltip />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="openAbility(row)">编辑</el-button>
                <el-popconfirm title="删除该能力点？" @confirm="removeAbility(row.abilityPointId)">
                  <template #reference><el-button size="small" type="danger">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="13">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>知识点映射</span>
              <el-button size="small" type="primary" plain @click="openBind">绑定知识点</el-button>
            </div>
          </template>
          <el-table :data="mappingRows" height="520" empty-text="暂无映射">
            <el-table-column prop="abilityName" label="能力点" min-width="130" show-overflow-tooltip />
            <el-table-column prop="knowledgeName" label="知识点" min-width="150" show-overflow-tooltip />
            <el-table-column prop="chapter" label="章节" width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button size="small" type="danger" text @click="unbind(row)">解绑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="abilityDialog" :title="abilityForm.abilityPointId ? '编辑能力点' : '新增能力点'" width="520px">
      <el-form :model="abilityForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="abilityForm.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="abilityForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abilityDialog=false">取消</el-button>
        <el-button type="primary" @click="saveAbility">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindDialog" title="绑定知识点" width="520px">
      <el-form label-width="80px">
        <el-form-item label="能力点">
          <el-select v-model="bindForm.abilityPointId" filterable style="width:100%">
            <el-option v-for="item in abilityPoints" :key="item.abilityPointId" :label="item.name" :value="item.abilityPointId" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="bindForm.knowledgePointId" filterable style="width:100%">
            <el-option v-for="item in knowledgePoints" :key="item.knowledgePointId" :label="item.name" :value="item.knowledgePointId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialog=false">取消</el-button>
        <el-button type="primary" @click="bindKnowledge">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  addAbilityPoint,
  bindAbilityKnowledgePoint,
  deleteAbilityPoint,
  generateAbilityMap,
  getAbilityMap,
  getKnowledgeGraph,
  searchCourse,
  unbindAbilityKnowledgePoint,
  updateAbilityPoint
} from '../api'

const courses = ref([])
const route = useRoute()
const selectedCourse = ref('')
const loading = ref(false)
const generating = ref(false)
const abilityPoints = ref([])
const mappings = ref([])
const knowledgePoints = ref([])
const abilityDialog = ref(false)
const bindDialog = ref(false)
const abilityForm = reactive({ abilityPointId: '', name: '', description: '' })
const bindForm = reactive({ abilityPointId: '', knowledgePointId: '' })

const mappingRows = computed(() => mappings.value.map(item => {
  const ability = abilityPoints.value.find(point => point.abilityPointId === item.abilityPointId)
  const knowledge = knowledgePoints.value.find(point => point.knowledgePointId === item.knowledgePointId)
  return {
    ...item,
    abilityName: ability?.name || item.abilityPointId,
    knowledgeName: knowledge?.name || item.knowledgePointId,
    chapter: knowledge?.chapter || '-'
  }
}))

async function loadCourses() {
  const res = await searchCourse('')
  if (res.data.code === 200) {
    courses.value = res.data.data || []
    if (route.query.courseCode) selectedCourse.value = String(route.query.courseCode)
    if (!selectedCourse.value && courses.value.length) selectedCourse.value = String(courses.value[0].courseCode)
  }
}

async function loadAll() {
  if (!selectedCourse.value) return
  loading.value = true
  try {
    const [mapRes, graphRes] = await Promise.all([
      getAbilityMap(selectedCourse.value),
      getKnowledgeGraph(selectedCourse.value)
    ])
    if (mapRes.data.code === 200) {
      abilityPoints.value = mapRes.data.data?.abilityPoints || []
      mappings.value = mapRes.data.data?.mappings || []
    } else ElMessage.error(mapRes.data.msg || '能力图谱加载失败')
    if (graphRes.data.code === 200) knowledgePoints.value = graphRes.data.data?.nodes || []
  } catch {
    ElMessage.error('能力图谱加载失败')
  } finally {
    loading.value = false
  }
}

function openAbility(row) {
  Object.assign(abilityForm, row || { abilityPointId: '', name: '', description: '' })
  abilityDialog.value = true
}

async function saveAbility() {
  if (!selectedCourse.value || !abilityForm.name) return ElMessage.warning('请填写能力点名称')
  const payload = { courseCode: selectedCourse.value, name: abilityForm.name, description: abilityForm.description }
  const res = abilityForm.abilityPointId
    ? await updateAbilityPoint(abilityForm.abilityPointId, payload)
    : await addAbilityPoint(payload)
  if (res.data.code === 200) {
    ElMessage.success('已保存')
    abilityDialog.value = false
    loadAll()
  } else ElMessage.error(res.data.msg || '保存失败')
}

async function removeAbility(id) {
  const res = await deleteAbilityPoint(id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    loadAll()
  } else ElMessage.error(res.data.msg || '删除失败')
}

function openBind() {
  Object.assign(bindForm, { abilityPointId: '', knowledgePointId: '' })
  bindDialog.value = true
}

async function bindKnowledge() {
  if (!bindForm.abilityPointId || !bindForm.knowledgePointId) return ElMessage.warning('请选择能力点和知识点')
  const res = await bindAbilityKnowledgePoint(bindForm.abilityPointId, bindForm.knowledgePointId)
  if (res.data.code === 200) {
    ElMessage.success('已绑定')
    bindDialog.value = false
    loadAll()
  } else ElMessage.error(res.data.msg || '绑定失败')
}

async function unbind(row) {
  const res = await unbindAbilityKnowledgePoint(row.abilityPointId, row.knowledgePointId)
  if (res.data.code === 200) {
    ElMessage.success('已解绑')
    loadAll()
  } else ElMessage.error(res.data.msg || '解绑失败')
}

async function generateMap() {
  if (!selectedCourse.value) return
  generating.value = true
  try {
    const res = await generateAbilityMap(selectedCourse.value)
    if (res.data.code === 200) {
      ElMessage.success('AI生成请求已完成')
      loadAll()
    } else ElMessage.error(res.data.msg || 'AI生成失败')
  } catch {
    ElMessage.error('AI生成失败')
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  await loadCourses()
  await loadAll()
})
</script>

<style scoped>
.ability-page { max-width: 1240px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.actions { display:flex; gap:10px; align-items:center; flex-wrap:wrap; justify-content:flex-end; }
.card-head { display:flex; justify-content:space-between; align-items:center; }
</style>
