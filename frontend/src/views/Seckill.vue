<template>
  <div class="seckill-page page-container">
    <div class="seckill-header">
      <h1>限时秒杀</h1>
      <p>超值商品和优惠券，限量抢购</p>
    </div>

    <!-- 秒杀类型切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <!-- 秒杀商品 -->
      <el-tab-pane label="秒杀商品" name="product">
        <div class="seckill-list">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" v-for="item in productList" :key="'p' + item.id">
              <div class="card seckill-card product-seckill">
                <div class="seckill-image" v-if="item.productImage">
                  <img :src="item.productImage" :alt="item.productName">
                </div>
                <div class="seckill-content">
                  <div class="seckill-title">{{ item.productName }}</div>

                  <div class="seckill-price-row">
                    <span class="seckill-price">¥{{ item.seckillPrice }}</span>
                    <span class="original-price">¥{{ item.originalPrice }}</span>
                    <span class="discount-tag" v-if="item.originalPrice > 0">
                      {{ Math.round(item.seckillPrice / item.originalPrice * 10) }}折
                    </span>
                  </div>

                  <div class="seckill-progress">
                    <div class="progress-label">
                      <span>已抢{{ calcProductPercent(item) }}%</span>
                      <span>剩余{{ item.stock }}件</span>
                    </div>
                    <el-progress
                      :percentage="calcProductPercent(item)"
                      :color="progressColor"
                      :show-text="false"
                    ></el-progress>
                  </div>

                  <div class="seckill-time">
                    <i class="el-icon-time"></i>
                    {{ formatTime(item.beginTime) }} - {{ formatTime(item.endTime) }}
                  </div>

                  <div class="seckill-status">
                    <el-tag :type="getProductStatusType(item)">{{ getProductStatusText(item) }}</el-tag>
                  </div>

                  <el-button
                    type="danger"
                    class="btn-seckill"
                    style="width: 100%; margin-top: 15px;"
                    :disabled="getProductStatus(item) !== 1 || item.stock <= 0"
                    :loading="submitting && currentId === 'p' + item.id"
                    @click="handleProductSeckill(item)"
                  >
                    {{ getProductButtonText(item) }}
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>
          <div v-if="productList.length === 0 && !loading" class="empty-state">
            <i class="el-icon-shopping-bag-1 empty-icon"></i>
            <p class="empty-text">暂无秒杀商品</p>
          </div>
        </div>
      </el-tab-pane>

      <!-- 秒杀优惠券 -->
      <el-tab-pane label="秒杀优惠券" name="voucher">
        <div class="seckill-list">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" v-for="item in voucherList" :key="'v' + item.id">
              <div class="card seckill-card voucher-seckill">
                <div class="seckill-content">
                  <div class="seckill-title">{{ item.title }}</div>
                  <div class="seckill-subtitle">{{ item.subTitle }}</div>
                  <div class="seckill-rules">{{ item.rules }}</div>

                  <div class="seckill-price-row">
                    <span class="seckill-price">¥{{ item.payValue }}</span>
                    <span class="original-price">¥{{ item.actualValue }}</span>
                  </div>

                  <div class="seckill-progress">
                    <div class="progress-label">
                      <span>已抢{{ calcVoucherPercent(item) }}%</span>
                      <span>剩余{{ item.stock }}件</span>
                    </div>
                    <el-progress
                      :percentage="calcVoucherPercent(item)"
                      :color="progressColor"
                      :show-text="false"
                    ></el-progress>
                  </div>

                  <div class="seckill-time">
                    <i class="el-icon-time"></i>
                    {{ formatTime(item.beginTime) }} - {{ formatTime(item.endTime) }}
                  </div>

                  <div class="seckill-status">
                    <el-tag :type="getVoucherStatusType(item.status)">{{ getVoucherStatusText(item.status) }}</el-tag>
                  </div>

                  <el-button
                    type="danger"
                    class="btn-seckill"
                    style="width: 100%; margin-top: 15px;"
                    :disabled="item.status !== 1 || item.stock <= 0"
                    :loading="submitting && currentId === 'v' + item.id"
                    @click="handleVoucherSeckill(item)"
                  >
                    {{ getVoucherButtonText(item) }}
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>
          <div v-if="voucherList.length === 0 && !loading" class="empty-state">
            <i class="el-icon-ticket empty-icon"></i>
            <p class="empty-text">暂无秒杀优惠券</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 秒杀商品下单弹窗（需要填写地址） -->
    <el-dialog title="确认秒杀" :visible.sync="orderDialogVisible" width="450px" :close-on-click-modal="false">
      <div class="order-confirm" v-if="selectedProduct">
        <div class="confirm-info">
          <span class="confirm-name">{{ selectedProduct.productName }}</span>
          <span class="confirm-price">秒杀价：¥{{ selectedProduct.seckillPrice }}</span>
        </div>
        <el-form :model="orderForm" :rules="orderRules" ref="orderForm" label-width="90px" style="margin-top: 20px;">
          <el-form-item label="收货地址" prop="address">
            <el-input v-model="orderForm.address" type="textarea" :rows="2" placeholder="请输入收货地址"></el-input>
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="orderForm.phone" placeholder="请输入联系电话" maxlength="11"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="confirmProductSeckill">确认抢购</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment'
import { getVoucherList, seckillOrder } from '../api/voucher'
import { getSeckillProductList, seckillProductOrder } from '../api/product'

export default {
  name: 'Seckill',
  data() {
    return {
      activeTab: 'product',
      productList: [],
      voucherList: [],
      loading: false,
      submitting: false,
      currentId: null,
      progressColor: [
        { color: '#f56c6c', percentage: 20 },
        { color: '#e6a23c', percentage: 50 },
        { color: '#67c23a', percentage: 80 }
      ],
      refreshTimer: null,
      // 商品秒杀下单
      orderDialogVisible: false,
      selectedProduct: null,
      orderForm: {
        address: '',
        phone: ''
      },
      orderRules: {
        address: [
          { required: true, message: '请输入收货地址', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadProductSeckills()
    this.loadVoucherSeckills()
    // 每10秒刷新
    this.refreshTimer = setInterval(() => {
      if (this.activeTab === 'product') {
        this.loadProductSeckills()
      } else {
        this.loadVoucherSeckills()
      }
    }, 10000)
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
  },
  methods: {
    handleTabChange(tab) {
      if (tab.name === 'product') {
        this.loadProductSeckills()
      } else {
        this.loadVoucherSeckills()
      }
    },
    async loadProductSeckills() {
      this.loading = true
      try {
        const res = await getSeckillProductList()
        this.productList = res || []
      } catch (error) {
        console.error('加载秒杀商品失败:', error)
      } finally {
        this.loading = false
      }
    },
    async loadVoucherSeckills() {
      this.loading = true
      try {
        const res = await getVoucherList(1)
        this.voucherList = (res || []).filter(v => v.type === 1).map(v => {
          const now = new Date().getTime()
          const beginTime = new Date(v.beginTime).getTime()
          const endTime = new Date(v.endTime).getTime()
          if (now < beginTime) {
            v.status = 0
          } else if (now > endTime) {
            v.status = 2
          } else {
            v.status = 1
          }
          return v
        })
      } catch (error) {
        console.error('加载秒杀券失败:', error)
      } finally {
        this.loading = false
      }
    },
    // ========== 商品秒杀 ==========
    getProductStatus(item) {
      const now = new Date().getTime()
      const begin = new Date(item.beginTime).getTime()
      const end = new Date(item.endTime).getTime()
      if (now < begin) return 0
      if (now > end) return 2
      return 1
    },
    getProductStatusType(item) {
      const s = this.getProductStatus(item)
      return { 0: 'info', 1: 'danger', 2: 'info' }[s] || 'info'
    },
    getProductStatusText(item) {
      const s = this.getProductStatus(item)
      return { 0: '未开始', 1: '抢购中', 2: '已结束' }[s] || '未知'
    },
    getProductButtonText(item) {
      const s = this.getProductStatus(item)
      if (s === 0) return '即将开始'
      if (s === 2) return '已结束'
      if (item.stock <= 0) return '已抢完'
      return '立即抢购'
    },
    calcProductPercent(item) {
      const total = item.stock + 100
      return Math.round((100 - item.stock) / total * 100)
    },
    handleProductSeckill(item) {
      if (!this.$store.getters.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      this.selectedProduct = item
      this.orderForm = { address: '', phone: '' }
      this.orderDialogVisible = true
    },
    confirmProductSeckill() {
      this.$refs.orderForm.validate(async valid => {
        if (!valid) return

        this.currentId = 'p' + this.selectedProduct.id
        this.submitting = true

        try {
          await seckillProductOrder(
            this.selectedProduct.id,
            this.orderForm.address,
            this.orderForm.phone
          )
          this.$message.success('抢购成功！')
          this.orderDialogVisible = false
          // 跳转到订单页
          this.$router.push({ path: '/order' })
        } catch (error) {
          console.error('抢购失败:', error)
        } finally {
          this.submitting = false
          this.currentId = null
        }
      })
    },
    // ========== 优惠券秒杀 ==========
    getVoucherStatusType(status) {
      return { 0: 'info', 1: 'danger', 2: 'info' }[status] || 'info'
    },
    getVoucherStatusText(status) {
      return { 0: '未开始', 1: '抢购中', 2: '已结束' }[status] || '未知'
    },
    getVoucherButtonText(item) {
      if (item.status === 0) return '即将开始'
      if (item.status === 2) return '已结束'
      if (item.stock <= 0) return '已抢完'
      return '立即抢购'
    },
    calcVoucherPercent(item) {
      const total = item.stock + 100
      return Math.round((100 - item.stock) / total * 100)
    },
    async handleVoucherSeckill(item) {
      if (!this.$store.getters.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }

      this.currentId = 'v' + item.id
      this.submitting = true

      try {
        await seckillOrder(item.id)
        this.$message.success('抢购成功！')
        this.$router.push({ path: '/order', query: { tab: 'voucher' } })
      } catch (error) {
        console.error('抢购失败:', error)
      } finally {
        this.submitting = false
        this.currentId = null
      }
    },
    formatTime(time) {
      return moment(time).format('MM-DD HH:mm')
    }
  }
}
</script>

<style lang="scss" scoped>
.seckill-page {
  padding-top: 20px;
}

.seckill-header {
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

.seckill-list {
  .seckill-card {
    margin-bottom: 20px;

    .seckill-image {
      height: 180px;
      overflow: hidden;
      border-radius: 8px 8px 0 0;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .seckill-content {
      .seckill-title {
        font-size: 18px;
        font-weight: bold;
        color: #333;
        margin-bottom: 6px;
      }

      .seckill-subtitle {
        color: #666;
        font-size: 14px;
        margin-bottom: 6px;
      }

      .seckill-rules {
        color: #999;
        font-size: 13px;
        margin-bottom: 8px;
      }

      .seckill-price-row {
        display: flex;
        align-items: baseline;
        gap: 10px;
        margin-bottom: 12px;

        .seckill-price {
          color: #f5222d;
          font-size: 26px;
          font-weight: bold;
        }

        .original-price {
          color: #999;
          font-size: 14px;
          text-decoration: line-through;
        }

        .discount-tag {
          background: #fff1f0;
          color: #f5222d;
          font-size: 12px;
          padding: 2px 6px;
          border-radius: 4px;
          font-weight: bold;
        }
      }

      .seckill-progress {
        margin: 10px 0;

        .progress-label {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #999;
          margin-bottom: 5px;
        }
      }

      .seckill-time {
        color: #999;
        font-size: 13px;
        margin-bottom: 6px;
      }

      .seckill-status {
        margin-top: 8px;
      }
    }
  }
}

.order-confirm {
  .confirm-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 6px;

    .confirm-name {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }

    .confirm-price {
      color: #f5222d;
      font-weight: bold;
      font-size: 16px;
    }
  }
}
</style>
