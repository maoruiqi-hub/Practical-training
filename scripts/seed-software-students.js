const BASE_URL = process.env.SEED_BASE_URL || 'http://localhost:8081/practical-training'
const ADMIN_USERNAME = process.env.SEED_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.SEED_PASSWORD || 'admin123'

let cookie = ''

const students = [
  ['7', '林亦辰', '软件工程1班', 'linyichen', '13821010101'],
  ['8', '陈若曦', '软件工程1班', 'chenruoxi', '13821010102'],
  ['9', '周子墨', '软件工程1班', 'zhouzimo', '13821010103'],
  ['10', '刘雨桐', '软件工程1班', 'liuyutong', '13821010104'],
  ['11', '赵明轩', '软件工程1班', 'zhaomingxuan', '13821010105'],
  ['12', '王思涵', '软件工程1班', 'wangsihan', '13821010106'],
  ['13', '孙嘉宁', '软件工程1班', 'sunjianing', '13821010107'],
  ['14', '高梓豪', '软件工程1班', 'gaozihang', '13821010108'],
  ['15', '何沐阳', '软件工程1班', 'hemuyang', '13821010109'],
  ['16', '郭雨晴', '软件工程1班', 'guoyuqing', '13821010110'],
  ['17', '郑浩然', '软件工程1班', 'zhenghaoran', '13821010111'],
  ['18', '唐诗语', '软件工程1班', 'tangshiyu', '13821010112'],
  ['19', '李思远', '软件工程2班', 'lisiyuan', '13821010201'],
  ['20', '宋佳琪', '软件工程2班', 'songjiaqi', '13821010202'],
  ['21', '许文博', '软件工程2班', 'xuwenbo', '13821010203'],
  ['22', '韩雨泽', '软件工程2班', 'hanyuze', '13821010204'],
  ['23', '马欣怡', '软件工程2班', 'maxinyi', '13821010205'],
  ['24', '朱昊天', '软件工程2班', 'zhuhaotian', '13821010206'],
  ['25', '梁可欣', '软件工程2班', 'liangkexin', '13821010207'],
  ['26', '罗俊熙', '软件工程2班', 'luojunxi', '13821010208'],
  ['27', '谢安然', '软件工程2班', 'xieanran', '13821010209'],
  ['28', '曹逸凡', '软件工程2班', 'caoyifan', '13821010210'],
  ['29', '邓梓萱', '软件工程2班', 'dengzixuan', '13821010211'],
  ['30', '彭一诺', '软件工程2班', 'pengyinuo', '13821010212']
]

async function request(path, options = {}) {
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(cookie ? { Cookie: cookie } : {}),
    ...(options.headers || {})
  }
  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  const setCookie = response.headers.get('set-cookie')
  if (setCookie) cookie = setCookie.split(';')[0]
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(`${options.method || 'GET'} ${path} HTTP ${response.status}: ${text}`)
  return data
}

function studentPayload([studentNo, name, className, username, phone]) {
  return {
    studentNo,
    name,
    college: '软件学院',
    className,
    courseGrades: '{}',
    username,
    password: '123456',
    phone
  }
}

async function loginAdmin() {
  const res = await request('/api/teachers/login', {
    method: 'POST',
    body: JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD })
  })
  if (res.code !== 200) throw new Error(`管理员登录失败：${res.msg}`)
}

async function listStudents() {
  const res = await request('/api/students/list')
  if (res.code !== 200) throw new Error(`学生列表读取失败：${res.msg}`)
  return res.data || []
}

async function registerOrUpdate(payload, existingByUsername, existingByNo) {
  const existing = existingByUsername.get(payload.username) || existingByNo.get(payload.studentNo)
  if (existing) {
    const res = await request(`/api/students/${existing.studentNo}`, {
      method: 'PUT',
      body: JSON.stringify({ ...existing, ...payload, studentNo: existing.studentNo })
    })
    if (res.code !== 200) throw new Error(`更新学生失败 ${payload.username}：${res.msg}`)
    return 'updated'
  }

  const res = await request('/api/students/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  if (res.code !== 200) throw new Error(`注册学生失败 ${payload.username}：${res.msg}`)
  return 'created'
}

async function updateZhangSan(existingByUsername) {
  const zhangsan = existingByUsername.get('zhangsan')
  if (!zhangsan) return 'missing'
  const res = await request(`/api/students/${zhangsan.studentNo}`, {
    method: 'PUT',
    body: JSON.stringify({
      ...zhangsan,
      name: '张三',
      college: '软件学院',
      className: '软件工程2班',
      courseGrades: zhangsan.courseGrades || '{}',
      username: zhangsan.username || 'zhangsan',
      password: zhangsan.password || '123456',
      phone: zhangsan.phone || '13821010299'
    })
  })
  if (res.code !== 200) throw new Error(`更新张三失败：${res.msg}`)
  return 'updated'
}

async function main() {
  await loginAdmin()
  let existing = await listStudents()
  let existingByUsername = new Map(existing.map(item => [item.username, item]))
  let existingByNo = new Map(existing.map(item => [item.studentNo, item]))

  const zhangsan = await updateZhangSan(existingByUsername)
  existing = await listStudents()
  existingByUsername = new Map(existing.map(item => [item.username, item]))
  existingByNo = new Map(existing.map(item => [item.studentNo, item]))

  const result = { created: 0, updated: 0 }
  for (const item of students) {
    const status = await registerOrUpdate(studentPayload(item), existingByUsername, existingByNo)
    result[status] += 1
  }

  const finalList = await listStudents()
  const softwareStudents = finalList.filter(item => item.college === '软件学院')
  const classOne = softwareStudents.filter(item => item.className === '软件工程1班').length
  const classTwo = softwareStudents.filter(item => item.className === '软件工程2班').length

  console.log(JSON.stringify({
    zhangsan,
    seededStudents: students.length,
    created: result.created,
    updated: result.updated,
    softwareCollegeStudents: softwareStudents.length,
    softwareEngineeringClass1: classOne,
    softwareEngineeringClass2: classTwo
  }, null, 2))
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
