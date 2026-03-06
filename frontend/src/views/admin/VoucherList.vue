<template>
  <div class="voucher-list-page">
    <div class="page-header">
      <h3>优惠券列表</h3>
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
        添加优惠券
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="voucherList" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="title" label="标题"></el-table-column>
        <el-table-column prop="subTitle" label="副标题"></el-table-column>
        <el-table-column prop="payValue" label="售价" width="100">
          <template slot-scope="scope">¥{{ scope.row.payValue }}</template>
        </el-table-column>
        <el-table-column prop="actualValue" label="面值" width="100">
          <template slot-scope="scope">¥{{ scope.row.actualValue }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === 1 ? 'danger' : 'success'">
              {{ scope.row.type === 1 ? '秒杀券' : '普通券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80"></el-table-column>
        <el-table-column label="有效期" width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.beginTime) }} - {{ formatTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
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

    <!-- 添加/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入优惠券标题"></el-input>
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subTitle" placeholder="请输入副标题"></el-input>
        </el-form-item>
        <el-form-item label="使用规则">
          <el-input v-model="form.rules" type="textarea" rows="2" placeholder="请输入使用规则"></el-input>
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="form.payValue" :min="0" :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="面值">
          <el-input-number v-model="form.actualValue" :min="0" :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :label="0">普通券</el-radio>
            <el-radio :label="1">秒杀券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.beginTime" type="datetime" placeholder="选择开始时间"></el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间"></el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVoucher">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment'

export default {
  name: 'VoucherList',
  data() {
    return {
      loading: false,
      voucherList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      dialogVisible: false,
      dialogTitle: '添加优惠券',
      form: {
        title: '',
        subTitle: '',
        rules: '',
        payValue: 0,
        actualValue: 0,
        type: 0,
        stock: 0,
        beginTime: null,
        endTime: null
      }
    }
  },
  created() {
    this.loadVouchers()
  },
  methods: {
    loadVouchers() {
      this.loading = true
      // 使用管理员接口查询所有优惠券
      this.$http.get('/admin/voucher/list').then(response => {
        const res = response.data
        if (res.success) {
          const list = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
          this.voucherList = list
          this.total = list.length
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadVouchers()
    },
    handleAdd() {
      this.dialogTitle = '添加优惠券'
      this.form = {
        shopId: 1,  // 默认商家ID为1，确保新增后能正常显示
        title: '',
        subTitle: '',
        rules: '',
        payValue: 0,
        actualValue: 0,
        type: 0,
        stock: 100,
        beginTime: new Date(),
        endTime: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑优惠券'
      this.form = { ...row }
      this.dialogVisible = true
    },
    saveVoucher() {
      const isUpdate = !!this.form.id
      const url = isUpdate ? `/admin/voucher/update` : '/admin/voucher/add'
      const method = isUpdate ? 'put' : 'post'
      this.$http[method](url, this.form).then(response => {
        const res = response.data
        if (res.success) {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadVouchers()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除该优惠券吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/admin/voucher/delete/${id}`).then(response => {
          const res = response.data
          if (res.success) {
            this.$message.success('删除成功')
            this.loadVouchers()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    formatTime(time) {
      return time ? moment(time).format('MM-DD HH:mm') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.voucher-list-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
  .pagination {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
