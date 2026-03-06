import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('../views/Product.vue'),
        meta: { title: '超市购物' }
      },
      {
        path: 'product/detail/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('../views/Seckill.vue'),
        meta: { title: '限时秒杀' }
      },
      {
        path: 'voucher',
        name: 'VoucherShop',
        component: () => import('../views/VoucherShop.vue'),
        meta: { title: '优惠券' }
      },
      {
        path: 'service',
        name: 'Service',
        component: () => import('../views/Service.vue'),
        meta: { title: '家政维修' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('../views/Order.vue'),
        meta: { title: '我的订单', requireAuth: true }
      },
      {
        path: 'booking',
        name: 'Booking',
        component: () => import('../views/Booking.vue'),
        meta: { title: '我的预约', requireAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心', requireAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  // 管理员路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/AdminLogin.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('../views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'product/list',
        name: 'ProductList',
        component: () => import('../views/admin/ProductList.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'product/add',
        name: 'ProductAdd',
        component: () => import('../views/admin/ProductAdd.vue'),
        meta: { title: '添加商品' }
      },
      {
        path: 'service/type',
        name: 'ServiceType',
        component: () => import('../views/admin/ServiceType.vue'),
        meta: { title: '服务类型' }
      },
      {
        path: 'service/booking',
        name: 'BookingManage',
        component: () => import('../views/admin/BookingManage.vue'),
        meta: { title: '预约管理' }
      },
      {
        path: 'voucher/list',
        name: 'VoucherList',
        component: () => import('../views/admin/VoucherList.vue'),
        meta: { title: '优惠券列表' }
      },
      {
        path: 'voucher/seckill',
        name: 'SeckillManage',
        component: () => import('../views/admin/SeckillManage.vue'),
        meta: { title: '秒杀管理' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'order',
        name: 'OrderManage',
        component: () => import('../views/admin/OrderManage.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('../views/admin/AdminProfile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes,
  scrollBehavior() {
    return { x: 0, y: 0 }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 邻里优享` : '邻里优享'
  
  // 检查是否需要登录
  if (to.meta.requireAuth && !store.getters.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else {
    next()
  }
})

export default router
