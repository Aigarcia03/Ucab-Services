<script setup>
import { ref, watch, onMounted } from 'vue';

const props = defineProps({
  servicioReserva: {
    type: Object,
    default: null
  }
});

const recursosDisponibles = ref([
  { value: 'Mesa de tenis #2', label: 'Mesa de tenis #2' },
  { value: 'Lab. Fabricación Digital', label: 'Lab. Fabricación Digital' },
  { value: 'Cancha de Baloncesto', label: 'Cancha de Baloncesto' }
]);

const getTitulo = (servicio) => {
  if (!servicio.descripcion) return servicio.nombreCategoria;
  const parts = servicio.descripcion.split('.');
  return parts[0].trim();
};

// 1. Base de datos simulada de Beneficiarios
const misBeneficiariosRegistrados = ref([
  { id: 1, nombres: 'Santiago Eduardo', apellidos: 'Mendoza Silva', cedula: 'V-32.987.654' },
  { id: 2, nombres: 'Ana Valeria', apellidos: 'González Pérez', cedula: 'V-28.111.222' }
]);

// 2. Estados del formulario alineados a tu diseño
const categoriaEspacio = ref('Área deportiva');
const recursoEspecifico = ref(recursosDisponibles.value[0].value);
const llevaEquipamiento = ref(false);
const llevaAcompanante = ref(false);
const fechaHora = ref('17/06/26 10:00 am- 12:00pm');
const beneficiarioSeleccionado = ref(''); 

// Campos del acompañante (Lado derecho de la pantalla)
const acompanante = ref({
  nombres: '',
  apellidos: '',
  cedula: ''
});

// 3. WATCHER: Auto-completado reactivo al seleccionar un beneficiario
watch(beneficiarioSeleccionado, (nuevoId) => {
  if (nuevoId) {
    const persona = misBeneficiariosRegistrados.value.find(b => b.id === Number(nuevoId));
    if (persona) {
      acompanante.value.nombres = persona.nombres;
      acompanante.value.apellidos = persona.apellidos;
      acompanante.value.cedula = persona.cedula;
    }
  } else {
    acompanante.value = { nombres: '', apellidos: '', cedula: '' };
  }
});

// 4. Auto-fill desde el servicio seleccionado en el catálogo
onMounted(() => {
  if (props.servicioReserva) {
    const s = props.servicioReserva;
    if (s.nombreCategoria === 'Laboratorios' || s.nombreCategoria === 'Área deportiva' || s.nombreCategoria === 'Auditorios') {
      categoriaEspacio.value = s.nombreCategoria;
    }
    const titulo = getTitulo(s);
    const match = recursosDisponibles.value.find(r =>
      titulo.toLowerCase().includes(r.value.toLowerCase()) ||
      r.value.toLowerCase().includes(titulo.toLowerCase())
    );
    if (match) {
      recursoEspecifico.value = match.value;
    } else {
      recursosDisponibles.value.push({ value: titulo, label: titulo });
      recursoEspecifico.value = titulo;
    }
  }
});

// 5. EMIT: Envío de datos al componente Padre (MainView)
const emit = defineEmits(['solicitarPagoProcesado']); 

const procesarReserva = () => {
  const datosParaPago = {
    descripcion: `Reserva de Espacio: ${recursoEspecifico.value} (${categoriaEspacio.value})`,
    monto: 12.50, // Costo simulado del espacio
    fecha: fechaHora.value,
    detalles: {
      equipamiento: llevaEquipamiento.value ? 'Sí' : 'No',
      acompanante: llevaAcompanante.value ? `${acompanante.value.nombres} ${acompanante.value.apellidos}` : 'Ninguno'
    }
  };

  // Emitimos el evento hacia MainView
  emit('solicitarPagoProcesado', datosParaPago);
};
</script>

<template>
  <div class="reserva-blueprint-container">
    <h2 class="form-title">Nueva reserva</h2>
    
    <div class="reserva-columns-layout">
      <div class="left-form-column">
        <div class="form-group">
          <label>Categoria de espacio</label>
          <select v-model="categoriaEspacio" class="custom-select">
            <option value="Área deportiva">Área deportiva</option>
            <option value="Laboratorios">Laboratorios</option>
            <option value="Auditorios">Auditorios</option>
          </select>
        </div>

        <div class="form-group">
          <label>Recurso específico</label>
          <select v-model="recursoEspecifico" class="custom-select">
            <option v-for="r in recursosDisponibles" :key="r.value" :value="r.value">{{ r.label }}</option>
          </select>
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

        <div v-if="llevaAcompanante" class="form-group internal-selector animate-fade">
          <label class="sub-label">¿Es un beneficiario registrado?</label>
          <select v-model="beneficiarioSeleccionado" class="custom-select-sub">
            <option value="">-- Escribir manualmente --</option>
            <option v-for="b in misBeneficiariosRegistrados" :key="b.id" :value="b.id">
              {{ b.nombres }} {{ b.apellidos }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>Fecha y hora</label>
          <select v-model="fechaHora" class="custom-select">
            <option value="17/06/26 10:00 am- 12:00pm">17/06/26 10:00 am- 12:00pm</option>
            <option value="18/06/26 02:00 pm- 04:00pm">18/06/26 02:00 pm- 04:00pm</option>
          </select>
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
          <label>Cedula</label>
          <input type="text" v-model="acompanante.cedula" :disabled="!llevaAcompanante" class="custom-input" placeholder="Ej. V-00000000" />
        </div>
      </div>
    </div>

    <div class="action-container-bottom">
      <button @click="procesarReserva" class="btn-guardar-reserva">
        Guardar reserva
      </button>
    </div>
  </div>
</template>

<style scoped>
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

.form-title {
  font-size: 2.2rem;
  font-weight: bold;
  margin-bottom: 25px;
  color: #1e2b38;
}

.reserva-columns-layout {
  display: flex;
  gap: 50px;
}

.left-form-column, .right-form-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dimmed-column {
  opacity: 0.4;
  pointer-events: none;
  transition: opacity 0.3s;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-weight: 600;
  font-size: 0.95rem;
}

.form-group-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 5px 0;
}

.label-text {
  font-weight: 600;
  font-size: 0.95rem;
}

/* Inputs y Selects Estilo Pastilla Neumórfica de tus maquetas */
.custom-select, .custom-input, .custom-select-sub {
  background-color: #e3edf7;
  border: none;
  padding: 12px 20px;
  border-radius: 20px;
  font-size: 1rem;
  color: #333;
  box-shadow: inset 2px 2px 5px rgba(0,0,0,0.05), 2px 2px 5px rgba(255,255,255,0.7);
  outline: none;
}

.custom-select-sub {
  background-color: #f0f5fa;
  border: 1px dashed #a4c6e2;
}

/* Botones selectores Check/X de tu UI */
.toggle-buttons {
  display: flex;
  gap: 10px;
}

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

/* Botón Guardar con Sombra Turquesa Limpia */
.action-container-right {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
}

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

.btn-guardar-reserva:hover {
  background-color: #1ba3d4;
  transform: translateY(-2px);
}

.animate-fade {
  animation: fadeIn 0.3s ease-in;
}

.action-container-bottom {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>