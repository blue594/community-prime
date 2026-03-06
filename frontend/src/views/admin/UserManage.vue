<template>
  <div class="user-manage-page">
    <div class="page-header">
      <h3>用户管理</h3>
    </div>

    <el-card class="table-card">
      <el-table :data="userList" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="120"></el-table-column>
        <el-table-column prop="nickName" label="昵称"></el-table-column>
        <el-table-column label="头像" width="80">
          <template slot-scope="scope">
            <el-avatar :size="40" :src="getImageUrl(scope.row.icon)" v-if="scope.row.icon"></el-avatar>
            <el-avatar :size="40" icon="el-icon-user" v-else></el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
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
  </div>
</template>

<script>
import moment from 'moment'

export default {
  name: 'UserManage',
  data() {
    return {
      loading: false,
      userList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    loadUsers() {
      this.loading = true
      this.$http.get('/admin/user/list', {
        params: { current: this.currentPage, size: this.pageSize }
      }).then(response => {
        console.log('User list response:', response.data)
        const res = response.data
        if (res.success) {
          // 兼容两种数据结构：直接数组或PageResult对象
          if (Array.isArray(res.data)) {
            this.userList = res.data
            this.total = res.data.length
          } else {
            this.userList = res.data.list || res.data.records || []
            this.total = res.data.total || 0
          }
        } else {
          this.$message.error(res.msg || '加载失败')
        }
      }).catch(err => {
        console.error('User list error:', err)
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadUsers()
    },
    handleDelete(id) {
      this.$confirm('确定要删除该用户吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/admin/user/delete/${id}`).then(response => {
          const res = response.data
          if (res.success) {
            this.$message.success('删除成功')
            this.loadUsers()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {
        // 用户点击取消，不做任何操作
      })
    },
    formatTime(time) {
      return time ? moment(time).format('YYYY-MM-DD HH:mm:ss') : '-'
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
.user-manage-page {
  .page-header {
    margin-bottom: 20px;
    h3 { margin: 0; }
  }
  .pagination {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
