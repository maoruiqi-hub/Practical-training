<template>
  <div class="teacher-profile-page">
    <!-- 顶部：课程选择 -->
    <div class="toolbar">
      <span style="font-size:16px;font-weight:bold">学生画像与个性化学习</span>
      <el-select v-model="selectedCourse" placeholder="选择课程" @change="onCourseChange" style="width:300px;margin-left:16px">
        <el-option v-for="c in courses" :key="c.courseCode" :label="c.courseName" :value="c.courseCode" />
      </el-select>
    </div>

    <!-- 学生画像列表 -->
    <el-card v-if="selectedCourse" style="margin-top:16px" v-loading="loading">
      <template #header>
        <span>学生画像总览（{{ students.length }}人）</span>
        <el-tag style="margin-left:8px" size="small">教学视角</el-tag>
        <el-input v-model="searchKey" placeholder="搜索学生姓名/学号" style="width:220px;margin-left:16px;float:right" clearable />
      </template>
      <el-empty v-if="!filteredStudents.length && !loading" description="暂无学生数据" />
      <el-table v-else :data="filteredStudents" stripe highlight-current-row @row-click="selectStudent" style="cursor:pointer">
        <el-table-column label="学生" min-width="150">
          <template #default="{ row }">
            <div style="font-weight:600">{{ row.name || '-' }}</div>
            <div style="font-size:12px;color:#909399">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="学习状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '未激活' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学习投入" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="engagementTagType(row)">{{ engagementLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="解题表现" min-width="150">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <el-progress :percentage="row.competencyAverage || 0" :stroke-width="6" :show-text="false" style="width:72px" />
              <span>{{ performanceLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="基础掌握" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="foundationTagType(row)">{{ foundationLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成长趋势" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="growthTagType(row)">{{ growthLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近活动" width="130">
          <template #default="{ row }">
            <span style="font-size:12px;color:#999">{{ row.lastActivityDate ? new Date(row.lastActivityDate).toLocaleDateString() : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="selectStudent(row)">查看</el-button>
            <el-button size="small" type="primary" plain @click.stop="openAssignDialog(row)">分配</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="!selectedCourse" description="请先选择课程查看学生画像" style="margin-top:60px" />

    <!-- 学生画像详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="900px" top="3vh" @opened="onDetailOpened">
      <template v-if="detailStudent">
        <div style="display:flex;justify-content:flex-end;margin-bottom:12px">
          <el-button type="success" :loading="interventionLoading" @click="generateIntervention">生成干预建议</el-button>
          <el-button type="primary" @click="openAssignDialog()">分配任务</el-button>
        </div>
        <!-- 教学诊断摘要 -->
        <el-row :gutter="16">
          <el-col :span="16">
            <el-card shadow="never">
              <template #header>
                <span>教学诊断摘要</span>
                <el-tag style="margin-left:8px" :type="detailStatusType">{{ detailProfile.status || '正常学习' }}</el-tag>
              </template>
              <el-row :gutter="12">
                <el-col :span="8">
                  <div style="text-align:center">
                    <div style="font-size:24px;font-weight:bold;color:#409eff">{{ detailAverageScore }}</div>
                    <div style="color:#999;font-size:12px">能力均分</div>
                    <el-progress :percentage="detailAverageScore" :show-text="false" :stroke-width="5" />
                  </div>
                </el-col>
                <el-col :span="8">
                  <div style="text-align:center">
                    <div style="font-size:24px;font-weight:bold;color:#e6a23c">{{ detailWeakCount }}</div>
                    <div style="color:#999;font-size:12px">薄弱能力点</div>
                    <el-progress :percentage="Math.min(100, detailWeakCount * 20)" color="#e6a23c" :show-text="false" :stroke-width="5" />
                  </div>
                </el-col>
                <el-col :span="8">
                  <div style="text-align:center">
                    <div style="font-size:24px;font-weight:bold;color:#67c23a">{{ detailBadgeCount }}</div>
                    <div style="color:#999;font-size:12px">激励记录</div>
                    <el-progress :percentage="Math.min(100, detailBadgeCount * 20)" color="#67c23a" :show-text="false" :stroke-width="5" />
                  </div>
                </el-col>
              </el-row>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never">
              <template #header><span>近期判断</span></template>
              <div>学习投入：<el-tag size="small" :type="engagementTagType(detailStudent)">{{ engagementLabel(detailStudent) }}</el-tag></div>
              <div style="margin-top:8px">解题表现：{{ performanceLabel(detailStudent) }}</div>
              <div style="margin-top:8px">基础掌握：{{ foundationLabel(detailStudent) }}</div>
              <div style="margin-top:8px">成长趋势：{{ growthLabel(detailStudent) }}</div>
              <div style="margin-top:8px;font-size:12px;color:#999">最近活动：{{ detailProfile.lastActivityDate ? new Date(detailProfile.lastActivityDate).toLocaleString() : '-' }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 详情Tab -->
        <el-tabs v-model="detailTab" style="margin-top:12px" @tab-change="onDetailTabChange">
          <el-tab-pane label="能力评分" name="competency">
            <div ref="detailRadar" style="width:100%;height:350px"></div>
          </el-tab-pane>
          <el-tab-pane label="个性化推荐" name="recommendations">
            <el-empty v-if="!detailRecs.length" description="暂无推荐" />
            <div v-for="rec in detailRecs" :key="rec.id"
                 style="padding:10px;margin-bottom:6px;background:#f5f7fa;border-radius:6px;display:flex;justify-content:space-between;align-items:center">
              <div>
                <el-tag size="small" :type="recTypeTag(rec.type)">{{ recTypeLabel(rec.type) }}</el-tag>
                <span style="margin-left:6px;font-weight:bold">{{ rec.targetName }}</span>
                <div style="color:#999;font-size:12px;margin-top:2px">{{ rec.reason }}</div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="干预建议" name="intervention">
            <div style="margin-bottom:12px">
              <el-button type="primary" :loading="interventionLoading" @click="generateIntervention">生成个别干预建议</el-button>
            </div>
            <el-empty v-if="!detailInterventions.length" description="暂无干预建议" />
            <div
              v-for="(item, index) in detailInterventions"
              :key="index"
              style="padding:10px;margin-bottom:8px;background:#f5f7fa;border-radius:6px"
            >
              <div style="display:flex;align-items:center;justify-content:space-between;gap:12px">
                <strong>{{ item.title || item.suggestion_type || item.type || '干预建议' }}</strong>
                <el-tag size="small" :type="urgencyTag(item.urgency || item.priority || item.level)">
                  {{ urgencyLabel(item.urgency || item.priority || item.level) }}
                </el-tag>
              </div>
              <div style="color:#606266;font-size:13px;margin-top:6px">
                {{ item.content || item.suggestion || item.message || item.raw_response || JSON.stringify(item) }}
              </div>
              <div v-if="item.generated_at || item.generatedAt" style="color:#999;font-size:12px;margin-top:4px">
                {{ item.generated_at || item.generatedAt }}
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="徽章来源" name="achievements">
            <el-empty v-if="!detailAchievements.length" description="暂无成就" />
            <el-table v-else :data="detailAchievements" stripe>
              <el-table-column label="徽章" min-width="140">
                <template #default="{ row }">
                  <span style="font-size:20px;margin-right:6px">{{ badgeIcon(row.name) }}</span>
                  <strong>{{ row.name }}</strong>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="授予规则" min-width="180" />
              <el-table-column label="可信事实快照" min-width="260">
                <template #default="{ row }">{{ badgeFactSummary(row) }}</template>
              </el-table-column>
              <el-table-column label="徽章编码" width="150">
                <template #default="{ row }"><code>{{ row.badgeCode || '-' }}</code></template>
              </el-table-column>
              <el-table-column label="授予时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.earnedAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="成长来源" name="growthHistory">
            <el-empty v-if="!detailGrowthHistory.length" description="暂无记录" />
            <el-table v-else :data="detailGrowthHistory" stripe>
              <el-table-column label="属性" width="100">
                <template #default="{ row }">{{ growthTypeLabel(row.type) }}</template>
              </el-table-column>
              <el-table-column label="变化" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.amount >= 0 ? 'success' : 'danger'" size="small">
                    {{ row.amount > 0 ? '+' : '' }}{{ row.amount }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="业务来源" min-width="150">
                <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
              </el-table-column>
              <el-table-column label="来源记录 ID" min-width="220">
                <template #default="{ row }"><code>{{ row.sourceId || '-' }}</code></template>
              </el-table-column>
              <el-table-column label="发生时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="能力变化" name="competencyHistory">
            <el-empty v-if="!detailCompHistory.length" description="暂无变化记录" />
            <div v-else ref="detailHistoryChart" style="width:100%;height:350px"></div>
          </el-tab-pane>
          <el-tab-pane label="学习反馈" name="feedback">
            <el-empty v-if="!detailFeedback" description="暂无反馈数据" />
            <div v-else>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="学习状态">{{ detailFeedback.status }}</el-descriptions-item>
                <el-descriptions-item label="能力均分">{{ detailAverageScore }}</el-descriptions-item>
                <el-descriptions-item label="薄弱能力点">{{ detailWeakCount }}</el-descriptions-item>
                <el-descriptions-item label="最近活动">{{ detailProfile.lastActivityDate ? new Date(detailProfile.lastActivityDate).toLocaleString() : '-' }}</el-descriptions-item>
              </el-descriptions>
              <div v-if="detailFeedback.weakPoints && detailFeedback.weakPoints.length" style="margin-top:12px">
                <p style="font-weight:bold;color:#f56c6c">薄弱知识点：</p>
                <el-tag v-for="wp in detailFeedback.weakPoints" :key="wp.name" style="margin:2px" size="small" type="danger">
                  {{ wp.name }}({{ wp.score }}分) - {{ wp.suggestion }}
                </el-tag>
              </div>
              <el-alert v-if="detailFeedback.nextAction" :title="detailFeedback.nextAction" type="info" :closable="false" style="margin-top:12px" show-icon />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配任务" width="560px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="目标学生">
          <el-input :model-value="detailStudent ? `${detailStudent.name}（${detailStudent.studentNo}）` : ''" disabled />
        </el-form-item>
        <el-form-item label="任务/测试" required>
          <el-select v-model="assignForm.taskNo" filterable placeholder="选择任务库中的任务" style="width:100%">
            <el-option
              v-for="task in taskLibrary"
              :key="task.taskNo"
              :label="`${task.taskName || task.description}｜${task.taskType}`"
              :value="task.taskNo"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.note" type="textarea" :rows="3" placeholder="如：函数基础薄弱，安排补救练习" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible=false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssignment">分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentUser } from '../utils/authContext'
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { assignTask, generateStudentIntervention, getTaskList, searchCourse } from '../api'
import {
  getCourseStudentProfiles, getProfileSummary, getCompetency,
  getRecommendations, getAchievements, getTitle,
  getGrowthHistory, getCompetencyHistory, getTestFeedback
} from '../api/profile'

const courses = ref([])
const selectedCourse = ref('')
const students = ref([])
const loading = ref(false)
const searchKey = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detailStudent = ref(null)
const detailProfile = ref({})
const detailCompetency = ref([])
const detailRecs = ref([])
const detailInterventions = ref([])
const detailAchievements = ref([])
const detailTitleText = ref('')
const detailBadgeCount = ref(0)
const detailGrowthHistory = ref([])
const detailCompHistory = ref([])
const detailFeedback = ref(null)
const detailTab = ref('competency')
const detailRadar = ref(null)
const detailHistoryChart = ref(null)
const assignVisible = ref(false)
const assigning = ref(false)
const interventionLoading = ref(false)
const taskLibrary = ref([])
const assignForm = ref({ taskNo: '', note: '' })

const filteredStudents = computed(() => {
  if (!searchKey.value) return students.value
  const k = searchKey.value.toLowerCase()
  return students.value.filter(s =>
    String(s.studentNo).includes(k) || (s.name || '').toLowerCase().includes(k)
  )
})

const detailAverageScore = computed(() => {
  const scores = detailCompetency.value.map(item => item.score).filter(score => score != null)
  if (!scores.length) return detailStudent.value?.competencyAverage || 0
  return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
})

const detailWeakCount = computed(() => {
  const scores = detailCompetency.value.map(item => item.score).filter(score => score != null)
  if (!scores.length) return detailStudent.value?.weakPointCount || 0
  return scores.filter(score => score < 60).length
})

const detailTitle = computed(() => {
  if (!detailStudent.value) return ''
  return `${detailStudent.value.name}（${detailStudent.value.studentNo}）的画像详情`
})

const detailStatusType = computed(() => {
  const s = detailProfile.value.status
  if (s === '存在风险') return 'danger'
  if (s === '进度滞后') return 'warning'
  if (s === '能力提升') return 'success'
  return ''
})

const statusTagType = (s) => {
  if (s === '存在风险') return 'danger'
  if (s === '进度滞后') return 'warning'
  if (s === '能力提升') return 'success'
  if (s === '正常学习') return ''
  return 'info'
}
const recTypeTag = (t) => ({ review_material: 'danger', practice: 'warning', extended_material: 'success', knowledge_point: '' }[t] || '')
const recTypeLabel = (t) => ({ review_material: '复习', practice: '练习', extended_material: '拓展', knowledge_point: '学习' }[t] || t)
const urgencyTag = (value) => ({ high: 'danger', medium: 'warning', low: 'info', 高: 'danger', 中: 'warning', 低: 'info' }[value] || 'info')
const urgencyLabel = (value) => ({ high: '高优先级', medium: '中优先级', low: '低优先级' }[value] || value || '普通')
const badgeIcon = (n) => ({ '连击王': '🔥', '完美主义': '💎', '速通者': '🏃', 'Pythonic': '🐍', 'Debug之眼': '🔍', '夜枭': '🦉', '助人者': '🤝' }[n] || '🏆')
const sourceLabel = (s) => ({
  answer: '服务端判题证据', quiz: '测验', boss: '综合测验', default: '答题',
  task_complete: '任务完成', exam_pass: '考试通过', streak: '连续学习',
  tower_attempt: '爬塔战斗记录', tower_growth_option: '爬塔选项/库存动作',
  boss_task_submission: 'Boss 任务满分提交'
}[s] || s || '未知来源')
const growthTypeLabel = (type) => ({ hp: '生命', atk: '解题表现', def: '基础掌握', exp: '成长值', coins: '金币', energy: '精力' }[type] || type)
const formatDateTime = value => value ? new Date(value).toLocaleString() : '-'
const badgeFactSummary = (achievement) => {
  try {
    const facts = JSON.parse(achievement.metadata || '{}').facts || {}
    const labels = {
      totalCorrect: '累计答对', consecutiveCorrect: '连续答对', timedComplete: '限时完成',
      fullScore: '满分提交', nightAnswers: '夜间答题', helpfulFeedback: '有效反馈',
      selfCorrections: '纠错题数', pythonicStyleCount: 'Pythonic 次数'
    }
    const values = Object.entries(facts)
      .filter(([, value]) => value === true || (typeof value === 'number' && value > 0))
      .map(([key, value]) => `${labels[key] || key}：${value === true ? '是' : value}`)
    return values.join('，') || '由服务端可信事实判定'
  } catch {
    return '历史记录未保存事实快照'
  }
}

const daysSince = (dateValue) => {
  if (!dateValue) return null
  const time = new Date(dateValue).getTime()
  if (Number.isNaN(time)) return null
  return Math.floor((Date.now() - time) / 86400000)
}

const engagementLabel = (row) => {
  if (!row || !row.hasProfile) return '未激活'
  const days = daysSince(row.lastActivityDate)
  if (days == null) return '待观察'
  if (days <= 3) return '活跃'
  if (days <= 7) return '稳定'
  if (days <= 14) return '偏低'
  return '需关注'
}

const engagementTagType = (row) => {
  const label = engagementLabel(row)
  if (label === '活跃' || label === '稳定') return 'success'
  if (label === '偏低' || label === '待观察') return 'warning'
  if (label === '需关注') return 'danger'
  return 'info'
}

const performanceLabel = (row) => {
  const score = row?.competencyAverage || 0
  if (!row?.hasProfile || score <= 0) return '待观察'
  if (score >= 85) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '待巩固'
  return '需关注'
}

const foundationLabel = (row) => {
  if (!row?.hasProfile) return '待观察'
  const weakCount = row.weakPointCount || 0
  if (weakCount >= 3) return '薄弱点较多'
  if (weakCount > 0) return `${weakCount} 个薄弱点`
  return '较稳定'
}

const foundationTagType = (row) => {
  const weakCount = row?.weakPointCount || 0
  if (!row?.hasProfile) return 'info'
  if (weakCount >= 3) return 'danger'
  if (weakCount > 0) return 'warning'
  return 'success'
}

const growthLabel = (row) => {
  const status = row?.status
  if (!row?.hasProfile) return '未激活'
  if (status === '能力提升') return '上升'
  if (status === '进度滞后' || status === '存在风险') return '需关注'
  return '平稳'
}

const growthTagType = (row) => {
  const label = growthLabel(row)
  if (label === '上升') return 'success'
  if (label === '需关注') return 'danger'
  if (label === '平稳') return ''
  return 'info'
}

const loadCourses = async () => {
  try {
    const { data } = await searchCourse('')
    if (data.code === 200) {
      const user = getCurrentUser()
      if (user.role === 'admin') {
        courses.value = data.data
      } else {
        courses.value = data.data.filter(c => c.teacher === user.name)
      }
      if (courses.value.length) selectedCourse.value = courses.value[0].courseCode
    }
  } catch (e) { /* ignore */ }
}

const loadStudents = async () => {
  if (!selectedCourse.value) return
  loading.value = true
  try {
    const { data } = await getCourseStudentProfiles(selectedCourse.value)
    if (data.code === 200) students.value = data.data
  } catch (e) {
    ElMessage.error('学生画像加载失败')
  }
  loading.value = false
}

const onCourseChange = () => {
  taskLibrary.value = []
  loadStudents()
}

const selectStudent = async (row) => {
  detailStudent.value = row
  detailVisible.value = true
  detailTab.value = 'competency'
  await loadDetailData()
}

const loadTaskLibrary = async () => {
  if (!selectedCourse.value) return
  try {
    const { data } = await getTaskList(selectedCourse.value)
    if (data.code === 200) taskLibrary.value = data.data || []
    else ElMessage.error(data.msg || '任务库加载失败')
  } catch {
    ElMessage.error('任务库加载失败')
  }
}

const openAssignDialog = async (row = null) => {
  if (row) detailStudent.value = row
  if (!detailStudent.value) return
  assignForm.value = { taskNo: '', note: '' }
  assignVisible.value = true
  if (!taskLibrary.value.length) await loadTaskLibrary()
}

const submitAssignment = async () => {
  if (!detailStudent.value || !assignForm.value.taskNo) {
    ElMessage.warning('请选择要分配的任务')
    return
  }
  assigning.value = true
  try {
    const { data } = await assignTask(assignForm.value.taskNo, {
      studentNo: String(detailStudent.value.studentNo),
      note: assignForm.value.note
    })
    if (data.code === 200) {
      ElMessage.success('任务已分配')
      assignVisible.value = false
    } else {
      ElMessage.error(data.msg || '任务分配失败')
    }
  } catch {
    ElMessage.error('任务分配失败')
  } finally {
    assigning.value = false
  }
}

const loadDetailData = async () => {
  if (!detailStudent.value) return
  const sn = parseInt(detailStudent.value.studentNo)
  const cc = parseInt(selectedCourse.value)

  try {
    const [sumRes, recRes, achRes, titleRes] = await Promise.all([
      getProfileSummary(sn, cc),
      getRecommendations(sn, cc),
      getAchievements(sn, cc),
      getTitle(sn, cc)
    ])
    if (sumRes.data.code === 200) {
      detailProfile.value = sumRes.data.data.profile
      detailCompetency.value = sumRes.data.data.competencyScores || []
    }
    if (recRes.data.code === 200) detailRecs.value = recRes.data.data
    if (achRes.data.code === 200) {
      detailAchievements.value = achRes.data.data
      detailBadgeCount.value = detailAchievements.value.filter(a => a.achievementType === 'badge').length
    }
    if (titleRes.data.code === 200) detailTitleText.value = titleRes.data.data
    detailInterventions.value = []
  } catch (e) { /* ignore */ }
}

const onDetailTabChange = (tab) => {
  if (!detailStudent.value) return
  const sn = parseInt(detailStudent.value.studentNo)
  const cc = parseInt(selectedCourse.value)
  if (tab === 'competency') nextTick(() => renderDetailRadar())
  if (tab === 'growthHistory') loadDetailGrowthHistory(sn, cc)
  if (tab === 'competencyHistory') { loadDetailCompHistory(sn, cc); nextTick(() => renderDetailHistoryChart()) }
  if (tab === 'feedback') loadDetailFeedback(sn, cc)
}

const generateIntervention = async () => {
  if (!detailStudent.value || !selectedCourse.value) return
  interventionLoading.value = true
  try {
    const { data } = await generateStudentIntervention(detailStudent.value.studentNo, selectedCourse.value)
    if (data.code === 200) {
      detailInterventions.value = data.data || []
      detailTab.value = 'intervention'
      ElMessage.success('干预建议已生成')
    } else {
      ElMessage.error(data.msg || '干预建议生成失败')
    }
  } finally {
    interventionLoading.value = false
  }
}

const onDetailOpened = () => nextTick(() => renderDetailRadar())

const loadDetailGrowthHistory = async (sn, cc) => {
  try {
    const { data } = await getGrowthHistory(sn, cc)
    if (data.code === 200) detailGrowthHistory.value = data.data
  } catch (e) { /* ignore */ }
}

const loadDetailCompHistory = async (sn, cc) => {
  try {
    const { data } = await getCompetencyHistory(sn, cc, null)
    if (data.code === 200) detailCompHistory.value = data.data
  } catch (e) { /* ignore */ }
}

const loadDetailFeedback = async (sn, cc) => {
  try {
    const { data } = await getTestFeedback(sn, cc)
    if (data.code === 200) detailFeedback.value = data.data
  } catch (e) { /* ignore */ }
}

const renderDetailRadar = () => {
  if (!detailRadar.value || !detailCompetency.value.length) return
  const chart = echarts.init(detailRadar.value)
  chart.setOption({
    radar: { indicator: detailCompetency.value.map(c => ({ name: c.abilityPointName, max: 100 })) },
    series: [{ type: 'radar', data: [{ value: detailCompetency.value.map(c => c.score), name: '能力评分' }],
      areaStyle: { color: 'rgba(64,158,255,0.2)' }, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } }]
  })
}

const renderDetailHistoryChart = () => {
  if (!detailHistoryChart.value || !detailCompHistory.value.length) return
  const chart = echarts.init(detailHistoryChart.value)
  const byPoint = {}
  detailCompHistory.value.forEach(h => {
    if (!byPoint[h.abilityPointId]) byPoint[h.abilityPointId] = []
    byPoint[h.abilityPointId].push(h)
  })
  const series = Object.entries(byPoint).map(([apid, items]) => ({
    name: apid, type: 'line', data: items.reverse().map(h => [h.changedAt, h.newScore]), smooth: true
  }))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: Object.keys(byPoint), type: 'scroll', bottom: 0 },
    xAxis: { type: 'time' }, yAxis: { min: 0, max: 100 }, series
  })
}

onMounted(async () => {
  await loadCourses()
  if (selectedCourse.value) loadStudents()
})
</script>

<style scoped>
.teacher-profile-page { padding: 20px; }
.toolbar { display: flex; align-items: center; }
</style>
