<template>
  <div class="order-manage-page">
    <div class="page-header">
      <h3>订单管理</h3>
    </div>

    <el-card class="table-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="商品订单" name="product">
          <el-table :data="productOrders" v-loading="loading" border>
            <el-table-column prop="id" label="订单号" width="180"></el-table-column>
            <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
            <el-table-column prop="productId" label="商品ID" width="80"></el-table-column>
            <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="100">
              <template slot-scope="scope">¥{{ scope.row.totalAmount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="scope">
                <el-tag :type="getProductStatusType(scope.row.status)">
                  {{ getProductStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="viewProductOrder(scope.row)">查看</el-button>
                <el-button size="mini" type="success" v-if="scope.row.status === 1" @click="shipOrder(scope.row.id)">发货</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="优惠券订单" name="voucher">
          <el-table :data="voucherOrders" v-loading="loading" border>
            <el-table-column prop="id" label="订单号" width="180"></el-table-column>
            <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
            <el-table-column prop="voucherId" label="优惠券ID" width="100"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="scope">
                <el-tag :type="getVoucherStatusType(scope.row.status)">
                  {{ getVoucherStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="verifyCode" label="核销码" width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.verifyCode" style="color: #f56c6c; font-weight: bold;">{{ scope.row.verifyCode }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="success" v-if="scope.row.status === 1" @click="verifyVoucher(scope.row.id)">核销</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import moment from 'moment'

export default {
  name: 'OrderManage',
  data() {
    return {
      activeTab: 'product',
      loading: false,
      productOrders: [],
      voucherOrders: [],
      total: 0,
      currentPage: 1,
      pageSize: 10
    }
  },
  created() {
    this.loadProductOrders()
  },
  methods: {
    handleTabChange(tab) {
      this.currentPage = 1
      if (tab.name === 'product') {
        this.loadProductOrders()
      } else {
        this.loadVoucherOrders()
      }
    },
    loadProductOrders() {
      this.loading = true
      this.$http.get('/admin/order/product/list', {
        params: { current: this.currentPage, size: this.pageSize }
      }).then(response => {
        console.log('Product orders response:', response.data)
        const res = response.data
        if (res.success) {
          // 兼容两种数据结构
          if (Array.isArray(res.data)) {
            this.productOrders = res.data
            this.total = res.data.length
          } else {
            this.productOrders = res.data.list || res.data.records || []
            this.total = res.data.total || 0
          }
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      }).catch(err => {
        console.error('Product orders error:', err)
      }).finally(() => {
        this.loading = false
      })
    },
    loadVoucherOrders() {
      this.loading = true
      this.$http.get('/admin/order/voucher/list', {
        params: { current: this.currentPage, size: this.pageSize }
      }).then(response => {
        console.log('Voucher orders response:', response.data)
        const res = response.data
        if (res.success) {
          // 兼容两种数据结构
          if (Array.isArray(res.data)) {
            this.voucherOrders = res.data
            this.total = res.data.length
          } else {
            this.voucherOrders = res.data.list || res.data.records || []
            this.total = res.data.total || 0
          }
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      }).catch(err => {
        console.error('Voucher orders error:', err)
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.currentPage = page
      if (this.activeTab === 'product') {
        this.loadProductOrders()
      } else {
        this.loadVoucherOrders()
      }
    },
    shipOrder(orderId) {
      this.$http.post(`/admin/order/product/ship/${orderId}`).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('发货成功')
          this.loadProductOrders()
        } else {
          this.$message.error(res.msg || '发货失败')
        }
      })
    },
    verifyVoucher(orderId) {
      this.$http.post(`/voucher/order/verify/${orderId}`).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('核销成功')
          this.loadVoucherOrders()
        } else {
          this.$message.error(res.msg || '核销失败')
        }
      })
    },
    viewProductOrder(order) {
      this.$message.info(`订单详情：${order.id}`)
    },
    getProductStatusType(status) {
      const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'info' }
      return map[status] || 'info'
    },
    getProductStatusText(status) {
      const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
      return map[status] || '未知'
    },
    getVoucherStatusType(status) {
      const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger', 5: 'info' }
      return map[status] || 'info'
    },
    getVoucherStatusText(status) {
      const map = { 0: '未支付', 1: '已支付', 2: '已核销', 3: '已取消', 4: '退款中', 5: '已退款' }
      return map[status] || '未知'
    },
    formatTime(time) {
      return time ? moment(time).format('YYYY-MM-DD HH:mm:ss') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.order-manage-page {
  .page-header {
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
  .pagination {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
