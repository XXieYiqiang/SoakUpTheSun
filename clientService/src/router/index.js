import Vue from 'vue';
import VueRouter from 'vue-router';

Vue.use(VueRouter);

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'), // 确保路径正确
    meta: { title: '身份验证', public: true } // 标记为公开，不需登录即可访问
  },
  {
    path: '/chatRoom',
    name: 'chatRoom',
    component: () => import('@/views/ChatRoom.vue')
  },
  {
    path: '/index',
    name: 'index',
    component: () => import('../views/UserHome.vue')
  },
  {
    path: '/setting',
    name: 'setting',
    component: () => import('../views/SettingView.vue')
  },
  {
    path: '/join',
    name: 'JoinRoom',
    component: () => import('../views/JoinRoom.vue')
  }
];

const router = new VueRouter({
  mode: 'history',
  base: '/',
  routes
});

// 全局捕获 NavigationDuplicated 错误
const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch((err) => err)
};

export default router;