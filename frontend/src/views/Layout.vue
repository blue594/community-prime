<template>
  <div class="layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/home')">
          <i class="el-icon-s-shop"></i>
          <span>邻里优享</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="nav-menu"
          mode="horizontal"
          router
          background-color="#fff"
          text-color="#333"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/product">超市购物</el-menu-item>
          <el-menu-item index="/seckill">限时秒杀</el-menu-item>
          <el-menu-item index="/voucher">餐饮优惠券</el-menu-item>
          <el-menu-item index="/service">家政维修</el-menu-item>
        </el-menu>
        <div class="user-actions">
          <template v-if="isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="getImageUrl(userInfo.icon) || defaultAvatar"></el-avatar>
                <span class="username">{{ userInfo.nickName || '用户' }}</span>
                <i class="el-icon-arrow-down"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="order">我的订单</el-dropdown-item>
                <el-dropdown-item command="booking">我的预约</el-dropdown-item>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主体内容 -->
    <el-main class="main">
      <router-view />
    </el-main>

    <!-- 底部 -->
    <el-footer class="footer">
      <div class="footer-content">
        <p>邻里优享 - 社区生活服务平台</p>
        <p class="copyright">© 2024 Community Prime. All rights reserved.</p>
      </div>
    </el-footer>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

export default {
  name: 'Layout',
  data() {
    return {
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    }
  },
  computed: {
    ...mapGetters(['isLoggedIn']),
    ...mapState(['userInfo']),
    activeMenu() {
      return this.$route.path
    }
  },
  methods: {
    handleCommand(command) {
      switch (command) {
        case 'order':
          if (this.$route.path !== '/order') this.$router.push('/order')
          break
        case 'booking':
          if (this.$route.path !== '/booking') this.$router.push('/booking')
          break
        case 'profile':
          if (this.$route.path !== '/profile') this.$router.push('/profile')
          break
        case 'logout':
          this.$confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            this.$store.dispatch('logout')
            this.$message.success('退出成功')
            this.$router.push('/login')
          }).catch(() => {
            // 用户点击取消，不做任何操作
          })
          break
      }
    },
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http')) return image
      const baseURL = process.env.VUE_APP_BASE_API || '/api'
      return baseURL + image
    }
  }
}
</script>

<style lang="scss" scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .logo {
    display: flex;
    align-items: center;
    font-size: 24px;
    font-weight: bold;
    color: #409EFF;
    cursor: pointer;

    i {
      font-size: 32px;
      margin-right: 8px;
    }
  }

  .nav-menu {
    flex: 1;
    margin: 0 40px;
    border-bottom: none;
  }

  .user-actions {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;

      .username {
        margin: 0 8px;
        color: #333;
      }
    }
  }
}

.main {
  flex: 1;
  padding: 0;
  background: #f5f5f5;
  overflow-x: hidden;
}

.footer {
  background: #333;
  color: #fff;
  text-align: center;
  padding: 20px 0;

  .footer-content {
    max-width: 1200px;
    margin: 0 auto;

    p {
      margin: 8px 0;
    }

    .copyright {
      color: #999;
      font-size: 12px;
    }
  }
}
</style>
