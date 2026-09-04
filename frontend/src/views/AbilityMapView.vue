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
        <el-button type="success" :disabled="abilityLimitReached" @click="openAbility()">新增能力点</el-button>
      </div>
    </div>

    <section class="map-board" v-loading="loading" element-loading-text="正在加载能力图谱...">
      <el-alert v-if="loadError" title="能力映射加载失败" :description="loadError" type="error" show-icon :closable="false" />
      <div class="map-summary">
        <div class="course-mark">
          <span class="course-code">{{ selectedCourse || '-' }}</span>
          <div>
            <h2>{{ currentCourse?.courseName || '课程能力图谱' }}</h2>
            <p>{{ abilityPoints.length }}/{{ MAX_ABILITY_POINTS }} 个能力点 / {{ mappings.length }} 个知识点映射</p>
          </div>
        </div>
        <div class="summary-metrics">
          <div>
            <b>{{ abilityPoints.length }}</b>
            <span>能力点</span>
          </div>
          <div>
            <b>{{ linkedKnowledgeCount }}</b>
            <span>已覆盖知识点</span>
          </div>
          <div>
            <b>{{ coverageRate }}%</b>
            <span>覆盖率</span>
          </div>
        </div>
      </div>

      <div v-if="abilityTree.length" class="tree-canvas">
        <div class="tree-root">
          <span>课程</span>
          <strong>{{ currentCourse?.courseName || selectedCourse }}</strong>
        </div>
        <div class="trunk-line" />
        <div class="ability-branches">
          <article
            v-for="(ability, index) in abilityTree"
            :key="ability.abilityPointId"
            class="ability-branch"
            :style="{ '--accent': branchColor(index), '--accent-bg': branchBg(index), '--branch-offset': branchOffset(index) }"
          >
            <div class="branch-stem" />
            <button class="ability-node" type="button" @click="openAbility(ability)">
              <span class="node-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span>
                <b>{{ ability.name }}</b>
                <small>{{ ability.description || '暂无说明' }}</small>
              </span>
            </button>
            <div class="knowledge-leaves">
              <span v-for="point in ability.knowledgePoints" :key="point.knowledgePointId" class="knowledge-leaf">
                {{ point.name }}
              </span>
              <span v-if="!ability.knowledgePoints.length" class="knowledge-leaf empty">未绑定</span>
            </div>
          </article>
        </div>
      </div>

      <el-empty v-else description="暂无能力图谱数据" />
    </section>

    <el-card class="competency-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-head">
          <div>
            <span>真能力映射</span>
            <small class="card-hint">教师只标记关系，系统后续校准相关关系的强度</small>
          </div>
          <div class="card-actions">
            <el-button size="small" type="primary" plain @click="openCompetency()">新增真能力</el-button>
            <el-button size="small" type="success" plain :loading="calibrating" @click="calibrateStrengths">生成候选版本</el-button>
            <el-button v-if="candidateVersion" size="small" type="warning" plain :loading="publishing" @click="publishCandidate">发布候选版本</el-button>
          </div>
        </div>
      </template>
      <el-alert
        v-if="!competencies.length"
        title="请先建立课程真能力，例如“调试纠错”“模块化设计”“综合应用”。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-table v-else :data="competencyMatrixRows" border stripe empty-text="暂无假能力点">
        <el-table-column fixed prop="name" label="假能力点" min-width="190" show-overflow-tooltip />
        <el-table-column
          v-for="competency in competencies"
          :key="competency.competencyId"
          :label="competency.name"
          min-width="170"
        >
          <template #header>
            <el-tooltip :content="competency.description || '暂无能力说明'" placement="top">
              <span>{{ competency.name }}</span>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            <div class="relation-cell">
              <el-select
                :model-value="row.relations.find(item => item.competency.competencyId === competency.competencyId)?.relation?.relationStatus || 'uncertain'"
                size="small"
                @change="changeCompetencyRelation(row, competency, $event)"
              >
                <el-option label="相关" value="related" />
                <el-option label="不相关" value="unrelated" />
                <el-option label="不确定" value="uncertain" />
              </el-select>
              <el-tag
                v-if="row.relations.find(item => item.competency.competencyId === competency.competencyId)?.relation?.strength > 0"
                size="small"
                effect="plain"
                type="success"
              >
                {{ (Number(row.relations.find(item => item.competency.competencyId === competency.competencyId)?.relation?.strength || 0) * 100).toFixed(0) }}%
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="competencies.length" class="competency-admin-list">
        <div v-for="competency in competencies" :key="competency.competencyId" class="competency-admin-item">
          <span>{{ competency.name }}</span>
          <small>{{ competency.description || '暂无能力说明' }}</small>
          <el-button size="small" text @click="openCompetency(competency)">编辑</el-button>
          <el-popconfirm title="删除该真能力及其映射？" @confirm="removeCompetency(competency)">
            <template #reference><el-button size="small" type="danger" text>删除</el-button></template>
          </el-popconfirm>
        </div>
      </div>
      <p v-if="competencies.length" class="matrix-footnote">当前发布版本 {{ matrixVersion }} · 百分比为当前强度，候选版本需教师确认后发布。</p>
    </el-card>

    <el-card v-if="competencies.length" class="observation-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-head">
          <div>
            <span>真能力观测任务</span>
            <small class="card-hint">标记已有任务实际测量的真能力，供后续强度校准使用</small>
          </div>
        </div>
      </template>
      <el-table :data="observationTaskRows" height="360" empty-text="暂无学习任务">
        <el-table-column prop="taskName" label="任务" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.taskName || row.taskNo }}</template>
        </el-table-column>
        <el-table-column prop="taskType" label="类型" width="130" />
        <el-table-column label="观测真能力" min-width="300">
          <template #default="{ row }">
            <el-select
              :model-value="row.selectedCompetencyIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="选择该任务测量的能力"
              style="width:100%"
              @change="changeTaskObservation(row, $event)"
            >
              <el-option v-for="item in competencies" :key="item.competencyId" :label="item.name" :value="item.competencyId" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="calibrationReport.length" class="observation-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <span>最近一次校准报告</span>
            <small class="card-hint">报告只解释数据支持程度，不会替教师改变“相关 / 不相关 / 不确定”</small>
          </div>
        </div>
      </template>
      <el-table :data="calibrationReportRows" border stripe empty-text="暂无校准记录">
        <el-table-column prop="abilityName" label="假能力点" min-width="190" show-overflow-tooltip />
        <el-table-column prop="competencyName" label="真能力" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sampleCount" label="共同样本" width="95" />
        <el-table-column label="行为相关度" width="110">
          <template #default="{ row }">{{ (Number(row.correlation || 0) * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column label="验证集" width="135">
          <template #default="{ row }">
            <span v-if="Number(row.validationSampleCount || 0) < 3">样本不足</span>
            <el-tag v-else :type="row.validationDirectionConsistent ? 'success' : 'danger'" size="small">
              {{ row.validationDirectionConsistent ? '方向一致' : '方向冲突' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置信度" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.confidence || 0) >= 0.5 ? 'success' : 'warning'" size="small">
              {{ Number(row.confidence || 0) >= 0.5 ? '较高' : '有限' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

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

    <el-dialog v-model="competencyDialog" :title="competencyForm.competencyId ? '编辑真能力' : '新增真能力'" width="520px">
      <el-form :model="competencyForm" label-width="90px">
        <el-form-item label="能力名称"><el-input v-model="competencyForm.name" placeholder="例如：调试纠错" /></el-form-item>
        <el-form-item label="能力说明"><el-input v-model="competencyForm.description" type="textarea" :rows="3" placeholder="描述学生能够完成的任务或行为" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="competencyDialog=false">取消</el-button>
        <el-button type="primary" @click="saveCompetency">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiDraftDialog" title="AI 能力图谱草稿" width="720px">
      <el-empty v-if="!aiDraft.length" description="暂无可采纳草稿" />
      <div v-else class="draft-list">
        <article
          v-for="(item, index) in aiDraft"
          :key="`${item.name}-${index}`"
          class="draft-item"
          :class="item.status"
        >
          <header>
            <div>
              <small>Draft {{ index + 1 }}</small>
              <h4>{{ item.name }}</h4>
            </div>
            <el-tag size="small" :type="draftTagType(item.status)">{{ draftStatusText(item.status) }}</el-tag>
          </header>
          <p>{{ item.description || '暂无说明' }}</p>
          <div class="draft-points">
            <span v-for="name in item.knowledgePointNames" :key="name">{{ name }}</span>
            <span v-if="!item.knowledgePointNames.length" class="empty">未指定知识点</span>
          </div>
          <strong v-if="item.error" class="draft-error">{{ item.error }}</strong>
        </article>
      </div>
      <template #footer>
        <el-button @click="aiDraftDialog=false">关闭</el-button>
        <el-button type="primary" :loading="adoptingDraft" :disabled="!aiDraft.length" @click="adoptDraft">
          采纳草稿
        </el-button>
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
  addTrueCompetency,
  calibrateAbilityCompetencyStrengths,
  publishAbilityCompetencyVersion,
  bindAbilityKnowledgePoint,
  deleteAbilityPoint,
  getAbilityCompetencyMap,
  getTaskList,
  generateAbilityMap,
  getAbilityMap,
  getKnowledgeGraph,
  searchCourse,
  saveAbilityCompetencyRelation,
  saveCompetencyTaskObservations,
  updateTrueCompetency,
  deleteTrueCompetency,
  unbindAbilityKnowledgePoint,
  updateAbilityPoint
} from '../api'

const courses = ref([])
const route = useRoute()
const selectedCourse = ref('')
const loading = ref(false)
const loadError = ref('')
const calibrating = ref(false)
const publishing = ref(false)
const generating = ref(false)
const abilityPoints = ref([])
const mappings = ref([])
const knowledgePoints = ref([])
const abilityDialog = ref(false)
const bindDialog = ref(false)
const aiDraftDialog = ref(false)
const competencyDialog = ref(false)
const aiDraft = ref([])
const adoptingDraft = ref(false)
const abilityForm = reactive({ abilityPointId: '', name: '', description: '' })
const bindForm = reactive({ abilityPointId: '', knowledgePointId: '' })
const competencyForm = reactive({ competencyId: '', name: '', description: '' })
const competencies = ref([])
const competencyRelations = ref([])
const competencyTasks = ref([])
const competencyObservations = ref([])
const matrixVersion = ref('v1')
const calibrationReport = ref([])
const candidateVersion = ref('')
const MAX_ABILITY_POINTS = 20

const currentCourse = computed(() => courses.value.find(course => String(course.courseCode) === String(selectedCourse.value)))
const abilityLimitReached = computed(() => abilityPoints.value.length >= MAX_ABILITY_POINTS)

const linkedKnowledgeCount = computed(() => new Set(mappings.value.map(item => item.knowledgePointId)).size)
const coverageRate = computed(() => {
  if (!knowledgePoints.value.length) return 0
  return Math.round((linkedKnowledgeCount.value / knowledgePoints.value.length) * 100)
})

const abilityTree = computed(() => abilityPoints.value.map(ability => {
  const linkedIds = mappings.value
    .filter(item => item.abilityPointId === ability.abilityPointId)
    .map(item => item.knowledgePointId)
  const linkedPoints = linkedIds
    .map(id => knowledgePoints.value.find(point => point.knowledgePointId === id))
    .filter(Boolean)
    .sort((a, b) => Number(a.lessonNo || 0) - Number(b.lessonNo || 0))
  return { ...ability, knowledgePoints: linkedPoints }
}))

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

const competencyMatrixRows = computed(() => abilityPoints.value.map(ability => ({
  ...ability,
  relations: competencies.value.map(competency => ({
    competency,
    relation: competencyRelations.value.find(item =>
      String(item.abilityPointId) === String(ability.abilityPointId) &&
      String(item.competencyId) === String(competency.competencyId)
    )
  }))
})))

const observationTaskRows = computed(() => competencyTasks.value.map(task => ({
  ...task,
  selectedCompetencyIds: competencies.value
    .filter(competency => competencyObservations.value.some(item =>
      String(item.taskNo) === String(task.taskNo) && String(item.competencyId) === String(competency.competencyId)
    ))
    .map(item => item.competencyId)
})))

const calibrationReportRows = computed(() => calibrationReport.value.map(item => ({
  ...item,
  abilityName: abilityPoints.value.find(point => String(point.abilityPointId) === String(item.abilityPointId))?.name || item.abilityPointId,
  competencyName: competencies.value.find(point => String(point.competencyId) === String(item.competencyId))?.name || item.competencyId
})))

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
  loadError.value = ''
  abilityPoints.value = []
  mappings.value = []
  knowledgePoints.value = []
  competencies.value = []
  competencyRelations.value = []
  competencyTasks.value = []
  competencyObservations.value = []
  calibrationReport.value = []
  candidateVersion.value = ''
  matrixVersion.value = 'v1'
  loading.value = true
  try {
    const [mapRes, graphRes, competencyRes, taskRes] = await Promise.all([
      getAbilityMap(selectedCourse.value),
      getKnowledgeGraph(selectedCourse.value),
      getAbilityCompetencyMap(selectedCourse.value),
      getTaskList(selectedCourse.value)
    ])
    const failed = [mapRes, graphRes, competencyRes, taskRes].find(response => response.data?.code !== 200)
    if (failed) throw new Error(failed.data?.msg || '课程能力数据加载失败')
    if (mapRes.data.code === 200) {
      abilityPoints.value = mapRes.data.data?.abilityPoints || []
      mappings.value = mapRes.data.data?.mappings || []
    } else ElMessage.error(mapRes.data.msg || '能力图谱加载失败')
    if (graphRes.data.code === 200) knowledgePoints.value = graphRes.data.data?.nodes || []
    if (competencyRes.data.code === 200) {
      competencies.value = competencyRes.data.data?.competencies || []
      competencyRelations.value = competencyRes.data.data?.relations || []
      matrixVersion.value = competencyRes.data.data?.matrixVersion || 'v1'
    }
    if (taskRes.data.code === 200) competencyTasks.value = taskRes.data.data || []
    else competencyTasks.value = []
  } catch {
    loadError.value = '当前课程的能力、知识点或观测任务暂时无法加载，请刷新重试。'
    ElMessage.error('能力图谱加载失败')
  } finally {
    loading.value = false
  }
}

function openCompetency(row) {
  Object.assign(competencyForm, row
    ? { competencyId: row.competencyId, name: row.name, description: row.description || '' }
    : { competencyId: '', name: '', description: '' })
  competencyDialog.value = true
}

async function saveCompetency() {
  const name = String(competencyForm.name || '').trim()
  if (!selectedCourse.value || !name) return ElMessage.warning('请填写真能力名称')
  if (competencies.value.some(item => item.competencyId !== competencyForm.competencyId
    && normalizeName(item.name) === normalizeName(name))) {
    return ElMessage.warning('该课程已存在同名真能力')
  }
  const res = competencyForm.competencyId
    ? await updateTrueCompetency(competencyForm.competencyId, {
        courseCode: selectedCourse.value, name, description: competencyForm.description
      })
    : await addTrueCompetency({ courseCode: selectedCourse.value, name, description: competencyForm.description })
  if (res.data.code === 200) {
    ElMessage.success(competencyForm.competencyId ? '真能力已更新' : '真能力已创建')
    competencyDialog.value = false
    await loadAll()
  } else ElMessage.error(res.data.msg || '创建失败')
}

async function removeCompetency(competency) {
  try {
    const res = await deleteTrueCompetency(competency.competencyId, selectedCourse.value)
    if (res.data.code !== 200) throw new Error(res.data.msg || '真能力删除失败')
    ElMessage.success('真能力已删除')
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message || '真能力删除失败')
  }
}

function relationLabel(status) {
  return { related: '相关', unrelated: '不相关', uncertain: '不确定' }[status] || '不确定'
}

function relationTagType(status) {
  return { related: 'success', unrelated: 'info', uncertain: 'warning' }[status] || 'warning'
}

async function changeCompetencyRelation(ability, competency, status) {
  const res = await saveAbilityCompetencyRelation({
    courseCode: selectedCourse.value,
    abilityPointId: ability.abilityPointId,
    competencyId: competency.competencyId,
    relationStatus: status
  })
  if (res.data.code === 200) {
    ElMessage.success('关系状态已保存')
    await loadAll()
  } else ElMessage.error(res.data.msg || '关系保存失败')
}

async function calibrateStrengths() {
  if (!selectedCourse.value) return
  calibrating.value = true
  try {
    const res = await calibrateAbilityCompetencyStrengths(selectedCourse.value)
    if (res.data.code === 200) {
      const count = res.data.data?.relationCount || 0
      calibrationReport.value = res.data.data?.relations || []
      candidateVersion.value = res.data.data?.candidateVersion || ''
      ElMessage.success(`已生成候选版本 ${candidateVersion.value}，共 ${count} 条相关关系`)
    } else ElMessage.error(res.data.msg || '强度校准失败')
  } catch {
    ElMessage.error('强度校准失败')
  } finally {
    calibrating.value = false
  }
}

async function publishCandidate() {
  if (!selectedCourse.value || !candidateVersion.value) return
  publishing.value = true
  try {
    const res = await publishAbilityCompetencyVersion(selectedCourse.value, candidateVersion.value)
    if (res.data.code === 200) {
      ElMessage.success(`已发布版本 ${candidateVersion.value}`)
      candidateVersion.value = ''
      calibrationReport.value = []
      await loadAll()
    } else ElMessage.error(res.data.msg || '版本发布失败')
  } catch {
    ElMessage.error('版本发布失败')
  } finally {
    publishing.value = false
  }
}

async function changeTaskObservation(task, competencyIds) {
  try {
    const selected = new Set((competencyIds || []).map(String))
    const res = await saveCompetencyTaskObservations(competencies.value.map(competency => ({
      courseCode: selectedCourse.value,
      taskNo: task.taskNo,
      competencyId: competency.competencyId,
      status: selected.has(String(competency.competencyId)) ? 'active' : 'inactive'
    })))
    if (res.data.code !== 200) throw new Error(res.data.msg || '观测任务标注保存失败')
    ElMessage.success('观测任务标注已保存')
    await loadAll()
  } catch {
    ElMessage.error('观测任务标注保存失败')
  }
}

function openAbility(row) {
  if (!row && abilityLimitReached.value) {
    ElMessage.warning('每门课程最多只能创建20个能力点')
    return
  }
  Object.assign(abilityForm, row || { abilityPointId: '', name: '', description: '' })
  abilityDialog.value = true
}

async function saveAbility() {
  const name = String(abilityForm.name || '').trim()
  if (!selectedCourse.value || !name) return ElMessage.warning('请填写能力点名称')
  const duplicate = abilityPoints.value.some(item =>
    item.abilityPointId !== abilityForm.abilityPointId && normalizeName(item.name) === normalizeName(name)
  )
  if (duplicate) return ElMessage.warning('该课程已存在同名能力点')
  if (!abilityForm.abilityPointId && abilityLimitReached.value) {
    return ElMessage.warning('每门课程最多只能创建20个能力点')
  }
  const payload = { courseCode: selectedCourse.value, name, description: abilityForm.description }
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
      const payload = res.data.data?.data || res.data.data || {}
      aiDraft.value = normalizeAbilityDraft(payload)
      aiDraftDialog.value = true
      if (aiDraft.value.length) ElMessage.success('AI 草稿已生成')
      else ElMessage.warning('AI 未返回可展示草稿')
    } else ElMessage.error(res.data.msg || 'AI生成失败')
  } catch {
    ElMessage.error('AI生成失败')
  } finally {
    generating.value = false
  }
}

function normalizeAbilityDraft(payload = {}) {
  const list = Array.isArray(payload)
    ? payload
    : payload.abilityPoints || payload.abilities || payload.items || []
  return list.map(item => {
    const rawKnowledgePoints = item.knowledgePoints ||
      item.knowledge_point_names ||
      item.knowledgePointNames ||
      item.points ||
      []
    const knowledgePointNames = (Array.isArray(rawKnowledgePoints) ? rawKnowledgePoints : [rawKnowledgePoints])
      .map(point => typeof point === 'string' ? point : point?.name || point?.title || point?.knowledgePointName || '')
      .filter(Boolean)
    return {
      name: item.name || item.title || '未命名能力点',
      description: item.description || item.reason || '',
      knowledgePointNames,
      status: 'pending',
      error: ''
    }
  })
}

function normalizeName(value) {
  return String(value || '').trim().toLowerCase()
}

function matchKnowledgePoint(name) {
  const key = normalizeName(name)
  if (!key) return null
  return knowledgePoints.value.find(point => normalizeName(point.name) === key) ||
    knowledgePoints.value.find(point => {
      const pointName = normalizeName(point.name)
      return pointName && (pointName.includes(key) || key.includes(pointName))
    })
}

async function adoptDraft() {
  if (!selectedCourse.value || !aiDraft.value.length) return
  adoptingDraft.value = true
  try {
    const existingNames = new Set(abilityPoints.value.map(item => normalizeName(item.name)))
    let remainingSlots = Math.max(0, MAX_ABILITY_POINTS - abilityPoints.value.length)
    for (const item of aiDraft.value) {
      item.status = 'saving'
      item.error = ''
      const normalizedDraftName = normalizeName(item.name)
      if (!normalizedDraftName || existingNames.has(normalizedDraftName)) {
        item.status = 'failed'
        item.error = '该课程已存在同名能力点'
        continue
      }
      if (remainingSlots <= 0) {
        item.status = 'failed'
        item.error = '每门课程最多只能创建20个能力点'
        continue
      }
      try {
        const addRes = await addAbilityPoint({
          courseCode: selectedCourse.value,
          name: item.name,
          description: item.description
        })
        if (addRes.data.code !== 200) throw new Error(addRes.data.msg || '能力点保存失败')
        const abilityPointId = addRes.data.data
        existingNames.add(normalizedDraftName)
        remainingSlots -= 1
        const failures = []
        for (const pointName of item.knowledgePointNames) {
          const point = matchKnowledgePoint(pointName)
          if (!point) {
            failures.push(`未匹配：${pointName}`)
            continue
          }
          const knowledgePointId = point.knowledgePointId || point.id || point.kpId
          const bindRes = await bindAbilityKnowledgePoint(abilityPointId, knowledgePointId)
          if (bindRes.data.code !== 200) failures.push(`绑定失败：${pointName}`)
        }
        item.status = failures.length ? 'partial' : 'saved'
        item.error = failures.join('；')
      } catch (error) {
        item.status = 'failed'
        item.error = error.message || '采纳失败'
      }
    }
    await loadAll()
    ElMessage.success('草稿采纳已处理')
  } finally {
    adoptingDraft.value = false
  }
}

function draftTagType(status) {
  if (status === 'saved') return 'success'
  if (status === 'partial') return 'warning'
  if (status === 'failed') return 'danger'
  if (status === 'saving') return 'info'
  return ''
}

function draftStatusText(status) {
  return {
    pending: '待采纳',
    saving: '采纳中',
    saved: '已采纳',
    partial: '部分采纳',
    failed: '失败'
  }[status] || '待采纳'
}

function branchColor(index) {
  const colors = ['#2563eb', '#0891b2', '#7c3aed', '#dc2626', '#ca8a04', '#059669', '#c2410c', '#4f46e5']
  return colors[index % colors.length]
}

function branchBg(index) {
  const colors = ['#eff6ff', '#ecfeff', '#f5f3ff', '#fef2f2', '#fefce8', '#ecfdf5', '#fff7ed', '#eef2ff']
  return colors[index % colors.length]
}

function branchOffset(index) {
  return `${(index % 2) * 14}px`
}

onMounted(async () => {
  await loadCourses()
  await loadAll()
})
</script>

<style scoped>
.ability-page { max-width: 1280px; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; }
.page-head h3 { margin:0 0 6px; }
.page-head p { margin:0; color:#606266; }
.actions { display:flex; gap:10px; align-items:center; flex-wrap:wrap; justify-content:flex-end; }
.card-head { display:flex; justify-content:space-between; align-items:center; }
.card-actions { display:flex; gap:8px; align-items:center; }
.card-hint { display:block; margin-top:4px; color:#909399; font-size:12px; }
.competency-card { margin:16px 0; }
.competency-admin-list { display:grid; gap:8px; margin-top:12px; padding-top:12px; border-top:1px solid var(--el-border-color-lighter); }
.competency-admin-item { display:flex; align-items:center; gap:10px; min-height:32px; }
.competency-admin-item span { min-width:150px; font-weight:600; }
.competency-admin-item small { flex:1; overflow:hidden; color:var(--el-text-color-secondary); text-overflow:ellipsis; white-space:nowrap; }
.observation-card { margin:16px 0; }
.relation-cell { display:flex; align-items:center; gap:6px; }
.relation-cell .el-select { width:108px; }
.matrix-footnote { margin:12px 0 0; color:#909399; font-size:12px; }
.map-board {
  background:
    linear-gradient(90deg, rgba(37, 99, 235, 0.07), transparent 30%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  margin-bottom: 16px;
  overflow: hidden;
}
.map-summary {
  display:flex;
  justify-content:space-between;
  align-items:center;
  gap:18px;
  margin-bottom:16px;
}
.course-mark {
  display:flex;
  align-items:center;
  gap:12px;
  min-width:0;
}
.course-code {
  width:48px;
  height:48px;
  border-radius:8px;
  display:grid;
  place-items:center;
  background:#111827;
  color:#fff;
  font-weight:700;
}
.course-mark h2 {
  margin:0;
  font-size:22px;
  line-height:1.25;
  color:#111827;
}
.course-mark p {
  margin:4px 0 0;
  color:#64748b;
}
.summary-metrics {
  display:grid;
  grid-template-columns:repeat(3, minmax(92px, 1fr));
  gap:8px;
}
.summary-metrics div {
  border:1px solid #e2e8f0;
  border-radius:8px;
  background:#fff;
  padding:10px 12px;
}
.summary-metrics b {
  display:block;
  font-size:22px;
  color:#0f172a;
  line-height:1;
}
.summary-metrics span {
  display:block;
  margin-top:6px;
  color:#64748b;
  font-size:12px;
}
.tree-canvas {
  position:relative;
  display:grid;
  grid-template-columns:180px 34px minmax(0, 1fr);
  gap:0;
  min-height:360px;
}
.tree-root {
  align-self:center;
  border:1px solid #cbd5e1;
  background:#fff;
  border-radius:8px;
  padding:18px;
  min-height:104px;
  display:flex;
  flex-direction:column;
  justify-content:center;
  box-shadow:0 10px 24px rgba(15, 23, 42, 0.08);
}
.tree-root span {
  color:#64748b;
  font-size:12px;
  margin-bottom:8px;
}
.tree-root strong {
  color:#0f172a;
  font-size:18px;
  line-height:1.35;
}
.trunk-line {
  position:relative;
}
.trunk-line::before {
  content:'';
  position:absolute;
  left:50%;
  top:24px;
  bottom:24px;
  width:2px;
  background:#cbd5e1;
}
.trunk-line::after {
  content:'';
  position:absolute;
  left:0;
  right:0;
  top:50%;
  height:2px;
  background:#cbd5e1;
}
.ability-branches {
  display:grid;
  gap:10px;
}
.ability-branch {
  --accent:#2563eb;
  --accent-bg:#eff6ff;
  display:grid;
  grid-template-columns:34px minmax(220px, 280px) minmax(0, 1fr);
  gap:0;
  align-items:center;
  transform:translateY(var(--branch-offset));
}
.branch-stem {
  height:2px;
  background:linear-gradient(90deg, #cbd5e1, var(--accent));
}
.ability-node {
  min-height:72px;
  border:1px solid #dbe3ee;
  border-left:5px solid var(--accent);
  border-radius:8px;
  background:#fff;
  display:flex;
  gap:10px;
  align-items:center;
  padding:12px;
  text-align:left;
  cursor:pointer;
  box-shadow:0 8px 18px rgba(15, 23, 42, 0.06);
}
.ability-node:hover {
  border-color:var(--accent);
  transform:translateY(-1px);
}
.node-index {
  width:34px;
  height:34px;
  flex:0 0 auto;
  border-radius:50%;
  display:grid;
  place-items:center;
  background:var(--accent-bg);
  color:var(--accent);
  font-weight:700;
  font-size:12px;
}
.ability-node b {
  display:block;
  color:#0f172a;
  font-size:14px;
  line-height:1.35;
}
.ability-node small {
  display:-webkit-box;
  -webkit-line-clamp:2;
  -webkit-box-orient:vertical;
  overflow:hidden;
  margin-top:4px;
  color:#64748b;
  line-height:1.4;
}
.knowledge-leaves {
  min-height:72px;
  display:flex;
  align-items:center;
  flex-wrap:wrap;
  gap:8px;
  padding-left:14px;
  position:relative;
}
.knowledge-leaves::before {
  content:'';
  position:absolute;
  left:0;
  top:50%;
  width:14px;
  height:2px;
  background:var(--accent);
}
.knowledge-leaf {
  max-width:170px;
  border:1px solid #e2e8f0;
  border-radius:999px;
  background:#fff;
  color:#334155;
  padding:6px 10px;
  font-size:12px;
  line-height:1.25;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}
.knowledge-leaf.empty {
  color:#94a3b8;
  border-style:dashed;
}
.draft-list {
  display:grid;
  gap:12px;
  max-height:58vh;
  overflow:auto;
}
.draft-item {
  display:grid;
  gap:10px;
  padding:14px;
  border:1px solid #e5e7eb;
  border-left:5px solid #94a3b8;
  border-radius:8px;
  background:#fff;
}
.draft-item.saved {
  border-left-color:#16a34a;
}
.draft-item.partial {
  border-left-color:#ca8a04;
}
.draft-item.failed {
  border-left-color:#dc2626;
}
.draft-item header {
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap:12px;
}
.draft-item small {
  color:#64748b;
  font-weight:700;
}
.draft-item h4 {
  margin:3px 0 0;
  color:#111827;
  font-size:16px;
  line-height:1.35;
}
.draft-item p {
  margin:0;
  color:#64748b;
  line-height:1.5;
}
.draft-points {
  display:flex;
  flex-wrap:wrap;
  gap:8px;
}
.draft-points span {
  padding:5px 9px;
  border-radius:999px;
  color:#334155;
  background:#f1f5f9;
  font-size:12px;
}
.draft-points .empty {
  color:#94a3b8;
  border:1px dashed #cbd5e1;
  background:#fff;
}
.draft-error {
  color:#b91c1c;
  font-size:13px;
  line-height:1.45;
}
@media (max-width: 960px) {
  .map-summary { align-items:flex-start; flex-direction:column; }
  .summary-metrics { width:100%; }
  .tree-canvas {
    grid-template-columns:1fr;
    gap:14px;
  }
  .trunk-line { display:none; }
  .ability-branch {
    grid-template-columns:1fr;
    transform:none;
  }
  .branch-stem,
  .knowledge-leaves::before { display:none; }
  .knowledge-leaves { padding:8px 0 0; }
}
@media (max-width: 640px) {
  .page-head { flex-direction:column; }
  .actions { width:100%; justify-content:flex-start; }
  .actions .el-select { width:100% !important; }
  .summary-metrics { grid-template-columns:1fr; }
  .map-board { padding:14px; }
}
</style>
