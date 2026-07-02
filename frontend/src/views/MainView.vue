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
            <img
              v-if="userProfile.avatar"
              :src="userProfile.avatar"
              alt="Avatar del usuario"
              class="profile-avatar"
            />
            <div v-else class="avatar-placeholder">{{ avatarInitials() }}</div>
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
          <div class="profile-main-view">
            <h2>Perfil Principal</h2>
            <p class="profile-intro">Bienvenida, {{ userProfile.firstName }}. Aquí está tu información registrada en la base de datos.</p>

            <section class="profile-card-panel profile-header-card">
              <div class="profile-header-main">
                <div>
                  <h3>{{ displayValue(userProfile.firstName) }} {{ displayValue(userProfile.lastName) }}</h3>
                  <p>{{ displayValue(userProfile.email) }}</p>
                </div>
                <div class="profile-header-tools">
                  <input
                    ref="avatarInputRef"
                    type="file"
                    accept="image/*"
                    class="avatar-file-input"
                    @change="handleAvatarChange"
                  />
                  <button class="btn-mini-action btn-avatar-change" @click="triggerAvatarPicker">
                    Cambiar foto
                  </button>
                  <div class="profile-chip">{{ displayValue(userProfile.ci) }}</div>
                </div>
              </div>

              <div class="profile-header-grid">
                <div><strong>Primer nombre</strong><span>{{ displayValue(userProfile.firstName) }}</span></div>
                <div><strong>Primer apellido</strong><span>{{ displayValue(userProfile.lastName) }}</span></div>
                <div><strong>Categoría</strong><span>{{ displayValue(userProfile.category) }}</span></div>
                <div><strong>Estado de cuenta</strong><span>{{ displayValue(userProfile.accountStatus) }}</span></div>
              </div>
            </section>

            <section class="profile-card-panel profile-card-panel--two-columns">
              <div class="profile-info-block">
                <div class="profile-summary-row"><strong>CI</strong><span>{{ displayValue(userProfile.ci) }}</span></div>
                <div class="profile-summary-row profile-editable-row">
                  <strong>Segundo nombre</strong>
                  <span>{{ displayValue(userProfile.secondName) }}</span>
                  <button class="btn-mini-action" :disabled="!canEditSecondField('secondName')" @click="handleEditField('secondName')">Modificar</button>
                </div>
                <div class="profile-summary-row profile-editable-row">
                  <strong>Segundo apellido</strong>
                  <span>{{ displayValue(userProfile.secondLastName) }}</span>
                  <button class="btn-mini-action" :disabled="!canEditSecondField('secondLastName')" @click="handleEditField('secondLastName')">Modificar</button>
                </div>
                <div class="profile-summary-row"><strong>Sexo</strong><span>{{ displayValue(userProfile.sex) }}</span></div>
                <div class="profile-summary-row"><strong>Fecha de nacimiento</strong><span>{{ displayValue(userProfile.birthDate) }}</span></div>
              </div>

              <div class="profile-info-block profile-info-block--highlight">
                <div class="profile-summary-row profile-editable-row">
                  <strong>Dirección</strong>
                  <span>{{ displayValue(userProfile.address) }}</span>
                  <button class="btn-mini-action" @click="handleEditField('address')">Modificar</button>
                </div>
                <div class="profile-summary-row profile-editable-row">
                  <strong>Teléfono</strong>
                  <span>{{ displayValue(userProfile.phone) }}</span>
                  <button class="btn-mini-action" @click="handleEditField('phone')">Modificar</button>
                </div>
                <div class="profile-summary-row"><strong>Última conexión</strong><span>{{ displayValue(userProfile.lastConnection) }}</span></div>
                <div class="profile-summary-row"><strong>Fecha cambio contraseña</strong><span>{{ displayValue(userProfile.passwordChangeDate) }}</span></div>
                <div class="profile-summary-row profile-summary-password-row">
                  <strong>Contraseña</strong>
                  <button class="btn-action btn-password-change" @click="handleChangePassword">Cambiar contraseña</button>
                </div>
              </div>
            </section>
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
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAuthUser, clearAuthUser, closeLatestSession, closeLatestSessionOnUnload } from '../services/authService';
import Reservation from './Reservation.vue';
import Payments from './Payments.vue';

const currentView = ref('feed');
const searchQuery = ref('');
const transaccionActiva = ref(null);
const avatarInputRef = ref(null);
const router = useRouter();

const userProfile = ref({
  ci: '',
  firstName: '',
  secondName: '',
  lastName: '',
  secondLastName: '',
  sex: '',
  email: '',
  address: '',
  birthDate: '',
  phone: '',
  category: '',
  accountStatus: '',
  lastConnection: ''
  ,passwordChangeDate: '',
  avatar: ''
});
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

async function loadUserProfile(authUser) {
  const profileQuery = authUser.email
    ? `email=${encodeURIComponent(authUser.email)}`
    : authUser.ci
      ? `ci=${encodeURIComponent(authUser.ci)}`
      : '';

  if (!profileQuery) {
    return;
  }

  try {
    const response = await fetch(`/api/auth/profile?${profileQuery}`);
    if (!response.ok) {
      return;
    }

    const profile = await response.json();
    userProfile.value = {
      ci: profile.ci || authUser.ci || '',
      firstName: profile.firstName || authUser.firstName || '',
      secondName: profile.secondName || '',
      lastName: profile.lastName || authUser.lastName || '',
      secondLastName: profile.secondLastName || '',
      sex: profile.sex || '',
      email: profile.email || authUser.email || '',
      address: profile.address || '',
      birthDate: profile.birthDate || '',
      phone: profile.phone || '',
      category: profile.category || authUser.category || 'Miembro UCAB',
      accountStatus: profile.accountStatus || '',
      lastConnection: profile.lastConnection || '',
      passwordChangeDate: profile.passwordChangeDate || ''
      ,avatar: profile.avatar || ''
    };
  } catch (error) {
    console.error('Error cargando perfil del usuario:', error);
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

  window.addEventListener('beforeunload', handleBeforeUnload);

  userProfile.value = {
    ci: authUser.ci || '',
    firstName: authUser.firstName || '',
    secondName: authUser.secondName || '',
    lastName: authUser.lastName || '',
    secondLastName: authUser.secondLastName || '',
    sex: authUser.sex || '',
    email: authUser.email || '',
    address: authUser.address || '',
    birthDate: authUser.birthDate || '',
    phone: authUser.phone || '',
    category: authUser.category || 'Miembro UCAB',
    accountStatus: authUser.accountStatus || '',
    lastConnection: authUser.lastConnection || '',
    passwordChangeDate: authUser.passwordChangeDate || ''
    ,avatar: authUser.avatar || ''
  };
  loadUserProfile(authUser);
  feedPosts.value = mockController.fetchFeedPosts();
  jobOffers.value = mockController.fetchJobOffers();
  loadTables();
});

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});

const logout = async () => {
  await closeLatestSession(userProfile.value.ci);
  window.removeEventListener('beforeunload', handleBeforeUnload);
  clearAuthUser();
  router.push({ name: 'home' });
};

const handleBeforeUnload = () => {
  closeLatestSessionOnUnload(userProfile.value.ci);
};

const handleFlujoPagoTransaccion = (datosRecibidos) => {
  transaccionActiva.value = datosRecibidos;
  currentView.value = 'pago';
};

const handleChangePassword = () => {
  alert('La opción para cambiar la contraseña se habilitará en el siguiente paso.');
};

const triggerAvatarPicker = () => {
  avatarInputRef.value?.click();
};

const avatarInitials = () => {
  const firstInitial = (userProfile.value.firstName || '').trim().charAt(0);
  const lastInitial = (userProfile.value.lastName || '').trim().charAt(0);
  const initials = `${firstInitial}${lastInitial}`.trim();
  return initials || 'U';
};

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }

  if (!file.type.startsWith('image/')) {
    alert('Debes seleccionar un archivo de imagen.');
    event.target.value = '';
    return;
  }

  try {
    const avatarDataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(new Error('No se pudo leer la imagen.'));
      reader.readAsDataURL(file);
    });

    const response = await fetch('/api/auth/profile/avatar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: userProfile.value.ci,
        avatar: avatarDataUrl
      })
    });

    const result = await response.json();
    if (!response.ok) {
      alert(result.error || 'No se pudo actualizar el avatar.');
      event.target.value = '';
      return;
    }

    userProfile.value.avatar = result.avatar || avatarDataUrl;
    alert(result.message || 'Avatar actualizado correctamente.');
  } catch (error) {
    console.error('Error actualizando avatar:', error);
    alert('No se pudo conectar con el servidor.');
  } finally {
    event.target.value = '';
  }
};

const displayValue = (value) => {
  return value === null || value === undefined || String(value).trim() === '' ? 'No registrado' : value;
};

const canEditSecondField = (field) => {
  return !userProfile.value[field] || String(userProfile.value[field]).trim() === '';
};

const fieldLabels = {
  secondName: 'Segundo nombre',
  secondLastName: 'Segundo apellido',
  address: 'Dirección',
  phone: 'Teléfono'
};

const fieldPlaceholders = {
  secondName: 'Ingresa el segundo nombre',
  secondLastName: 'Ingresa el segundo apellido',
  address: 'Ingresa la dirección actualizada',
  phone: 'Ingresa el teléfono actualizado'
};

const handleEditField = async (field) => {
  if ((field === 'secondName' || field === 'secondLastName') && !canEditSecondField(field)) {
    alert('Este campo ya tiene valor y no se puede modificar.');
    return;
  }

  const currentValue = userProfile.value[field] || '';
  const nextValue = window.prompt(fieldLabels[field] || 'Modificar campo', currentValue || '');

  if (nextValue === null) {
    return;
  }

  const trimmedValue = nextValue.trim();
  if (!trimmedValue) {
    alert('El valor no puede quedar vacío.');
    return;
  }

  try {
    const response = await fetch('/api/auth/profile/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: userProfile.value.ci,
        field,
        value: trimmedValue
      })
    });

    const result = await response.json();
    if (!response.ok) {
      alert(result.error || 'No se pudo actualizar el perfil.');
      return;
    }

    userProfile.value[field] = trimmedValue;
    alert(result.message || 'Perfil actualizado correctamente.');
  } catch (error) {
    console.error('Error actualizando perfil:', error);
    alert('No se pudo conectar con el servidor.');
  }
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
.avatar-file-input {
  display: none;
}

.btn-avatar-change {
  margin-bottom: 10px;
  background-color: #ffffff;
  color: #0f172a;
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.avatar-placeholder {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #1d4ed8;
  font-weight: 700;
  font-size: 28px;
  border: 3px solid #f5e1a4;
}

.profile-avatar-wrapper {
  cursor: default;
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

.profile-main-view {
  width: 100%;
  max-width: 1080px;
}

.profile-main-view h2 {
  color: #173a2e;
  font-size: 28px;
  margin-bottom: 8px;
}

.profile-intro {
  color: #4f5b53;
  margin-bottom: 18px;
  font-size: 15px;
}

.profile-card-panel {
  background: linear-gradient(180deg, #dcedc8 0%, #d3eab8 100%);
  border-radius: 24px;
  padding: 24px 28px;
  box-shadow: 0 10px 20px rgba(32, 72, 43, 0.08);
  border: 1px solid rgba(25, 80, 33, 0.08);
  margin-bottom: 16px;
}

.profile-header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(58, 96, 63, 0.25);
}

.profile-header-main h3 {
  font-size: 24px;
  color: #173a2e;
  margin-bottom: 6px;
}

.profile-header-main p {
  color: #4f5b53;
}

.profile-chip {
  background: #f1f5f2;
  color: #173a2e;
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 700;
  min-width: 120px;
  text-align: center;
  box-shadow: inset 0 0 0 1px rgba(23, 58, 46, 0.08);
}

.profile-header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-header-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 20px;
}

.profile-header-grid div,
.profile-info-block .profile-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
}

.profile-header-grid strong,
.profile-info-block strong,
.profile-summary-password-row strong {
  color: #173a2e;
  font-weight: 800;
}

.profile-header-grid span,
.profile-info-block span {
  color: #334155;
}

.profile-card-panel--two-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.profile-info-block {
  background: rgba(255, 255, 255, 0.28);
  border-radius: 20px;
  padding: 18px 20px;
  border: 1px solid rgba(23, 58, 46, 0.08);
}

.profile-info-block--highlight {
  background: rgba(248, 255, 239, 0.42);
}

.profile-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.profile-editable-row {
  gap: 14px;
}

.profile-summary-password-row {
  justify-content: space-between;
}

.btn-action,
.btn-mini-action,
.btn-edit,
.btn-password-change,
.btn-guardar-reserva {
  background: linear-gradient(180deg, #39aaf2 0%, #2f9de6 100%);
  color: white;
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(41, 146, 213, 0.22);
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}

.btn-action:hover,
.btn-mini-action:hover,
.btn-edit:hover,
.btn-password-change:hover,
.btn-guardar-reserva:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(41, 146, 213, 0.26);
}

.btn-action:disabled,
.btn-mini-action:disabled,
.btn-edit:disabled,
.btn-password-change:disabled,
.btn-guardar-reserva:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-mini-action {
  padding: 8px 14px;
  font-size: 12px;
  white-space: nowrap;
}

.btn-edit {
  width: 100%;
  justify-content: center;
  margin-top: 6px;
}

.btn-password-change {
  min-width: 180px;
}

.profile-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.profile-actions .btn-action {
  min-width: 160px;
}

@media (max-width: 1024px) {
  .profile-header-grid,
  .profile-card-panel--two-columns {
    grid-template-columns: 1fr;
  }

  .profile-header-main {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .profile-actions {
    flex-direction: column;
  }

  .profile-actions .btn-action {
    width: 100%;
  }
}
</style>
