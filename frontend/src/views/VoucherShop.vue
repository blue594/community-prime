<template>
  <div class="voucher-shop-page page-container">
    <div class="shop-header">
      <h1>餐饮休闲优惠券</h1>
      <p>精选到店优惠券，享受超值折扣</p>
    </div>

    <!-- 优惠券列表 -->
    <div class="voucher-list">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in voucherList" :key="item.id">
          <div class="card voucher-card">
            <div class="voucher-badge">
              <el-tag type="success" size="small">到店券</el-tag>
            </div>
            <div class="voucher-title">{{ item.title }}</div>
            <div class="voucher-subtitle">{{ item.subTitle }}</div>
            <div class="voucher-rules">
              <i class="el-icon-info"></i> {{ item.rules }}
            </div>

            <div class="voucher-price">
              <span class="pay-value">¥{{ item.payValue }}</span>
              <span class="actual-value">原价 ¥{{ item.actualValue }}</span>
              <span class="discount" v-if="item.actualValue > 0">
                {{ Math.round(item.payValue / item.actualValue * 10) }}折
              </span>
            </div>

            <div class="voucher-stock" v-if="item.stock !== null">
              <span>剩余 {{ item.stock }} 张</span>
            </div>

            <div class="voucher-time" v-if="item.beginTime && item.endTime">
              <i class="el-icon-time"></i>
              有效期：{{ formatTime(item.beginTime) }} - {{ formatTime(item.endTime) }}
            </div>

            <el-button
              type="primary"
              style="width: 100%; margin-top: 15px;"
              :disabled="item.stock !== null && item.stock <= 0"
              :loading="submitting && currentVoucherId === item.id"
              @click="handleBuy(item)"
            >
              {{ item.stock !== null && item.stock <= 0 ? '已售罄' : '立即购买' }}
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 空状态 -->
    <div v-if="voucherList.length === 0 && !loading" class="empty-state">
      <i class="el-icon-ticket empty-icon"></i>
      <p class="empty-text">暂无优惠券</p>
    </div>
  </div>
</template>

<script>
import moment from 'moment'
import { getNormalVouchers, buyVoucher } from '../api/voucher'

export default {
  name: 'VoucherShop',
  data() {
    return {
      voucherList: [],
      loading: false,
      submitting: false,
      currentVoucherId: null
    }
  },
  created() {
    this.loadVouchers()
  },
  methods: {
    async loadVouchers() {
      this.loading = true
      try {
        const res = await getNormalVouchers(1)
        this.voucherList = res || []
      } catch (error) {
        console.error('加载优惠券失败:', error)
      } finally {
        this.loading = false
      }
    },
    formatTime(time) {
      return moment(time).format('YYYY-MM-DD')
    },
    async handleBuy(item) {
      if (!this.$store.getters.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }

      try {
        await this.$confirm(
          `确定购买「${item.title}」吗？售价 ¥${item.payValue}`,
          '确认购买',
          {
            confirmButtonText: '确认购买',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
      } catch (e) {
        return // 用户取消
      }

      this.currentVoucherId = item.id
      this.submitting = true

      try {
        await buyVoucher(item.id)
        this.$message.success('下单成功，请前往订单页面支付！')
        // 跳转到订单页面的优惠券tab
        this.$router.push({ path: '/order', query: { tab: 'voucher' } })
      } catch (error) {
        console.error('购买失败:', error)
      } finally {
        this.submitting = false
        this.currentVoucherId = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.voucher-shop-page {
  padding-top: 20px;
}

.shop-header {
  text-align: center;
  margin-bottom: 30px;

  h1 {
    font-size: 32px;
    color: #333;
    margin-bottom: 8px;
  }

  p {
    color: #999;
    font-size: 16px;
  }
}

.voucher-list {
  .voucher-card {
    margin-bottom: 20px;
    position: relative;

    .voucher-badge {
      position: absolute;
      top: 15px;
      right: 15px;
    }

    .voucher-title {
      font-size: 20px;
      font-weight: bold;
      color: #333;
      margin-bottom: 8px;
      padding-right: 60px;
    }

    .voucher-subtitle {
      color: #666;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .voucher-rules {
      color: #999;
      font-size: 13px;
      margin-bottom: 12px;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 4px;

      i {
        margin-right: 4px;
      }
    }

    .voucher-price {
      margin-bottom: 10px;
      display: flex;
      align-items: baseline;
      gap: 10px;

      .pay-value {
        color: #f5222d;
        font-size: 28px;
        font-weight: bold;
      }

      .actual-value {
        color: #999;
        font-size: 14px;
        text-decoration: line-through;
      }

      .discount {
        background: #fff1f0;
        color: #f5222d;
        font-size: 12px;
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: bold;
      }
    }

    .voucher-stock {
      color: #999;
      font-size: 13px;
      margin-bottom: 8px;
    }

    .voucher-time {
      color: #999;
      font-size: 13px;
      margin-bottom: 5px;

      i {
        margin-right: 4px;
      }
    }
  }
}
</style>
