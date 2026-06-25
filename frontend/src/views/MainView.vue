<template>
  <div class="dashboard-container">
    <header class="top-header">
      <div class="logo">UCAB-Services</div>
      <div class="header-right">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="Ingresa tu búsqueda aquí..." 
          class="search-bar" 
          @keyup.enter="handleSearch"
        />
        <div class="header-links">
          <span class="user-link" @click="currentView = 'perfil'">Hola, {{ userProfile.firstName }}</span>
          <span class="divider">|</span>
          <a href="#" @click.prevent="currentView = 'solicitudes'">Solicitudes</a>
          <span class="divider">|</span>
          <a href="#" @click.prevent="goToComunidades">Comunidades</a>
        </div>
      </div>
    </header>

    <div class="main-layout">
      
      <aside class="left-sidebar">
        <div class="profile-card">
          <div class="profile-avatar-wrapper">
            <div class="avatar-mock-circle"></div>
          </div>
          <h2 class="profile-name">{{ userProfile.firstName }} {{ userProfile.lastName }}</h2>
          <button class="btn-edit" @click="currentView = 'perfil'">
            <svg class="icon-small" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            Editar información
          </button>
        </div>

        <nav class="sidebar-nav">
          <ul class="nav-list">
            <li class="nav-item" :class="{ active: currentView === 'notificaciones' }" @click="currentView = 'notificaciones'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0"/></svg>
              <span>Notificaciones</span>
            </li>
            <li class="nav-item" :class="{ active: currentView === 'solicitudes' }" @click="currentView = 'solicitudes'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z"/></svg>
              <span>Mis solicitudes</span>
            </li>
          </ul>

          <hr class="nav-divider" />

          <ul class="nav-list">
            <li class="nav-item" :class="{ active: currentView === 'cuentas' }" @click="currentView = 'cuentas'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
              <span>Estado de cuentas</span>
            </li>
            <li class="nav-item" :class="{ active: currentView === 'reservaciones' }" @click="currentView = 'reservaciones'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span>Reservaciones</span>
            </li>
            <li class="nav-item" :class="{ active: currentView === 'feed' }" @click="currentView = 'feed'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
              <span>Feed</span>
            </li>
          </ul>
        </nav>
      </aside>

      <template v-if="currentView === 'feed'">
        <main class="feed-content">
          <article v-for="post in feedPosts" :key="post.id" class="post-card">
            <header class="post-header">
              <div class="post-author-info">
                <div class="author-logo" :class="post.logoBgClass">
                  <span v-if="post.logoText" style="color: white; font-weight: bold; font-size: 20px;">{{ post.logoText }}</span>
                  <img v-if="post.logoImg" :src="post.logoImg" :alt="post.author" />
                </div>
                <div>
                  <h3 class="author-name">{{ post.author }}</h3>
                  <p class="author-subtitle">{{ post.authorType }}</p>
                </div>
              </div>
            </header>
            <div class="post-body">
              <h4 class="post-title">{{ post.title }}</h4>
              <p class="post-description">{{ post.description }}</p>
            </div>
          </article>
        </main>

        <aside class="right-sidebar">
          <div class="right-section-header">
            <h3 class="section-title">Bolsa de trabajo</h3>
          </div>

          <div v-for="job in jobOffers" :key="job.id" class="side-card">
            <div class="side-card-header">
              <h4>{{ job.company }}</h4>
              <span class="tag">{{ job.type }}</span>
            </div>
            <p class="side-card-desc">
              <b>{{ job.title }}</b><br>
              {{ job.description }}
            </p>
            <div class="side-card-actions">
              <button class="btn-action" @click="handleMoreInfo(job.company)">Más información</button>
            </div>
          </div>

          <div class="side-card ad-card">
            <div class="ad-badge">ANUNCIO INSTITUCIONAL</div>
            <h4 class="ad-title">¡No pierdas tu reserva!</h4>
            <p class="ad-desc">
              Recuerda confirmar tu reserva con al menos 24h de antelación. El sistema lo procesará automáticamente.
            </p>
          </div>
        </aside>
      </template>

      <template v-else-if="currentView === 'reservaciones'">
        <main class="dynamic-full-content">
          <Reservation @solicitarPagoProcesado="handleFlujoPagoTransaccion" />
        </main>
      </template>

      <template v-else-if="currentView === 'pago'">
        <main class="dynamic-full-content">
          <Payments :transaction="transaccionActiva" @payment-confirmed="confirmarPagoFinal" />
        </main>
      </template>

      <template v-else-if="currentView === 'solicitudes'">
        <main class="dynamic-full-content">
          <div class="view-card-container">
            <h2>Mis solicitudes (temporal)</h2>
            <p>Listado de solicitudes en desarrollo. Este es un acceso provisional al módulo de pagos.</p>
            <!-- TEMPORAL: botón provisional para navegar a Payments.vue con una transacción de ejemplo -->
            <div style="margin-top: 15px; display:flex; gap:12px;">
              <button class="btn-action" @click="currentView = 'feed'">Volver al Feed</button>
              <button class="btn-guardar-reserva" @click="irAPagoProvisional" style="background-color:#2cb5e8;">Ir a Pago (provisional)</button>
            </div>
          </div>
        </main>
      </template>

      <template v-else>
        <main class="dynamic-full-content">
          <div class="view-card-container">
            <h2>Sección: {{ currentView.toUpperCase() }}</h2>
            <p>Contenido en desarrollo conectado al controlador central.</p>
            <button class="btn-action" @click="currentView = 'feed'">Volver al Feed</button>
          </div>
        </main>
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Reservation from './Reservation.vue';
import Payments from './Payments.vue';

const currentView = ref('feed');
const searchQuery = ref('');
const transaccionActiva = ref(null);

const userProfile = ref({ firstName: '', lastName: '' });
const feedPosts = ref([]);
const jobOffers = ref([]); // Agregado el arreglo para la barra derecha

const mockController = {
  fetchUserData() {
    return { firstName: 'Maria', lastName: 'González' };
  },
  fetchFeedPosts() {
    return [
      { id: 1, author: 'Empresas Polar', authorType: 'Aliado externo', logoBgClass: 'blue-bg', title: 'Oferta Laboral: Analista de Estrategia Digital', description: 'Buscamos jóvenes talentos recién egresados de Ingeniería, Administración o Comunicación Social de la UCAB...' }
    ];
  },
  fetchJobOffers() {
    return [
      { id: 1, company: 'Banco Banesco', type: 'Aliado externo', title: 'Buscando analista de datos junior', description: 'Buscamos un recién egresado en ingeniería informática, promedio mínimo 15, horario flexible...' }
    ];
  }
};

onMounted(() => {
  userProfile.value = mockController.fetchUserData();
  feedPosts.value = mockController.fetchFeedPosts();
  jobOffers.value = mockController.fetchJobOffers(); // Cargamos las ofertas al iniciar
});

const handleFlujoPagoTransaccion = (datosRecibidos) => {
  transaccionActiva.value = datosRecibidos; 
  currentView.value = 'pago'; 
};

// TEMPORAL: handler provisional que crea una transacción de ejemplo y abre el flujo de pago
const irAPagoProvisional = () => {
  transaccionActiva.value = {
    descripcion: 'Pago provisional desde Solicitudes',
    monto: 15.00
  }
  currentView.value = 'pago'
}

const confirmarPagoFinal = () => {
  alert('¡Pago procesado y registrado con éxito en el sistema!');
  currentView.value = 'feed';
  transaccionActiva.value = null;
};

const handleSearch = () => console.log('Buscando:', searchQuery.value);
const goToComunidades = () => console.log('Módulo comunidades...');
const handleMoreInfo = (company) => console.log('Más info de:', company);
</script>

<style scoped src="./Dashboard.css"></style>
<style scoped>
.avatar-mock-circle {
  width: 90px;
  height: 90px;
  background-color: #e5e7eb;
  border-radius: 50%;
  background-image: url('https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200');
  background-size: cover;
  border: 3px solid #f5e1a4;
}
.dynamic-full-content {
  flex: 1;
  padding: 30px;
  background-color: #f7f9f6;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}
</style>