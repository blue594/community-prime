<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <i class="el-icon-s-shop"></i>
        <h1>邻里优享</h1>
        <p>社区生活服务平台</p>
      </div>

      <!-- 登录/注册切换 -->
      <div class="login-tabs">
        <span :class="{ active: !isRegister }" @click="switchToLogin">登录</span>
        <span :class="{ active: isRegister }" @click="switchToRegister">注册</span>
      </div>

      <!-- 登录表单 -->
      <el-form v-if="!isRegister" :model="loginForm" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="phone">
          <el-input
            v-model="loginForm.phone"
            placeholder="请输入手机号"
            prefix-icon="el-icon-mobile-phone"
            maxlength="11"
          ></el-input>
        </el-form-item>

        <!-- 登录方式切换 -->
        <div class="login-type">
          <el-radio-group v-model="loginForm.loginType">
            <el-radio :label="1">验证码登录</el-radio>
            <el-radio :label="2">密码登录</el-radio>
          </el-radio-group>
        </div>

        <!-- 验证码登录 -->
        <el-form-item v-if="loginForm.loginType === 1" prop="code">
          <div class="code-input">
            <el-input
              v-model="loginForm.code"
              placeholder="请输入验证码"
              prefix-icon="el-icon-message"
              maxlength="6"
            ></el-input>
            <el-button
              type="primary"
              :disabled="codeSending || countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s后重试` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 密码登录 -->
        <el-form-item v-else prop="password">
          <el-input
            v-model="loginForm.password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            type="password"
            show-password
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%;"
            @click="handleLogin"
          >登录</el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form v-else :model="registerForm" :rules="registerRules" ref="registerForm" class="login-form">
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            prefix-icon="el-icon-mobile-phone"
            maxlength="11"
          ></el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            placeholder="请设置密码（至少6位）"
            prefix-icon="el-icon-lock"
            type="password"
            show-password
          ></el-input>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            placeholder="请确认密码"
            prefix-icon="el-icon-lock"
            type="password"
            show-password
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="registerLoading"
            style="width: 100%;"
            @click="handleRegister"
          >注册</el-button>
        </el-form-item>
      </el-form>

      <div class="login-tips" v-if="!isRegister">
        <p>验证码登录：新用户将自动注册，默认密码为手机号后6位</p>
        <p>密码登录：初始密码为 123456</p>
      </div>
      <div class="login-tips" v-else>
        <p>注册成功后请返回登录页面</p>
      </div>

      <div class="admin-entry">
        <el-divider></el-divider>
        <el-button type="text" @click="$router.push('/admin/login')">
          <i class="el-icon-s-tools"></i> 管理员入口
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { sendCode, login, register } from '../api/user'

export default {
  name: 'Login',
  data() {
    return {
      isRegister: false,
      loginForm: {
        phone: '',
        code: '',
        password: '',
        loginType: 1
      },
      registerForm: {
        phone: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { len: 6, message: '验证码为6位数字', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ]
      },
      registerRules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请设置密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      },
      loading: false,
      registerLoading: false,
      codeSending: false,
      countdown: 0,
      timer: null
    }
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    validateConfirmPassword(rule, value, callback) {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    },
    switchToLogin() {
      this.isRegister = false
      this.$refs.registerForm && this.$refs.registerForm.resetFields()
    },
    switchToRegister() {
      this.isRegister = true
      this.$refs.loginForm && this.$refs.loginForm.resetFields()
    },
    async sendCode() {
      if (!/^1[3-9]\d{9}$/.test(this.loginForm.phone)) {
        this.$message.warning('请输入正确的手机号')
        return
      }

      this.codeSending = true
      try {
        await sendCode(this.loginForm.phone)
        this.$message.success('验证码已发送，请查看后端日志')
        this.startCountdown()
      } catch (error) {
        console.error('发送验证码失败:', error)
      } finally {
        this.codeSending = false
      }
    },
    startCountdown() {
      this.countdown = 60
      this.timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(this.timer)
        }
      }, 1000)
    },
    handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) return

        this.loading = true
        try {
          const res = await login(this.loginForm)
          this.$store.dispatch('login', res)
          this.$message.success('登录成功')
          
          // 跳转到之前的页面或首页
          const redirect = this.$route.query.redirect || '/home'
          this.$router.push(redirect)
        } catch (error) {
          console.error('登录失败:', error)
        } finally {
          this.loading = false
        }
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(async valid => {
        if (!valid) return

        this.registerLoading = true
        try {
          await register(this.registerForm.phone, this.registerForm.password)
          this.$message.success('注册成功，请登录')
          this.isRegister = false
          this.loginForm.phone = this.registerForm.phone
          this.registerForm = { phone: '', password: '', confirmPassword: '' }
        } catch (error) {
          console.error('注册失败:', error)
        } finally {
          this.registerLoading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;

  i {
    font-size: 48px;
    color: #409EFF;
  }

  h1 {
    font-size: 28px;
    color: #333;
    margin: 16px 0 8px;
  }

  p {
    color: #999;
    font-size: 14px;
  }
}

.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  border-bottom: 2px solid #eee;
  
  span {
    padding: 10px 30px;
    cursor: pointer;
    font-size: 16px;
    color: #999;
    position: relative;
    transition: all 0.3s;
    
    &.active {
      color: #409EFF;
      font-weight: bold;
      
      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        right: 0;
        height: 2px;
        background: #409EFF;
      }
    }
    
    &:hover {
      color: #409EFF;
    }
  }
}

.login-type {
  margin-bottom: 20px;
  text-align: center;
}

.login-form {
  .code-input {
    display: flex;
    gap: 10px;

    .el-input {
      flex: 1;
    }

    .el-button {
      width: 120px;
    }
  }
}

.login-tips {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  text-align: center;

  p {
    color: #999;
    font-size: 12px;
    margin: 4px 0;
  }
}

.admin-entry {
  text-align: center;
  margin-top: 10px;

  .el-divider {
    margin: 20px 0;
  }
}

@media (max-width: 480px) {
  .login-container {
    width: 90%;
    padding: 30px 20px;
  }
}
</style>
