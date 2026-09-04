if (process.env.SEED_ALLOW_DEMO !== '1') {
  throw new Error('这是演示数据脚本。请仅在测试环境显式设置 SEED_ALLOW_DEMO=1 后执行。')
}

const BASE_URL = process.env.SEED_BASE_URL || 'http://localhost:8081/practical-training'
const ADMIN_USERNAME = process.env.SEED_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.SEED_PASSWORD || 'admin123'
const COURSE_CODE = process.env.SEED_COURSE_CODE || '1'
const STUDENT_PASSWORD = process.env.SEED_STUDENT_PASSWORD || '123456'
const FORCE_PROFILE_RESUBMIT = process.env.SEED_FORCE_PROFILE_RESUBMIT === '1'

const quizPlans = [
  {
    taskName: 'Python基础语法随堂测验',
    description: 'Python基础语法随堂测验',
    lessonNo: '2',
    lessonRange: ['1', '2', '3'],
    questionCount: 9
  },
  {
    taskName: 'Python控制结构与集合测验',
    description: 'Python控制结构与集合测验',
    lessonNo: '6',
    lessonRange: ['4', '5', '6', '7'],
    questionCount: 10
  },
  {
    taskName: 'Python函数与文件阶段测验',
    description: 'Python函数与文件阶段测验',
    lessonNo: '10',
    lessonRange: ['8', '9', '10', '11'],
    questionCount: 10
  }
]

const studentPlans = [
  { username: 'xuqingyuan', label: '画像优秀', rates: [0.9, 0.9, 0.8] },
  { username: 'shenjiayi', label: '画像中上', rates: [0.8, 0.7, 0.8] },
  { username: 'yujinghao', label: '画像中等', rates: [0.6, 0.6, 0.6] },
  { username: 'qinyao', label: '画像薄弱', rates: [0.4, 0.5, 0.4] },
  { username: 'luomingze', label: '画像风险', rates: [0.2, 0.3, 0.2] },
  { username: 'linyichen', label: '优秀稳定', rates: [1, 0.9, 0.9] },
  { username: 'chenruoxi', label: '优秀波动', rates: [0.9, 0.8, 0.9] },
  { username: 'zhouzimo', label: '稳步掌握', rates: [0.8, 0.8, 0.7] },
  { username: 'liuyutong', label: '中等水平', rates: [0.7, 0.6, 0.7] },
  { username: 'zhaomingxuan', label: '中等偏弱', rates: [0.6, 0.5, 0.6] },
  { username: 'lisiyuan', label: '后续追赶', rates: [0.5, 0.6, 0.7] },
  { username: 'zhangsan', label: '薄弱学生', rates: [0.4, 0.4, 0.5] },
  { username: 'songjiaqi', label: '基础薄弱', rates: [0.3, 0.4, 0.4] },
  { username: 'xuwenbo', label: '风险学生', rates: [0.2, 0.3, 0.3] },
  { username: 'hanyuze', label: '缺口明显', rates: [0.3, 0.2, 0.4] }
]

const profileCompatibleStudents = [
  { studentNo: '2', name: '徐清源', className: '软件工程1班', username: 'xuqingyuan', phone: '13821010302' },
  { studentNo: '3', name: '沈佳怡', className: '软件工程1班', username: 'shenjiayi', phone: '13821010303' },
  { studentNo: '4', name: '俞景皓', className: '软件工程1班', username: 'yujinghao', phone: '13821010304' },
  { studentNo: '5', name: '秦瑶', className: '软件工程2班', username: 'qinyao', phone: '13821010305' },
  { studentNo: '6', name: '罗明泽', className: '软件工程2班', username: 'luomingze', phone: '13821010306' }
]

let adminCookie = ''

function getCookie(headers) {
  const setCookie = headers.get('set-cookie')
  return setCookie ? setCookie.split(';')[0] : ''
}

async function request(path, options = {}, cookie = adminCookie) {
  const headers = {
    ...(cookie ? { Cookie: cookie } : {}),
    ...(options.headers || {})
  }
  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  const newCookie = getCookie(response.headers)
  if (newCookie && cookie === adminCookie) adminCookie = newCookie
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(`${options.method || 'GET'} ${path} HTTP ${response.status}: ${text}`)
  if (data && data.code !== 200) throw new Error(`${options.method || 'GET'} ${path}: ${data.msg || data.message || text}`)
  return { data, cookie: newCookie || cookie }
}

async function loginAdmin() {
  const result = await request('/api/teachers/login', {
    method: 'POST',
    body: JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD })
  }, '')
  adminCookie = result.cookie
}

async function loginStudent(username) {
  const response = await fetch(`${BASE_URL}/api/students/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: STUDENT_PASSWORD })
  })
  const cookie = getCookie(response.headers)
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok || !data || data.code !== 200) {
    throw new Error(`学生登录失败 ${username}: ${text}`)
  }
  return { student: data.data, cookie }
}

async function listTasks() {
  const res = await request(`/api/tasks?course_id=${encodeURIComponent(COURSE_CODE)}`)
  return res.data.data || []
}

async function listQuestions() {
  const res = await request(`/api/questions?course_id=${encodeURIComponent(COURSE_CODE)}`)
  return res.data.data || []
}

async function listStudents() {
  const res = await request('/api/students/list')
  return res.data.data || []
}

async function listTaskQuestionIds(taskNo) {
  const res = await request(`/api/questions/task/${taskNo}`)
  return new Set((res.data.data || []).map(item => String(item.questionId)))
}

async function listSubmissions(taskNo) {
  const res = await request(`/api/tasks/${taskNo}/submissions`)
  return res.data.data || []
}

async function createTask(plan, selectedQuestions) {
  const form = new FormData()
  form.append('courseCode', COURSE_CODE)
  form.append('taskName', plan.taskName)
  form.append('lessonNo', plan.lessonNo)
  form.append('knowledgePoints', [...new Set(selectedQuestions.map(q => q.knowledgePointId).filter(Boolean))].join(','))
  form.append('taskType', '在线测验')
  form.append('description', plan.description)
  form.append('submitMethod', '在线答题')
  form.append('score', String(selectedQuestions.reduce((sum, q) => sum + (Number(q.score) || 0), 0)))
  form.append('gradingRule', '客观题系统自动评阅')
  form.append('status', 'published')
  form.append('allowLate', '1')
  form.append('maxAttempts', '20')

  const res = await request('/api/tasks', { method: 'POST', body: form })
  return String(res.data.data)
}

async function ensureProfileCompatibleStudents() {
  const existing = await listStudents()
  const byNo = new Map(existing.map(item => [String(item.studentNo), item]))
  const byUsername = new Map(existing.map(item => [item.username, item]))
  const result = { created: 0, updated: 0 }

  for (const item of profileCompatibleStudents) {
    const payload = {
      ...item,
      college: '软件学院',
      courseGrades: '{}',
      password: STUDENT_PASSWORD
    }
    const matched = byNo.get(item.studentNo) || byUsername.get(item.username)
    if (matched) {
      await request(`/api/students/${matched.studentNo}`, {
        method: 'PUT',
        body: JSON.stringify({ ...matched, ...payload, studentNo: matched.studentNo })
      })
      result.updated += 1
    } else {
      await request('/api/students/register', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
      result.created += 1
    }
  }
  return result
}

async function ensureTask(plan, selectedQuestions, existingTasks) {
  const existing = existingTasks.find(task => task.taskName === plan.taskName || task.description === plan.description)
  const taskNo = existing ? String(existing.taskNo) : await createTask(plan, selectedQuestions)
  if (existing) {
    await request(`/api/tasks/${COURSE_CODE}/${taskNo}`, {
      method: 'PUT',
      body: JSON.stringify({
        taskName: plan.taskName,
        lessonNo: plan.lessonNo,
        knowledgePoints: [...new Set(selectedQuestions.map(q => q.knowledgePointId).filter(Boolean))].join(','),
        taskType: '在线测验',
        description: plan.description,
        submitMethod: '在线答题',
        score: selectedQuestions.reduce((sum, q) => sum + (Number(q.score) || 0), 0),
        gradingRule: '客观题系统自动评阅',
        status: 'published',
        allowLate: 1,
        maxAttempts: 20
      })
    })
  }

  const bound = await listTaskQuestionIds(taskNo)
  const missing = selectedQuestions.map(q => String(q.questionId)).filter(id => !bound.has(id))
  if (missing.length) {
    await request(`/api/questions/task/${taskNo}`, { method: 'POST', body: JSON.stringify(missing) })
  }
  return taskNo
}

async function ensureAssignments(preparedTasks) {
  const students = await listStudents()
  const studentNos = studentPlans.map(plan => {
    const student = students.find(item => item.username === plan.username)
    if (!student) throw new Error(`未找到种子学生：${plan.username}`)
    return String(student.studentNo)
  })

  for (const task of preparedTasks) {
    await request('/api/tasks/assignments', {
      method: 'POST',
      body: JSON.stringify({
        taskNo: task.taskNo,
        studentNos,
        note: '学情分析演示数据'
      })
    })
  }
}

function selectQuestions(plan, allQuestions) {
  const allowedTypes = new Set(['single', 'multi', 'fill'])
  const candidates = allQuestions
    .filter(q => allowedTypes.has(q.type))
    .filter(q => plan.lessonRange.includes(String(q.lessonNo)))
    .sort((a, b) => Number(a.lessonNo) - Number(b.lessonNo) || Number(a.questionId) - Number(b.questionId))

  const selected = []
  const seenLessons = new Set()
  for (const question of candidates) {
    if (selected.length >= plan.questionCount) break
    if (!seenLessons.has(String(question.lessonNo))) {
      selected.push(question)
      seenLessons.add(String(question.lessonNo))
    }
  }
  for (const question of candidates) {
    if (selected.length >= plan.questionCount) break
    if (!selected.some(item => String(item.questionId) === String(question.questionId))) selected.push(question)
  }
  if (selected.length < Math.min(6, plan.questionCount)) {
    throw new Error(`${plan.taskName} 可用客观题不足，当前只有 ${selected.length} 道`)
  }
  return selected
}

function wrongAnswer(question) {
  if (question.type === 'multi') return ''
  if (question.type === 'single') return 'Z'
  return 'wrong'
}

function buildAnswers(questions, correctRate) {
  const correctCount = Math.round(questions.length * correctRate)
  return questions.map((question, index) => ({
    no: String(question.questionId),
    response: index < correctCount ? question.answer : wrongAnswer(question)
  }))
}

async function submitQuiz(taskNo, questions, studentPlan, quizIndex) {
  const { student, cookie } = await loginStudent(studentPlan.username)
  const answers = buildAnswers(questions, studentPlan.rates[quizIndex])
  const form = new FormData()
  form.append('content', JSON.stringify(answers))
  const res = await request(`/api/tasks/${taskNo}/submit`, { method: 'POST', body: form }, cookie)
  return { studentNo: student.studentNo, name: student.name, message: res.data.data }
}

async function main() {
  await loginAdmin()
  const profileStudents = await ensureProfileCompatibleStudents()
  const [questions, existingTasks] = await Promise.all([listQuestions(), listTasks()])
  const preparedTasks = []

  for (const plan of quizPlans) {
    const selectedQuestions = selectQuestions(plan, questions)
    const taskNo = await ensureTask(plan, selectedQuestions, existingTasks)
    preparedTasks.push({ ...plan, taskNo, questions: selectedQuestions })
  }

  await ensureAssignments(preparedTasks)

  const summary = {
    courseCode: COURSE_CODE,
    tasks: preparedTasks.map(task => ({
      taskNo: task.taskNo,
      taskName: task.taskName,
      questionCount: task.questions.length
    })),
    submitted: 0,
    skipped: 0,
    profileCompatibleStudents: profileStudents,
    students: []
  }

  for (let quizIndex = 0; quizIndex < preparedTasks.length; quizIndex += 1) {
    const task = preparedTasks[quizIndex]
    const existingSubmissions = await listSubmissions(task.taskNo)
    const submittedStudentNos = new Set(existingSubmissions.map(item => String(item.studentNo)))

    for (const studentPlan of studentPlans) {
      const login = await loginStudent(studentPlan.username)
      const shouldForceResubmit = FORCE_PROFILE_RESUBMIT && profileCompatibleStudents
        .some(item => item.username === studentPlan.username)
      if (submittedStudentNos.has(String(login.student.studentNo)) && !shouldForceResubmit) {
        summary.skipped += 1
        summary.students.push({
          taskName: task.taskName,
          username: studentPlan.username,
          studentNo: login.student.studentNo,
          name: login.student.name,
          label: studentPlan.label,
          status: 'skipped-existing'
        })
        continue
      }
      const submitted = await submitQuiz(task.taskNo, task.questions, studentPlan, quizIndex)
      summary.submitted += 1
      summary.students.push({
        taskName: task.taskName,
        username: studentPlan.username,
        studentNo: submitted.studentNo,
        name: submitted.name,
        label: studentPlan.label,
        targetCorrectRate: studentPlan.rates[quizIndex],
        status: 'submitted'
      })
    }
  }

  const verification = []
  for (const task of preparedTasks) {
    const submissions = await listSubmissions(task.taskNo)
    verification.push({
      taskNo: task.taskNo,
      taskName: task.taskName,
      activeSubmissions: submissions.length,
      scores: submissions.map(item => `${item.studentName || item.studentNo}:${item.score}`).join(', ')
    })
  }

  console.log(JSON.stringify({ ...summary, verification }, null, 2))
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
