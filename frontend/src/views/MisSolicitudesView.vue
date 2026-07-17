<template>
  <div class="solicitudes-wrapper">
    <h2 class="page-title">Mis solicitudes</h2>

    <div v-if="cargando" class="loading-state">Cargando...</div>

    <div v-else-if="solicitudes.length === 0" class="empty-state">
      No tienes solicitudes registradas.
    </div>

    <div v-for="(s, i) in solicitudes" :key="i" class="solicitud-card">
      <div class="sol-header">
        <div>
          <strong class="sol-servicio">{{ s.nombrecategoria }}</strong>
          <span class="sol-desc">{{ s.descripcion }}</span>
        </div>
        <span class="status-badge" :class="s.estado">{{ s.estado }}</span>
      </div>
      <div class="sol-body">
        <div class="sol-row"><span class="label">Creado</span><span>{{ formatFecha(s.fechacreacion) }}</span></div>
        <div class="sol-row"><span class="label">Cierre</span><span>{{ s.fechacierre ? formatFecha(s.fechacierre) : '—' }}</span></div>
      </div>
      <div class="sol-footer">
        <button class="btn-sm" @click="verPasos(s)">Ver progreso</button>
      </div>

      <div v-if="pasosVisibles[s.ci + s.fechacreacion]" class="pasos-section">
        <div v-if="cargandoPasos" class="loading-state">Cargando pasos...</div>
        <div v-else-if="pasos.length === 0" class="empty-state">Sin pasos registrados.</div>
        <div v-for="p in pasos" :key="p.ordenSecuencial" class="paso-item">
          <div class="paso-num">Paso {{ p.ordenSecuencial }}</div>
          <div class="paso-info">
            <p>{{ p.descripcionInteraccion }}</p>
            <span class="paso-responsable" v-if="p.responsableAsignado">Responsable: {{ p.responsableAsignado }}</span>
          </div>
          <span class="status-badge" :class="p.estado">{{ p.estado }}</span>
          <button v-if="esAdmin && p.estado !== 'completado'" class="btn-sm btn-complete" @click="completarPaso(p)">✓</button>
        </div>

        <div v-if="esAdmin" class="paso-add-form">
          <input v-model="nuevaDesc" class="input-paso" placeholder="Describa el siguiente paso..." @keyup.enter="agregarPaso" />
          <button class="btn-sm btn-primary" @click="agregarPaso" :disabled="!nuevaDesc.trim()">+ Agregar paso</button>
        </div>
        <p v-if="pasoMsg" class="paso-msg" :class="pasoMsgError ? 'error' : 'exito'">{{ pasoMsg }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({ userCi: { type: Number, default: null }, activeRole: { type: String, default: '' } })
const esAdmin = computed(() => props.activeRole === 'Personal Administrativo')

const solicitudes = ref([])
const cargando = ref(true)
const pasos = ref([])
const cargandoPasos = ref(false)
const pasosVisibles = ref({})
const tramiteActivo = ref(null)
const nuevaDesc = ref('')
const pasoMsg = ref('')
const pasoMsgError = ref(false)

const formatFecha = (f) => {
  if (!f) return '—'
  return new Date(f).toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

async function cargarSolicitudes() {
  if (!props.userCi) { cargando.value = false; return }
  try {
    const res = await fetch(`/api/tramites?ci=${props.userCi}`)
    if (res.ok) solicitudes.value = await res.json()
  } catch (e) {
    console.error('Error cargando solicitudes', e)
  } finally {
    cargando.value = false
  }
}

async function verPasos(s) {
  const key = s.ci + s.fechacreacion
  pasosVisibles.value = { [key]: !pasosVisibles.value[key] }

  if (!pasosVisibles.value[key]) return

  tramiteActivo.value = s
  nuevaDesc.value = ''
  pasoMsg.value = ''

  cargandoPasos.value = true
  try {
    const params = new URLSearchParams({ ci: s.ci, idPrestadora: s.idprestadora, nombreCategoria: s.nombrecategoria, descripcion: s.descripcion, fechaCreacion: s.fechacreacion })
    const res = await fetch(`/api/admin/pasos?${params}`)
    if (res.ok) pasos.value = await res.json()
  } catch (e) {
    console.error('Error cargando pasos', e)
    pasos.value = []
  } finally {
    cargandoPasos.value = false
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
        adminCi: props.userCi,
        ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria,
        descripcion: t.descripcion, fechaCreacion: t.fechacreacion,
        descripcionInteraccion: nuevaDesc.value.trim()
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

async function completarPaso(p) {
  const t = tramiteActivo.value
  if (!t) return
  pasoMsg.value = ''
  try {
    const res = await fetch(`/api/admin/pasos/${p.ordenSecuencial}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        adminCi: props.userCi,
        ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria,
        descripcion: t.descripcion, fechaCreacion: t.fechacreacion,
        estado: 'completado',
        responsable: ''
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

onMounted(cargarSolicitudes)
</script>

<style scoped>
.solicitudes-wrapper { width: 100%; }
.page-title { color: #173a2e; font-size: 28px; margin-bottom: 20px; }
.loading-state, .empty-state { background: white; border-radius: 16px; padding: 40px; text-align: center; color: #666; }
.solicitud-card { background: white; border-radius: 16px; padding: 20px; margin-bottom: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.sol-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.sol-servicio { font-size: 1.1rem; color: #173a2e; display: block; }
.sol-desc { font-size: 0.85rem; color: #6b7280; }
.sol-body { display: flex; gap: 30px; font-size: 0.9rem; color: #334155; margin-bottom: 12px; }
.sol-row { display: flex; gap: 6px; }
.sol-row .label { font-weight: 600; color: #173a2e; }
.sol-footer { border-top: 1px solid #eef2ee; padding-top: 12px; }
.status-badge { padding: 3px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; display: inline-block; }
.status-badge.activo { background: #dcedc8; color: #33691e; }
.status-badge.finalizado { background: #dbeafe; color: #1e40af; }
.pasos-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid #eef2ee; }
.paso-item { display: flex; gap: 12px; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #f3f4f6; }
.paso-num { font-weight: 700; color: #173a2e; min-width: 60px; font-size: 0.85rem; }
.paso-info { flex: 1; }
.paso-info p { font-size: 0.9rem; color: #334155; margin: 0; }
.paso-responsable { font-size: 0.8rem; color: #9ca3af; }
.btn-sm { padding: 6px 14px; border: none; border-radius: 999px; font-size: 12px; font-weight: 700; cursor: pointer; background: #e5e7eb; color: #374151; }
.btn-primary { background: #173a2e; color: white; }
.btn-complete { background: #2e7d32; color: white; padding: 4px 10px; font-size: 14px; line-height: 1; }
.paso-add-form { display: flex; gap: 8px; margin-top: 12px; }
.input-paso { flex: 1; padding: 8px 14px; border-radius: 999px; border: 1px solid #dcedc8; outline: none; font-size: 0.85rem; }
.paso-msg { font-size: 0.8rem; font-weight: 600; margin-top: 6px; }
.paso-msg.exito { color: #2e7d32; }
.paso-msg.error { color: #c62828; }
</style>
