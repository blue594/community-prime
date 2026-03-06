<template>
  <div class="dashboard-page">
    <h2>欢迎使用邻里优享后台管理系统</h2>
    <p class="subtitle">请从左侧菜单选择功能进行管理</p>

    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #409EFF;">
              <i class="el-icon-s-goods"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.productCount || 0 }}</div>
              <div class="stat-label">商品数量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #67C23A;">
              <i class="el-icon-s-order"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.orderCount || 0 }}</div>
              <div class="stat-label">订单数量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #E6A23C;">
              <i class="el-icon-s-custom"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount || 0 }}</div>
              <div class="stat-label">用户数量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #F56C6C;">
              <i class="el-icon-s-tools"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.bookingCount || 0 }}</div>
              <div class="stat-label">预约数量</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {}
    }
  },
  created() {
    this.loadStats()
  },
  methods: {
    loadStats() {
      this.$http.get('/admin/dashboard/stats')
        .then(response => {
          const res = response.data
          if (res.success) {
            this.stats = res.data
          }
        })
        .catch(err => {
          console.error('加载统计数据失败:', err)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  h2 {
    margin-bottom: 10px;
    color: #303133;
  }

  .subtitle {
    color: #909399;
    margin-bottom: 30px;
  }

  .stats-cards {
    .stat-card {
      display: flex;
      align-items: center;
      padding: 20px;

      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 15px;

        i {
          font-size: 30px;
          color: #fff;
        }
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 5px;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
}
</style>
