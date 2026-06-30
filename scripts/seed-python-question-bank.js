const BASE_URL = process.env.SEED_BASE_URL || 'http://localhost:3000/practical-training'
const USERNAME = process.env.SEED_USERNAME || 'admin'
const PASSWORD = process.env.SEED_PASSWORD || 'admin123'
const COURSE_CODE = '1'

let cookie = ''

const lessons = {
  intro: '1',
  syntax: '2',
  operators: '3',
  control: '4',
  listTuple: '5',
  dictSet: '6',
  string: '7',
  function: '8',
  module: '9',
  file: '10',
  exception: '11',
  oopBasic: '12',
  oopAdvanced: '13',
  data: '14',
  web: '15',
  crawler: '16',
  project: '17'
}

const knowledgePoints = [
  ['intro', 'Python简介与环境搭建', '理解Python语言特点、解释器、虚拟环境和开发工具配置。', '第1章 Python简介与环境搭建', 3],
  ['syntax', 'Python基础语法与数据类型', '掌握变量、缩进、注释、基础数据类型和类型转换。', '第2章 Python基础语法与数据类型', 5],
  ['operators', '运算符与表达式', '掌握算术、比较、逻辑、成员、身份运算符和优先级。', '第3章 运算符与表达式', 4],
  ['control', '程序控制结构', '掌握条件分支、for循环、while循环、break和continue。', '第4章 程序控制结构', 5],
  ['listTuple', '列表与元组', '掌握列表、元组、索引切片、常用方法和推导式。', '第5章 列表与元组', 5],
  ['dictSet', '字典与集合', '掌握字典键值操作、集合去重以及交并差运算。', '第6章 字典与集合', 5],
  ['string', '字符串处理', '掌握字符串切片、格式化、常用方法和正则表达式基础。', '第7章 字符串处理', 4],
  ['function', '函数定义与调用', '掌握函数定义、参数、返回值、作用域、lambda和装饰器基础。', '第8章 函数定义与调用', 5],
  ['module', '模块与包', '掌握import机制、自定义模块、包结构、pip和依赖管理。', '第9章 模块与包', 4],
  ['file', '文件操作', '掌握文本文件、CSV、JSON读写和with上下文管理器。', '第10章 文件操作', 5],
  ['exception', '异常处理', '掌握try-except-else-finally、异常类型和自定义异常。', '第11章 异常处理', 4],
  ['oopBasic', '面向对象编程基础', '掌握类、对象、实例属性、方法、封装、继承和多态。', '第12章 面向对象编程基础', 5],
  ['oopAdvanced', '面向对象高级特性', '掌握类方法、静态方法、属性方法、抽象类和Mixin。', '第13章 面向对象高级特性', 4],
  ['data', '数据分析入门', '掌握NumPy数组、Pandas DataFrame和基础可视化。', '第14章 数据分析入门', 4],
  ['web', 'Web开发入门', '掌握Flask路由、请求响应、模板和REST API基础。', '第15章 Web开发入门', 3],
  ['crawler', '爬虫实战', '掌握HTTP请求、解析页面、反爬策略和数据持久化。', '第16章 爬虫实战', 3],
  ['project', 'Python项目实战综合案例', '综合运用Python完成数据处理、Web或自动化项目。', '第17章 Python项目实战综合案例', 5]
]

const relationPairs = [
  ['intro', 'syntax'],
  ['syntax', 'operators'],
  ['operators', 'control'],
  ['control', 'listTuple'],
  ['listTuple', 'dictSet'],
  ['dictSet', 'string'],
  ['string', 'function'],
  ['function', 'module'],
  ['module', 'file'],
  ['file', 'exception'],
  ['function', 'oopBasic'],
  ['oopBasic', 'oopAdvanced'],
  ['listTuple', 'data'],
  ['dictSet', 'data'],
  ['function', 'web'],
  ['string', 'crawler'],
  ['file', 'project'],
  ['exception', 'project'],
  ['oopBasic', 'project'],
  ['data', 'project'],
  ['web', 'project'],
  ['crawler', 'project']
]

const specs = {
  intro: {
    single: ['关于Python虚拟环境，下列说法正确的是？', ['虚拟环境可以隔离不同项目的依赖包', '虚拟环境会自动提升代码运行速度', '虚拟环境只能在Linux系统使用', '虚拟环境会把源码编译成机器码'], 'A'],
    multi: ['下列哪些属于Python开发环境搭建时常见的组成部分？', ['Python解释器', '包管理工具pip', '代码编辑器或IDE', '显卡驱动控制面板'], 'A,B,C'],
    fill: ['使用命令行查看当前Python版本的常用命令是____。', 'python --version'],
    essay: ['说明解释型语言与编译型语言在运行方式上的主要差异，并结合Python举例。'],
    program: ['编写一个脚本，打印Python版本号和当前工作目录，用于验证开发环境是否可用。']
  },
  syntax: {
    single: ['在Python中，哪个语句可以查看变量x的数据类型？', ['type(x)', 'typeof(x)', 'class(x)', 'x.type()'], 'A'],
    multi: ['下列哪些是Python内置基础数据类型？', ['int', 'float', 'str', 'table'], 'A,B,C'],
    fill: ['将字符串"123"转换为整数的表达式是____。', 'int("123")'],
    essay: ['解释Python中缩进的语法意义，并说明缩进错误可能导致的问题。'],
    program: ['编写函数normalize_age(value)，将字符串或数字形式的年龄转换为整数；非法输入返回None。']
  },
  operators: {
    single: ['表达式 7 // 2 的结果是？', ['3', '3.5', '4', '1'], 'A'],
    multi: ['下列哪些表达式的结果为True？', ['3 in [1, 2, 3]', 'not False', '5 > 3 and 2 < 1', '"py" == "py"'], 'A,B,D'],
    fill: ['判断变量x是否在闭区间[0, 100]内的Python表达式可写为____。', '0 <= x <= 100'],
    essay: ['比较==与is的区别，并说明在判断字符串内容时应优先使用哪一个。'],
    program: ['编写函数calc_discount(price, member)，会员打8折，非会员不打折，返回保留两位小数的应付金额。']
  },
  control: {
    single: ['在for循环中，continue语句的作用是？', ['跳过本轮循环剩余语句，进入下一轮', '立即结束整个程序', '结束整个循环', '重新定义循环变量'], 'A'],
    multi: ['下列哪些结构可以用于重复执行代码块？', ['for', 'while', 'if', 'try'], 'A,B'],
    fill: ['生成0到4整数序列的表达式是____。', 'range(5)'],
    essay: ['说明for循环和while循环适用场景的差异，并各举一个例子。'],
    program: ['编写函数sum_even(nums)，使用循环统计列表中所有偶数的和。']
  },
  listTuple: {
    single: ['列表 nums = [1, 2, 3] 执行 nums.append(4) 后，nums的值是？', ['[1, 2, 3, 4]', '[4, 1, 2, 3]', '[1, 2, 3]', 'None'], 'A'],
    multi: ['下列哪些操作会修改原列表对象？', ['list.append(x)', 'list.sort()', 'list.copy()', 'list.reverse()'], 'A,B,D'],
    fill: ['取列表items最后一个元素的表达式是____。', 'items[-1]'],
    essay: ['比较列表和元组的可变性差异，并说明何时更适合使用元组。'],
    program: ['编写函数top_three(scores)，返回分数列表中从高到低排列的前三个分数。']
  },
  dictSet: {
    single: ['字典 d = {"a": 1} 中，安全读取不存在键"b"并给默认值0的方法是？', ['d.get("b", 0)', 'd["b", 0]', 'd.fetch("b", 0)', 'get(d, "b")'], 'A'],
    multi: ['下列哪些属于集合set支持的典型操作？', ['交集', '并集', '差集', '按索引取值'], 'A,B,C'],
    fill: ['创建空字典应使用____。', '{}'],
    essay: ['说明字典键为什么必须是可哈希对象，并举例说明列表不能作为字典键的原因。'],
    program: ['编写函数word_count(words)，接收单词列表，返回每个单词出现次数的字典。']
  },
  string: {
    single: ['表达式 "Python"[0:2] 的结果是？', ['Py', 'Pyt', 'yt', 'on'], 'A'],
    multi: ['下列哪些是字符串常用方法？', ['strip()', 'split()', 'join()', 'append()'], 'A,B,C'],
    fill: ['使用f-string输出变量name的写法是____。', 'f"{name}"'],
    essay: ['说明字符串不可变的含义，并分析频繁拼接字符串时为什么推荐使用join。'],
    program: ['编写函数clean_phone(text)，去掉手机号字符串中的空格和短横线，只保留数字字符。']
  },
  function: {
    single: ['函数没有显式return语句时，默认返回什么？', ['None', '0', 'False', '空字符串'], 'A'],
    multi: ['下列哪些属于Python函数参数形式？', ['位置参数', '关键字参数', '默认参数', '可变参数'], 'A,B,C,D'],
    fill: ['定义匿名函数求平方可写为____。', 'lambda x: x * x'],
    essay: ['解释局部变量和全局变量的作用域差异，并说明global关键字的影响。'],
    program: ['编写装饰器timer，统计被装饰函数执行耗时并打印函数名。']
  },
  module: {
    single: ['从模块math中只导入sqrt函数的语句是？', ['from math import sqrt', 'import sqrt from math', 'include math.sqrt', 'using math sqrt'], 'A'],
    multi: ['下列哪些文件或目录通常与Python包有关？', ['__init__.py', 'pyproject.toml', 'requirements.txt', 'index.html'], 'A,B,C'],
    fill: ['安装第三方包requests的常用命令是____。', 'pip install requests'],
    essay: ['说明模块、包和库之间的关系，并解释为什么要避免循环导入。'],
    program: ['将常用数学工具函数拆分到utils.py，并在main.py中导入调用其中的平均值函数。']
  },
  file: {
    single: ['使用with open(...) as f读取文件的主要优点是？', ['自动关闭文件资源', '自动加密文件', '自动压缩文件', '自动把文本转成JSON'], 'A'],
    multi: ['下列哪些模式可以用于打开文本文件？', ['r', 'w', 'a', 'delete'], 'A,B,C'],
    fill: ['以UTF-8编码读取data.txt的写法可用open("data.txt", "r", encoding=____)。', '"utf-8"'],
    essay: ['比较JSON和CSV适合保存的数据结构，并说明各自常见应用场景。'],
    program: ['编写函数load_scores(path)，读取CSV文件中的姓名和分数，返回字典{name: score}。']
  },
  exception: {
    single: ['try-except-finally结构中，finally代码块通常何时执行？', ['无论是否发生异常都会执行', '只有没有异常时执行', '只有发生异常时执行', '只有return之后不执行'], 'A'],
    multi: ['下列哪些是Python常见异常类型？', ['ValueError', 'KeyError', 'IndexError', 'HttpMagicError'], 'A,B,C'],
    fill: ['主动抛出异常使用的关键字是____。', 'raise'],
    essay: ['说明捕获Exception与捕获具体异常类型的区别，以及过度宽泛捕获的风险。'],
    program: ['编写函数safe_div(a, b)，当除数为0或参数无法转数字时返回None，否则返回除法结果。']
  },
  oopBasic: {
    single: ['类的实例方法第一个参数通常命名为？', ['self', 'this', 'cls', 'object'], 'A'],
    multi: ['面向对象三大特征通常包括哪些？', ['封装', '继承', '多态', '排序'], 'A,B,C'],
    fill: ['定义类Person的语句开头可写为____。', 'class Person:'],
    essay: ['用学生类为例说明属性和方法分别表示什么，以及封装带来的好处。'],
    program: ['设计BankAccount类，支持存款、取款和查询余额，取款余额不足时抛出异常。']
  },
  oopAdvanced: {
    single: ['@classmethod修饰的方法第一个参数通常表示什么？', ['类对象', '实例对象', '模块对象', '函数对象'], 'A'],
    multi: ['下列哪些属于Python面向对象高级特性？', ['property', 'staticmethod', 'classmethod', 'while'], 'A,B,C'],
    fill: ['声明抽象基类通常可从abc模块导入____。', 'ABC'],
    essay: ['比较实例方法、类方法和静态方法的适用场景。'],
    program: ['定义抽象类Shape，要求子类实现area方法，并实现Circle和Rectangle两个子类。']
  },
  data: {
    single: ['Pandas中读取CSV文件常用函数是？', ['pd.read_csv', 'pd.open_csv', 'np.read_csv', 'csv.load_frame'], 'A'],
    multi: ['下列哪些是Pandas DataFrame常见操作？', ['筛选行', '分组聚合', '缺失值处理', '修改网页路由'], 'A,B,C'],
    fill: ['通常使用import pandas as ____ 导入Pandas。', 'pd'],
    essay: ['说明NumPy数组相较普通列表在数值计算中的优势。'],
    program: ['使用Pandas读取成绩表，按班级计算平均分，并输出平均分最高的班级。']
  },
  web: {
    single: ['Flask中定义路由常用的装饰器是？', ['@app.route', '@app.path', '@flask.url', '@server.api'], 'A'],
    multi: ['下列哪些属于HTTP请求方法？', ['GET', 'POST', 'PUT', 'LOOP'], 'A,B,C'],
    fill: ['Flask中返回JSON响应常用函数是____。', 'jsonify'],
    essay: ['说明GET和POST请求在语义和参数传递上的差异。'],
    program: ['使用Flask实现GET /health接口，返回JSON：{"status": "ok"}。']
  },
  crawler: {
    single: ['使用requests发送GET请求的常用写法是？', ['requests.get(url)', 'requests.open(url)', 'http.get(url)', 'urllib.fetch(url)'], 'A'],
    multi: ['爬虫程序应关注哪些工程问题？', ['请求频率控制', '异常重试', '编码处理', '忽略robots协议和法律风险'], 'A,B,C'],
    fill: ['BeautifulSoup中按CSS选择器查找元素常用方法是____。', 'select'],
    essay: ['说明爬虫中设置User-Agent、超时和重试机制的原因。'],
    program: ['编写函数fetch_titles(url)，请求网页并解析所有h1/h2标题文本，返回列表。']
  },
  project: {
    single: ['综合项目开发中，最适合管理依赖版本的文件是？', ['requirements.txt', 'README.png', 'main.exe', 'data.tmp'], 'A'],
    multi: ['Python项目交付时通常需要包含哪些内容？', ['README说明', '依赖清单', '测试用例', '本机绝对路径硬编码'], 'A,B,C'],
    fill: ['Python单元测试常用标准库模块是____。', 'unittest'],
    essay: ['说明如何把一个Python脚本逐步重构为可维护的小型项目。'],
    program: ['完成一个命令行待办事项管理器，支持新增、完成、列表展示，并将数据保存到JSON文件。']
  }
}

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
  let data
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`${options.method || 'GET'} ${path} returned non-JSON: ${text}`)
  }
  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${path} HTTP ${response.status}: ${text}`)
  }
  return data
}

async function login() {
  const res = await request('/api/teachers/login', {
    method: 'POST',
    body: JSON.stringify({ username: USERNAME, password: PASSWORD })
  })
  if (res.code !== 200) throw new Error(`Login failed: ${res.msg}`)
}

async function ensureKnowledgePoints() {
  let current = await getKnowledgePoints()
  const byName = new Map(current.map(point => [point.name, point]))

  for (const [key, name, description, chapter, importance] of knowledgePoints) {
    if (byName.has(name)) continue
    const res = await request('/api/knowledge-points', {
      method: 'POST',
      body: JSON.stringify({
        courseCode: COURSE_CODE,
        lessonNo: lessons[key],
        name,
        description,
        chapter,
        importance,
        generationMethod: 'manual'
      })
    })
    if (res.code !== 200) throw new Error(`Create knowledge point failed: ${name}: ${res.msg}`)
  }

  current = await getKnowledgePoints()
  const byKey = new Map()
  for (const [key, name] of knowledgePoints) {
    const point = current.find(item => item.name === name)
    if (!point) throw new Error(`Knowledge point missing after creation: ${name}`)
    byKey.set(key, point)
  }
  return byKey
}

async function getKnowledgePoints() {
  const res = await request(`/api/knowledge-points?courseCode=${COURSE_CODE}`)
  if (res.code !== 200) throw new Error(`Load knowledge points failed: ${res.msg}`)
  return res.data || []
}

async function ensureRelations(pointsByKey) {
  for (const [fromKey, toKey] of relationPairs) {
    const from = pointsByKey.get(fromKey)
    const to = pointsByKey.get(toKey)
    const res = await request('/api/knowledge-relations', {
      method: 'POST',
      body: JSON.stringify({
        fromKnowledgePointId: from.knowledgePointId,
        toKnowledgePointId: to.knowledgePointId,
        relationType: 'prerequisite'
      })
    })
    if (res.code !== 200 && res.msg !== '知识点关系已存在') {
      throw new Error(`Create relation failed: ${from.name} -> ${to.name}: ${res.msg}`)
    }
  }
}

function buildQuestions(pointsByKey) {
  const questions = []
  for (const [key, name] of knowledgePoints.map(([key, name]) => [key, name])) {
    const point = pointsByKey.get(key)
    const spec = specs[key]
    const common = {
      courseCode: COURSE_CODE,
      lessonNo: lessons[key],
      knowledgePointId: point.knowledgePointId
    }
    questions.push({
      ...common,
      type: 'single',
      stem: `【${name}】${spec.single[0]}`,
      options: JSON.stringify(spec.single[1]),
      answer: spec.single[2],
      difficulty: 1,
      score: 5
    })
    questions.push({
      ...common,
      type: 'multi',
      stem: `【${name}】${spec.multi[0]}`,
      options: JSON.stringify(spec.multi[1]),
      answer: spec.multi[2],
      difficulty: 2,
      score: 8
    })
    questions.push({
      ...common,
      type: 'fill',
      stem: `【${name}】${spec.fill[0]}`,
      options: null,
      answer: spec.fill[1],
      difficulty: 3,
      score: 8
    })
    questions.push({
      ...common,
      type: 'essay',
      stem: `【${name}】${spec.essay[0]}`,
      options: null,
      answer: '评分要点：概念准确；能结合Python语法或项目场景说明；表达清晰，有必要示例。',
      difficulty: 4,
      score: 15
    })
    questions.push({
      ...common,
      type: 'program',
      stem: `【${name}】${spec.program[0]}`,
      options: null,
      answer: '评分要点：函数或程序结构清晰；覆盖正常输入与边界情况；命名规范；必要时包含异常处理或测试样例。',
      difficulty: 5,
      score: 20
    })
  }
  return questions
}

async function existingQuestionStems() {
  const res = await request(`/api/questions?course_id=${COURSE_CODE}`)
  if (res.code !== 200) throw new Error(`Load questions failed: ${res.msg}`)
  return new Set((res.data || []).map(question => question.stem))
}

async function ensureQuestions(pointsByKey) {
  const existing = await existingQuestionStems()
  const questions = buildQuestions(pointsByKey)
  let created = 0
  let skipped = 0
  for (const question of questions) {
    if (existing.has(question.stem)) {
      skipped += 1
      continue
    }
    const res = await request('/api/questions', {
      method: 'POST',
      body: JSON.stringify(question)
    })
    if (res.code !== 200) throw new Error(`Create question failed: ${question.stem}: ${res.msg}`)
    created += 1
  }
  return { total: questions.length, created, skipped }
}

async function main() {
  await login()
  const pointsByKey = await ensureKnowledgePoints()
  await ensureRelations(pointsByKey)
  const result = await ensureQuestions(pointsByKey)
  const points = await getKnowledgePoints()
  const finalQuestions = await request(`/api/questions?course_id=${COURSE_CODE}`)
  console.log(JSON.stringify({
    courseCode: COURSE_CODE,
    knowledgePoints: points.length,
    questionTemplates: result.total,
    questionsCreated: result.created,
    questionsSkipped: result.skipped,
    questionsInCourse: finalQuestions.data?.length ?? 0
  }, null, 2))
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
