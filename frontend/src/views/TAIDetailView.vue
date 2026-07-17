<template>
  <div class="tai-wrapper">
    <h2 class="page-title">Mi TAI</h2>

    <div v-if="cargando" class="loading-state">Cargando...</div>

    <template v-else>
      <div class="tai-info-card">
        <div class="tai-header">
          <span class="tai-label">POS</span>
          <span class="tai-valor">{{ taiInfo.pos ?? '—' }}</span>
        </div>
        <div class="tai-header">
          <span class="tai-label">UID</span>
          <span class="tai-valor">{{ taiInfo.uid ?? '—' }}</span>
        </div>
        <div class="tai-header">
          <span class="tai-label">Estado</span>
          <span class="tai-estado" :class="taiInfo.estado || 'activo'">{{ taiInfo.estado || 'activo' }}</span>
        </div>
      </div>

      <div v-if="taiInfo.estado === 'bloqueado'" class="bloqueado-banner">
        Esta tarjeta TAI está bloqueada. No se pueden realizar abonos. Contacta al administrador.
      </div>

      <div class="abono-section">
        <h3>Abono presente</h3>
        <div class="abono-form">
          <input
            type="number"
            v-model.number="montoAbono"
            placeholder="Monto (mín 1 USD)"
            min="1"
            class="abono-input"
            :disabled="taiInfo.estado === 'bloqueado'"
          />
          <button @click="hacerAbono" :disabled="!montoAbono || montoAbono < 1 || abonando || taiInfo.estado === 'bloqueado'" class="btn-abono">
            {{ abonando ? 'Procesando...' : 'Abonar' }}
          </button>
        </div>
        <p v-if="abonoMsg" class="abono-msg" :class="abonoError ? 'error' : 'exito'">{{ abonoMsg }}</p>
      </div>

      <h3 class="facturas-title">Pagos y facturas asociadas</h3>

      <div v-if="registros.length === 0" class="empty-state">
        No hay movimientos registrados en este TAI.
      </div>

      <div class="movimientos-list">
        <div v-for="(r, i) in registros" :key="i" class="movimiento-card">
          <div class="mov-header">
            <span class="mov-fecha">{{ formatFecha(r.fechahorapago) }}</span>
            <span class="mov-monto">${{ r.montorecibido ?? '—' }}</span>
          </div>
          <div class="mov-body">
            <div class="mov-row">
              <span class="label">Factura N°:</span>
              <span class="value">{{ r.facturanumero ?? '—' }}</span>
            </div>
            <div class="mov-row">
              <span class="label">Estatus:</span>
              <span class="value" :class="r.estatus">{{ r.estatus ?? '—' }}</span>
            </div>
            <div class="mov-row">
              <span class="label">Deuda:</span>
              <span class="value">${{ r.deuda ?? '0' }}</span>
            </div>
            <div class="mov-row">
              <span class="label">Monto acumulado:</span>
              <span class="value">${{ r.montoacumulado ?? '0' }}</span>
            </div>
            <div class="mov-row">
              <span class="label">Emisión:</span>
              <span class="value">{{ r.fechahoraemision ? formatFecha(r.fechahoraemision) : '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const props = defineProps({ userCi: { type: Number, default: null } });

const taiInfo = ref({});
const registros = ref([]);
const cargando = ref(true);
const montoAbono = ref(0);
const abonando = ref(false);
const abonoMsg = ref('');
const abonoError = ref(false);

const fetchTAI = async () => {
  if (!props.userCi) {
    cargando.value = false;
    return;
  }
  try {
    const res = await fetch(`http://localhost:8080/api/tai?ci=${props.userCi}`);
    if (res.ok) {
      const data = await res.json();
      if (data.tai) taiInfo.value = data.tai;
      registros.value = data.registros || [];
    }
  } catch (e) {
    console.error('Error cargando TAI', e);
  } finally {
    cargando.value = false;
  }
};

const hacerAbono = async () => {
  if (!montoAbono.value || montoAbono.value <= 0) return;
  abonando.value = true;
  abonoMsg.value = '';
  abonoError.value = false;
  try {
    const res = await fetch('http://localhost:8080/api/tai/abono', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: props.userCi,
        pos: taiInfo.value.pos || 1,
        monto: montoAbono.value
      })
    });
    const data = await res.json();
    if (res.ok) {
      abonoMsg.value = 'Abono registrado exitosamente.';
      montoAbono.value = 0;
      await fetchTAI();
    } else {
      abonoMsg.value = data.error || 'Error al registrar abono.';
      abonoError.value = true;
    }
  } catch (e) {
    abonoMsg.value = 'Error de conexión con el servidor.';
    abonoError.value = true;
  } finally {
    abonando.value = false;
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

onMounted(fetchTAI);
</script>

<style scoped>
.tai-wrapper {
  width: 100%;
  max-width: 700px;
}

.page-title {
  color: #173a2e;
  font-size: 28px;
  margin-bottom: 18px;
}

.loading-state, .empty-state {
  background: white;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  color: #666;
}

.tai-info-card {
  background: linear-gradient(135deg, #dcedc8, #c5e1a5);
  border-radius: 16px;
  padding: 20px 28px;
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.04);
}

.tai-header {
  display: flex;
  flex-direction: column;
}

.tai-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #558b2f;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.tai-valor {
  font-size: 1.6rem;
  font-weight: 800;
  color: #173a2e;
}

.abono-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.04);
}

.abono-section h3 {
  margin: 0 0 12px;
  color: #173a2e;
  font-size: 1.1rem;
}

.abono-form {
  display: flex;
  gap: 10px;
  align-items: center;
}

.abono-input {
  padding: 10px 16px;
  border-radius: 14px;
  border: 1px solid #dcedc8;
  outline: none;
  font-size: 1rem;
  max-width: 150px;
}

.btn-abono {
  background: linear-gradient(135deg, #43a047, #388e3c);
  color: white;
  border: none;
  border-radius: 14px;
  padding: 10px 24px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s;
}

.btn-abono:hover {
  transform: translateY(-1px);
}

.btn-abono:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.abono-msg {
  margin-top: 10px;
  font-size: 0.9rem;
  font-weight: 600;
}

.abono-msg.exito {
  color: #2e7d32;
}

.abono-msg.error {
  color: #c62828;
}

.facturas-title {
  color: #173a2e;
  font-size: 1.1rem;
  margin-bottom: 12px;
}

.movimientos-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.movimiento-card {
  background: white;
  border-radius: 14px;
  padding: 16px 20px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.mov-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #eef2ee;
}

.mov-fecha {
  font-weight: 600;
  color: #173a2e;
  font-size: 0.9rem;
}

.mov-monto {
  font-weight: 800;
  color: #2e7d32;
  font-size: 1.1rem;
}

.mov-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 20px;
}

.mov-row {
  display: flex;
  gap: 6px;
  font-size: 0.85rem;
}

.mov-row .label {
  color: #666;
  font-weight: 500;
  min-width: 90px;
}

.mov-row .value {
  color: #173a2e;
  font-weight: 600;
}

.mov-row .pagada {
  color: #2e7d32;
}

.mov-row .pendiente {
  color: #e65100;
}

.tai-estado {
  font-size: 1.1rem;
  font-weight: 800;
  padding: 4px 16px;
  border-radius: 999px;
  text-transform: uppercase;
}
.tai-estado.activo {
  background: #dcedc8;
  color: #33691e;
}
.tai-estado.bloqueado {
  background: #fce4ec;
  color: #c62828;
}

.bloqueado-banner {
  background: #fef2f2;
  color: #991b1b;
  padding: 14px 20px;
  border-radius: 14px;
  margin-bottom: 20px;
  font-weight: 600;
  font-size: 0.95rem;
  border: 1px solid #fecaca;
}
</style>
