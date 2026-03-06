<template>
  <div class="product-list-page">
    <div class="page-header">
      <h3>商品列表</h3>
      <el-button type="primary" icon="el-icon-plus" @click="$router.push('/admin/product/add')">
        添加商品
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="productList" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="图片" width="100">
          <template slot-scope="scope">
            <el-image
              :src="getImageUrl(scope.row.image)"
              :preview-src-list="[getImageUrl(scope.row.image)]"
              style="width: 60px; height: 60px;"
              fit="cover"
            ></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称"></el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template slot-scope="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80"></el-table-column>
        <el-table-column prop="sold" label="销量" width="80"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="parseInt(scope.row.status) === 1 ? 'success' : 'info'">
              {{ parseInt(scope.row.status) === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="editProduct(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteProduct(scope.row.id)">删除</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog title="编辑商品" :visible.sync="editDialogVisible" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="商品名称">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="商品价格">
          <el-input-number v-model="editForm.price" :min="0" :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="editForm.stock" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/product/upload-image"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="editForm.image" :src="getImageUrl(editForm.image)" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input type="textarea" v-model="editForm.description" rows="4"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ProductList',
  data() {
    return {
      loading: false,
      productList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      editDialogVisible: false,
      editForm: {},
      uploadHeaders: {
        'admin-token': localStorage.getItem('adminToken')
      }
    }
  },
  created() {
    this.loadProducts()
  },
  methods: {
    loadProducts() {
      this.loading = true
      this.$http.get('/admin/product/list', {
        params: {
          current: this.currentPage,
          size: this.pageSize
        }
      })
        .then(response => {
          // main.js 使用的是原生 axios，需要解包 response.data
          const res = response.data
          if (res.success) {
            this.productList = res.data.list || []
            this.total = res.data.total || 0
          } else {
            this.$message.error(res.msg || '加载失败')
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadProducts()
    },
    editProduct(row) {
      // 确保 status 是数字类型
      this.editForm = {
        ...row,
        status: parseInt(row.status) || 0
      }
      this.editDialogVisible = true
    },
    saveEdit() {
      this.$http.put('/admin/product/update', this.editForm)
        .then(response => {
          const res = response.data
          if (res.success) {
            this.$message.success('更新成功')
            this.editDialogVisible = false
            this.loadProducts()
          } else {
            this.$message.error(res.msg || '更新失败')
          }
        })
    },
    deleteProduct(id) {
      this.$confirm('确定要删除该商品吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$http.delete(`/admin/product/delete/${id}`)
          .then(response => {
            const res = response.data
            if (res.success) {
              this.$message.success('删除成功')
              this.loadProducts()
            } else {
              this.$message.error(res.msg || '删除失败')
            }
          })
      }).catch(() => {})
    },
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isImage) {
        this.$message.error('只能上传图片文件')
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB')
      }
      return isImage && isLt2M
    },
    handleImageSuccess(res) {
      // 上传接口返回原始响应，需要检查结构
      if (res && (res.success || res.data)) {
        this.editForm.image = res.data || res
        this.$message.success('上传成功')
      } else {
        this.$message.error('上传失败')
      }
    },
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http')) return image
      // 添加 baseURL 前缀
      const baseURL = process.env.VUE_APP_BASE_API || '/api'
      return baseURL + image
    }
  }
}
</script>

<style lang="scss" scoped>
.product-list-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
    }
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      text-align: right;
    }
  }

  .avatar-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 178px;
    height: 178px;

    &:hover {
      border-color: #409EFF;
    }

    .avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 178px;
      line-height: 178px;
      text-align: center;
    }

    .avatar {
      width: 178px;
      height: 178px;
      display: block;
      object-fit: cover;
    }
  }
}
</style>
