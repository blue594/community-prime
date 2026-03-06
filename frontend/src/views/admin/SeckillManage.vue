<template>
  <div class="seckill-manage-page">
    <div class="page-header">
      <h3>秒杀管理</h3>
      <el-button type="danger" icon="el-icon-plus" @click="handleAdd">
        添加秒杀商品
      </el-button>
    </div>

    <el-card class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="商品秒杀" name="product">
          <el-table :data="productSeckillList" v-loading="loading" border>
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column label="商品" min-width="200">
              <template slot-scope="scope">
                <div style="display: flex; align-items: center; gap: 10px;">
                  <el-image :src="getImageUrl(scope.row.productImage)" style="width: 50px; height: 50px;" fit="cover"></el-image>
                  <span>{{ scope.row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="seckillPrice" label="秒杀价" width="100">
              <template slot-scope="scope">¥{{ scope.row.seckillPrice }}</template>
            </el-table-column>
            <el-table-column prop="originalPrice" label="原价" width="100">
              <template slot-scope="scope">¥{{ scope.row.originalPrice }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80"></el-table-column>
            <el-table-column label="时间" width="200">
              <template slot-scope="scope">
                {{ formatTime(scope.row.beginTime) }} - {{ formatTime(scope.row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template slot-scope="scope">
                <el-tag :type="getStatusType(scope.row)">{{ getStatusText(scope.row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button size="mini" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="优惠券秒杀" name="voucher">
          <el-table :data="voucherSeckillList" v-loading="loading" border>
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="title" label="标题"></el-table-column>
            <el-table-column prop="payValue" label="秒杀价" width="100">
              <template slot-scope="scope">¥{{ scope.row.payValue }}</template>
            </el-table-column>
            <el-table-column prop="actualValue" label="面值" width="100">
              <template slot-scope="scope">¥{{ scope.row.actualValue }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80"></el-table-column>
            <el-table-column label="时间" width="200">
              <template slot-scope="scope">
                {{ formatTime(scope.row.beginTime) }} - {{ formatTime(scope.row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="handleEditVoucher(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加/编辑商品秒杀对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="选择商品">
          <el-select v-model="form.productId" placeholder="请选择商品" style="width: 100%;">
            <el-option v-for="item in productList" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价格">
          <el-input-number v-model="form.seckillPrice" :min="0" :precision="2" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.beginTime" type="datetime" placeholder="选择开始时间" style="width: 100%;"></el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%;"></el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSeckill">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment'

export default {
  name: 'SeckillManage',
  data() {
    return {
      activeTab: 'product',
      loading: false,
      productSeckillList: [],
      voucherSeckillList: [],
      productList: [],
      dialogVisible: false,
      dialogTitle: '添加秒杀商品',
      form: {
        productId: null,
        seckillPrice: 0,
        stock: 0,
        beginTime: null,
        endTime: null
      }
    }
  },
  created() {
    this.loadProductSeckills()
    this.loadVoucherSeckills()
    this.loadProducts()
  },
  methods: {
    loadProductSeckills() {
      this.loading = true
      this.$http.get('/product/seckill/list').then(response => {
        const res = response.data
        if (res.success) {
          const list = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
          this.productSeckillList = list
        }
      }).finally(() => {
        this.loading = false
      })
    },
    loadVoucherSeckills() {
      this.$http.get('/voucher/list/1').then(response => {
        const res = response.data
        if (res.success) {
          const list = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
          this.voucherSeckillList = list.filter(v => v.type === 1)
        }
      })
    },
    loadProducts() {
      this.$http.get('/admin/product/list', { params: { current: 1, size: 100 } }).then(response => {
        const res = response.data
        if (res.success) {
          this.productList = res.data.list || res.data.records || []
        }
      })
    },
    handleAdd() {
      this.dialogTitle = '添加秒杀商品'
      this.form = {
        productId: null,
        seckillPrice: 0,
        stock: 100,
        beginTime: new Date(),
        endTime: new Date(Date.now() + 24 * 60 * 60 * 1000)
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑秒杀商品'
      this.form = { ...row }
      this.dialogVisible = true
    },
    saveSeckill() {
      const url = this.form.id ? '/admin/seckill/update' : '/admin/seckill/add'
      this.$http.post(url, this.form).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadProductSeckills()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除该秒杀活动吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/admin/seckill/delete/${id}`).then(response => {
          const res = response.data
          if (res.success) {
            this.$message.success('删除成功')
            this.loadProductSeckills()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    handleEditVoucher(row) {
      this.$router.push('/admin/voucher/list')
    },
    getStatusType(row) {
      const now = new Date().getTime()
      const begin = new Date(row.beginTime).getTime()
      const end = new Date(row.endTime).getTime()
      if (now < begin) return 'info'
      if (now > end) return 'info'
      return 'danger'
    },
    getStatusText(row) {
      const now = new Date().getTime()
      const begin = new Date(row.beginTime).getTime()
      const end = new Date(row.endTime).getTime()
      if (now < begin) return '未开始'
      if (now > end) return '已结束'
      return '进行中'
    },
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http')) return image
      const baseURL = process.env.VUE_APP_BASE_API || '/api'
      return baseURL + image
    },
    formatTime(time) {
      return time ? moment(time).format('MM-DD HH:mm') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.seckill-manage-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
}
</style>
