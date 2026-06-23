<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>用户注册</h2>
      <el-tabs v-model="regType" class="reg-tabs">
        <el-tab-pane label="学生注册" name="student" />
        <el-tab-pane label="教师注册" name="teacher" />
      </el-tabs>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item v-if="regType==='student'" label="学号">
          <el-input v-model="form.studentNo" placeholder="学号（可选，留空自动生成）" />
        </el-form-item>
        <el-form-item v-if="regType==='teacher'" label="教职工码">
          <el-input v-model="form.teacherNo" placeholder="工号（可选，留空自动生成）" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" placeholder="所属学院" />
        </el-form-item>
        <el-form-item v-if="regType==='student'" label="班级">
          <el-input v-model="form.className" placeholder="如：计科202班" />
        </el-form-item>
        <el-form-item v-if="regType==='teacher'" label="专业">
          <el-input v-model="form.major" placeholder="专业方向" />
        </el-form-item>
        <el-form-item v-if="regType==='teacher'" label="联系电话">
          <el-input v-model="form.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width:100%">注册</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center">
        <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentRegister, teacherRegister } from '../api'

const router = useRouter()
const regType = ref('student')
const form = reactive({ username: '', password: '', name: '', studentNo: '', teacherNo: '', college: '', className: '', major: '', phone: '' })

const handleRegister = async () => {
  try {
    const api = regType.value === 'student' ? studentRegister : teacherRegister
    const res = await api(form)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch {
    ElMessage.error('注册失败')
  }
}
</script>

<style scoped>
.register-container { display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f0f2f5; padding:20px 0; }
.register-card { width:480px; }
.register-card h2 { text-align:center; margin-bottom:20px; }
.reg-tabs { margin-bottom:20px; }
</style>
