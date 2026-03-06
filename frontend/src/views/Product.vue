<template>
  <div class="product-page page-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品"
        prefix-icon="el-icon-search"
        @keyup.enter.native="handleSearch"
      >
        <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
      </el-input>
    </div>

    <!-- 商品列表 -->
    <div class="product-list">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in productList" :key="item.id">
          <div class="card product-card" @click="goToDetail(item.id)">
            <img :src="getImageUrl(item.image) || defaultImage" class="product-image" :alt="item.name">
            <div class="product-name">{{ item.name }}</div>
            <div class="product-desc" v-if="item.description">{{ item.description }}</div>
            <div class="flex-between">
              <span class="product-price">{{ item.price }}</span>
              <span class="product-stock">库存{{ item.stock }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      ></el-pagination>
    </div>

    <!-- 空状态 -->
    <div v-if="productList.length === 0 && !loading" class="empty-state">
      <i class="el-icon-goods empty-icon"></i>
      <p class="empty-text">暂无商品</p>
    </div>
  </div>
</template>

<script>
import { getProductList, searchProduct } from '../api/product'

export default {
  name: 'Product',
  data() {
    return {
      defaultImage: 'https://via.placeholder.com/300x200?text=No+Image',
      searchKeyword: '',
      productList: [],
      total: 0,
      currentPage: 1,
      pageSize: 12,
      loading: false
    }
  },
  created() {
    this.loadProducts()
  },
  methods: {
    async loadProducts() {
      this.loading = true
      try {
        const res = await getProductList({
          current: this.currentPage,
          size: this.pageSize
        })
        this.productList = res.list || []
        this.total = res.total || 0
      } catch (error) {
        console.error('加载商品失败:', error)
      } finally {
        this.loading = false
      }
    },
    async handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.loadProducts()
        return
      }

      this.loading = true
      try {
        const res = await searchProduct(this.searchKeyword.trim())
        this.productList = res || []
        this.total = 0 // 搜索结果不分页
      } catch (error) {
        console.error('搜索商品失败:', error)
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadProducts()
    },
    goToDetail(id) {
      this.$router.push(`/product/detail/${id}`)
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
.product-page {
  padding-top: 20px;

  .search-bar {
    max-width: 500px;
    margin: 0 auto 30px;
  }

  .product-list {
    margin-bottom: 30px;
  }

  .product-card {
    margin-bottom: 20px;

    .product-desc {
      color: #999;
      font-size: 12px;
      margin: 8px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .product-stock {
      color: #999;
      font-size: 12px;
    }
  }

  .pagination {
    text-align: center;
    margin-top: 30px;
  }
}
</style>
