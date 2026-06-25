import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import MainView from '../views/MainView.vue' // TEMPORAL: ruta auxiliar para revisar MainView

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
      component: MainView // TEMPORAL: ruta auxiliar para revisar MainView. Eliminar cuando ya no sea necesaria.
    },
    // Aquí agregarás más pantallas en el futuro. Ejemplo:
    // {
    //   path: '/dashboard',
    //   name: 'dashboard',
    //   component: () => import('../views/DashboardView.vue')
    // }
  ]
})

export default router