<template>
  <div class="admin-profile-page">
    <div class="page-header">
      <h3>个人中心</h3>
    </div>

    <el-card>
      <div class="profile-header">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/upload-icon"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <el-avatar :size="100" :src="getImageUrl(adminInfo.icon) || defaultAvatar" class="avatar-image"></el-avatar>
            <div class="avatar-mask">
              <i class="el-icon-camera"></i>
              <span>更换头像</span>
            </div>
          </el-upload>
        </div>
      </div>
      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="管理员ID">
          <span>{{ adminInfo.id }}</span>
        </el-form-item>
        <el-form-item label="账号">
          <span>{{ adminInfo.username }}</span>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" style="width: 300px;"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="adminInfo.role === 1 ? 'danger' : 'primary'">
            {{ adminInfo.role === 1 ? '超级管理员' : '普通管理员' }}
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存</el-button>
          <el-button @click="showChangePassword = true">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" :visible.sync="showChangePassword" width="400px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showChangePassword = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'AdminProfile',
  data() {
    return {
      adminInfo: {},
      form: {
        name: ''
      },
      showChangePassword: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      uploadHeaders: {
        'admin-token': localStorage.getItem('adminToken')
      }
    }
  },
  created() {
    this.loadAdminInfo()
  },
  methods: {
    loadAdminInfo() {
      // 从localStorage获取基本信息
      const info = localStorage.getItem('adminInfo')
      if (info) {
        this.adminInfo = JSON.parse(info)
        this.form.name = this.adminInfo.name || ''
      }
      // 从服务器获取最新信息（包含头像等）
      this.$http.get('/admin/me').then(response => {
        const res = response.data
        console.log('Admin info response:', res)
        if (res.success) {
          this.adminInfo = res.data
          console.log('Admin icon:', this.adminInfo.icon)
          this.form.name = this.adminInfo.name || ''
          // 更新localStorage
          localStorage.setItem('adminInfo', JSON.stringify(res.data))
        }
      })
    },
    saveProfile() {
      this.$http.post('/admin/update-name', { name: this.form.name }).then(response => {
        // request.js会解包响应
        this.$message.success('保存成功')
        this.adminInfo = { ...this.adminInfo, name: this.form.name }
        localStorage.setItem('adminInfo', JSON.stringify(this.adminInfo))
      }).catch(error => {
        console.error('保存失败:', error)
        this.$message.error('保存失败')
      })
    },
    changePassword() {
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.error('两次输入的密码不一致')
        return
      }
      this.$http.post('/admin/change-password', {
        oldPassword: this.passwordForm.oldPassword,
        newPassword: this.passwordForm.newPassword
      }).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('密码修改成功')
          this.showChangePassword = false
          this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
        } else {
          this.$message.error(res.msg || '修改失败')
        }
      })
    },
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isImage) {
        this.$message.error('只能上传图片文件')
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB')
      }
      return isImage && isLt2M
    },
    handleAvatarSuccess(res, file) {
      console.log('Admin upload success:', res, typeof res)
      // res已经是解包后的数据（即res.data）
      let iconUrl = res
      if (typeof res === 'object') {
        iconUrl = res.data || res
      }
      console.log('Admin icon URL:', iconUrl)
      // 调用更新头像接口
      this.$http.post('/admin/update-icon', { icon: iconUrl }).then(response => {
        console.log('Admin update icon response:', response)
        // request.js会解包响应
        this.$message.success('头像更新成功')
        // 更新本地存储的管理员信息 - 使用新对象确保响应式
        this.adminInfo = { ...this.adminInfo, icon: iconUrl }
        localStorage.setItem('adminInfo', JSON.stringify(this.adminInfo))
        // 触发全局事件，通知其他组件更新头像
        this.$emit('admin-info-updated', this.adminInfo)
      }).catch(error => {
        console.error('Update icon error:', error)
        this.$message.error('头像更新失败')
      })
    },
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http')) return image
      // 添加 baseURL 前缀
      const baseURL = process.env.VUE_APP_BASE_API || '/api'
      return baseURL + image
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-profile-page {
  .page-header {
    margin-bottom: 20px;
    h3 { margin: 0; }
  }

  .profile-header {
    display: flex;
    justify-content: center;
    margin-bottom: 30px;
    padding-top: 20px;

    .avatar-section {
      .avatar-uploader {
        position: relative;
        cursor: pointer;
        display: inline-block;

        .avatar-image {
          transition: all 0.3s;
        }

        .avatar-mask {
          position: absolute;
          top: 0;
          left: 0;
          width: 100px;
          height: 100px;
          border-radius: 50%;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: all 0.3s;
          color: #fff;

          i {
            font-size: 24px;
            margin-bottom: 4px;
          }

          span {
            font-size: 12px;
          }
        }

        &:hover {
          .avatar-mask {
            opacity: 1;
          }

          .avatar-image {
            filter: brightness(0.8);
          }
        }
      }
    }
  }

  .profile-form {
    max-width: 500px;
    margin: 0 auto;
  }
}
</style>
