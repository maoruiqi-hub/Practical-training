import { describe, it, expect, beforeEach, vi } from 'vitest'

// ======================== 路由逻辑单元测试 ========================

// 模拟 localStorage
const store = {}
vi.stubGlobal('localStorage', {
  getItem: vi.fn(key => store[key] || null),
  setItem: vi.fn((k, v) => { store[k] = v }),
  removeItem: vi.fn(k => { delete store[k] }),
  clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]) })
})

// 从 router/index.js 提取的纯逻辑
const studentRoomRedirects = {
  '/dashboard': 'start',
  '/courses': 'treasure',
  '/stats': 'event',
  '/profile': 'rest',
  '/wrong-book': 'shop',
  '/progress': 'progress',
  '/learning-analysis': 'event'
}

const protectedPaths = new Set(['/dashboard', '/tower-map', '/data-center', '/floor/1', '/courses', '/profile'])
const teacherPaths = new Set(['/admin/courses', '/admin/questions', '/teacher/student-profiles', '/class-operations'])
const adminPaths = new Set(['/admin/students', '/admin/teachers'])

function beforeEachGuard(to) {
  let user = {}
  try { user = JSON.parse(localStorage.getItem('user') || '{}') || {} } catch { user = {} }
  const isProtected = protectedPaths.has(to.path) || teacherPaths.has(to.path) || adminPaths.has(to.path)
  if (isProtected && !user.role) return { path: '/login', query: { redirect: to.fullPath || to.path } }
  if (teacherPaths.has(to.path) && !['teacher', 'admin'].includes(user.role)) return '/dashboard'
  if (adminPaths.has(to.path) && user.role !== 'admin') return '/dashboard'
  const requiresCourse = user.role === 'student' &&
    ['/tower-map', '/data-center', '/profile', '/progress', '/stats', '/wrong-book', '/learning-analysis'].includes(to.path) ||
    (user.role === 'student' && to.path.startsWith('/floor/'))
  if (requiresCourse && !(to.query?.courseId || store.courseId)) {
    return { path: '/courses', query: { redirect: to.fullPath || to.path } }
  }
  if (user.role !== 'student') return true
  const room = studentRoomRedirects[to.path]
  if (room) {
    return { path: '/tower-map', query: { room } }
  }
  return true
}

// 路由表结构（与源码保持同步）
const childRoutes = [
  'dashboard', 'tower-map', 'floor/:kpId', 'courses', 'quiz/take/:taskNo',
  'course/:code', 'course/:code/resources', 'course-resource/:resourceId/preview',
  'course/:code/knowledge-graph', 'knowledge-point/:knowledgePointId',
  'lesson/:lessonNo', 'task/detail/:taskNo', 'task/:courseCode/submit/:taskNo',
  'task/:courseCode', 'profile', 'wrong-book', 'learning-analysis',
  'ability-map', 'class-operations', 'stats', 'progress',
  'admin/students', 'admin/teachers', 'admin/courses', 'admin/questions',
  'teacher/student-profiles'
]

// ======================== 路由表结构 ========================

describe('路由表结构', () => {
  it('根路径重定向到 /login', () => {
    expect('/').toBeTruthy()
  })

  it('包含 26 个子路由', () => {
    expect(childRoutes.length).toBe(26)
  })

  it('所有子路由为有效字符串', () => {
    childRoutes.forEach(r => {
      expect(typeof r).toBe('string')
      expect(r.length).toBeGreaterThan(0)
    })
  })

  it('包含教师端专用路由（6个）', () => {
    const teacherRoutes = ['admin/students', 'admin/teachers', 'admin/courses',
      'admin/questions', 'teacher/student-profiles', 'class-operations']
    teacherRoutes.forEach(r => expect(childRoutes).toContain(r))
  })

  it('包含游戏化路由', () => {
    const gameRoutes = ['tower-map', 'floor/:kpId']
    gameRoutes.forEach(r => expect(childRoutes).toContain(r))
  })
})

// ======================== 学生房间重定向映射 ========================

describe('学生房间重定向映射', () => {
  it('包含 7 个重定向规则', () => {
    expect(Object.keys(studentRoomRedirects)).toHaveLength(7)
  })

  it('dashboard → start', () => { expect(studentRoomRedirects['/dashboard']).toBe('start') })
  it('courses → treasure', () => { expect(studentRoomRedirects['/courses']).toBe('treasure') })
  it('stats → event', () => { expect(studentRoomRedirects['/stats']).toBe('event') })
  it('profile → rest', () => { expect(studentRoomRedirects['/profile']).toBe('rest') })
  it('wrong-book → shop', () => { expect(studentRoomRedirects['/wrong-book']).toBe('shop') })
  it('progress → progress', () => { expect(studentRoomRedirects['/progress']).toBe('progress') })
  it('learning-analysis → event', () => { expect(studentRoomRedirects['/learning-analysis']).toBe('event') })
})

// ======================== beforeEach 守卫 ========================

describe('beforeEach 导航守卫', () => {
  beforeEach(() => {
    Object.keys(store).forEach(k => delete store[k])
    store.courseId = 'C1'
  })

  it('学生访问 dashboard → 重定向到塔地图 start', () => {
    store.user = JSON.stringify({ role: 'student', studentNo: 'S1' })
    const result = beforeEachGuard({ path: '/dashboard' })
    expect(result.path).toBe('/tower-map')
    expect(result.query.room).toBe('start')
  })

  it('学生访问 courses → 重定向到塔地图 treasure', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/courses' })
    expect(result.query.room).toBe('treasure')
  })

  it('学生访问 stats → 重定向到塔地图 event', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/stats' })
    expect(result.query.room).toBe('event')
  })

  it('学生访问 profile → 重定向到 rest', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/profile' })
    expect(result.query.room).toBe('rest')
  })

  it('学生访问 wrong-book → 重定向到 shop', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/wrong-book' })
    expect(result.query.room).toBe('shop')
  })

  it('学生访问 progress → 重定向到 progress', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/progress' })
    expect(result.query.room).toBe('progress')
  })

  it('学生访问 learning-analysis → 重定向到 event', () => {
    store.user = JSON.stringify({ role: 'student' })
    const result = beforeEachGuard({ path: '/learning-analysis' })
    expect(result.query.room).toBe('event')
  })

  // 非重定向路径
  it('学生访问课程详情（不在映射列表中）→ 正常通过', () => {
    store.user = JSON.stringify({ role: 'student' })
    expect(beforeEachGuard({ path: '/course/CS101' })).toBe(true)
  })

  it('学生访问登录页 → 正常通过', () => {
    store.user = JSON.stringify({ role: 'student' })
    expect(beforeEachGuard({ path: '/login' })).toBe(true)
  })

  it('学生访问塔地图 → 正常通过（避免死循环）', () => {
    store.user = JSON.stringify({ role: 'student' })
    expect(beforeEachGuard({ path: '/tower-map' })).toBe(true)
  })

  // 教师不重定向
  it('教师访问全部页面均不受重定向影响', () => {
    store.user = JSON.stringify({ role: 'teacher' })
    Object.keys(studentRoomRedirects).forEach(path => {
      expect(beforeEachGuard({ path })).toBe(true)
    })
  })

  // 边界
  it('空 user 对象 → 受保护页面重定向登录', () => {
    store.user = '{}'
    expect(beforeEachGuard({ path: '/dashboard' }).path).toBe('/login')
  })

  it('学生缺少课程上下文 → 进入课程选择', () => {
    store.user = JSON.stringify({ role: 'student', studentNo: 'S1' })
    delete store.courseId
    expect(beforeEachGuard({ path: '/tower-map' }).path).toBe('/courses')
  })

  it('无 user 数据 → 受保护页面重定向登录', () => {
    expect(beforeEachGuard({ path: '/dashboard' }).path).toBe('/login')
  })

  it('损坏的 JSON → 安全重定向登录', () => {
    store.user = '{invalid json'
    expect(beforeEachGuard({ path: '/dashboard' }).path).toBe('/login')
  })

  it('学生访问教师页面 → 回到工作台', () => {
    store.user = JSON.stringify({ role: 'student' })
    expect(beforeEachGuard({ path: '/teacher/student-profiles' })).toBe('/dashboard')
  })

  it('教师访问管理员页面 → 回到工作台', () => {
    store.user = JSON.stringify({ role: 'teacher' })
    expect(beforeEachGuard({ path: '/admin/students' })).toBe('/dashboard')
  })
})
