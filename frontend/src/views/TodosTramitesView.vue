<template>
  <div class="todos-tramites-wrapper">
    <h2 class="page-title">Todos los trámites</h2>

    <div v-if="cargando" class="loading-state">Cargando...</div>

    <div v-else-if="tramites.length === 0" class="empty-state">No hay trámites registrados.</div>

    <div v-for="(t, i) in tramites" :key="i" class="tramite-card">
      <div class="tramite-header">
        <div class="tramite-info">
          <strong class="tramite-servicio">{{ t.nombrecategoria }}</strong>
          <span class="tramite-desc">{{ t.descripcion }}</span>
          <span class="tramite-member">— {{ t.primernombre }} {{ t.primerapellido }} (CI: {{ t.ci }})</span>
        </div>
        <span class="status-badge" :class="t.estado">{{ t.estado }}</span>
      </div>
      <div class="tramite-body">
        <span class="tramite-date">Creado: {{ t.fechacreacion }}</span>
        <span v-if="t.fechacierre" class="tramite-date">Cierre: {{ t.fechacierre }}</span>
      </div>
      <div class="tramite-footer">
        <button class="btn-sm" @click="verPasos(t)">Ver progreso</button>
        <button v-if="t.estado === 'activo' && esAdmin" class="btn-sm btn-primary" @click="abrirPago(t)">Registrar pago</button>
      </div>

      <div v-if="pasosVisibles[t.ci + t.fechacreacion]" class="pasos-section">
        <div v-if="cargandoPasos" class="loading-state">Cargando pasos...</div>
        <div v-else-if="pasos.length === 0" class="empty-state">Sin pasos registrados.</div>
        <div v-for="p in pasos" :key="p.ordenSecuencial" class="paso-item">
          <div class="paso-num">Paso {{ p.ordenSecuencial }}</div>
          <div class="paso-info">
            <p>{{ p.descripcionInteraccion }}</p>
            <span v-if="p.responsableAsignado" class="paso-responsable">Responsable: {{ p.responsableAsignado }}</span>
          </div>
          <span class="status-badge" :class="p.estado">{{ p.estado }}</span>
          <button v-if="p.estado !== 'completado' && esAdmin" class="btn-sm btn-complete" @click="completarPaso(p)">✓</button>
        </div>

        <div v-if="esAdmin" class="paso-add-form">
          <input v-model="nuevaDesc" class="input-paso" placeholder="Nuevo paso..." @keyup.enter="agregarPaso" />
          <button class="btn-sm btn-primary" @click="agregarPaso" :disabled="!nuevaDesc.trim()">+ Agregar paso</button>
        </div>
        <p v-if="pasoMsg" class="paso-msg" :class="pasoMsgError ? 'error' : 'exito'">{{ pasoMsg }}</p>
      </div>
    </div>

    <!-- Modal: Registrar pago -->
    <div v-if="showPagoModal" class="modal-overlay" @click.self="showPagoModal = false">
      <div class="modal-card">
        <h3>Registrar Pago — {{ tramitePago?.nombrecategoria }}</h3>
        <p class="modal-hint">{{ tramitePago?.primernombre }} {{ tramitePago?.primerapellido }} (CI: {{ tramitePago?.ci }}) — {{ tramitePago?.descripcion }}</p>

        <label class="form-label">Monto</label>
        <input v-model="pagoForm.monto" type="number" step="0.01" class="admin-input full-width" placeholder="0.00" />

        <label class="form-label">Método de pago</label>
        <select v-model="pagoForm.metodo" class="admin-input full-width">
          <option value="tarjeta">Tarjeta</option>
          <option value="movil">Pago móvil</option>
          <option value="efectivo">Efectivo</option>
          <option value="tai">TAI</option>
        </select>

        <template v-if="pagoForm.metodo === 'tarjeta'">
          <input v-model="pagoForm.nroTarjeta" class="admin-input full-width" placeholder="Nro. de tarjeta" />
          <select v-model="pagoForm.companiaEmisora" class="admin-input full-width">
            <option value="Visa">Visa</option>
            <option value="Mastercard">Mastercard</option>
            <option value="American Express">American Express</option>
            <option value="Cabal">Cabal</option>
            <option value="Maestro">Maestro</option>
          </select>
          <input v-model="pagoForm.monedaLiquidacion" class="admin-input full-width" placeholder="Moneda (USD/EUR/VES)" />
          <select v-model="pagoForm.tipoRed" class="admin-input full-width">
            <option value="nacional">Nacional</option>
            <option value="internacional">Internacional</option>
          </select>
          <input v-model="pagoForm.fechaVencimiento" type="month" class="admin-input full-width" />
        </template>

        <template v-if="pagoForm.metodo === 'movil'">
          <input v-model="pagoForm.nroReferencia" class="admin-input full-width" placeholder="Nro. de referencia" />
          <input v-model="pagoForm.telefonoEmisor" class="admin-input full-width" placeholder="Teléfono del emisor" />
          <select v-model="pagoForm.banco" class="admin-input full-width">
            <option value="Bancamiga">Bancamiga</option>
            <option value="Banesco">Banesco</option>
            <option value="Mercantil">Mercantil</option>
            <option value="Provincial">Provincial</option>
            <option value="Venezuela">Banco de Venezuela</option>
          </select>
        </template>

        <template v-if="pagoForm.metodo === 'efectivo'">
          <select v-model="pagoForm.monedaDeCurso" class="admin-input full-width">
            <option value="bolivares">Bolívares</option>
            <option value="divisas">Divisas</option>
          </select>
        </template>

        <template v-if="pagoForm.metodo === 'tai'">
          <input v-model="pagoForm.pos" type="number" class="admin-input full-width" placeholder="POS" />
          <input v-model="pagoForm.uid" type="number" class="admin-input full-width" placeholder="UID (cédula)" />
        </template>

        <div v-if="pagoMsg" class="paso-msg" :class="pagoMsgError ? 'error' : 'exito'">{{ pagoMsg }}</div>

        <div class="modal-actions">
          <button class="btn-sm btn-primary" @click="registrarPagoTramite" :disabled="pagoProcesando">
            {{ pagoProcesando ? 'Procesando...' : 'Registrar pago' }}
          </button>
          <button class="btn-sm" @click="showPagoModal = false">Cancelar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({ userCi: { type: Number, default: null }, esAdmin: { type: Boolean, default: false } })

const tramites = ref([])
const cargando = ref(true)
const pasos = ref([])
const cargandoPasos = ref(false)
const pasosVisibles = ref({})
const tramiteActivo = ref(null)
const nuevaDesc = ref('')
const pasoMsg = ref('')
const pasoMsgError = ref(false)
const showPagoModal = ref(false)
const tramitePago = ref(null)
const pagoProcesando = ref(false)
const pagoMsg = ref('')
const pagoMsgError = ref(false)
const pagoForm = ref({
  monto: '',
  metodo: 'tarjeta',
  nroTarjeta: '', companiaEmisora: 'Visa', monedaLiquidacion: 'USD', tipoRed: 'nacional', fechaVencimiento: '',
  nroReferencia: '', telefonoEmisor: '', banco: 'Bancamiga',
  monedaDeCurso: 'bolivares',
  pos: '', uid: ''
})

async function cargarTramites() {
  try {
    const res = await fetch(`/api/admin/tramites`)
    if (res.ok) tramites.value = await res.json()
  } catch (e) {
    console.error('Error cargando trámites', e)
  } finally {
    cargando.value = false
  }
}

async function verPasos(t) {
  const key = t.ci + t.fechacreacion
  pasosVisibles.value = { [key]: !pasosVisibles.value[key] }
  if (!pasosVisibles.value[key]) return

  tramiteActivo.value = t
  nuevaDesc.value = ''
  pasoMsg.value = ''
  cargandoPasos.value = true
  try {
    const params = new URLSearchParams({ ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria, descripcion: t.descripcion, fechaCreacion: t.fechacreacion })
    const res = await fetch(`/api/admin/pasos?${params}`)
    if (res.ok) pasos.value = await res.json()
  } catch (e) {
    console.error('Error cargando pasos', e)
    pasos.value = []
  } finally {
    cargandoPasos.value = false
  }
}

async function completarPaso(p) {
  const t = tramiteActivo.value
  if (!t) return
  pasoMsg.value = ''
  try {
    const res = await fetch(`/api/admin/pasos/${p.ordenSecuencial}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria,
        descripcion: t.descripcion, fechaCreacion: t.fechacreacion,
        estado: 'completado', responsable: '', adminCi: props.userCi
      })
    })
    const data = await res.json()
    if (res.ok) {
      pasoMsg.value = data.message
      pasoMsgError.value = false
      verPasos(t)
    } else {
      pasoMsg.value = data.error || 'Error al completar paso'
      pasoMsgError.value = true
    }
  } catch (e) {
    pasoMsg.value = 'Error de conexión'
    pasoMsgError.value = true
  }
}

async function agregarPaso() {
  const t = tramiteActivo.value
  if (!t || !nuevaDesc.value.trim()) return
  pasoMsg.value = ''
  try {
    const res = await fetch(`/api/admin/pasos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria,
        descripcion: t.descripcion, fechaCreacion: t.fechacreacion,
        descripcionInteraccion: nuevaDesc.value.trim(), adminCi: props.userCi
      })
    })
    const data = await res.json()
    if (res.ok) {
      pasoMsg.value = data.message
      pasoMsgError.value = false
      nuevaDesc.value = ''
      verPasos(t)
    } else {
      pasoMsg.value = data.error || 'Error al agregar paso'
      pasoMsgError.value = true
    }
  } catch (e) {
    pasoMsg.value = 'Error de conexión'
    pasoMsgError.value = true
  }
}

onMounted(cargarTramites)

function abrirPago(t) {
  tramitePago.value = t
  pagoForm.value = { monto: '', metodo: 'tarjeta', nroTarjeta: '', companiaEmisora: 'Visa', monedaLiquidacion: 'USD', tipoRed: 'nacional', fechaVencimiento: '', nroReferencia: '', telefonoEmisor: '', banco: 'Bancamiga', monedaDeCurso: 'bolivares', pos: '', uid: '' }
  pagoMsg.value = ''
  showPagoModal.value = true
}

async function registrarPagoTramite() {
  const t = tramitePago.value
  if (!t) return
  pagoProcesando.value = true
  pagoMsg.value = ''
  try {
    const body = { ...pagoForm.value, monto: parseFloat(pagoForm.value.monto) || 0, adminCi: props.userCi }
    const res = await fetch(`/api/admin/tramites/${t.ci}/${t.fechacreacion}/pago`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (res.ok) {
      pagoMsg.value = data.message
      pagoMsgError.value = false
      showPagoModal.value = false
      cargarTramites()
    } else {
      pagoMsg.value = data.error || 'Error al registrar pago'
      pagoMsgError.value = true
    }
  } catch (e) {
    pagoMsg.value = 'Error de conexión'
    pagoMsgError.value = true
  } finally {
    pagoProcesando.value = false
  }
}
</script>

<style scoped>
.todos-tramites-wrapper { width: 100%; }
.page-title { color: #173a2e; font-size: 28px; margin-bottom: 20px; }
.loading-state, .empty-state { background: white; border-radius: 16px; padding: 40px; text-align: center; color: #666; }
.tramite-card { background: white; border-radius: 16px; padding: 20px; margin-bottom: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.tramite-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.tramite-info { display: flex; flex-direction: column; gap: 2px; }
.tramite-servicio { font-size: 1.1rem; color: #173a2e; }
.tramite-desc { font-size: 0.85rem; color: #6b7280; }
.tramite-member { font-size: 0.8rem; color: #9ca3af; }
.tramite-body { display: flex; gap: 20px; font-size: 0.85rem; color: #334155; margin-bottom: 10px; }
.tramite-date { color: #6b7280; }
.tramite-footer { border-top: 1px solid #eef2ee; padding-top: 12px; }
.status-badge { padding: 3px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; display: inline-block; }
.status-badge.activo { background: #dcedc8; color: #33691e; }
.status-badge.finalizado { background: #dbeafe; color: #1e40af; }
.status-badge.completado { background: #dcedc8; color: #33691e; }
.status-badge.en\ curso { background: #fef3c7; color: #92400e; }
.pasos-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid #eef2ee; }
.paso-item { display: flex; gap: 12px; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #f3f4f6; }
.paso-num { font-weight: 700; color: #173a2e; min-width: 60px; font-size: 0.85rem; }
.paso-info { flex: 1; }
.paso-info p { font-size: 0.9rem; color: #334155; margin: 0; }
.paso-responsable { font-size: 0.8rem; color: #9ca3af; }
.btn-sm { padding: 6px 14px; border: none; border-radius: 999px; font-size: 12px; font-weight: 700; cursor: pointer; background: #e5e7eb; color: #374151; }
.btn-primary { background: linear-gradient(180deg, #39aaf2 0%, #2f9de6 100%); color: white; }
.btn-complete { background: #2e7d32; color: white; padding: 4px 10px; font-size: 14px; line-height: 1; }
.paso-add-form { display: flex; gap: 8px; margin-top: 12px; }
.input-paso { flex: 1; padding: 8px 14px; border-radius: 999px; border: 1px solid #dcedc8; outline: none; font-size: 0.85rem; }
.paso-msg { font-size: 0.8rem; font-weight: 600; margin-top: 6px; }
.paso-msg.exito { color: #2e7d32; }
.paso-msg.error { color: #c62828; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: white; border-radius: 24px; padding: 28px; min-width: 400px; max-width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-card h3 { color: #173a2e; margin-bottom: 16px; }
.modal-card .full-width { width: 100%; margin-bottom: 12px; }
.modal-hint { font-size: 0.85rem; color: #6b7280; margin-bottom: 12px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.form-label { font-weight: 600; margin-bottom: 6px; display: block; font-size: 0.85rem; color: #173a2e; }
.admin-input { padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 10px; font-size: 13px; }
</style>
