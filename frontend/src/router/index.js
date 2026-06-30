import { createRouter, createWebHistory } from 'vue-router'
import { getAuthUser } from '../services/authService'
import HomeView from '../views/HomeView.vue'
import MainView from '../views/MainView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView // Cuando la ruta sea "/", carga la pantalla de inicio
    },
    {
      path: '/main',
      name: 'main',
      component: MainView
    },
    // Aquí agregarás más pantallas en el futuro. Ejemplo:
    // {
    //   path: '/dashboard',
    //   name: 'dashboard',
    //   component: () => import('../views/DashboardView.vue')
    // }
  ]
})

router.beforeEach((to, from, next) => {
  const authUser = getAuthUser()
  if (to.name !== 'home' && !authUser) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router