<template>
  <div class="admin-login-page">
    <div class="login-container">
      <div class="login-header">
        <h2>管理员登录</h2>
        <p>邻里优享后台管理系统</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="el-icon-user"
            placeholder="请输入管理员账号"
            size="large"
          ></el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="请输入密码"
            size="large"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tips">
        <p>默认账号：admin / admin123</p>
        <p>测试账号：test / 123456</p>
      </div>

      <div class="login-footer">
        <el-button type="text" @click="$router.push('/login')">
          <i class="el-icon-arrow-left"></i> 返回用户登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminLogin',
  data() {
    return {
      loading: false,
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入管理员账号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          // 使用原生 axios 避免拦截器处理，因为登录接口需要特殊处理
          const axios = require('axios')
          axios.post('/api/admin/login', null, {
            params: {
              username: this.loginForm.username,
              password: this.loginForm.password
            }
          })
            .then(response => {
              const res = response.data
              if (res.success) {
                // 保存token
                localStorage.setItem('adminToken', res.data.token)
                localStorage.setItem('adminInfo', JSON.stringify(res.data))
                this.$message.success('登录成功')
                this.$router.push('/admin/dashboard')
              } else {
                this.$message.error(res.msg || '登录失败')
              }
            })
            .catch(err => {
              if (err.response && err.response.data && err.response.data.message) {
                this.$message.error(err.response.data.message)
              } else {
                this.$message.error('登录失败：' + err.message)
              }
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-container {
    width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

    .login-header {
      text-align: center;
      margin-bottom: 30px;

      h2 {
        font-size: 24px;
        color: #333;
        margin-bottom: 10px;
      }

      p {
        color: #999;
        font-size: 14px;
      }
    }

    .login-form {
      .login-btn {
        width: 100%;
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
        margin: 5px 0;
      }
    }

    .login-footer {
      margin-top: 15px;
      text-align: center;
    }
  }
}
</style>
