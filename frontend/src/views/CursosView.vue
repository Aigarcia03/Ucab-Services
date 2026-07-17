<template>
  <div class="cursos-wrapper">
    <h2 class="page-title">{{ esProfesor ? 'Mis cursos' : 'Mi historial de cursos' }}</h2>

    <div v-if="cargando" class="empty-state">Cargando cursos...</div>

    <div v-else-if="cursos.length === 0" class="empty-state">No hay cursos registrados.</div>

    <div v-for="(curso, i) in cursos" :key="i" class="curso-card">
      <div class="curso-header">
        <div>
          <strong class="curso-nombre">{{ curso.materia }}</strong>
          <span class="curso-fecha">{{ formatFecha(curso.fechainicio) }} — {{ curso.fechafin ? formatFecha(curso.fechafin) : 'En curso' }}</span>
        </div>
        <div class="curso-estado">
          <span class="tag-periodo" :class="cursoVigente(curso) ? 'vigente' : 'finalizado'">
            {{ cursoVigente(curso) ? 'Vigente' : 'Finalizado' }}
          </span>
          <span v-if="!esProfesor" class="tag-nota">Nota: {{ curso.nota ?? '—' }}</span>
        </div>
      </div>

      <div v-if="esProfesor" class="curso-actions">
        <button @click="toggleEstudiantes(i)" class="btn-ver-est">
          {{ cursoAbierto === i ? 'Ocultar estudiantes' : 'Ver estudiantes' }}
        </button>
      </div>

      <div v-if="esProfesor && cursoAbierto === i" class="estudiantes-list">
        <div v-if="cargandoEst" class="empty-state" style="padding: 15px;">Cargando...</div>
        <div v-else-if="estudiantes.length === 0" class="empty-state" style="padding: 15px;">Sin estudiantes inscritos.</div>
        <div v-for="est in estudiantes" :key="est.ci" class="est-row">
          <span class="est-nombre">{{ est.primerNombre }} {{ est.primerApellido }}</span>
          <div class="est-nota-group">
            <input type="number" step="0.5" min="0" max="20"
                   :value="est.nota"
                   @input="est.notaEdit = $event.target.value"
                   class="input-nota" placeholder="Nota" />
            <button @click="guardarNota(curso, est)" class="btn-guardar-nota" :disabled="notaEditando[est.ci]">
              {{ notaEditando[est.ci] ? 'Guardando...' : 'Guardar' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  userCi: { type: Number, default: null },
  activeRole: { type: String, default: '' }
})

const esProfesor = computed(() => props.activeRole === 'Profesor')

const cursos = ref([])
const cargando = ref(true)
const cursoAbierto = ref(null)
const estudiantes = ref([])
const cargandoEst = ref(false)
const notaEditando = ref({})

const formatFecha = (f) => {
  if (!f) return '—'
  return new Date(f).toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const cursoVigente = (curso) => {
  if (!curso.fechafin) return true
  return new Date(curso.fechafin) >= new Date()
}

const rolParam = computed(() => esProfesor.value ? 'profesor' : 'estudiante')

const cargarCursos = async () => {
  try {
    const res = await fetch(`/api/cursos?ci=${props.userCi}&rol=${rolParam.value}`)
    if (res.ok) cursos.value = await res.json()
  } catch (e) {
    console.error('Error cargando cursos', e)
  } finally {
    cargando.value = false
  }
}

const toggleEstudiantes = async (i) => {
  if (cursoAbierto.value === i) {
    cursoAbierto.value = null
    return
  }
  cursoAbierto.value = i
  const curso = cursos.value[i]
  cargandoEst.value = true
  try {
    const res = await fetch(`/api/cursos/${encodeURIComponent(curso.materia)}/${curso.fechainicio}/estudiantes?ciProfesor=${props.userCi}`)
    if (res.ok) {
      estudiantes.value = await res.json()
      estudiantes.value.forEach(e => { e.notaEdit = e.nota })
    }
  } catch (e) {
    console.error('Error cargando estudiantes', e)
  } finally {
    cargandoEst.value = false
  }
}

const guardarNota = async (curso, est) => {
  const nota = parseFloat(est.notaEdit)
  if (isNaN(nota) || nota < 0 || nota > 20) return

  notaEditando.value[est.ci] = true
  try {
    const res = await fetch(`/api/cursos/${encodeURIComponent(curso.materia)}/${curso.fechainicio}/nota`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ciEstudiante: est.ci, ciProfesor: props.userCi, nota })
    })
    const data = await res.json()
    if (res.ok) {
      est.nota = nota
      est.notaEdit = nota
      alert(data.mensaje)
    } else {
      alert(data.error || 'Error al guardar nota')
    }
  } catch (e) {
    alert('Error de conexión')
  } finally {
    notaEditando.value[est.ci] = false
  }
}

onMounted(cargarCursos)
</script>

<style scoped>
.cursos-wrapper { width: 100%; max-width: 900px; }
.page-title { color: #173a2e; font-size: 28px; margin-bottom: 18px; }
.empty-state { background: white; border-radius: 16px; padding: 40px; text-align: center; color: #666; }
.curso-card {
  background: white; border-radius: 14px; padding: 18px 22px;
  margin-bottom: 14px; box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}
.curso-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #eef2ee;
  flex-wrap: wrap; gap: 8px;
}
.curso-nombre { font-size: 1.1rem; color: #173a2e; display: block; }
.curso-fecha { font-size: 0.85rem; color: #666; }
.curso-estado { display: flex; align-items: center; gap: 12px; }
.tag-periodo { padding: 3px 12px; border-radius: 12px; font-size: 0.8rem; font-weight: 700; }
.tag-periodo.vigente { background: #dcedc8; color: #33691e; }
.tag-periodo.finalizado { background: #fce4ec; color: #c62828; }
.tag-nota { font-weight: 700; color: #173a2e; }
.curso-actions { margin-top: 4px; }
.btn-ver-est {
  background: #2cb5e8; color: white; border: none; border-radius: 12px;
  padding: 7px 18px; font-weight: 700; cursor: pointer; font-size: 0.85rem;
}
.estudiantes-list { margin-top: 12px; border-top: 1px solid #eef2ee; padding-top: 12px; }
.est-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; background: #f7f9f6; border-radius: 10px; margin-bottom: 8px;
  flex-wrap: wrap; gap: 8px;
}
.est-nombre { font-weight: 600; color: #173a2e; }
.est-nota-group { display: flex; gap: 8px; align-items: center; }
.input-nota {
  width: 80px; padding: 6px 10px; border-radius: 10px; border: 1px solid #dcedc8;
  outline: none; font-size: 0.9rem; text-align: center;
}
.btn-guardar-nota {
  background: linear-gradient(135deg, #43a047, #388e3c); color: white;
  border: none; border-radius: 10px; padding: 6px 16px; font-weight: 700; cursor: pointer; font-size: 0.85rem;
}
.btn-guardar-nota:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
