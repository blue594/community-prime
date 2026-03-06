<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <i class="el-icon-s-shop"></i>
        <span>邻里优享</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <i class="el-icon-s-home"></i>
          <span slot="title">控制台</span>
        </el-menu-item>

        <el-submenu index="product">
          <template slot="title">
            <i class="el-icon-s-goods"></i>
            <span>商品管理</span>
          </template>
          <el-menu-item index="/admin/product/list">商品列表</el-menu-item>
          <el-menu-item index="/admin/product/add">添加商品</el-menu-item>
        </el-submenu>

        <el-submenu index="service">
          <template slot="title">
            <i class="el-icon-s-tools"></i>
            <span>家政管理</span>
          </template>
          <el-menu-item index="/admin/service/type">服务类型</el-menu-item>
          <el-menu-item index="/admin/service/booking">预约管理</el-menu-item>
        </el-submenu>

        <el-submenu index="voucher">
          <template slot="title">
            <i class="el-icon-s-ticket"></i>
            <span>优惠券管理</span>
          </template>
          <el-menu-item index="/admin/voucher/list">优惠券列表</el-menu-item>
          <el-menu-item index="/admin/voucher/seckill">秒杀管理</el-menu-item>
        </el-submenu>

        <el-menu-item index="/admin/user">
          <i class="el-icon-s-custom"></i>
          <span slot="title">用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/order">
          <i class="el-icon-s-order"></i>
          <span slot="title">订单管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部导航 -->
      <header class="admin-header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="getImageUrl(adminInfo.icon)" icon="el-icon-user-solid"></el-avatar>
              <span class="username">{{ adminInfo.name || adminInfo.username }}</span>
              <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="userHome">返回用户端</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminLayout',
  data() {
    return {
      adminInfo: {}
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  created() {
    this.loadAdminInfo()
    // 监听localStorage变化，同步更新头像
    window.addEventListener('storage', this.handleStorageChange)
  },
  beforeDestroy() {
    window.removeEventListener('storage', this.handleStorageChange)
  },
  methods: {
    loadAdminInfo() {
      const info = localStorage.getItem('adminInfo')
      if (info) {
        this.adminInfo = JSON.parse(info)
      }
    },
    handleStorageChange(e) {
      if (e.key === 'adminInfo') {
        this.loadAdminInfo()
      }
    },
    handleCommand(command) {
      switch (command) {
        case 'profile':
          if (this.$route.path !== '/admin/profile') {
            this.$router.push('/admin/profile')
          }
          break
        case 'userHome':
          if (this.$route.path !== '/home') {
            this.$router.push('/home')
          }
          break
        case 'logout':
          this.$confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            localStorage.removeItem('adminToken')
            localStorage.removeItem('adminInfo')
            this.$message.success('退出成功')
            this.$router.push('/admin/login')
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
.admin-layout {
  display: flex;
  min-height: 100vh;

  .sidebar {
    width: 210px;
    background-color: #304156;
    position: fixed;
    height: 100%;
    left: 0;
    top: 0;
    z-index: 100;

    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      font-weight: bold;
      border-bottom: 1px solid #1f2d3d;

      i {
        font-size: 24px;
        margin-right: 10px;
      }
    }

    .admin-menu {
      border-right: none;
    }
  }

  .main-container {
    flex: 1;
    margin-left: 210px;
    background-color: #f0f2f5;
    min-height: 100vh;

    .admin-header {
      height: 60px;
      background: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 20px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);

      .header-right {
        .user-info {
          display: flex;
          align-items: center;
          cursor: pointer;

          .username {
            margin: 0 8px;
            color: #606266;
          }
        }
      }
    }

    .admin-main {
      padding: 20px;
    }
  }
}
</style>
