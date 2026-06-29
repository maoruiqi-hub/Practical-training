<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>AI智慧课程平台</h2>
      <el-tabs v-model="loginType" class="login-tabs">
        <el-tab-pane label="学生登录" name="student" />
        <el-tab-pane label="教师登录" name="teacher" />
      </el-tabs>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center">
        <el-link type="primary" @click="$router.push('/register')">没有账号？去注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentLogin, teacherLogin } from '../api'

const router = useRouter()
const loginType = ref('student')
const form = reactive({ username: '', password: '' })

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
.login-container { display:flex; justify-content:center; align-items:center; height:100vh; background:#f0f2f5; }
.login-card { width:420px; }
.login-card h2 { text-align:center; margin-bottom:20px; }
.login-tabs { margin-bottom:20px; }
</style>
