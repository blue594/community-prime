<template>
  <div class="service-page page-container">
    <div class="service-header">
      <h1>家政维修服务</h1>
      <p>专业服务团队，品质生活保障</p>
    </div>

    <!-- 服务分类 -->
    <div class="category-tabs">
      <el-radio-group v-model="currentCategory" @change="handleCategoryChange">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="1">家政服务</el-radio-button>
        <el-radio-button :label="2">维修服务</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 服务列表 -->
    <div class="service-list">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in serviceList" :key="item.id">
          <div class="card service-card">
            <div class="service-image">
              <img :src="getImageUrl(item.image) || defaultImage" :alt="item.name">
              <div class="service-category">
                <el-tag size="small" :type="item.category === 1 ? 'success' : 'warning'">
                  {{ item.category === 1 ? '家政' : '维修' }}
                </el-tag>
              </div>
            </div>
            <div class="service-content">
              <h3 class="service-name">{{ item.name }}</h3>
              <p class="service-desc">{{ item.description }}</p>

              <!-- 评分显示 -->
              <div class="service-rating" v-if="ratingMap[item.id]">
                <el-rate
                  :value="ratingMap[item.id].avgRating"
                  disabled
                  show-score
                  text-color="#ff9900"
                  :score-template="ratingMap[item.id].avgRating + '分'"
                ></el-rate>
                <span class="review-count">{{ ratingMap[item.id].reviewCount }}条评价</span>
              </div>
              <div class="service-rating no-review" v-else>
                <span>暂无评价</span>
              </div>

              <div class="service-price">
                <span class="price">¥{{ item.basePrice }}</span>
                <span class="unit">/{{ item.unit }}</span>
              </div>
              <div class="service-actions">
                <el-button 
                  type="primary" 
                  style="flex: 1;"
                  @click="handleBooking(item)"
                >立即预约</el-button>
                <el-button
                  type="text"
                  @click="showReviews(item)"
                  v-if="ratingMap[item.id] && ratingMap[item.id].reviewCount > 0"
                >查看评价</el-button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 空状态 -->
    <div v-if="serviceList.length === 0 && !loading" class="empty-state">
      <i class="el-icon-service empty-icon"></i>
      <p class="empty-text">暂无服务</p>
    </div>

    <!-- 预约弹窗 -->
    <el-dialog title="服务预约" :visible.sync="bookingDialogVisible" width="500px">
      <el-form :model="bookingForm" :rules="bookingRules" ref="bookingForm" label-width="100px">
        <el-form-item label="服务名称">
          <span>{{ selectedService ? selectedService.name : '' }}</span>
        </el-form-item>
        <el-form-item label="预约日期" prop="bookingDate">
          <el-date-picker
            v-model="bookingForm.bookingDate"
            type="date"
            placeholder="选择日期"
            :picker-options="datePickerOptions"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="预约时间" prop="startTime">
          <el-time-select
            v-model="bookingForm.startTime"
            :picker-options="{
              start: '08:00',
              step: '01:00',
              end: '18:00'
            }"
            placeholder="选择时间"
            style="width: 100%;"
          ></el-time-select>
        </el-form-item>
        <el-form-item label="服务地址" prop="address">
          <el-input 
            v-model="bookingForm.address" 
            type="textarea"
            :rows="2"
            placeholder="请输入详细服务地址"
          ></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="bookingForm.phone" placeholder="请输入联系电话" maxlength="11"></el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="bookingForm.remark" 
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息（选填）"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">确认预约</el-button>
      </div>
    </el-dialog>

    <!-- 评价列表弹窗 -->
    <el-dialog :title="reviewDialogTitle" :visible.sync="reviewDialogVisible" width="600px">
      <div class="review-summary" v-if="reviewService">
        <div class="summary-rating">
          <span class="big-score">{{ reviewRatingStats.avgRating || 0 }}</span>
          <el-rate :value="reviewRatingStats.avgRating || 0" disabled></el-rate>
          <span class="total-count">共{{ reviewRatingStats.reviewCount || 0 }}条评价</span>
        </div>
      </div>
      <div class="review-list">
        <div v-for="review in reviewList" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="review-user">{{ review.userNickName || '匿名用户' }}</span>
            <el-rate :value="review.rating" disabled show-score text-color="#ff9900" :score-template="review.rating + ''"></el-rate>
            <span class="review-time">{{ formatReviewTime(review.createTime) }}</span>
          </div>
          <div class="review-content" v-if="review.content">{{ review.content }}</div>
          <div class="review-content no-content" v-else>该用户未留下文字评价</div>
        </div>
        <div v-if="reviewList.length === 0" class="empty-review">
          <p>暂无评价</p>
        </div>
      </div>
      <div class="review-pagination" v-if="reviewTotal > 10">
        <el-pagination
          small
          background
          layout="prev, pager, next"
          :total="reviewTotal"
          :page-size="10"
          :current-page="reviewPage"
          @current-change="handleReviewPageChange"
        ></el-pagination>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment'
import { getServiceTypeList, createBooking, getRatingStats, getReviewList } from '../api/service'

export default {
  name: 'Service',
  data() {
    return {
      defaultImage: 'https://via.placeholder.com/300x200?text=Service',
      currentCategory: null,
      serviceList: [],
      loading: false,
      bookingDialogVisible: false,
      selectedService: null,
      bookingForm: {
        serviceTypeId: null,
        bookingDate: '',
        startTime: '',
        address: '',
        phone: '',
        remark: ''
      },
      bookingRules: {
        bookingDate: [
          { required: true, message: '请选择预约日期', trigger: 'change' }
        ],
        startTime: [
          { required: true, message: '请选择预约时间', trigger: 'change' }
        ],
        address: [
          { required: true, message: '请输入服务地址', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      },
      datePickerOptions: {
        disabledDate(time) {
          return time.getTime() < Date.now() - 8.64e7
        }
      },
      submitting: false,
      // 评分数据
      ratingMap: {},
      // 评价弹窗
      reviewDialogVisible: false,
      reviewService: null,
      reviewRatingStats: {},
      reviewList: [],
      reviewTotal: 0,
      reviewPage: 1
    }
  },
  computed: {
    reviewDialogTitle() {
      return this.reviewService ? `${this.reviewService.name} - 用户评价` : '用户评价'
    }
  },
  created() {
    this.loadServices()
  },
  methods: {
    async loadServices() {
      this.loading = true
      try {
        const res = await getServiceTypeList(this.currentCategory)
        this.serviceList = res || []
        // 加载每个服务的评分
        this.loadAllRatings()
      } catch (error) {
        console.error('加载服务列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    async loadAllRatings() {
      // 使用 Promise.all 并行加载评分，避免串行等待
      const promises = this.serviceList.map(async (service) => {
        try {
          const stats = await getRatingStats(service.id)
          if (stats && stats.reviewCount > 0) {
            this.$set(this.ratingMap, service.id, stats)
          }
        } catch (error) {
          // 静默处理
        }
      })
      await Promise.all(promises)
    },
    handleCategoryChange() {
      this.loadServices()
    },
    handleBooking(service) {
      if (!this.$store.getters.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      this.selectedService = service
      this.bookingForm.serviceTypeId = service.id
      this.bookingDialogVisible = true
    },
    submitBooking() {
      this.$refs.bookingForm.validate(async valid => {
        if (!valid) return

        this.submitting = true
        try {
          // 格式化日期和时间
          const bookingData = {
            ...this.bookingForm,
            bookingDate: moment(this.bookingForm.bookingDate).format('YYYY-MM-DD'),
            startTime: this.bookingForm.startTime + ':00'
          }

          await createBooking(bookingData)
          this.$message.success('预约成功')
          this.bookingDialogVisible = false
          this.resetForm()
          // 跳转到我的预约
          this.$router.push('/booking')
        } catch (error) {
          console.error('预约失败:', error)
        } finally {
          this.submitting = false
        }
      })
    },
    resetForm() {
      this.bookingForm = {
        serviceTypeId: null,
        bookingDate: '',
        startTime: '',
        address: '',
        phone: '',
        remark: ''
      }
      this.selectedService = null
      this.$refs.bookingForm && this.$refs.bookingForm.resetFields()
    },
    async showReviews(service) {
      this.reviewService = service
      this.reviewPage = 1
      this.reviewDialogVisible = true
      await this.loadReviewData()
    },
    async loadReviewData() {
      try {
        // 获取评分统计
        const stats = await getRatingStats(this.reviewService.id)
        this.reviewRatingStats = stats || {}
        // 获取评价列表
        const res = await getReviewList(this.reviewService.id, this.reviewPage, 10)
        this.reviewList = (res && res.list) || []
        this.reviewTotal = (res && res.total) || 0
      } catch (error) {
        console.error('加载评价失败:', error)
      }
    },
    handleReviewPageChange(page) {
      this.reviewPage = page
      this.loadReviewData()
    },
    formatReviewTime(time) {
      return moment(time).format('YYYY-MM-DD HH:mm')
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
.service-page {
  padding-top: 20px;
}

.service-header {
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

.category-tabs {
  text-align: center;
  margin-bottom: 30px;
}

.service-list {
  .service-card {
    margin-bottom: 20px;
    overflow: hidden;

    .service-image {
      position: relative;
      height: 200px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s;
      }

      &:hover img {
        transform: scale(1.05);
      }

      .service-category {
        position: absolute;
        top: 10px;
        left: 10px;
      }
    }

    .service-content {
      padding: 15px;

      .service-name {
        font-size: 18px;
        color: #333;
        margin-bottom: 8px;
      }

      .service-desc {
        color: #999;
        font-size: 13px;
        line-height: 1.5;
        margin-bottom: 12px;
        height: 40px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }

      .service-rating {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        gap: 8px;

        .review-count {
          color: #999;
          font-size: 12px;
        }

        &.no-review {
          color: #ccc;
          font-size: 13px;
        }
      }

      .service-price {
        margin-bottom: 15px;

        .price {
          color: #f5222d;
          font-size: 24px;
          font-weight: bold;
        }

        .unit {
          color: #999;
          font-size: 14px;
        }
      }

      .service-actions {
        display: flex;
        align-items: center;
        gap: 10px;
      }
    }
  }
}

/* 评价弹窗 */
.review-summary {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;

  .summary-rating {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;

    .big-score {
      font-size: 36px;
      font-weight: bold;
      color: #ff9900;
    }

    .total-count {
      color: #999;
      font-size: 13px;
    }
  }
}

.review-list {
  .review-item {
    padding: 15px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .review-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .review-user {
        font-weight: 500;
        color: #333;
      }

      .review-time {
        color: #999;
        font-size: 12px;
        margin-left: auto;
      }
    }

    .review-content {
      color: #666;
      font-size: 14px;
      line-height: 1.6;

      &.no-content {
        color: #ccc;
        font-style: italic;
      }
    }
  }

  .empty-review {
    text-align: center;
    color: #999;
    padding: 40px 0;
  }
}

.review-pagination {
  text-align: center;
  margin-top: 20px;
}
</style>
