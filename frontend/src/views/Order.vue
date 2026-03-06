<template>
  <div class="order-page page-container">
    <h1 class="page-title">我的订单</h1>

    <!-- 订单类型切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="商品订单" name="product">
        <!-- 状态筛选 -->
        <div class="filter-bar">
          <el-radio-group v-model="productStatus" size="small" @change="loadProductOrders">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">待付款</el-radio-button>
            <el-radio-button :label="1">待发货</el-radio-button>
            <el-radio-button :label="2">待收货</el-radio-button>
            <el-radio-button :label="3">已完成</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 商品订单列表 -->
        <div class="order-list">
          <div v-for="order in productOrders" :key="order.id" class="card order-item">
            <div class="order-header">
              <span class="order-id">订单号：{{ order.id }}</span>
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
              <el-tag :type="getProductStatusType(order.status)" size="small">
                {{ getProductStatusText(order.status) }}
              </el-tag>
            </div>
            <div class="order-body">
              <div class="product-info">
                <div class="info-row">
                  <span class="label">商品ID：</span>
                  <span class="value">{{ order.productId }}</span>
                </div>
                <div class="info-row">
                  <span class="label">数量：</span>
                  <span class="value">{{ order.quantity }}</span>
                </div>
                <div class="info-row">
                  <span class="label">收货地址：</span>
                  <span class="value">{{ order.address }}</span>
                </div>
                <div class="info-row">
                  <span class="label">联系电话：</span>
                  <span class="value">{{ order.phone }}</span>
                </div>
              </div>
              <div class="order-amount">
                <div class="amount-label">订单金额</div>
                <div class="amount-value">¥{{ order.totalAmount }}</div>
              </div>
            </div>
            <div class="order-footer" v-if="order.status === 0">
              <el-button type="danger" size="small" @click="cancelProductOrder(order.id)">取消订单</el-button>
              <el-button type="primary" size="small" @click="payProductOrder(order)">立即支付</el-button>
            </div>
            <div class="order-footer" v-if="order.status === 2">
              <el-button type="success" size="small" @click="confirmReceipt(order.id)">确认收货</el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="productTotal > 0">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="productTotal"
            :page-size="pageSize"
            :current-page="productPage"
            @current-change="handleProductPageChange"
          ></el-pagination>
        </div>
      </el-tab-pane>

      <el-tab-pane label="优惠券订单" name="voucher">
        <!-- 优惠券订单列表 -->
        <div class="order-list">
          <div v-for="order in voucherOrders" :key="order.id" class="card order-item">
            <div class="order-header">
              <span class="order-id">订单号：{{ order.id }}</span>
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
              <el-tag :type="getVoucherStatusType(order.status)" size="small">
                {{ getVoucherStatusText(order.status) }}
              </el-tag>
            </div>
            <div class="order-body">
              <div class="voucher-info">
                <div class="info-row">
                  <span class="label">优惠券ID：</span>
                  <span class="value">{{ order.voucherId }}</span>
                </div>
                <!-- 核销码（已支付时显示） -->
                <div class="info-row verify-code-row" v-if="order.status === 1 && order.verifyCode">
                  <span class="label">核销码：</span>
                  <span class="verify-code">{{ order.verifyCode }}</span>
                  <el-button type="text" size="mini" @click="copyVerifyCode(order.verifyCode)">
                    <i class="el-icon-copy-document"></i> 复制
                  </el-button>
                </div>
                <div class="verify-tip" v-if="order.status === 1 && order.verifyCode">
                  <i class="el-icon-warning-outline"></i>
                  请到店出示核销码，由商家扫码核销
                </div>
              </div>
            </div>
            <div class="order-footer" v-if="order.status === 0">
              <el-button type="primary" size="small" @click="payVoucher(order)">立即支付</el-button>
            </div>
            <div class="order-footer" v-if="order.status === 1">
              <el-button type="success" size="small" @click="verifyVoucher(order)">到店核销</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 空状态 -->
    <div v-if="(activeTab === 'product' && productOrders.length === 0) || 
                (activeTab === 'voucher' && voucherOrders.length === 0)" 
         class="empty-state">
      <i class="el-icon-document empty-icon"></i>
      <p class="empty-text">暂无订单</p>
      <el-button type="primary" @click="$router.push('/product')">去购物</el-button>
    </div>
  </div>
</template>

<script>
import moment from 'moment'
import { getOrderList, cancelOrder, payOrder } from '../api/product'
import { getMyVoucherOrders, payVoucherOrder } from '../api/voucher'

export default {
  name: 'Order',
  data() {
    return {
      activeTab: 'product',
      productStatus: null,
      productOrders: [],
      productTotal: 0,
      productPage: 1,
      pageSize: 10,
      voucherOrders: [],
      loading: false
    }
  },
  created() {
    // 检查是否从秒杀页面跳转过来，需要显示优惠券订单tab
    if (this.$route.query.tab === 'voucher') {
      this.activeTab = 'voucher'
      this.loadVoucherOrders()
    } else {
      this.loadProductOrders()
    }
  },
  methods: {
    async loadProductOrders() {
      this.loading = true
      try {
        const res = await getOrderList({
          status: this.productStatus,
          current: this.productPage,
          size: this.pageSize
        })
        this.productOrders = res.list || []
        this.productTotal = res.total || 0
      } catch (error) {
        console.error('加载订单失败:', error)
      } finally {
        this.loading = false
      }
    },
    async loadVoucherOrders() {
      this.loading = true
      try {
        const res = await getMyVoucherOrders()
        this.voucherOrders = res || []
      } catch (error) {
        console.error('加载优惠券订单失败:', error)
      } finally {
        this.loading = false
      }
    },
    handleTabChange(tab) {
      if (tab.name === 'product') {
        this.loadProductOrders()
      } else {
        this.loadVoucherOrders()
      }
    },
    handleProductPageChange(page) {
      this.productPage = page
      this.loadProductOrders()
    },
    async cancelProductOrder(orderId) {
      try {
        await this.$confirm('确定要取消该订单吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await cancelOrder(orderId)
        this.$message.success('订单已取消')
        this.loadProductOrders()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('取消订单失败:', error)
        }
      }
    },
    async payProductOrder(order) {
      try {
        await this.$confirm(`确定要支付订单 ${order.id} 吗？金额：¥${order.totalAmount}`, '确认支付', {
          confirmButtonText: '确认支付',
          cancelButtonText: '取消',
          type: 'info'
        })
        await payOrder(order.id)
        this.$message.success('支付成功')
        this.loadProductOrders()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('支付失败:', error)
        }
      }
    },
    async confirmReceipt(orderId) {
      try {
        await this.$confirm('确认已收到商品？', '确认收货', {
          confirmButtonText: '确认收货',
          cancelButtonText: '取消',
          type: 'success'
        })
        const { confirmReceipt } = await import('../api/product')
        await confirmReceipt(orderId)
        this.$message.success('确认收货成功，订单已完成')
        this.loadProductOrders()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('确认收货失败:', error)
        }
      }
    },
    async payVoucher(order) {
      try {
        await this.$confirm(`确定要支付优惠券订单吗？`, '确认支付', {
          confirmButtonText: '确认支付',
          cancelButtonText: '取消',
          type: 'info'
        })
        await payVoucherOrder(order.id)
        this.$message.success('支付成功')
        this.loadVoucherOrders()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('支付失败:', error)
        }
      }
    },
    async verifyVoucher(order) {
      try {
        await this.$confirm('确认到店核销此优惠券？', '核销确认', {
          confirmButtonText: '确认核销',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const { verifyVoucher } = await import('../api/voucher')
        await verifyVoucher(order.id)
        this.$message.success('核销成功')
        this.loadVoucherOrders()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('核销失败:', error)
        }
      }
    },
    copyVerifyCode(code) {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(code).then(() => {
          this.$message.success('核销码已复制')
        })
      } else {
        // fallback
        const input = document.createElement('input')
        input.value = code
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('核销码已复制')
      }
    },
    formatTime(time) {
      return moment(time).format('YYYY-MM-DD HH:mm:ss')
    },
    getProductStatusType(status) {
      const map = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'info',
        4: 'danger'
      }
      return map[status] || 'info'
    },
    getProductStatusText(status) {
      const map = {
        0: '待付款',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消'
      }
      return map[status] || '未知'
    },
    getVoucherStatusType(status) {
      const map = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'danger',
        4: 'info',
        5: 'info'
      }
      return map[status] || 'info'
    },
    getVoucherStatusText(status) {
      const map = {
        0: '未支付',
        1: '已支付',
        2: '已核销',
        3: '已取消',
        4: '退款中',
        5: '已退款'
      }
      return map[status] || '未知'
    }
  }
}
</script>

<style lang="scss" scoped>
.order-page {
  padding-top: 20px;

  .page-title {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .filter-bar {
    margin-bottom: 20px;
  }

  .order-list {
    .order-item {
      margin-bottom: 20px;

      .order-header {
        display: flex;
        align-items: center;
        gap: 20px;
        padding-bottom: 15px;
        border-bottom: 1px solid #eee;
        margin-bottom: 15px;

        .order-id {
          color: #333;
          font-weight: 500;
        }

        .order-time {
          color: #999;
          font-size: 13px;
        }
      }

      .order-body {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;

        .product-info,
        .voucher-info {
          flex: 1;

          .info-row {
            margin-bottom: 8px;

            .label {
              color: #999;
            }

            .value {
              color: #333;
            }
          }

          .verify-code-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .verify-code {
              font-size: 24px;
              font-weight: bold;
              color: #f5222d;
              letter-spacing: 4px;
              font-family: 'Courier New', monospace;
              background: #fff1f0;
              padding: 4px 12px;
              border-radius: 4px;
              border: 1px dashed #f5222d;
            }
          }

          .verify-tip {
            color: #fa8c16;
            font-size: 13px;
            margin-top: 8px;
            padding: 6px 10px;
            background: #fff7e6;
            border-radius: 4px;

            i {
              margin-right: 4px;
            }
          }
        }

        .order-amount {
          text-align: right;

          .amount-label {
            color: #999;
            font-size: 13px;
            margin-bottom: 5px;
          }

          .amount-value {
            color: #f5222d;
            font-size: 20px;
            font-weight: bold;
          }
        }
      }

      .order-footer {
        margin-top: 15px;
        padding-top: 15px;
        border-top: 1px solid #eee;
        text-align: right;
      }
    }
  }

  .pagination {
    text-align: center;
    margin-top: 30px;
  }
}
</style>
