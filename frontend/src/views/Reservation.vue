<script setup>
import { ref, computed, watch, onMounted } from 'vue';

const props = defineProps({
  servicioReserva: { type: Object, default: null },
  userCi: { type: Number, default: null }
});

const espacios = ref([]);
const bloques = ref([]);
const cargando = ref(true);

const recursoEspecifico = ref('');
const llevaEquipamiento = ref(false);
const llevaAcompanante = ref(false);
const bloqueSeleccionado = ref('');
const fechaHoraDisplay = ref('');

const acompanante = ref({ nombres: '', apellidos: '', cedula: '' });

const emit = defineEmits(['solicitarPagoProcesado']);

const getTitulo = (servicio) => {
  if (!servicio.descripcion) return servicio.nombreCategoria;
  const parts = servicio.descripcion.split('.');
  return parts[0].trim();
};

const espaciosFiltrados = computed(() => {
  return espacios.value;
});

const bloquesDisponibles = computed(() => {
  if (!recursoEspecifico.value) return [];
  const [nro, dir, nom] = recursoEspecifico.value.split('|');
  return bloques.value.filter(b =>
    String(b.nroidentificador) === nro &&
    b.direccion === dir &&
    b.nombre === nom
  );
});

watch(recursoEspecifico, () => { bloqueSeleccionado.value = ''; });
watch(bloqueSeleccionado, (val) => {
  if (val) {
    const b = bloquesDisponibles.value.find(bl => bl.fechahorainicio === val);
    if (b) {
      fechaHoraDisplay.value = formatFecha(b.fechahorainicio) + ' — ' + formatFecha(b.fechahorafin);
    }
  }
});

const formatFecha = (f) => {
  if (!f) return '—';
  return new Date(f).toLocaleString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
};

async function cargarDatos() {
  try {
    const [r1, r2] = await Promise.all([
      fetch('/api/reservas/espacios'),
      fetch('/api/reservas/bloques')
    ]);
    if (r1.ok) espacios.value = await r1.json();
    if (r2.ok) bloques.value = await r2.json();
  } catch (e) {
    console.error('Error cargando datos de reserva', e);
  } finally {
    cargando.value = false;
  }
}

onMounted(() => {
  cargarDatos();
  // Don't pre-filter spaces by servicio category; user picks from all available
});

const procesarReserva = async () => {
  const bloque = bloquesDisponibles.value.find(b => b.fechahorainicio === bloqueSeleccionado.value);
  if (!bloque) return;

  try {
    const res = await fetch('/api/reservas/solicitar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: props.userCi,
        nroIdentificador: bloque.nroidentificador,
        direccion: bloque.direccion,
        nombre: bloque.nombre,
        fechaHoraInicio: bloque.fechahorainicio,
        nombreCategoria: 'Reservas',
        descripcion: 'Reservación de espacio',
        idPrestadora: 1,
        llevaAcompanante: llevaAcompanante.value,
        acompananteNombres: acompanante.value.nombres,
        acompananteApellidos: acompanante.value.apellidos,
        acompananteCedula: acompanante.value.cedula,
        llevaEquipamiento: llevaEquipamiento.value
      })
    });

    if (!res.ok) {
      const err = await res.json();
      alert(err.error || 'Error al crear la reserva');
      return;
    }

    const data = await res.json();
    alert('Reserva creada correctamente');
    await cargarDatos();

    const datosParaPago = {
      descripcion: `Reserva: ${recursoEspecifico.value}`,
      monto: 12.50,
      fecha: fechaHoraDisplay.value,
      detalles: {
        espacio: recursoEspecifico.value,
        equipamiento: llevaEquipamiento.value ? 'Sí' : 'No',
        acompanante: llevaAcompanante.value ? `${acompanante.value.nombres} ${acompanante.value.apellidos}` : 'Ninguno'
      },
      ci: props.userCi,
      idPrestadora: data.idPrestadora,
      nombreCategoria: data.nombreCategoria,
      descripcionTramite: data.descripcion,
      fechaCreacion: data.fechaCreacion
    };
    emit('solicitarPagoProcesado', datosParaPago);
  } catch (e) {
    alert('Error de conexión al crear la reserva');
  }
};
</script>

<template>
  <div v-if="cargando" class="loading-state">Cargando espacios disponibles...</div>

  <div v-else class="reserva-blueprint-container">
    <h2 class="form-title">Nueva reserva</h2>

    <div class="reserva-columns-layout">
      <div class="left-form-column">
        <div class="form-group">
          <label>Espacio específico</label>
          <select v-model="recursoEspecifico" class="custom-select">
            <option value="">Seleccionar...</option>
            <option v-for="e in espaciosFiltrados" :key="e.nroidentificador + e.direccion + e.nombre"
              :value="e.nroidentificador + '|' + e.direccion + '|' + e.nombre">
              {{ e.nroidentificador }} — {{ e.edificacionnombre }} (Cap. {{ e.capacidadmaxima }})
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>Bloque horario disponible</label>
          <select v-model="bloqueSeleccionado" class="custom-select">
            <option value="">Seleccionar...</option>
            <option v-for="b in bloquesDisponibles" :key="b.fechahorainicio" :value="b.fechahorainicio">
              {{ formatFecha(b.fechahorainicio) }} — {{ formatFecha(b.fechahorafin) }}
            </option>
          </select>
          <p v-if="fechaHoraDisplay" class="selected-time">{{ fechaHoraDisplay }}</p>
        </div>

        <div class="form-group-row">
          <span class="label-text">¿Lleva equipamiento propio?</span>
          <div class="toggle-buttons">
            <button type="button" :class="{ active: llevaEquipamiento }" @click="llevaEquipamiento = true" class="btn-toggle check">✓</button>
            <button type="button" :class="{ active: !llevaEquipamiento }" @click="llevaEquipamiento = false" class="btn-toggle cross">✕</button>
          </div>
        </div>

        <div class="form-group-row">
          <span class="label-text">¿Lleva un acompañante?</span>
          <div class="toggle-buttons">
            <button type="button" :class="{ active: llevaAcompanante }" @click="llevaAcompanante = true" class="btn-toggle check">✓</button>
            <button type="button" :class="{ active: !llevaAcompanante }" @click="llevaAcompanante = false" class="btn-toggle cross">✕</button>
          </div>
        </div>
      </div>

      <div class="right-form-column" :class="{ 'dimmed-column': !llevaAcompanante }">
        <div class="form-group">
          <label>Nombres:</label>
          <input type="text" v-model="acompanante.nombres" :disabled="!llevaAcompanante" class="custom-input" placeholder="Ej. Santiago" />
        </div>
        <div class="form-group">
          <label>Apellidos:</label>
          <input type="text" v-model="acompanante.apellidos" :disabled="!llevaAcompanante" class="custom-input" placeholder="Ej. Mendoza" />
        </div>
        <div class="form-group">
          <label>Cédula</label>
          <input type="text" v-model="acompanante.cedula" :disabled="!llevaAcompanante" class="custom-input" placeholder="Ej. V-00000000" />
        </div>
      </div>
    </div>

    <div class="action-container-bottom">
      <button @click="procesarReserva" class="btn-guardar-reserva" :disabled="!recursoEspecifico || !bloqueSeleccionado">
        Guardar reserva
      </button>
    </div>
  </div>
</template>

<style scoped>
.loading-state { background: white; border-radius: 16px; padding: 40px; text-align: center; color: #666; }
.reserva-blueprint-container {
  background-color: #a4c6e2;
  border-radius: 35px;
  padding: 40px;
  color: #2c3e50;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  width: 100%;
  max-width: 950px;
  margin: 0 auto;
}
.form-title { font-size: 2.2rem; font-weight: bold; margin-bottom: 25px; color: #1e2b38; }
.reserva-columns-layout { display: flex; gap: 50px; }
.left-form-column, .right-form-column { flex: 1; display: flex; flex-direction: column; gap: 18px; }
.dimmed-column { opacity: 0.4; pointer-events: none; transition: opacity 0.3s; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-weight: 600; font-size: 0.95rem; }
.form-group-row { display: flex; justify-content: space-between; align-items: center; margin: 5px 0; }
.label-text { font-weight: 600; font-size: 0.95rem; }
.selected-time { font-size: 0.85rem; color: #1e2b38; font-weight: 600; margin-top: 4px; }
.custom-select, .custom-input {
  background-color: #e3edf7;
  border: none;
  padding: 12px 20px;
  border-radius: 20px;
  font-size: 1rem;
  color: #333;
  box-shadow: inset 2px 2px 5px rgba(0,0,0,0.05), 2px 2px 5px rgba(255,255,255,0.7);
  outline: none;
}
.toggle-buttons { display: flex; gap: 10px; }
.btn-toggle {
  border: none;
  width: 50px;
  height: 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 2px 2px 5px rgba(0,0,0,0.1);
  transition: all 0.2s;
}
.btn-toggle.check { background-color: #e2e8f0; color: #4a5568; }
.btn-toggle.cross { background-color: #e2e8f0; color: #4a5568; }
.btn-toggle.check.active { background-color: #48bb78; color: white; }
.btn-toggle.cross.active { background-color: #f56565; color: white; }
.btn-guardar-reserva {
  background-color: #2cb5e8;
  color: white;
  border: none;
  padding: 14px 35px;
  border-radius: 25px;
  font-weight: bold;
  font-size: 1.1rem;
  cursor: pointer;
  box-shadow: 0 5px 15px rgba(44, 181, 232, 0.4);
  transition: transform 0.2s, background-color 0.2s;
}
.btn-guardar-reserva:hover { background-color: #1ba3d4; transform: translateY(-2px); }
.btn-guardar-reserva:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.action-container-bottom { margin-top: 30px; display: flex; justify-content: flex-end; }
</style>
