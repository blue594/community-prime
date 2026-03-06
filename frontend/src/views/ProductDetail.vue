<template>
  <div class="product-detail-page page-container">
    <el-page-header @back="$router.back()" title="商品详情"></el-page-header>

    <div class="product-detail" v-if="product">
      <el-row :gutter="40">
        <!-- 商品图片 -->
        <el-col :xs="24" :md="12">
          <div class="product-image-container">
            <img 
              :src="product.image || defaultImage" 
              :alt="product.name"
              @error="handleImageError"
            >
          </div>
        </el-col>

        <!-- 商品信息 -->
        <el-col :xs="24" :md="12">
          <div class="product-info">
            <h1 class="product-title">{{ product.name }}</h1>
            <p class="product-desc">{{ product.description }}</p>
            
            <div class="product-price-section">
              <span class="price-label">价格</span>
              <span class="price-value">¥{{ product.price }}</span>
            </div>

            <div class="product-meta">
              <div class="meta-item">
                <span class="label">销量</span>
                <span class="value">{{ product.sold }}件</span>
              </div>
              <div class="meta-item">
                <span class="label">库存</span>
                <span class="value">{{ product.stock }}件</span>
              </div>
            </div>

            <!-- 购买表单 -->
            <div class="buy-section" v-if="isLoggedIn">
              <el-form :model="orderForm" label-width="80px">
                <el-form-item label="购买数量">
                  <el-input-number 
                    v-model="orderForm.quantity" 
                    :min="1" 
                    :max="product.stock"
                    :disabled="product.stock <= 0"
                  ></el-input-number>
                </el-form-item>
                <el-form-item label="收货地址">
                  <el-input 
                    v-model="orderForm.address" 
                    placeholder="请输入详细收货地址"
                    type="textarea"
                    :rows="2"
                  ></el-input>
                </el-form-item>
                <el-form-item label="联系电话">
                  <el-input 
                    v-model="orderForm.phone" 
                    placeholder="请输入收货人手机号"
                    maxlength="11"
                  ></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button 
                    type="primary" 
                    size="large"
                    :disabled="product.stock <= 0"
                    :loading="submitting"
                    @click="handleBuy"
                  >
                    {{ product.stock > 0 ? '立即购买' : '暂时缺货' }}
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <div class="login-tip" v-else>
              <el-button type="primary" @click="$router.push('/login')">请先登录</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getProductDetail, createOrder } from '../api/product'

export default {
  name: 'ProductDetail',
  data() {
    return {
      defaultImage: 'https://via.placeholder.com/400x400?text=No+Image',
      product: null,
      orderForm: {
        quantity: 1,
        address: '',
        phone: ''
      },
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['isLoggedIn'])
  },
  created() {
    this.loadProductDetail()
  },
  methods: {
    async loadProductDetail() {
      const id = this.$route.params.id
      try {
        const res = await getProductDetail(id)
        this.product = res
        // 处理图片URL，确保是完整URL
        if (this.product.image && !this.product.image.startsWith('http')) {
          // 如果是相对路径，可以在这里添加域名前缀
          // this.product.image = 'http://your-domain.com' + this.product.image
        }
      } catch (error) {
        console.error('加载商品详情失败:', error)
        this.$message.error('商品不存在')
        this.$router.back()
      }
    },
    handleImageError(e) {
      e.target.src = this.defaultImage
    },
    async handleBuy() {
      if (!this.orderForm.address.trim()) {
        this.$message.warning('请输入收货地址')
        return
      }
      if (!/^1[3-9]\d{9}$/.test(this.orderForm.phone)) {
        this.$message.warning('请输入正确的手机号')
        return
      }

      this.submitting = true
      try {
        const res = await createOrder({
          productId: this.product.id,
          quantity: this.orderForm.quantity,
          address: this.orderForm.address,
          phone: this.orderForm.phone
        })
        this.$message.success('订单创建成功')
        this.$router.push('/order')
      } catch (error) {
        console.error('创建订单失败:', error)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.product-detail-page {
  padding-top: 20px;
}

.product-detail {
  margin-top: 30px;
}

.product-image-container {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  img {
    width: 100%;
    height: auto;
    display: block;
  }
}

.product-info {
  .product-title {
    font-size: 24px;
    color: #333;
    margin-bottom: 16px;
  }

  .product-desc {
    color: #666;
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 20px;
  }

  .product-price-section {
    background: #fff5f5;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;

    .price-label {
      color: #999;
      font-size: 14px;
      margin-right: 12px;
    }

    .price-value {
      color: #f5222d;
      font-size: 32px;
      font-weight: bold;

      &::before {
        content: '¥';
        font-size: 20px;
      }
    }
  }

  .product-meta {
    display: flex;
    gap: 40px;
    margin-bottom: 30px;

    .meta-item {
      .label {
        color: #999;
        font-size: 14px;
        margin-right: 8px;
      }

      .value {
        color: #333;
        font-size: 14px;
      }
    }
  }

  .buy-section {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .login-tip {
    text-align: center;
    padding: 40px;
    background: #f5f5f5;
    border-radius: 8px;
  }
}
</style>
