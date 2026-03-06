<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound'
}
</script>

<style lang="scss" scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;

  .content {
    text-align: center;

    h1 {
      font-size: 120px;
      color: #409EFF;
      margin-bottom: 20px;
    }

    p {
      font-size: 24px;
      color: #666;
      margin-bottom: 30px;
    }
  }
}
</style>
