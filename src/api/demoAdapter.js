const ok = data => ({
  data: { code: 200, data, msg: 'success' },
  status: 200,
  statusText: 'OK',
  headers: { 'x-demo-mode': 'true' },
  config: {}
})

const profile = {
  hp: 92,
  maxHp: 100,
  atk: 68,
  def: 61,
  exp: 760,
  level: 3,
  coins: 128,
  energy: 5
}

const knowledgeNames = [
  'Python 课程导论',
  '基础语法与数据类型',
  '运算符与表达式',
  '程序控制结构',
  '列表与元组',
  '字典与集合',
  '字符串处理',
  '函数定义与调用',
  '模块与包',
  '文件读写',
  '异常处理',
  '面向对象基础',
  '数据分析基础',
  '综合项目实践'
]

const roomTypes = [
  'diagnosis', 'elite', 'treasure', 'diagnosis', 'elite', 'rest', 'diagnosis',
  'elite', 'shop', 'diagnosis', 'elite', 'event', 'diagnosis', 'boss'
]

const nodes = knowledgeNames.map((name, index) => ({
  runId: 'demo-run',
  nodeId: `demo-node-${index + 1}`,
  nodeOrder: index + 1,
  row: index + 1,
  col: 1,
  knowledgePointId: String(1001 + index),
  knowledgePointName: name,
  masteryRate: index < 4 ? 82 - index * 4 : index === 4 ? 56 : 50,
  masterySource: 'knowledge_mastery',
  abilityPointId: String(2001 + Math.floor(index / 2)),
  abilityPointName: ['编程基础能力', '流程控制能力', '数据处理能力', '函数抽象能力'][Math.min(3, Math.floor(index / 2))],
  status: index < 4 ? 'cleared' : index === 4 ? 'weak' : 'locked',
  roomType: roomTypes[index]
}))

const questions = [
  { questionId: 'q-1', knowledgePointId: '1005', type: 'single', stem: '列表追加一个元素应使用哪个方法？', options: ['append', 'extend', 'insertAll', 'push'], answer: 'A', score: 10, difficulty: 2 },
  { questionId: 'q-2', knowledgePointId: '1005', type: 'single', stem: '下列哪个对象是不可变序列？', options: ['list', 'set', 'tuple', 'dict'], answer: 'C', score: 10, difficulty: 2 },
  { questionId: 'q-3', knowledgePointId: '1005', type: 'fill', stem: '使用 ______ 可以获取列表元素个数。', answer: 'len', score: 10, difficulty: 2 },
  { questionId: 'q-4', knowledgePointId: '1006', type: 'single', stem: '字典通过什么访问对应的值？', options: ['索引', '键', '集合', '切片'], answer: 'B', score: 10, difficulty: 2 },
  { questionId: 'q-5', knowledgePointId: '1006', type: 'multi', stem: '集合具有哪些特征？', options: ['元素唯一', '支持去重', '固定有序', '可做集合运算'], answer: 'A,B,D', score: 10, difficulty: 3 },
  { questionId: 'q-6', knowledgePointId: '1004', type: 'single', stem: '循环中跳过本次迭代应使用哪个关键字？', options: ['break', 'continue', 'return', 'pass'], answer: 'B', score: 10, difficulty: 2 }
]

const abilityPoints = [
  { abilityPointId: '2001', name: '编程基础能力', description: '理解语法、变量与流程控制。' },
  { abilityPointId: '2002', name: '数据结构处理能力', description: '使用列表、字典和字符串解决问题。' },
  { abilityPointId: '2003', name: '函数抽象能力', description: '通过函数与模块组织程序。' }
]

const knowledgePoints = knowledgeNames.slice(0, 7).map((name, index) => ({
  knowledgePointId: String(1001 + index),
  name,
  importance: index % 3 + 1
}))

const mappings = knowledgePoints.map((point, index) => ({
  abilityPointId: String(2001 + Math.min(2, Math.floor(index / 3))),
  knowledgePointId: point.knowledgePointId
}))

function pickData(config) {
  const url = String(config.url || '')
  const method = String(config.method || 'get').toLowerCase()

  if (url === '/api/students/login') {
    return { studentNo: '1', name: '张三', username: 'zhangsan', college: '软件学院', className: '软件工程2班' }
  }
  if (url === '/api/teachers/login') {
    return { teacherNo: '2', name: '李明', username: 'liming', role: 'teacher', college: '软件学院' }
  }
  if (/\/api\/students\/[^/]+\/profile$/.test(url)) return { profile }
  if (/\/api\/students\/[^/]+\/tower-run$/.test(url)) {
    return { runId: 'demo-run', courseCode: '1', status: 'active', nodes }
  }
  if (/\/tower-run\/[^/]+\/nodes\/[^/]+\/enter$/.test(url)) {
    return { evaluationId: 'demo-evaluation', profile }
  }
  if (/\/tower-run\/[^/]+\/nodes\/[^/]+\/complete$/.test(url)) {
    return { runId: 'demo-run', courseCode: '1', status: 'active', nodes }
  }
  if (/\/tower-run\/[^/]+\/nodes\/[^/]+\/question-pack$/.test(url)) {
    return {
      packId: 'demo-pack',
      questions,
      strategy: {
        strategyVersion: 4,
        selectionPolicy: 'elite-3-current-2-same-ability-1-historical-weakness',
        currentKnowledgeCount: 3,
        sameAbilityCount: 2,
        historicalWeaknessCount: 1,
        quotaFallback: false
      }
    }
  }
  if (/\/tower-run\/[^/]+\/nodes\/[^/]+$/.test(url)) return nodes[4]
  if (url === '/api/leaderboard' || url === '/api/profile/leaderboard') {
    return [
      { studentNo: '7', studentName: '林亦辰', score: 86, rank: 1 },
      { studentNo: '1', studentName: '张三', score: 78, rank: 2 },
      { studentNo: '12', studentName: '王思涵', score: 74, rank: 3 }
    ]
  }
  if (url === '/api/courses/search' || url === '/api/courses/list') {
    return [{ courseCode: '1', courseName: 'Python 程序设计', teacher: '李明', teacherNo: '2', credits: 4, hours: 64 }]
  }
  if (url === '/api/ability-map') return { abilityPoints, mappings }
  if (url === '/api/knowledge-graph') return { nodes: knowledgePoints, edges: [] }
  if (url.startsWith('/api/knowledge-mastery/student/')) {
    return knowledgePoints.map((point, index) => ({
      knowledgePointId: point.knowledgePointId,
      masteryScore: [78, 74, 70, 63, 56, 67, 72][index],
      sourceType: 'demo_evidence'
    }))
  }
  if (/\/ability-radar$/.test(url)) {
    return {
      mode: 'current',
      dimensions: abilityPoints.map((point, index) => ({
        abilityPointId: point.abilityPointId,
        name: point.name,
        beforeScore: [68, 61, 55][index],
        score: [74, 67, 64][index],
        delta: [6, 6, 9][index]
      })),
      summary: '函数抽象能力提升明显，建议继续复练变量作用域。'
    }
  }
  if (url.startsWith('/api/profile/')) {
    return { profile, status: '正常学习', weakPoints: [{ name: '变量作用域', score: 56 }], nextAction: '继续完成列表与元组训练' }
  }
  if (url === '/api/tasks' || url === '/api/submissions/my') return []
  if (url.includes('/submissions')) return []
  if (url === '/api/classes') {
    return [{ id: 'class-1', className: '软件工程2班', courseId: '1', studentCount: 30 }]
  }
  if (method !== 'get') return { demo: true }
  return []
}

export default function demoAdapter(config) {
  const response = ok(pickData(config))
  response.config = config
  return Promise.resolve(response)
}
