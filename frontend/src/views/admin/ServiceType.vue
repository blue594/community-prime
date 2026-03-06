<template>
  <div class="service-type-page">
    <div class="page-header">
      <h3>家政服务类型管理</h3>
      <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">添加服务类型</el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="serviceTypeList" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="图标" width="100">
          <template slot-scope="scope">
            <el-image
              :src="getImageUrl(scope.row.image)"
              style="width: 60px; height: 60px;"
              fit="cover"
            ></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="服务名称"></el-table-column>
        <el-table-column prop="description" label="服务描述" show-overflow-tooltip></el-table-column>
        <el-table-column prop="basePrice" label="基础价格" width="100">
          <template slot-scope="scope">
            ¥{{ scope.row.basePrice }}/小时
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="parseInt(scope.row.status) === 1 ? 'success' : 'info'">
              {{ parseInt(scope.row.status) === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="editServiceType(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteServiceType(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入服务名称"></el-input>
        </el-form-item>

        <el-form-item label="基础价格" prop="basePrice">
          <el-input-number v-model="form.basePrice" :min="0" :precision="2"></el-input-number>
          <span class="unit">元/小时</span>
        </el-form-item>

        <el-form-item label="服务状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="服务图标" prop="image">
          <el-upload
            class="avatar-uploader"
            action="/api/admin/service-type/upload-image"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="form.image" :src="getImageUrl(form.image)" class="avatar">
            <div v-else class="upload-placeholder">
              <i class="el-icon-plus"></i>
              <div class="upload-text">点击上传</div>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="服务描述" prop="description">
          <el-input
            type="textarea"
            v-model="form.description"
            :rows="4"
            placeholder="请输入服务描述"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ServiceType',
  data() {
    return {
      loading: false,
      serviceTypeList: [],
      dialogVisible: false,
      dialogTitle: '添加服务类型',
      isEdit: false,
      form: {
        id: null,
        name: '',
        basePrice: 0,
        status: 1,
        image: '',
        description: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入服务名称', trigger: 'blur' }
        ],
        basePrice: [
          { required: true, message: '请输入基础价格', trigger: 'blur' }
        ],
        image: [
          { required: true, message: '请上传服务图标', trigger: 'change' }
        ]
      },
      uploadHeaders: {
        'admin-token': localStorage.getItem('adminToken')
      }
    }
  },
  created() {
    this.loadServiceTypes()
  },
  methods: {
    loadServiceTypes() {
      this.loading = true
      this.$http.get('/admin/service-type/list')
        .then(response => {
          const res = response.data
          if (res.success) {
            const list = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
            this.serviceTypeList = list
          } else {
            this.$message.error(res.msg || '加载失败')
          }
        })
        .catch(err => {
          console.error('Service type list error:', err)
        })
        .finally(() => {
          this.loading = false
        })
    },
    showAddDialog() {
      this.isEdit = false
      this.dialogTitle = '添加服务类型'
      this.form = {
        id: null,
        name: '',
        basePrice: 0,
        status: 1,
        image: '',
        description: ''
      }
      this.dialogVisible = true
    },
    editServiceType(row) {
      this.isEdit = true
      this.dialogTitle = '编辑服务类型'
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.submitting = true
          const url = this.isEdit ? '/admin/service-type/update' : '/admin/service-type/add'
          const method = this.isEdit ? 'put' : 'post'

          this.$http[method](url, this.form)
            .then(response => {
              const res = response.data
              if (res.success) {
                this.$message.success(this.isEdit ? '更新成功' : '添加成功')
                this.dialogVisible = false
                this.loadServiceTypes()
              } else {
                this.$message.error(res.msg || '操作失败')
              }
            })
            .catch(err => {
              this.$message.error('操作失败：' + err.message)
            })
            .finally(() => {
              this.submitting = false
            })
        }
      })
    },
    deleteServiceType(id) {
      this.$confirm('确定要删除该服务类型吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$http.delete(`/admin/service-type/delete/${id}`)
          .then(response => {
            const res = response.data
            if (res.success) {
              this.$message.success('删除成功')
              this.loadServiceTypes()
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
      if (res.success) {
        this.form.image = res.data
        this.$message.success('上传成功')
      } else {
        this.$message.error(res.msg || '上传失败')
      }
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
.service-type-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
    }
  }

  .unit {
    margin-left: 10px;
    color: #909399;
  }

  .avatar-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 150px;
    height: 150px;
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
        font-size: 24px;
        margin-bottom: 8px;
      }

      .upload-text {
        font-size: 12px;
      }
    }

    .avatar {
      width: 150px;
      height: 150px;
      display: block;
      object-fit: cover;
    }
  }
}
</style>
