<template>
  <div class="profile-page page-container">
    <h1 class="page-title">个人中心</h1>

    <div class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="card user-card">
        <div class="user-avatar">
          <el-upload
            class="avatar-uploader"
            action="/api/user/upload-icon"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            :on-error="handleUploadError"
          >
            <el-avatar :size="100" :src="getImageUrl(userInfo.icon) || defaultAvatar" class="avatar-image"></el-avatar>
            <div class="avatar-mask">
              <i class="el-icon-camera"></i>
              <span>更换头像</span>
            </div>
          </el-upload>
        </div>
        <div class="user-info">
          <div class="name-edit">
            <h2 class="user-name" v-if="!editingName">{{ userInfo.nickName || '用户' }}</h2>
            <el-input v-else v-model="newNickName" size="small" style="width: 150px;" @keyup.enter.native="saveNickName" @blur="saveNickName" ref="nickNameInput"></el-input>
            <el-button type="text" icon="el-icon-edit" size="mini" @click="editNickName" v-if="!editingName"></el-button>
          </div>
          <p class="user-phone">{{ userInfo.phone || '' }}</p>
        </div>
      </div>

      <!-- 功能入口 -->
      <div class="feature-grid">
        <div class="feature-item" @click="$router.push('/order')">
          <i class="el-icon-s-order"></i>
          <span>我的订单</span>
        </div>
        <div class="feature-item" @click="$router.push('/booking')">
          <i class="el-icon-date"></i>
          <span>我的预约</span>
        </div>
        <div class="feature-item" @click="$router.push('/seckill')">
          <i class="el-icon-time"></i>
          <span>限时秒杀</span>
        </div>
        <div class="feature-item" @click="handleLogout">
          <i class="el-icon-switch-button"></i>
          <span>退出登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'Profile',
  data() {
    return {
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      uploadHeaders: {
        'authorization': localStorage.getItem('token')
      },
      editingName: false,
      newNickName: ''
    }
  },
  computed: {
    ...mapState(['userInfo'])
  },
  methods: {
    handleLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('logout')
        this.$message.success('退出成功')
        this.$router.push('/home')
      }).catch(() => {})
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
    async handleAvatarSuccess(res, file) {
      console.log('Upload success response:', res, typeof res)
      // 注意：res已经是解包后的数据（即res.data）
      // 上传接口返回的是图片URL字符串
      let iconUrl = res
      if (typeof res === 'object') {
        iconUrl = res.data || res
      }
      console.log('Icon URL:', iconUrl)
      // 调用更新头像接口
      try {
        const { updateIcon } = await import('../api/user')
        await updateIcon(iconUrl)
        // request.js会解包响应，成功时直接返回data
        this.$message.success('头像更新成功')
        // 刷新用户信息
        await this.$store.dispatch('getUserInfo')
        console.log('Updated userInfo:', this.userInfo)
      } catch (error) {
        console.error('Update icon error:', error)
        this.$message.error('头像更新失败')
      }
    },
    handleUploadError(err) {
      console.error('Upload error:', err)
      this.$message.error('上传失败，请检查网络或登录状态')
    },
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http')) return image
      // 添加 baseURL 前缀
      const baseURL = process.env.VUE_APP_BASE_API || '/api'
      return baseURL + image
    },
    editNickName() {
      this.newNickName = this.userInfo.nickName || ''
      this.editingName = true
      this.$nextTick(() => {
        this.$refs.nickNameInput.focus()
      })
    },
    async saveNickName() {
      if (!this.newNickName.trim()) {
        this.editingName = false
        return
      }
      if (this.newNickName.trim() === this.userInfo.nickName) {
        this.editingName = false
        return
      }
      try {
        const { updateNickName } = await import('../api/user')
        await updateNickName(this.newNickName.trim())
        this.$message.success('昵称修改成功')
        await this.$store.dispatch('getUserInfo')
        this.editingName = false
      } catch (error) {
        console.error('修改昵称失败:', error)
        this.$message.error('修改昵称失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  padding-top: 20px;

  .page-title {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .profile-content {
    max-width: 600px;
    margin: 0 auto;
  }

  .user-card {
    display: flex;
    align-items: center;
    padding: 30px;
    margin-bottom: 20px;

    .user-avatar {
      margin-right: 30px;

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

    .user-info {
      .name-edit {
        display: flex;
        align-items: center;
        margin-bottom: 8px;

        .user-name {
          font-size: 24px;
          color: #333;
          margin-right: 8px;
        }

        .el-button {
          padding: 0;
        }
      }

      .user-phone {
        color: #999;
        font-size: 14px;
      }
    }
  }

  .feature-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    .feature-item {
      background: #fff;
      border-radius: 8px;
      padding: 30px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
      }

      i {
        font-size: 32px;
        color: #409EFF;
        margin-bottom: 12px;
        display: block;
      }

      span {
        color: #333;
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 480px) {
  .profile-page {
    .user-card {
      flex-direction: column;
      text-align: center;

      .user-avatar {
        margin-right: 0;
        margin-bottom: 20px;
      }
    }

    .feature-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
