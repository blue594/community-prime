<template>
  <div class="booking-page page-container">
    <h1 class="page-title">我的预约</h1>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="bookingStatus" size="small" @change="loadBookings">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="0">待确认</el-radio-button>
        <el-radio-button :label="1">已确认</el-radio-button>
        <el-radio-button :label="2">服务中</el-radio-button>
        <el-radio-button :label="3">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 预约列表 -->
    <div class="booking-list">
      <div v-for="booking in bookings" :key="booking.id" class="card booking-item">
        <div class="booking-header">
          <span class="booking-id">预约号：{{ booking.id }}</span>
          <span class="booking-time">{{ formatTime(booking.createTime) }}</span>
          <el-tag :type="getStatusType(booking.status)" size="small">
            {{ getStatusText(booking.status) }}
          </el-tag>
        </div>
        <div class="booking-body">
          <div class="service-info">
            <h3 class="service-name">{{ booking.serviceName }}</h3>
            <div class="info-row">
              <span class="label">预约时间：</span>
              <span class="value">{{ booking.bookingDate }} {{ booking.startTime }} - {{ booking.endTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">服务地址：</span>
              <span class="value">{{ booking.address }}</span>
            </div>
            <div class="info-row">
              <span class="label">联系电话：</span>
              <span class="value">{{ booking.phone }}</span>
            </div>
            <div class="info-row" v-if="booking.remark">
              <span class="label">备注：</span>
              <span class="value">{{ booking.remark }}</span>
            </div>
          </div>
          <div class="booking-price">
            <div class="price-label">服务价格</div>
            <div class="price-value">¥{{ booking.price }}</div>
          </div>
        </div>
        <div class="booking-footer">
          <el-button v-if="booking.status === 0 || booking.status === 1"
            type="danger" size="small" @click="handleCancel(booking.id)">取消预约</el-button>
          <el-button v-if="booking.status === 3 && !booking.reviewed"
            type="warning" size="small" @click="openReviewDialog(booking)">评价服务</el-button>
          <el-tag v-if="booking.reviewed" type="success" size="small">已评价</el-tag>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="bookings.length === 0 && !loading" class="empty-state">
      <i class="el-icon-date empty-icon"></i>
      <p class="empty-text">暂无预约</p>
      <el-button type="primary" @click="$router.push('/service')">去预约</el-button>
    </div>

    <!-- 评价弹窗 -->
    <el-dialog title="服务评价" :visible.sync="reviewDialogVisible" width="500px" :close-on-click-modal="false">
      <div class="review-form" v-if="currentBooking">
        <div class="review-service-name">{{ currentBooking.serviceName }}</div>
        <div class="review-field">
          <span class="review-label">服务评分：</span>
          <el-rate
            v-model="reviewForm.rating"
            :texts="['很差', '较差', '一般', '满意', '非常满意']"
            show-text
            :colors="['#F56C6C', '#E6A23C', '#409EFF', '#67C23A', '#67C23A']"
          ></el-rate>
        </div>
        <div class="review-field">
          <span class="review-label">评价内容：</span>
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            placeholder="请分享您对这次服务的体验和感受（选填）"
            maxlength="500"
            show-word-limit
          ></el-input>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReviewHandler">提交评价</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment'
import { getMyBookings, cancelBooking, submitReview } from '../api/service'

export default {
  name: 'Booking',
  data() {
    return {
      bookingStatus: null,
      bookings: [],
      loading: false,
      // 评价相关
      reviewDialogVisible: false,
      reviewSubmitting: false,
      currentBooking: null,
      reviewForm: {
        rating: 5,
        content: ''
      }
    }
  },
  created() {
    this.loadBookings()
  },
  methods: {
    async loadBookings() {
      this.loading = true
      try {
        const res = await getMyBookings(this.bookingStatus)
        this.bookings = res || []
      } catch (error) {
        console.error('加载预约列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    async handleCancel(bookingId) {
      try {
        await this.$confirm('确定要取消该预约吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await cancelBooking(bookingId)
        this.$message.success('预约已取消')
        this.loadBookings()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('取消预约失败:', error)
        }
      }
    },
    openReviewDialog(booking) {
      this.currentBooking = booking
      this.reviewForm = { rating: 5, content: '' }
      this.reviewDialogVisible = true
    },
    async submitReviewHandler() {
      if (!this.reviewForm.rating || this.reviewForm.rating < 1) {
        this.$message.warning('请选择评分')
        return
      }

      this.reviewSubmitting = true
      try {
        await submitReview(
          this.currentBooking.id,
          this.reviewForm.rating,
          this.reviewForm.content
        )
        this.$message.success('评价成功，感谢您的反馈！')
        this.reviewDialogVisible = false
        // 标记为已评价
        this.currentBooking.reviewed = true
        this.loadBookings()
      } catch (error) {
        console.error('评价失败:', error)
      } finally {
        this.reviewSubmitting = false
      }
    },
    formatTime(time) {
      return moment(time).format('YYYY-MM-DD HH:mm:ss')
    },
    getStatusType(status) {
      const map = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'info',
        4: 'danger'
      }
      return map[status] || 'info'
    },
    getStatusText(status) {
      const map = {
        0: '待确认',
        1: '已确认',
        2: '服务中',
        3: '已完成',
        4: '已取消'
      }
      return map[status] || '未知'
    }
  }
}
</script>

<style lang="scss" scoped>
.booking-page {
  padding-top: 20px;

  .page-title {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .filter-bar {
    margin-bottom: 20px;
  }

  .booking-list {
    .booking-item {
      margin-bottom: 20px;

      .booking-header {
        display: flex;
        align-items: center;
        gap: 20px;
        padding-bottom: 15px;
        border-bottom: 1px solid #eee;
        margin-bottom: 15px;

        .booking-id {
          color: #333;
          font-weight: 500;
        }

        .booking-time {
          color: #999;
          font-size: 13px;
        }
      }

      .booking-body {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;

        .service-info {
          flex: 1;

          .service-name {
            font-size: 18px;
            color: #333;
            margin-bottom: 12px;
          }

          .info-row {
            margin-bottom: 8px;

            .label {
              color: #999;
            }

            .value {
              color: #333;
            }
          }
        }

        .booking-price {
          text-align: right;

          .price-label {
            color: #999;
            font-size: 13px;
            margin-bottom: 5px;
          }

          .price-value {
            color: #f5222d;
            font-size: 20px;
            font-weight: bold;
          }
        }
      }

      .booking-footer {
        margin-top: 15px;
        padding-top: 15px;
        border-top: 1px solid #eee;
        text-align: right;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 10px;
      }
    }
  }
}

.review-form {
  .review-service-name {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #eee;
  }

  .review-field {
    margin-bottom: 20px;

    .review-label {
      display: block;
      color: #666;
      font-size: 14px;
      margin-bottom: 8px;
    }
  }
}
</style>
