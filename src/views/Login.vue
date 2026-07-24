<template>
  <div class="login-container">
    <main class="login-shell">
      <section class="brand-panel">
        <div class="brand-badge">AI Course</div>
        <h1>AI智慧课程平台</h1>
        <p>让课程管理、学习分析与智能辅助保持清晰、高效、易用。</p>
      </section>

      <section class="login-card" aria-label="登录">
        <div class="card-heading">
          <p>欢迎回来</p>
          <h2>{{ loginType === 'student' ? '学生端登录' : '教师端登录' }}</h2>
        </div>

        <el-tabs v-model="loginType" class="login-tabs" stretch>
          <el-tab-pane label="学生登录" name="student" />
          <el-tab-pane label="教师登录" name="teacher" />
        </el-tabs>

        <el-form :model="form" class="login-form" label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button class="login-button" type="primary" size="large" @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <div class="register-link">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">去注册</el-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentLogin, teacherLogin } from '../api'

const router = useRouter()
const loginType = ref('student')
const form = reactive({ username: 'zhangsan', password: '123456' })

const getErrorMessage = error =>
  error?.response?.data?.msg ||
  error?.response?.data?.message ||
  error?.message ||
  '登录失败'

const handleLogin = async () => {
  try {
    const api = loginType.value === 'student' ? studentLogin : teacherLogin
    const res = await api(form)
    if (res.data.code === 200) {
      const user = res.data.data
      user.role = loginType.value === 'student' ? 'student' : (res.data.data.role || 'teacher')
      localStorage.setItem('user', JSON.stringify(user))
      ElMessage.success('登录成功')
      router.push(user.role === 'student' ? '/tower-map' : '/dashboard')
    } else {
      ElMessage.error(res.data.msg || '登录失败')
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  overflow: hidden;
  background: #eef3f7;
}

.login-shell {
  width: min(1040px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 440px);
  overflow: hidden;
  border: 1px solid rgba(20, 38, 58, 0.08);
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(31, 51, 73, 0.14);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px;
  color: #112236;
  background: #f7fafc;
}

.brand-panel::after {
  content: '';
  position: absolute;
  left: 56px;
  bottom: 58px;
  width: 96px;
  height: 4px;
  border-radius: 999px;
  background: #147f91;
}

.brand-badge {
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  color: #0e6671;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  background: rgba(40, 176, 164, 0.12);
}

.brand-panel h1 {
  max-width: 420px;
  margin: 26px 0 16px;
  font-size: 46px;
  line-height: 1.15;
  letter-spacing: 0;
}

.brand-panel p {
  max-width: 470px;
  margin: 0;
  color: #526274;
  font-size: 17px;
  line-height: 1.8;
}

.login-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 58px 48px;
  background: #ffffff;
}

.card-heading {
  text-align: center;
}

.card-heading p {
  margin: 0 0 8px;
  color: #7a8797;
  font-size: 14px;
}

.card-heading h2 {
  margin: 0;
  color: #15263a;
  font-size: 28px;
  line-height: 1.3;
  letter-spacing: 0;
}

.login-tabs {
  margin: 34px 0 26px;
}

.login-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.login-tabs :deep(.el-tabs__nav) {
  width: 100%;
  padding: 5px;
  border-radius: 14px;
  background: #eef3f7;
}

.login-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0;
  border-radius: 10px;
  color: #647386;
  font-weight: 700;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #0e6671;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(20, 45, 70, 0.1);
}

.login-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #2c3d50;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #dbe4ec inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1d8a91 inset, 0 8px 22px rgba(29, 138, 145, 0.12);
}

.login-button {
  width: 100%;
  height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 12px;
  font-weight: 700;
  background: #147f91;
  box-shadow: 0 12px 24px rgba(20, 127, 145, 0.18);
}

.login-button:hover,
.login-button:focus {
  background: #0f6f7f;
}

.register-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 26px;
  color: #7a8797;
  font-size: 14px;
}

@media (max-width: 860px) {
  .login-container {
    padding: 24px 16px;
  }

  .login-shell {
    min-height: auto;
    grid-template-columns: 1fr;
    border-radius: 22px;
  }

  .brand-panel {
    padding: 34px 28px 28px;
  }

  .brand-panel::after {
    left: 28px;
    bottom: 28px;
  }

  .brand-panel h1 {
    font-size: 34px;
  }

  .login-card {
    padding: 34px 28px 36px;
  }
}

@media (max-width: 560px) {
  .brand-panel::after {
    display: none;
  }

  .card-heading h2 {
    font-size: 24px;
  }
}
</style>
