<template>
  <div class="product-add-page">
    <div class="page-header">
      <h3>添加商品</h3>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card class="form-card">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称"></el-input>
        </el-form-item>

        <el-form-item label="商品价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" placeholder="请输入商品价格"></el-input-number>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" placeholder="请输入库存"></el-input-number>
        </el-form-item>

        <el-form-item label="商品状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="商品图片" prop="image">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/product/upload-image"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="form.image" :src="form.image" class="avatar">
            <div v-else class="upload-placeholder">
              <i class="el-icon-plus"></i>
              <div class="upload-text">点击上传图片</div>
            </div>
          </el-upload>
          <div class="upload-tip">支持jpg、png格式，大小不超过2MB</div>
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            type="textarea"
            v-model="form.description"
            :rows="4"
            placeholder="请输入商品描述"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ProductAdd',
  data() {
    return {
      submitting: false,
      form: {
        name: '',
        price: 0,
        stock: 0,
        status: 1,
        image: '',
        description: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入商品名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        price: [
          { required: true, message: '请输入商品价格', trigger: 'blur' }
        ],
        stock: [
          { required: true, message: '请输入库存', trigger: 'blur' }
        ],
        image: [
          { required: true, message: '请上传商品图片', trigger: 'change' }
        ]
      },
      uploadHeaders: {
        'admin-token': localStorage.getItem('adminToken')
      }
    }
  },
  methods: {
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.submitting = true
          this.$http.post('/admin/product/add', this.form)
            .then(response => {
              const res = response.data
              if (res.success) {
                this.$message.success('添加成功')
                this.$router.push('/admin/product/list')
              } else {
                this.$message.error(res.msg || '添加失败')
              }
            })
            .catch(err => {
              this.$message.error('添加失败：' + err.message)
            })
            .finally(() => {
              this.submitting = false
            })
        }
      })
    },
    resetForm() {
      this.$refs.form.resetFields()
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
    handleImageSuccess(res, file) {
      // 上传组件直接返回响应数据
      if (res.success) {
        this.form.image = res.data
        this.$message.success('上传成功')
      } else {
        this.$message.error(res.msg || '上传失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.product-add-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
    }
  }

  .form-card {
    max-width: 800px;

    .avatar-uploader {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      width: 200px;
      height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        border-color: #409EFF;
      }

      .upload-placeholder {
        text-align: center;
        color: #8c939d;

        i {
          font-size: 28px;
          margin-bottom: 10px;
        }

        .upload-text {
          font-size: 12px;
        }
      }

      .avatar {
        width: 200px;
        height: 200px;
        display: block;
        object-fit: cover;
      }
    }

    .upload-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 8px;
    }
  }
}
</style>
