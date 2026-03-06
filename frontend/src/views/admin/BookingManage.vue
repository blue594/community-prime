<template>
  <div class="booking-manage-page">
    <div class="page-header">
      <h3>预约管理</h3>
    </div>

    <el-card class="table-card">
      <el-table :data="bookingList" v-loading="loading" border>
        <el-table-column prop="id" label="预约号" width="100"></el-table-column>
        <el-table-column prop="userId" label="用户ID" width="80"></el-table-column>
        <el-table-column prop="serviceName" label="服务名称"></el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template slot-scope="scope">¥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column label="预约时间" width="180">
          <template slot-scope="scope">
            {{ scope.row.bookingDate }} {{ scope.row.startTime }}-{{ scope.row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120"></el-table-column>
        <el-table-column prop="address" label="服务地址" show-overflow-tooltip></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" v-if="scope.row.status === 0" @click="updateStatus(scope.row.id, 1)">确认</el-button>
            <el-button size="mini" type="success" v-if="scope.row.status === 1" @click="updateStatus(scope.row.id, 2)">开始服务</el-button>
            <el-button size="mini" type="success" v-if="scope.row.status === 2" @click="updateStatus(scope.row.id, 3)">完成</el-button>
            <el-button size="mini" type="danger" v-if="scope.row.status < 3" @click="cancelBooking(scope.row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

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
  name: 'BookingManage',
  data() {
    return {
      loading: false,
      bookingList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10
    }
  },
  created() {
    this.loadBookings()
  },
  methods: {
    loadBookings() {
      this.loading = true
      this.$http.get('/admin/booking/list', {
        params: { current: this.currentPage, size: this.pageSize }
      }).then(response => {
        const res = response.data
        if (res.success) {
          this.bookingList = res.data.list || res.data.records || []
          this.total = res.data.total || 0
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadBookings()
    },
    updateStatus(id, status) {
      this.$http.post(`/admin/booking/update-status/${id}`, { status }).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('状态更新成功')
          this.loadBookings()
        } else {
          this.$message.error(res.msg || '更新失败')
        }
      })
    },
    cancelBooking(id) {
      this.$confirm('确定要取消该预约吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.post(`/admin/booking/cancel/${id}`).then(response => {
          const res = response.data
          if (res.success) {
            this.$message.success('取消成功')
            this.loadBookings()
          } else {
            this.$message.error(res.msg || '取消失败')
          }
        })
      }).catch(() => {})
    },
    getStatusType(status) {
      const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger' }
      return map[status] || 'info'
    },
    getStatusText(status) {
      const map = { 0: '待确认', 1: '已确认', 2: '服务中', 3: '已完成', 4: '已取消' }
      return map[status] || '未知'
    },
    formatTime(time) {
      return time ? moment(time).format('YYYY-MM-DD HH:mm:ss') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.booking-manage-page {
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
