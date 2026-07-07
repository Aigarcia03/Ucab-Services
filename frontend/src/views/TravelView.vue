<template>
  <div class="travel-wrapper">
    <h2 class="page-title">Viajes por sedes</h2>

    <div class="filters-row">
      <div class="filter-group">
        <label>Sede de origen</label>
        <select v-model="sedeFiltro" @change="fetchViajes">
          <option value="">Todas las sedes</option>
          <option v-for="s in sedes" :key="s.ubicacion" :value="s.ubicacion">{{ s.ubicacion }}</option>
        </select>
      </div>
    </div>

    <div v-if="viajes.length === 0" class="empty-state">
      <p>No hay viajes registrados{{ sedeFiltro ? ' para esta sede' : '' }}.</p>
    </div>

    <div class="viajes-list">
      <div v-for="(viaje, index) in viajes" :key="index" class="viaje-card">
        <div class="viaje-header">
          <div class="ruta-info">
            <span class="sede-origen">{{ viaje.sedeorigen }}</span>
            <span class="flecha">&rarr;</span>
            <span class="sede-destino">{{ viaje.destino }}</span>
          </div>
          <span class="vehiculo-tag" :class="viaje.tipovehiculo">
            {{ viaje.tipovehiculo === 'autobús' ? 'Autobús' : 'Carro' }}
          </span>
        </div>

        <div class="viaje-body">
          <div class="detail-row">
            <span class="label">Placa:</span>
            <span class="value">{{ viaje.placa }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Capacidad:</span>
            <span class="value">{{ viaje.capacidad }} personas</span>
          </div>
          <div class="detail-row">
            <span class="label">Salida:</span>
            <span class="value">{{ formatFecha(viaje.fechahorainicio) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Llegada estimada:</span>
            <span class="value">{{ viaje.fechahorafin ? formatFecha(viaje.fechahorafin) : '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Disponible:</span>
            <span class="value" :class="viaje.disponibilidad ? 'disponible' : 'no-disponible'">
              {{ viaje.disponibilidad ? 'Sí' : 'No' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const viajes = ref([]);
const sedes = ref([]);
const sedeFiltro = ref('');

const fetchSedes = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/viajes/sedes');
    if (res.ok) sedes.value = await res.json();
  } catch (e) {
    console.error('Error cargando sedes', e);
  }
};

const fetchViajes = async () => {
  try {
    const url = sedeFiltro.value
      ? `http://localhost:8080/api/viajes?sede=${encodeURIComponent(sedeFiltro.value)}`
      : 'http://localhost:8080/api/viajes';
    const res = await fetch(url);
    if (res.ok) viajes.value = await res.json();
  } catch (e) {
    console.error('Error cargando viajes', e);
  }
};

const formatFecha = (fecha) => {
  if (!fecha) return '—';
  const d = new Date(fecha);
  return d.toLocaleString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
};

onMounted(() => {
  fetchSedes();
  fetchViajes();
});
</script>

<style scoped>
.travel-wrapper {
  width: 100%;
  max-width: 900px;
}

.page-title {
  color: #173a2e;
  font-size: 28px;
  margin-bottom: 18px;
}

.filters-row {
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-weight: 600;
  font-size: 0.9rem;
  color: #173a2e;
}

.filter-group select {
  padding: 10px 15px;
  border-radius: 14px;
  border: none;
  background-color: #dcedc8;
  font-weight: 600;
  outline: none;
  max-width: 250px;
}

.empty-state {
  background: white;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  color: #666;
}

.viajes-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.viaje-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.04);
}

.viaje-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2ee;
}

.ruta-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 1.1rem;
  color: #173a2e;
}

.flecha {
  color: #2cb5e8;
  font-size: 1.3rem;
}

.sede-destino {
  color: #2cb5e8;
}

.vehiculo-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
}

.vehiculo-tag.autobús {
  background: #dcedc8;
  color: #33691e;
}

.vehiculo-tag.carro {
  background: #c8e6f6;
  color: #01579b;
}

.viaje-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 20px;
}

.detail-row {
  display: flex;
  gap: 6px;
  font-size: 0.9rem;
}

.detail-row .label {
  color: #666;
  font-weight: 500;
  min-width: 100px;
}

.detail-row .value {
  color: #173a2e;
  font-weight: 600;
}

.detail-row .disponible {
  color: #2e7d32;
}

.detail-row .no-disponible {
  color: #c62828;
}
</style>
