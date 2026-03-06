<template>
  <div class="home">
    <!-- 轮播图 -->
    <div class="banner">
      <el-carousel height="400px">
        <el-carousel-item>
          <div class="banner-item" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <div class="banner-content">
              <h1>邻里优享</h1>
              <p>社区生活服务平台，让便利触手可及</p>
              <el-button type="primary" size="large" @click="$router.push('/product')">立即购物</el-button>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item>
          <div class="banner-item" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <div class="banner-content">
              <h1>限时秒杀</h1>
              <p>超值优惠券，限量抢购</p>
              <el-button type="danger" size="large" @click="$router.push('/seckill')">去抢购</el-button>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item>
          <div class="banner-item" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <div class="banner-content">
              <h1>家政维修</h1>
              <p>专业服务，品质保障</p>
              <el-button type="success" size="large" @click="$router.push('/service')">预约服务</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 功能入口 -->
    <div class="page-container">
      <div class="feature-cards">
        <div class="feature-card" @click="$router.push('/product')">
          <div class="icon" style="background: #e6f7ff;">
            <i class="el-icon-shopping-cart-full" style="color: #1890ff;"></i>
          </div>
          <h3>超市购物</h3>
          <p>次日送达，品质保证</p>
        </div>
        <div class="feature-card" @click="$router.push('/seckill')">
          <div class="icon" style="background: #fff1f0;">
            <i class="el-icon-time" style="color: #ff4d4f;"></i>
          </div>
          <h3>限时秒杀</h3>
          <p>超值优惠，限量抢购</p>
        </div>
        <div class="feature-card" @click="$router.push('/voucher')">
          <div class="icon" style="background: #fff7e6;">
            <i class="el-icon-ticket" style="color: #fa8c16;"></i>
          </div>
          <h3>餐饮优惠券</h3>
          <p>到店消费，超值折扣</p>
        </div>
        <div class="feature-card" @click="$router.push('/service')">
          <div class="icon" style="background: #f6ffed;">
            <i class="el-icon-service" style="color: #52c41a;"></i>
          </div>
          <h3>家政维修</h3>
          <p>专业服务，随叫随到</p>
        </div>
      </div>

      <!-- 热门商品 -->
      <div class="section">
        <div class="section-header">
          <h2>热门商品</h2>
          <el-button type="text" @click="$router.push('/product')">查看更多 <i class="el-icon-arrow-right"></i></el-button>
        </div>
        <el-row :gutter="20">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in hotProducts" :key="item.id">
            <div class="card product-card" @click="goToProductDetail(item.id)">
              <img :src="getImageUrl(item.image) || defaultImage" class="product-image" :alt="item.name">
              <div class="product-name">{{ item.name }}</div>
              <div class="flex-between">
                <span class="product-price">{{ item.price }}</span>
                <span class="product-sold">已售{{ item.sold }}</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 秒杀活动 -->
      <div class="section">
        <div class="section-header">
          <h2>限时秒杀</h2>
          <el-button type="text" @click="$router.push('/seckill')">查看更多 <i class="el-icon-arrow-right"></i></el-button>
        </div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" v-for="item in seckillVouchers" :key="item.id">
            <div class="card voucher-card seckill">
              <div class="voucher-title">{{ item.title }}</div>
              <div class="voucher-rules">{{ item.rules }}</div>
              <div class="voucher-price">
                <span class="pay-value">{{ item.payValue }}</span>
                <span class="actual-value">¥{{ item.actualValue }}</span>
              </div>
              <div class="voucher-stock">剩余库存：{{ item.stock }}件</div>
              <el-button 
                type="danger" 
                class="btn-seckill" 
                style="width: 100%;"
                @click="goToSeckill(item.id)"
              >立即抢购</el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script>
import { getProductList } from '../api/product'
import { getVoucherList } from '../api/voucher'

export default {
  name: 'Home',
  data() {
    return {
      defaultImage: 'https://via.placeholder.com/300x200?text=No+Image',
      hotProducts: [],
      seckillVouchers: []
    }
  },
  created() {
    this.loadHotProducts()
    this.loadSeckillVouchers()
  },
  methods: {
    async loadHotProducts() {
      try {
        const res = await getProductList({ current: 1, size: 8 })
        this.hotProducts = res.list || []
      } catch (error) {
        console.error('加载热门商品失败:', error)
      }
    },
    async loadSeckillVouchers() {
      try {
        // 默认查询商家ID为1的优惠券
        const res = await getVoucherList(1)
        // 只显示秒杀券
        this.seckillVouchers = (res || []).filter(v => v.type === 1).slice(0, 3)
      } catch (error) {
        console.error('加载秒杀券失败:', error)
      }
    },
    goToProductDetail(id) {
      this.$router.push(`/product/detail/${id}`)
    },
    goToSeckill(id) {
      this.$router.push('/seckill')
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
.home {
  .banner {
    .banner-item {
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;

      .banner-content {
        text-align: center;

        h1 {
          font-size: 48px;
          margin-bottom: 16px;
        }

        p {
          font-size: 20px;
          margin-bottom: 24px;
          opacity: 0.9;
        }
      }
    }
  }

  .feature-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin: 30px 0;

    .feature-card {
      background: #fff;
      border-radius: 8px;
      padding: 30px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
      }

      .icon {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 16px;

        i {
          font-size: 32px;
        }
      }

      h3 {
        font-size: 18px;
        margin-bottom: 8px;
        color: #333;
      }

      p {
        color: #999;
        font-size: 14px;
      }
    }
  }

  .section {
    margin: 40px 0;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h2 {
        font-size: 24px;
        color: #333;
        position: relative;
        padding-left: 12px;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 20px;
          background: #409EFF;
          border-radius: 2px;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .home {
    .feature-cards {
      grid-template-columns: 1fr;
    }
  }
}
</style>
