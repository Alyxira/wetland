import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// MainHome 作为首屏，可以保持同步引入
import MainHome from './MainHome.vue'

// 其他页面改为动态导入 (懒加载)
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    meta: { title: '首页' },
    component: MainHome
  },
  {
    path: '/overview',
    name: 'Overview',
    meta: { title: '湿地全景' },
    component: () => import('./views/Overview.vue')
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    meta: { title: '湿地详情' },
    component: () => import('./views/Detail.vue')
  },
  {
    path: '/date-detail/:id',
    name: 'DateDetail',
    meta: { title: '水体反演数据' },
    component: () => import('./views/DateDetail.vue')
  },
  {
    path: '/consult',
    component: () => import('./views/Consult.vue'),
    children: [
      {
        path: '',
        name: 'Consult',
        meta: { page: 'index', title: '导览咨询' },
        component: () => import('./components/Consult/ScenicIndexView.vue')
      },
      {
        path: 'travel-map',
        name: 'ConsultTravelMap',
        meta: { page: 'travel-map', title: '景区地图' },
        component: () => import('./components/Consult/TravelMapHostView.vue')
      },
      {
        path: ':scenicId',
        name: 'ConsultScenicHome',
        meta: { page: 'home', title: '景区导览' },
        component: () => import('./components/Consult/HomeView.vue')
      },
      {
        path: ':scenicId/explore',
        name: 'ConsultScenicExplore',
        meta: { page: 'explore', title: '景区探索' },
        component: () => import('./components/Consult/ExploreView.vue')
      },
      {
        path: ':scenicId/cloud-tour',
        name: 'ConsultScenicCloud',
        meta: { page: 'cloud', title: '沉浸云游', enableAssistant: false },
        component: () => import('./components/Consult/CloudTourView.vue')
      }
    ]
  },
  {
    path: '/screen',
    name: 'Screen',
    meta: { title: '数智泽屏' },
    component: () => import('./views/Screen.vue')
  },
  {
    path: '/community',
    name: 'Community',
    meta: { title: '游踪漫话' },
    component: () => import('./views/Community.vue')
  },
  {
    path: '/science',
    name: 'Science',
    meta: { title: '生态图鉴' },
    component: () => import('./views/Science.vue')
  },
  {
    path: '/flora/:id',
    name: 'FloraDetail',
    meta: { title: '动植物详情' },
    component: () => import('./views/FloraDetail.vue')
  },
  {
    path: '/ai',
    name: 'AIChat',
    meta: { title: '泽畔智语' },
    component: () => import('./views/AIChat.vue')
  },
  {
    path: '/search',
    name: 'Search',
    meta: { title: '详细搜索' },
    component: () => import('./views/Search.vue')
  },
  {
    path: '/auth',
    name: 'Auth',
    meta: { title: '登录认证' },
    component: () => import('./views/Auth.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    meta: { title: '个人中心' },
    component: () => import('./views/Profile.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

export default router
