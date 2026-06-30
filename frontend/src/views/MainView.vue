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
          <span class="divider">|</span>
          <a href="#" @click.prevent="logout">Salir</a>
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
          <button class="btn-logout" @click="logout">Cerrar sesión</button>
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
            <li class="nav-item" :class="{ active: currentView === 'db' }" @click="currentView = 'db'">
              <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
              <span>Base de datos</span>
            </li>
          </ul>
        </nav>
      </aside>

      <template v-if="currentView === 'perfil'">
        <main class="dynamic-full-content">
          <div class="view-card-container profile-main-view">
            <h2>Perfil Principal</h2>
            <p class="profile-intro">Bienvenida, {{ userProfile.firstName }}. Aquí está tu panel principal.</p>
            <div class="profile-summary">
              <div class="profile-summary-row"><strong>Nombre:</strong> {{ userProfile.firstName }} {{ userProfile.lastName }}</div>
              <div class="profile-summary-row"><strong>Rol:</strong> Estudiante UCAB</div>
              <div class="profile-summary-row"><strong>Correo:</strong> {{ userProfile.email }}</div>
              <div class="profile-summary-row"><strong>Categoría:</strong> {{ userProfile.category }}</div>
            </div>
            <div class="profile-actions">
              <button class="btn-action" @click="currentView = 'feed'">Ver novedades</button>
              <button class="btn-action" @click="currentView = 'solicitudes'">Ver solicitudes</button>
            </div>
          </div>
        </main>
      </template>
      <template v-else-if="currentView === 'feed'">
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
            <div style="margin-top: 15px; display:flex; gap:12px;">
              <button class="btn-action" @click="currentView = 'feed'">Volver al Feed</button>
              <button class="btn-guardar-reserva" @click="irAPagoProvisional" style="background-color:#2cb5e8;">Ir a Pago (provisional)</button>
            </div>
          </div>
        </main>
      </template>

      <template v-else-if="currentView === 'db'">
        <main class="dynamic-full-content">
          <div class="view-card-container db-browser">
            <h2>Explorador de Base de Datos</h2>
            <div class="db-layout">
              <div class="tables-list">
                <h3>Tablas</h3>
                <ul>
                  <li v-for="t in tables" :key="t.name">
                    <a href="#" @click.prevent="selectTable(t.name)">{{ t.name }}</a>
                  </li>
                </ul>
              </div>

              <div class="table-details" v-if="selectedTable">
                <h3>Tabla: {{ selectedTable }}</h3>
                <div class="columns">
                  <h4>Columnas</h4>
                  <ul>
                    <li v-for="c in columns" :key="c.name">{{ c.name }} ({{ c.typeName }})</li>
                  </ul>
                </div>

                <div class="rows">
                  <h4>Filas (máx {{ rowLimit }})</h4>
                  <table>
                    <thead>
                      <tr>
                        <th v-for="col in columns" :key="col.name">{{ col.name }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(r, idx) in rows" :key="idx">
                        <td v-for="col in columns" :key="col.name">{{ r[col.name] }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
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
import { useRouter } from 'vue-router';
import { getAuthUser, clearAuthUser } from '../services/authService';
import Reservation from './Reservation.vue';
import Payments from './Payments.vue';

const currentView = ref('feed');
const searchQuery = ref('');
const transaccionActiva = ref(null);
const router = useRouter();

const userProfile = ref({ firstName: '', lastName: '', email: '', category: '' });
const feedPosts = ref([]);
const jobOffers = ref([]);
const tables = ref([]);
const selectedTable = ref(null);
const columns = ref([]);
const rows = ref([]);
const rowLimit = ref(100);

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

async function loadTables() {
  try {
    const res = await fetch('/api/tables');
    tables.value = await res.json();
  } catch (e) {
    console.error('Error cargando tablas', e);
    tables.value = [];
  }
}

async function selectTable(name) {
  selectedTable.value = name;
  columns.value = [];
  rows.value = [];
  try {
    const cRes = await fetch(`/api/tables/${name}/columns`);
    columns.value = await cRes.json();
  } catch (e) {
    console.error('Error cargando columnas', e);
    columns.value = [];
  }
  try {
    const rRes = await fetch(`/api/tables/${name}/rows?limit=${rowLimit.value}`);
    rows.value = await rRes.json();
  } catch (e) {
    console.error('Error cargando filas', e);
    rows.value = [];
  }
}

onMounted(() => {
  const authUser = getAuthUser();
  if (!authUser) {
    router.replace({ name: 'home' });
    return;
  }

  userProfile.value = {
    firstName: authUser.firstName || '',
    lastName: authUser.lastName || '',
    email: authUser.email || '',
    category: authUser.category || 'Miembro UCAB'
  };
  feedPosts.value = mockController.fetchFeedPosts();
  jobOffers.value = mockController.fetchJobOffers();
  loadTables();
});

const logout = () => {
  clearAuthUser();
  router.push({ name: 'home' });
};

const handleFlujoPagoTransaccion = (datosRecibidos) => {
  transaccionActiva.value = datosRecibidos;
  currentView.value = 'pago';
};

const irAPagoProvisional = () => {
  transaccionActiva.value = {
    descripcion: 'Pago provisional desde Solicitudes',
    monto: 15.00
  };
  currentView.value = 'pago';
};

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
.btn-logout {
  margin-top: 12px;
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 14px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1;
  width: auto;
  min-width: 110px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  transition: background-color 0.25s ease;
}
.btn-logout:hover {
  background-color: #dc2626;
}
.dynamic-full-content {
  flex: 1;
  padding: 30px;
  background-color: #f7f9f6;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}
.db-browser {
  width: 100%;
}
.db-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
}
.tables-list,
.table-details {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
}
.tables-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.tables-list li {
  margin-bottom: 10px;
}
.tables-list a {
  color: #1d4ed8;
  text-decoration: none;
}
.tables-list a:hover {
  text-decoration: underline;
}
.columns ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.columns li {
  margin-bottom: 6px;
}
.rows table {
  width: 100%;
  border-collapse: collapse;
}
.rows th,
.rows td {
  border: 1px solid #e5e7eb;
  padding: 10px;
}
.rows th {
  background: #f3f4f6;
}
</style>
