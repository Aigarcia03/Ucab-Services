<template>
  <div class="catalog-container">
    <div class="filters-header">
      <div class="filter-group">
        <label>Categorias</label>
        <select v-model="selectedCategory" @change="fetchServices">
          <option value="">Todos</option>
          <option value="Estudiantil">Estudiantil</option>
          <option value="Laboratorios">Laboratorios</option>
        </select>
      </div>
      <div class="filter-group filter-tags">
        <label>Etiquetas</label>
        <input type="text" class="tags-input" />
      </div>
    </div>

    <div class="catalog-content">
      <div class="service-card" v-for="(servicio, index) in servicios" :key="index">
        <div class="card-info">
          <h3>{{ getTitulo(servicio) }}</h3>
          <p class="description">{{ getDescripcion(servicio) }}</p>
          <div class="card-meta">
            <p>Costo: {{ servicio.precioBase }} $</p>
          </div>
        </div>
        <div class="card-actions">
          <template v-if="servicio.nombreCategoria === 'Estudiantil'">
            <button class="btn-action btn-pagar" @click="handlePagar(servicio)">
              Pagar
            </button>
            <button class="btn-action btn-cuenta" @click="handleAgregar(servicio)">
              Estado de Cuenta
            </button>
          </template>

          <button
            v-else-if="servicio.nombreCategoria === 'Laboratorios'"
            class="btn-action btn-reservar"
            @click="handleReservar(servicio)"
          >
            Reservar
          </button>

          <button
            v-else
            class="btn-action btn-pagar"
            @click="handlePagar(servicio)"
          >
            Pagar
          </button>
        </div>
      </div>

      <div v-if="servicios.length === 0" class="no-results">
        <p>No se encontraron servicios para esta categoría.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { estadoCuentaStore } from '../services/estadoCuentaStore';

const props = defineProps({ userCi: { type: Number, default: null } });
const emit = defineEmits(['navigate', 'pagar']);

const selectedCategory = ref('');
const servicios = ref([]);

const fetchServices = async () => {
  try {
    const url = selectedCategory.value
      ? `http://localhost:8080/api/servicios?categoria=${selectedCategory.value}`
      : 'http://localhost:8080/api/servicios';
    const response = await fetch(url);
    if (response.ok) {
      servicios.value = await response.json();
    } else {
      console.error('Error al obtener servicios');
    }
  } catch (error) {
    console.error('Error de red al obtener servicios', error);
  }
};

onMounted(() => {
  fetchServices();
});

const getTitulo = (servicio) => {
  if (!servicio.descripcion) return servicio.nombreCategoria;
  const parts = servicio.descripcion.split('.');
  return parts[0].trim();
};

const getDescripcion = (servicio) => {
  if (!servicio.descripcion) return '';
  const parts = servicio.descripcion.split('.');
  if (parts.length > 1) {
    return parts.slice(1).join('.').trim();
  }
  return servicio.descripcion;
};

// Adds the service to Estado de Cuenta (pending bill)
const handleAgregar = async (servicio) => {
  const servicioConTitulo = { ...servicio, titulo: getTitulo(servicio) };
  const ok = await estadoCuentaStore.agregarAlEstadoDeCuenta(servicioConTitulo, props.userCi);
  if (ok) {
    alert(`"${servicioConTitulo.titulo}" agregado a tu Estado de Cuenta.`);
  } else {
    alert('No se pudo guardar el servicio. Asegúrate de que el backend está activo.');
  }
};

// Sends the service to the payment flow (same as reservation flow)
const handlePagar = (servicio) => {
  emit('pagar', {
    descripcion: getTitulo(servicio),
    monto: servicio.precioBase,
    detalles: { categoria: servicio.nombreCategoria }
  });
};

// Sends to the reservations view with the service data
const handleReservar = (servicio) => {
  emit('navigate', { view: 'reservaciones', servicio });
};
</script>

<style scoped>
.catalog-container {
  width: 100%;
  max-width: 1200px;
  background-color: #dcedc8;
  border-radius: 20px;
  padding: 20px;
  min-height: 500px;
}

.filters-header {
  background-color: #c5e1a5;
  border-radius: 12px;
  padding: 15px 25px;
  display: flex;
  gap: 30px;
  margin-bottom: 25px;
}

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-group label {
  font-size: 14px;
  font-weight: 600;
  color: #173a2e;
  margin-bottom: 5px;
}

.filter-group select, .filter-group input {
  padding: 8px 15px;
  border-radius: 20px;
  border: none;
  background-color: #a5d6a7;
  color: #173a2e;
  font-weight: bold;
  outline: none;
}
.filter-group select {
  min-width: 150px;
  appearance: none;
}
.tags-input {
  min-width: 250px;
  background-color: #ffffff !important;
}

.catalog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.service-card {
  background-color: #ffffff;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.card-info {
  flex: 1;
}

.card-info h3 {
  color: #000;
  font-size: 20px;
  margin-bottom: 10px;
  margin-top: 0;
}

.description {
  color: #444;
  margin-bottom: 12px;
  line-height: 1.5;
}

.card-meta p {
  color: #666;
  margin: 5px 0;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;
}

.btn-action {
  border: none;
  border-radius: 20px;
  padding: 9px 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.btn-action:hover {
  transform: translateY(-1px);
}

.btn-pagar {
  background: linear-gradient(135deg, #39aaf2, #2f9de6);
  color: white;
  box-shadow: 0 4px 12px rgba(41,146,213,0.3);
}

.btn-cuenta {
  background: linear-gradient(135deg, #66bb6a, #43a047);
  color: white;
  box-shadow: 0 4px 12px rgba(67,160,71,0.3);
}

.btn-reservar {
  background: linear-gradient(135deg, #39aaf2, #2f9de6);
  color: white;
  box-shadow: 0 4px 12px rgba(41,146,213,0.3);
}

.no-results {
  text-align: center;
  color: #666;
  padding: 40px;
}
</style>
